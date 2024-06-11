    docker compose stop minio minio-setup nessie kafka kafka-connect shadowtraffic
    docker compose rm nessie minio minio-setup kafka kafka-connect shadowtraffic
   
    docker compose up -d dremio

# Part 1 - Initialise environment - Demonstrate Nessie catalog
    docker compose up -d minio minio-setup nessie

# Part 1.1 - Cleanup demo
    docker compose stop minio minio-setup nessie kafka
    docker compose rm nessie minio minio-setup kafka
    docker compose up -d nessie minio minio-setup kafka zookeeper kafka-rest-proxy

# Part 2 - Show concept demo
        cp /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config-orig.json /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config.json
    docker compose up --build -d kafka-connect
    docker compose up -d shadowtraffic

    Run transformation process
    Check realtime dashboard

# Part 3 - Introduce problematic stream

    Add new status key to order stream in Shadowtraffic config and restart
        cp /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config-replace.json /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config.json
    
        docker compose restart shadowtraffic

    Verify status is being added to the order table in Dremio
        select * from Nessie."order" at BRANCH "main" where status is not null and status !='NULL'

    !! Start the migration of the model !!
    
    Create new source branch 'orderstatuschange'
    Adjust spark job to source status from order
    
    Backfill of status into order table
        
        UPDATE Nessie."order" at BRANCH "orderstatuschange" 
        SET status = (
            SELECT status 
            FROM orderstatus 
            WHERE orderstatus.orderid = "order".orderid
            )

    Run transformation job
    Technical checkout of data in Dremio
        select * from Nessie."order" at BRANCH "main" where status is not null and status !='NULL'
    
    Pull request of feature branch to main
    Change source branch to main and pull
    Merge nessie branch to main
    
        MERGE branch "orderstatuschange" INTO BRANCH "main"
    
    Run transformation process
    Check realtime dashboard

# Part 4 - Recovery


Resolve nulls in Shadowtraffic
    docker compose stop shadowtraffic
    Fix status key to order stream in Shadowtraffic config
    Restart shadowtraffic
        docker compose restart shadowtraffic

Locate offset of first null
Create a recovery topic in intellij
DONE Write a Spark job to read all offsets from first null and write enriched events to new recovery topic
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