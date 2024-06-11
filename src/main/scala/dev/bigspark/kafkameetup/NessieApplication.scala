package dev.bigspark.kafkameetup

import org.apache.spark.sql.{DataFrame, SparkSession}

object NessieApplication extends App with SharedSparkSession {

  spark.sparkContext.setLogLevel("FATAL")

  def cleanUpQueries():Unit = {
    spark.sql("DROP TABLE IF EXISTS nessie.names")
    spark.sql("DROP BRANCH IF EXISTS trustpilot_new_recruit IN nessie")
  }
  cleanUpQueries()

  def runTestQueries():Unit = {
    spark.sql("CREATE TABLE IF NOT EXISTS nessie.names (name STRING) USING iceberg")
    spark.sql("INSERT INTO nessie.names VALUES ('David Walker'), ('Angus Neilson')")
    spark.sql("SELECT * FROM nessie.names").show()
    spark.sql("CREATE BRANCH IF NOT EXISTS new_recruit IN nessie")
    spark.sql("USE REFERENCE new_recruit IN nessie")
    spark.sql("INSERT INTO nessie.names VALUES ('Chris Finlayson')")
    spark.sql("SELECT * FROM nessie.names").show()
    spark.sql("USE REFERENCE main IN nessie")
    spark.sql("SELECT * FROM nessie.names").show()
  }
  // runTestQueries()

  def createBranch(branchName: String): Unit = {
    val sql_str=(s"CREATE BRANCH IF NOT EXISTS $branchName IN nessie")
    println(sql_str)
    spark.sql(sql_str)
  }

  def dropBranch(branchName: String): Unit = {
    spark.sql(s"DROP BRANCH IF EXISTS $branchName IN nessie")
  }

  def useReference(refName: String): Unit = {
    val sql_str=(s"USE REFERENCE $refName IN nessie")
    println(sql_str)
    spark.sql(sql_str).show(false)
  }

  def createTable(tableName: String): Unit = {
    val sql_str=(s"CREATE TABLE IF NOT EXISTS nessie.$tableName (name STRING) USING iceberg")
    println(sql_str)
    spark.sql(sql_str)
  }

  def dropTable(tableName: String): Unit = {
    val sql_str=(s"DROP TABLE IF EXISTS nessie.$tableName")
    println(sql_str)
    spark.sql(sql_str)
  }

  def insertData(tableName: String, data: String): Unit = {
    val sql_str=(s"INSERT INTO nessie.$tableName VALUES $data")
    println(sql_str)
    spark.sql(sql_str)
  }

  def selectData(tableName: String): DataFrame = {
    val sql_str=(s"SELECT * FROM nessie.$tableName")
    println(sql_str)
    return spark.sql(sql_str)
  }

  def mergeData(sourceBranchName: String, targetBranchName: String): Unit = {
    useReference(sourceBranchName)
    val sql_str=(s"MERGE BRANCH INTO $targetBranchName in nessie")
    println(sql_str)
    spark.sql(sql_str)
  }

  createTable("names")
  insertData("names", "('David Walker'), ('Angus Neilson')")
  selectData("names").show()
  //TODO Check nessie to show table creation on main
  createBranch("kafka_meetup_recruit")
  useReference("kafka_meetup_recruit")
  //TODO Check nessie to show table creation on main
  insertData("names", "('Chris Finlayson')")
  selectData("names").show()

  //TODO Check nessie to show insert operation on new branch
  useReference("main")
  selectData("names").show()
  //TODO Check nessie to show insert not on main branch
  mergeData("kafka_meetup_recruit", "main")
  useReference("main")
  //TODO Check nessie to show merge operation
  selectData("names").show()
  
  //TODO Check nessie to show branch removal
  dropBranch("kafka_meetup_recruit")
}
