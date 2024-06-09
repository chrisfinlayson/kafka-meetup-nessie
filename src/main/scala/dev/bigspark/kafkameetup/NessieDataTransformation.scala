package dev.bigspark.kafkameetup

import org.apache.spark.sql.{DataFrame}
import org.apache.spark.sql.functions.col

object NessieDataTransformation extends App with SharedSparkSession {

  spark.sparkContext.setLogLevel("ERROR")

  def createBranch(branchName: String): Unit = {
    spark.sql(s"CREATE BRANCH IF NOT EXISTS $branchName IN nessie")
  }

  def useReference(refName: String): Unit = {
    spark.sql(s"USE REFERENCE $refName IN nessie").show(false)
  }

  def createTable(tableName: String): Unit = {
    spark.sql(s"CREATE TABLE IF NOT EXISTS nessie.$tableName (name STRING) USING iceberg")
  }

  def insertData(tableName: String, data: String): Unit = {
    spark.sql(s"INSERT INTO nessie.$tableName VALUES $data")
  }

  def selectData(tableName: String): DataFrame = {
    spark.sql(s"SELECT * FROM nessie.$tableName")
  }

val customer = selectData("customer")
val product = selectData("product")
val orderStatus = selectData("orderstatus")
val orderLine = selectData("orderline")
val order = selectData("order")


println("Customer Schema:")
customer.printSchema()
println("Product Schema:")
product.printSchema()
println("Order Status Schema:")
orderStatus.printSchema()
println("Order Line Schema:")
orderLine.printSchema()
println("Order Schema:")
order.printSchema()

val orderWithStatus = order.join(orderStatus, Seq("table"), "left")
val orderWithCustomer = orderWithStatus.join(customer, Seq("contactFirstName", "contactLastName"), "left")
val orderWithCustomerAndLines = orderWithCustomer.join(orderLine, Seq("table"), "left")
val finalDF = orderWithCustomerAndLines.join(product, Seq("table"), "left")

val resultDF = finalDF.select(
  col("orderLine.orderNumber"),
  col("orderLine.quantityOrdered"),
  col("orderLine.priceEach"),
  col("orderLine.productCode"),
  col("orderLine.sales"),
  col("order.orderDate"),
  col("orderStatus.status"),
  col("product.productLine"),
  col("product.msrp"), 
  col("order.customerName"),
  col("customer.phone"),
  col("customer.addressLine1"),
  col("customer.addressLine2"),
  col("customer.city"),
  col("customer.state"),
  col("customer.postalCode"),
  col("customer.country"),
  col("customer.territory"),
  col("customer.contactLastName"),
  col("customer.contactFirstName"),
  col("orderLine.dealSize")
)

  resultDF.show(10,false)
//
//// Persist the final DataFrame into a new Iceberg table
//resultDF.write
//  .format("iceberg")
//  .mode("overwrite")
//  .save("iceberg.flat_structure_table")

}
