package dev.bigspark.kafkameetup

import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}
import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition
import java.util.Properties
import scala.collection.JavaConverters._


object OrderStatusRecoveryApplication extends App with SharedSparkSession {
val kafkaBootstrapServers = "localhost:9092"

// Define Kafka parameters for the 'OrderEventStream' topic
val orderKafkaParams = Map[String, String](
  "bootstrap.servers" -> kafkaBootstrapServers,
  "subscribe" -> "OrderEventStream",
  "startingOffsets" -> """{"OrderEventStream":{"0":3469}}"""
)
// Read the 'OrderEventStream' topic from Kafka starting from a specific offset
val orderDF = spark.readStream
  .format("kafka")
  .option("kafka.bootstrap.servers", kafkaBootstrapServers)
  .option("subscribe", "OrderEventStream")
  .option("startingOffsets", """{"OrderEventStream":{"0":3469}}""")
  .load()
// Extract the value and cast it to a string
val orderValueDF = orderDF.selectExpr("CAST(value AS STRING)")

// Read the existing 'orderstatus' table
val orderStatusDF = spark.read.format("iceberg").load("nessie.orderstatus")

// Extract 'orderID' from 'value' in orderValueDF
val orderValueWithIDDF = orderValueDF.withColumn("orderID", get_json_object(col("value"), "$.orderID"))

// Join the orderValueWithIDDF with orderStatusDF to get the correct 'status' values
val recoveryDF = orderValueWithIDDF.join(orderStatusDF, "orderID")
  .selectExpr("CAST(orderID AS STRING) AS key", "to_json(struct(*)) AS value")
// Define Kafka parameters for the 'OrderEventRecovery' topic
val recoveryKafkaParams = Map[String, String](
  "kafka.bootstrap.servers" -> kafkaBootstrapServers,
  "topic" -> "OrderEventRecovery"
)

// Write the recovery data to the 'OrderEventRecovery' topic
val recoveryQuery = recoveryDF.writeStream
  .format("kafka")
  .options(recoveryKafkaParams)
  .option("checkpointLocation", "./checkpoint/") // Add checkpoint location
  .trigger(Trigger.Once())
  .start()

recoveryQuery.awaitTermination()

// Read the 'OrderEventRecovery' topic from Kafka
val recoveryCheckDF = spark.readStream
  .format("kafka")
  .option("kafka.bootstrap.servers", kafkaBootstrapServers)
  .option("subscribe", "OrderEventRecovery")
  .load()
// Extract the value and cast it to a string
val recoveryCheckValueDF = recoveryCheckDF.selectExpr("CAST(value AS STRING)")

// Filter rows where 'status' is null
val nullStatusCheckDF = recoveryCheckValueDF.filter(col("value").contains("\"status\":null"))

// If there are no null statuses in the recovery topic, write the recovery data back to the 'OrderEventStream' topic and print the last offset committed

val recoveryToOrderQuery = recoveryDF.writeStream
.format("kafka")
.options(orderKafkaParams)
.option("checkpointLocation", "./checkpoint/") // Add checkpoint location
.trigger(Trigger.Once())
.start()

recoveryToOrderQuery.awaitTermination()

// Get the last offset committed
val lastOffsetCommitted = recoveryToOrderQuery.lastProgress.sources(0).endOffset
println(s"Last offset committed: $lastOffsetCommitted")

}
