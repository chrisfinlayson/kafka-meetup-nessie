package dev.bigspark.kafkameetup

import dev.bigspark.kafkameetup.NessieApplication.{authType, fullPathToWarehouse, ref, url}
import org.apache.spark.sql.SparkSession

trait SharedSparkSession {
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
}
