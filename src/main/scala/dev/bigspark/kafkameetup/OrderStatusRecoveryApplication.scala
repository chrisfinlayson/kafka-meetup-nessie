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

// Define Kafka parameters for the 'order' topic
val orderKafkaParams = Map[String, String](
  "bootstrap.servers" -> kafkaBootstrapServers,
  "subscribe" -> "order",
  "startingOffsets" -> """{"order":{"0":23}}"""
)
// Read the 'order' topic from Kafka starting from a specific offset
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
  "topic" -> "OrderEventrecovery"
)

// Write the recovery data to the 'order-recovery' topic
val recoveryQuery = recoveryDF.writeStream
  .format("kafka")
  .options(recoveryKafkaParams)
  .trigger(Trigger.Once())
  .start()

recoveryQuery.awaitTermination()


// Read the 'order-recovery' topic from Kafka
val recoveryCheckDF = spark.readStream.format("kafka").options(recoveryKafkaParams).load()

// Extract the value and cast it to a string
val recoveryCheckValueDF = recoveryCheckDF.selectExpr("CAST(value AS STRING)")

// Filter rows where 'status' is null
val nullStatusCheckDF = recoveryCheckValueDF.filter(col("value").contains("\"status\":null"))

// If there are no null statuses in the recovery topic, write the recovery data back to the 'order' topic
if (nullStatusCheckDF.count() == 0) {
  val recoveryToOrderQuery = recoveryDF.writeStream
    .format("kafka")
    .options(orderKafkaParams)
    .trigger(Trigger.Once())
    .start()

  recoveryToOrderQuery.awaitTermination()
}


// Create a map to hold offset values for the topics
val offsetValues = Map[String, Long](
  "orders" -> 0L,
  "orderstatus" -> 0L,
  "orderlines" -> 0L,
  "products" -> 0L,
  "customers" -> 0L
)

// Kafka properties
val props = new Properties()
props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)

// Create an AdminClient
val adminClient = AdminClient.create(props)

// Reset consumer group values on the broker for each topic
offsetValues.foreach { case (topic, offset) =>
  val consumerGroup = s"${topic}-group"
  val topicPartition = new TopicPartition(topic, 0)
  val offsetAndMetadata = new OffsetAndMetadata(offset)

  // Create a map of TopicPartition to OffsetAndMetadata
  val offsets = Map(topicPartition -> offsetAndMetadata).asJava

  // Commit the offsets for the consumer group
  adminClient.alterConsumerGroupOffsets(consumerGroup, offsets).all().get()
}

// Close the AdminClient
adminClient.close()

}
