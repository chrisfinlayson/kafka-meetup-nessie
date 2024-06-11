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

  val updatedOrderDF = spark.sql(
    """
      |Select o.customerID, o.offset, o.orderDate, o.orderID, os.status, o.table
      |from nessie.order o
      |join nessie.orderstatus os
      |on o.orderID=os.orderID
      |""".stripMargin).dropDuplicates()
  updatedOrderDF.createOrReplaceTempView("updatedOrder")
  spark.sql("DROP TABLE IF EXISTS nessie.order")
  spark.sql("CREATE TABLE IF NOT EXISTS nessie.order AS SELECT * FROM updatedOrder")
  }

updateOrderStatus()

}