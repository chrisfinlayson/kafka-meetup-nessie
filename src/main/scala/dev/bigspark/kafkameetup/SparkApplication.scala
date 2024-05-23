package dev.bigspark.kafkameetup

import dev.bigspark.kafkameetup.SparkApplication.spark
import org.apache.spark.sql.SparkSession

object SparkApplication extends App{

  // Full url of the Nessie API endpoint to nessie
  val url = "http://localhost:19120/api/v1";
  // Where to store nessie tables
  val fullPathToWarehouse = "spark-warehouse";
  // The ref or context that nessie will operate on
  // (if different from default branch).
  // Can be the name of a Nessie branch or tag name.
  val ref = "main";
  // Nessie authentication type (NONE, BEARER, OAUTH2 or AWS)
  val authType = "NONE";
  // for a local spark instance
  val spark = SparkSession.builder()
    .appName("sample-spark")
    .config("spark.jars.packages", "org.apache.iceberg:iceberg-spark-runtime-3.3_2.12:1.5.2" +
      ",org.projectnessie.nessie-integrations:nessie-spark-extensions-3.3_2.12:0.83.2")
    .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions," +
      "org.projectnessie.spark.extensions.NessieSparkSessionExtensions")
    .config("spark.sql.catalog.nessie.uri", url)
    .config("spark.sql.catalog.nessie.ref", ref)
    .config("spark.sql.catalog.nessie.authentication.type", authType)
    .config("spark.sql.catalog.nessie.catalog-impl", "org.apache.iceberg.nessie.NessieCatalog")
    .config("spark.sql.catalog.nessie.warehouse", fullPathToWarehouse)
    .config("spark.sql.catalog.nessie", "org.apache.iceberg.spark.SparkCatalog")
    .master("local[*]").getOrCreate()

  import spark.implicits._
  print("\n Showing Sample DataFrame")
  List((1, 20),(2, 30),(3, 50),(4, 50)).toDF("score", "data").show()

  def runQueries():Unit = {

    spark.sql("DROP TABLE IF EXISTS nessie.names").show()

    spark.sql("CREATE TABLE IF NOT EXISTS nessie.names (name STRING) USING iceberg").show()

    spark.sql("INSERT INTO nessie.names VALUES ('Alex Merced'), ('Dipankar Mazumdar'), ('Jason Huges')").show()

    spark.sql("SELECT * FROM nessie.names").show()

    spark.sql("CREATE BRANCH IF NOT EXISTS my_branch IN nessie").show()

    spark.sql("USE REFERENCE my_branch IN nessie").show()

    spark.sql("INSERT INTO nessie.names VALUES ('Alex Merced'), ('Dipankar Mazumdar'), ('Jason Huges')").show()

    spark.sql("SELECT * FROM nessie.names").show()

    spark.sql("USE REFERENCE main IN nessie").show()

    spark.sql("SELECT * FROM nessie.names").show()
  }

  runQueries()
}
