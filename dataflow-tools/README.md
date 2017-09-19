# Links

The tools are using [Google Cloud Dataflow](https://cloud.google.com/dataflow/) and [Cloud Bigtable](https://cloud.google.com/bigtable/).

# Running the tool

mvn clean package exec:exec -DStepsCount -Dbigtable.projectID=<project-id> -Dbigtable.instanceID=<bt-instance-id> -Dbigtable.table=<table-name> -Dgs=gs://<bucket> -Dnetwork=<network> -Dsubnetwork=<subnetwork>


# Useful

You can verify the results by typing:

    gsutil ls gs://my_bucket/**

    gsutil cp gs://my_bucket/count .
    cat count


You can verify that the job is still running:

    gcloud  dataflow jobs list

