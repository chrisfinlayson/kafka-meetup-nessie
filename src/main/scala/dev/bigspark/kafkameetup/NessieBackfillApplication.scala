package dev.bigspark.kafkameetup


import org.apache.spark.sql.functions.{col, from_unixtime}
import scala.sys.process._

object NessieBackfillApplication extends App with SharedSparkSession with NessieMethods {

spark.sparkContext.setLogLevel("ERROR")
val gitBranch = Process("git rev-parse --abbrev-ref HEAD").!!.trim

println(s"Current git branch: $gitBranch")

createBranch(gitBranch)
useReference(gitBranch)

def updateOrderStatus(): Unit = {
  val orderStatusDF = spark.sql("SELECT * FROM nessie.orderstatus")
  val orderDF = spark.sql("SELECT * FROM nessie.order")

  val updatedOrderDF = orderDF.alias("o")
    .join(orderStatusDF.alias("os"), col("o.orderid") === col("os.orderid"))
    .select(col("o.*"))
    .withColumn("status", col("o.status"))
  updatedOrderDF.createOrReplaceTempView("updatedOrder")
  spark.sql("DROP TABLE IF EXISTS nessie.order")
  spark.sql("CREATE TABLE IF NOT EXISTS nessie.order AS SELECT * FROM updatedOrder")
  }

updateOrderStatus()

}