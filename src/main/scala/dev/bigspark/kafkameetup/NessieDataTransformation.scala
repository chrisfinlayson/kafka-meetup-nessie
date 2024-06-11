package dev.bigspark.kafkameetup

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, from_unixtime}
import org.apache.spark.sql.types.{DateType, TimestampType}

object NessieDataTransformation extends App with SharedSparkSession with NessieMethods {

  spark.sparkContext.setLogLevel("ERROR")

  def cleanUp(): Unit = {
    dropTable("customer")
    dropTable("product")
    dropTable("orderstatus")
    dropTable("orderline")
    dropTable("order")
    dropTable("modelCustomerOrder")
  }

  def createTable(df: DataFrame): Unit = {
    //// Persist a DataFrame into a new Iceberg table
    df.write
      .format("iceberg")
      .mode("overwrite")
      .option("path", "s3a://warehouse/modelCustomerOrder")
      .saveAsTable("nessie.modelCustomerOrder")
  }

  def insertIntoTable(df: DataFrame): Unit = {
    // Read the existing data
    val existingDF = spark.read
      .format("iceberg")
      .load("nessie.modelCustomerOrder")
    // Union the new data with the existing data
    val unionDF = existingDF.union(df)
    // Drop duplicates
    val deduplicatedDF = unionDF.dropDuplicates()
    // Write the deduplicated data back to the table
    deduplicatedDF.write
      .format("iceberg")
      .mode("overwrite")
      .option("path", "s3a://warehouse/modelCustomerOrder")
      .saveAsTable("nessie.modelCustomerOrder")
  }

  def conformRawToOrderModel(): DataFrame = {
    val customer = selectData("customer")
    val product = selectData("product")
    val orderStatus = selectData("orderstatus")
    val orderLine = selectData("orderline")
    val order = selectData("order")

    // Joining the 'order' and 'orderStatus' DataFrames on 'orderID' column
    val orderWithStatus = order.join(orderStatus, order("orderID") === orderStatus("orderID"))

    // Joining the result of the previous join with 'customer' DataFrame on 'customerID' column
    val orderWithCustomer = orderWithStatus.join(customer, order("customerID") === customer("customerID"), "left")

    // Joining the result of the previous join with 'orderLine' DataFrame on 'orderID' column
    val orderWithCustomerAndLines = orderWithCustomer.join(orderLine, order("orderID") === orderLine("orderLineID"), "left")

    // Joining the result of the previous join with 'product' DataFrame on 'productID' column
    val finalDF = orderWithCustomerAndLines.join(product, orderLine("productID") === product("productID"), "left")
    val resultDF = finalDF.select(
      col("orderStatus.orderID").alias("order_number"),
      col("orderLine.quantityOrdered").alias("quantity_ordered"),
      col("orderLine.priceEach").alias("price_each"),
      col("product.productCode").alias("product_code"),
      col("orderLine.sales").alias("sales"),
      from_unixtime(col("order.orderDate") / 1000).cast(TimestampType).alias("order_date"),
      col("order.status").alias("status"),
      col("product.productLine").alias("product_line"),
      col("product.msrp").alias("msrp"),
      col("customer.customerName").alias("customer_name"),
      col("customer.phone").alias("phone"),
      col("customer.addressLine1").alias("address_line1"),
      col("customer.addressLine2").alias("address_line2"),
      col("customer.city").alias("city"),
      col("customer.state").alias("state"),
      col("customer.postalCode").alias("postal_code"),
      col("customer.country").alias("country"),
      col("customer.territory").alias("territory"),
      col("orderLine.dealSize").alias("deal_size")
    )
    resultDF

  }
  
//  while (true) {
    import sys.process._
    val gitBranch = "git rev-parse --abbrev-ref HEAD".!!.trim
    println(s"Current git branch: $gitBranch")
//    cleanUp()
    createBranch(gitBranch)
    useReference(gitBranch)
    val df = conformRawToOrderModel()
    dropTable("modelCustomerOrder")
    createTable(df)
//    Thread.sleep(30000)
//  }



}