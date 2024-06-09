package dev.bigspark.kafkameetup

import org.apache.spark.sql.{DataFrame, SparkSession}

object NessieApplication extends App{

  // Full url of the Nessie API endpoint to nessie
  val url = "http://localhost:19120/api/v1";
  // Where to store nessie tables
  val fullPathToWarehouse = "s3a://warehouse";
  // The ref or context that nessie will operate on
  // (if different from default branch).
  // Can be the name of a Nessie branch or tag name.
  val ref = "main";
  // Nessie authentication type (NONE, BEARER, OAUTH2 or AWS)
  val authType = "NONE";
  // for a local spark instance
  val spark = SparkSession.builder()
    .appName("Kafka meetup")
    .config("spark.jars.packages", "org.apache.iceberg:iceberg-spark-runtime-3.3_2.12:1.5.2" +
      ",org.projectnessie.nessie-integrations:nessie-spark-extensions-3.3_2.12:0.83.2" +
    ",org.apache.hadoop:hadoop-aws:3.3.4")
    .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions," +
      "org.projectnessie.spark.extensions.NessieSparkSessionExtensions")
    .config("spark.sql.catalog.nessie.uri", url)
    .config("spark.sql.catalog.nessie.ref", ref)
    .config("spark.sql.catalog.nessie.authentication.type", authType)
    .config("spark.sql.catalog.nessie.catalog-impl", "org.apache.iceberg.nessie.NessieCatalog")
    .config("spark.sql.catalog.nessie.warehouse", fullPathToWarehouse)
    .config("spark.sql.catalog.nessie", "org.apache.iceberg.spark.SparkCatalog")
    .config("spark.hadoop.fs.s3a.access.key", "admin")
    .config("spark.hadoop.fs.s3a.secret.key", "password")
    .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
    .config("spark.hadoop.fs.s3a.path.style.access", "true")
    .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
    .config("spark.dremio.s3.compat", "true")
    .master("local[*]").getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

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

//  createTable("names")
//  insertData("names", "('David Walker'), ('Angus Neilson')")
//  selectData("names").show()
//  createBranch("trustpilot_new_recruit")
//  useReference("trustpilot_new_recruit")
//  insertData("names", "('Chris Finlayson')")
//  selectData("names").show()
//  useReference("main")
//  selectData("names").show()


}
