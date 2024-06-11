    docker compose up -d dremio

# Part 1 - Initialise environment - Demonstrate Nessie catalog
    docker compose up -d minio minio-setup nessie

# Part 1.1 - Cleanup demo
    docker compose stop minio minio-setup nessie kafka
    docker compose rm nessie minio minio-setup kafka
    docker compose up -d nessie minio minio-setup kafka zookeeper kafka-rest-proxy

# Part 2 - Show concept demo
    docker compose up --build -d kafka-connect
    docker compose up -d shadowtraffic

    Run transformation process

# Part 3 - Introduce problematic stream
    Create new source branch
    Add new status key to order stream in Shadowtraffic
    Restart shadowtraffic
        docker compose stop shadowtraffic
        docker compose rm shadowtraffic
        docker compose up -d shadowtraffic
    Adjust spark join to order from orderstatus
    Run transformation
    Technical checkout of data in Dremio
    Merge source branch to main
    Merge nessie branch to main

# Part 4 - Recovery

Kill shadowtraffic
Kill kafka connect sink
    docker compose stop kafka-connect shadowtraffic
    docker compose rm kafka-connect shadowtraffic

Locate offset of first null
Create a recovery topic in intellij
DONE Write a Spark job to read all offsets from first null and write topic to new recovery topic
DONE Check recovery topic
DONE Produce events back to order topic, note first offset

Revert source branch to pre-merge
Revert nessie branch to pre-merge
Update consumer group values to last values for all topics

Re-run transformation process

Fix shadowtraffic spec
Restart shadowtraffic
Restart kafka sink
    docker compose up --build -d kafka-connect
    docker compose up -d shadowtraffic

Re-run transformation process

Recovered :) 