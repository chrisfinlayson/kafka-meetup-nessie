docker compose up -d dremio

# Part 1 - Initialise environment - Demonstrate Nessie catalog
docker compose up -d minio minio-setup nessie

# Part 1.1 - Cleanup demo
docker compose stop minio minio-setup nessie
docker compose rm nessie minio minio-setup
docker compose up -d nessie minio minio-setup kafka zookeeper kafka-rest-proxy

# Part 2 - Show concept demo
docker compose up --build kafka-connect
docker compose up shadowtraffic

# Part 3 - Introduce problematic stream
Create new source branch
Add null order status values to Order stream // CHANGE TO DEFECT IN SPARK CODE
<!-- Restart shadowtraffic -->
Run transformation
Technical checkout of data in Dremio
Merge source branch to main
Merge nessie branch to main // Automate?

# Part 4 - Recovery
Kill kafka connect sink

Locate offset of first null
Revert source branch to pre-merge
Revert nessie branch to pre-merge
Re-run transformation

Update consumer group values for topic
Restart kafka sink

Recovered :) 