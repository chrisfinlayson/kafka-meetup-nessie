PRE REQS
CHECK - Tranformation job is at starting pos - orderStatus.status
CHECK - Delete test branches

    docker compose stop minio minio-setup nessie kafka
    docker compose rm nessie minio minio-setup kafka
    docker compose up -d nessie minio minio-setup kafka zookeeper kafka-rest-proxy


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

    Show events being generated
        kcat -u -b localhost:9092 -G OrderEventStream OrderLineEventStream ProductEventStream | jq
    
    SHow tables being created in nessie 
    Run transformation process
    Check realtime dashboard

# Part 3 - Introduce problematic stream

    Show table prior to change
        select * from Nessie."order" at BRANCH "main"

    Add new status key to order stream in Shadowtraffic config and restart
        cp /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config-replace.json /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config.json
    
        docker compose restart shadowtraffic

    Verify status is being added to the order table in Dremio
        select * from Nessie."order" at BRANCH "main" where nvl(status, 'NULL') !='NULL'

    !! Start the migration of the model !!

    Stop producer/consumer
        docker compose stop kafka-connect shadowtraffic
    
    Create new source branch 'orderstatuschange'
        git checkout -b orderstatuschange

    Adjust spark job to source status from order
    
    Backfill of status into order table
        
        Execute NessieBackfillApplication
    
    Verify backfill

        select * from Nessie."order" at BRANCH "orderstatuschange" where nvl(status, 'NULL') !='NULL' 

    Run transformation job
    Technical checkout of data in Dremio
        
        select * from Nessie."modelCustomerOrder" at BRANCH "orderstatuschange" where nvl(status, 'NULL') !='NULL' 
    
    Pull request of feature branch to main
    Change source branch to main 

    Merge operation of nessie branch to main

        MERGE BRANCH "orderstatuschange" INTO "main" IN Nessie

    Run backfill process

    Restart producer/consumer
        docker compose stop kafka-connect shadowtraffic

    Run transformation process
    Check realtime dashboard

# Part 4 - Recovery
Pause kafka sink and Shadowtraffic
    docker compose stop kafka-connect shadowtraffic

Resolve nulls defect in Data producer
    docker compose stop shadowtraffic
    Fix status key to order stream in Shadowtraffic config

Locate offset of first status value successfully written to order table
    select min("offset") from Nessie."order" at BRANCH "main" where status is not null

Record offset value: 3569




Create a recovery topic in intellij
Write a Spark job to read all offsets from first null and write enriched events to new recovery topic
Check recovery topic
Produce events back to order topic, note first offset



Revert nessie branch to pre-merge
    ALTER BRANCH "main" ASSIGN COMMIT "e7fcf1cb42e9ac3583f8cff078cea9cf095e4842330746db9abce5fba54a46b0" in nessie

Update consumer group values to last values for all topics

select 'order', max("offset") from Nessie."order" at BRANCH "main"
UNION ALL
select 'orderstatus', max("offset") from Nessie."order" at BRANCH "main"
UNION ALL
select 'orderline', max("offset") from Nessie."orderline" at BRANCH "main"
UNION ALL
select 'product', max("offset") from Nessie."product" at BRANCH "main"
UNION ALL
select 'customer', max("offset") from Nessie."customer" at BRANCH "main"

order
7840
orderstatus
7840
orderline
20872
product
5228
customer
5228

Update consumer group value for Order topic to first recovery value

Re-run transformation process

Restart shadowtraffic
    docker compose restart shadowtraffic


Fix shadowtraffic spec
Restart shadowtraffic
Restart kafka sink
    docker compose up --build -d kafka-connect
    docker compose up -d shadowtraffic

Re-run transformation process

Recovered :) 