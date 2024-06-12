# PRE REQS

CHECK - Delete test branches
CHECK - Pull from baseline branch
CHECK - Tranformation job is at starting pos - orderStatus.status

    docker compose stop minio minio-setup nessie kafka
    docker compose rm nessie minio minio-setup kafka
    docker compose up -d nessie minio minio-setup kafka zookeeper kafka-rest-proxy
    docker compose stop kafka-connect shadowtraffic    
    docker compose rm kafka-connect shadowtraffic

# DEMO 1
# Part 1 - Initialise environment - Demonstrate Nessie catalog
    docker compose up -d minio minio-setup nessie

# Part 1.1 - Cleanup demo
    docker compose stop minio minio-setup nessie kafka
    docker compose rm nessie minio minio-setup kafka
    docker compose up -d nessie minio minio-setup kafka zookeeper kafka-rest-proxy 

# DEMO 2
## - Show concept demo - BAU

     cp /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config-orig.json /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config.json
    docker compose up --build -d kafka-connect
    docker compose up -d shadowtraffic 

    Show events being generated
         kcat -u -b localhost:9092 -G OrderEventStream OrderLineEventStream ProductEventStream | jq 
    
    Show tables being created in nessie 
    Run transformation process
    Check realtime dashboard

## - Introduce upstream change

    Show table prior to change
        select * from Nessie."order" at BRANCH "main"

    Show event stream prior to change
         kcat -u -b localhost:9092 -t OrderEventStream | jq

    Add new status key to order stream in Shadowtraffic config and restart
        cp /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config-replace.json /Users/christopherfinlayson/dev/dremio-nessie-kafka-connect/shadow-config.json
    
        docker compose restart shadowtraffic

    Locate schema change event in Nessie
        Iceberg schema change against table order

    Verify status is being added to the order table in Dremio
        select * from Nessie."order" at BRANCH "main" where nvl(status, 'NULL') !='NULL'

## !! Start the migration of the model !!

    Stop producer/consumer
        docker compose stop kafka-connect
    
    STOP spark transformation job
    
    Create new source branch 'orderstatuschange'
        git checkout -b orderstatuschange

    Adjust spark job to source status from order
    
    Backfill of status into order table
        
        Execute NessieBackfillApplication
    
    <!-- Verify backfill

        select * from Nessie."order" at BRANCH "orderstatuschange" -->

    Run transformation job

    Technical checkout of data in Dremio by Analyst
    Run data quality check

        select status, count(*)
        from Nessie."modelCustomerOrder" at BRANCH "orderstatuschange" 
        group by status
    
    SLIDE
    
    Pull request of feature branch to main
    Change source branch to main and pull

    Merge operation of nessie branch to main

         MERGE BRANCH "orderstatuschange" INTO "main" IN Nessie 

    Restart producer/consumer
         docker compose restart kafka-connect 

    Run transformation process IN LOOP
    Check realtime dashboard

# DEMO 3 - Recovery
Pause kafka sink 
     docker compose stop kafka-connect  

Show corruption on dashboard?

Show corruption in table

     select status, count(*)
        from Nessie."modelCustomerOrder" at BRANCH "main" 
        group by status; 

Stop transformation Spark job

Revert nessie branch to pre-merge
     ALTER BRANCH "main" ASSIGN COMMIT "e99bd33671c69607e23a72739c2ae4681f817ecb92d618baaff96dacba4ad231" in nessie 

Revert source branch to pre-merge state

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


Update consumer group value for Order topic to first recovery value
    Using ConsumerGroupOffsetApplication

Restart kafka sink
     docker compose restart kafka-connect 

Re-run transformation process

Recovered :) 