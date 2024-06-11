package dev.bigspark.kafkameetup

object StreamingApplication extends App with SharedSparkSession {
val kafkaBootstrapServers = "localhost:9092"

// Define the Kafka parameters, topic and bootstrap server
val customersKafkaParams = Map[String, String](
  "kafka.bootstrap.servers" -> kafkaBootstrapServers,
  "subscribe" -> "accounts"
)

import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

// Define Kafka parameters for the 'order' topic
val orderKafkaParams = Map[String, String](
  "kafka.bootstrap.servers" -> kafkaBootstrapServers,
  "subscribe" -> "order"
)

// Read the 'order' topic from Kafka
val orderDF = spark.readStream.format("kafka").options(orderKafkaParams).load()

// Extract the value and cast it to a string
val orderValueDF = orderDF.selectExpr("CAST(value AS STRING)")

// Filter rows where 'status' is null
val nullStatusDF = orderValueDF.filter(col("value").contains("\"status\":null"))

// Read the existing 'orderstatus' table
val orderStatusDF = spark.read.format("iceberg").load("nessie.orderstatus")

// Join the nullStatusDF with orderStatusDF to get the correct 'status' values
val recoveryDF = nullStatusDF.join(orderStatusDF, "orderID")
  .selectExpr("CAST(key AS STRING)", "to_json(struct(*)) AS value")

// Define Kafka parameters for the 'order-recovery' topic
val recoveryKafkaParams = Map[String, String](
  "kafka.bootstrap.servers" -> kafkaBootstrapServers,
  "topic" -> "order-recovery"
)

// Write the recovery data to the 'order-recovery' topic
val recoveryQuery = recoveryDF.writeStream
  .format("kafka")
  .options(recoveryKafkaParams)
  .trigger(Trigger.Once())
  .start()

recoveryQuery.awaitTermination()

}
