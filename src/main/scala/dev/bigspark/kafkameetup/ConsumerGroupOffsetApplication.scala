package dev.bigspark.kafkameetup

import scala.sys.process._

object ConsumerGroupOffsetApplication extends App  {

val topicOffsets = Map(
  "OrderEventStream" -> 556,
  "OrderStatusEventStream" -> 556,
  "OrderLineEventStream" -> 556,
  "ProductEventStream" -> 556,
  "CustomerEventStream" -> 556
)

topicOffsets.foreach { case (topic, offset) =>
  val command = s"docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group connect-iceberg-sink-connector --topic $topic --reset-offsets --to-offset $offset --execute"
  command.!
  val command2 = s"docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group cg-control-iceberg-sink-connector --topic $topic --reset-offsets --to-offset $offset --execute"
  command2.!
}
}