package dev.bigspark.kafkameetup

object StreamingApplication extends App with SharedSparkSession {
val kafkaBootstrapServers = "localhost:9092"

// Define the Kafka parameters, topic and bootstrap server
val customersKafkaParams = Map[String, String](
  "kafka.bootstrap.servers" -> kafkaBootstrapServers,
  "subscribe" -> "accounts"
)

// Create a streaming DataFrame that represents the data received from Kafka
val accountsDF = spark.readStream.format("kafka").options(customersKafkaParams).load()

// Display the stream to the console
val query = accountsDF.writeStream.format("console").start()
query.processAllAvailable()
for(i <- 1 to 10) {
  if(query.status.isTriggerActive) {
    Thread.sleep(1000)
  } else {
    query.processAllAvailable()
  }
}
query.stop()
//transactionsDF.writeStream.format("console").start()

}
