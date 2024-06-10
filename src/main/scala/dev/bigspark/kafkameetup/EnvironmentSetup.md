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
