package dev.bigspark.kafkameetup

import org.apache.spark.sql.DataFrame

trait NessieMethods extends SharedSparkSession {

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
    spark.sql(sql_str)
  }

  def mergeData(sourceBranchName: String, targetBranchName: String): Unit = {
    useReference(sourceBranchName)
    val sql_str=(s"MERGE BRANCH INTO $targetBranchName in nessie")
    println(sql_str)
    spark.sql(sql_str)
  }

}
