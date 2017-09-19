/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dsmatveev.tools.bigtable;

import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableScanConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.Read;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.GroupByKey;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.KV;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * This pipeline needs to be configured with four command line options for bigtable:
 * </p>
 * <ul>
 *  <li> --bigtableProjectId=[bigtable project]
 *  <li> --bigtableInstanceId=[bigtable instance id]
 *  <li> --bigtableTableId=[bigtable tableName]
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the four
 * Bigtable parameters from your favorite development environment.
 */
public class StepsCount {

  /**
   * Options needed for running the pipelne.  It needs a
   *
   */
  public static interface CountOptions extends CloudBigtableOptions {
    void setResultLocation(String resultLocation);
    String getResultLocation();
  }

  static DoFn<KV<String, Iterable<String>>, String>stringifier = new DoFn<KV<String, Iterable<String>>, String>() {
    private static final long serialVersionUID = 1L;

    @Override
    public void processElement(DoFn<KV<String, Iterable<String>>, String>.ProcessContext context) throws Exception {
      context.output(context.element().getKey() + ": " + context.element().getValue().iterator().next());
    }
  };

  static DoFn<Result, KV<String, String>> extractor = new DoFn<Result, KV<String, String>>() {
    private static final long serialVersionUID = 1L;

    @Override
    public void processElement(DoFn<Result, KV<String, String>>.ProcessContext context) throws Exception {
      String rowKey = Bytes.toString(context.element().getRow());
      String[] ids = rowKey.split("#");
      context.output(KV.of(new StringBuilder(ids[0]).reverse().toString(), ids[1]));
    }
  };

  public static void main(String[] args) {
    CountOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CountOptions.class);

    Scan scan = new Scan();
    scan.setCacheBlocks(false);
    scan.setFilter(new FirstKeyOnlyFilter());

    // CloudBigtableTableConfiguration contains the project, zone, cluster and table to connect to.
    // You can supply an optional Scan() to filter the rows that will be read.
    CloudBigtableScanConfiguration config =
        CloudBigtableScanConfiguration.fromCBTOptions(options, scan);

    Pipeline p = Pipeline.create(options);

    p
       .apply("Read from BigTable", Read.from(CloudBigtableIO.read(config)))
       .apply("Extract Ids", ParDo.of(extractor))
       .apply("Group by articleId", GroupByKey.create())
       .apply("Stringify results", ParDo.of(stringifier))
       .apply("Write to File", TextIO.Write.to(options.getResultLocation()).withoutSharding());

    p.run();
  }
}
