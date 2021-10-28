import sbtsonar.SonarPlugin.autoImport.sonarProperties

name := "pop"

version := "0.1"

scalaVersion := "2.13.5"

mainClass in (Compile, run) := Some("ch.epfl.pop.Server")
mainClass in (Compile, packageBin) := Some("ch.epfl.pop.Server")

lazy val scoverageSettings = Seq(
  coverageEnabled in Compile := true,
  coverageEnabled in Test := true,
  coverageEnabled in packageBin := false,
  
)



scapegoatVersion in ThisBuild := "1.4.8"
scapegoatReports := Seq("xml")

// temporarily report scapegoat errors as warnings, to avoid broken builds
scalacOptions in Scapegoat += "-P:scapegoat:overrideLevels:all=Warning"

// Configure Sonar
sonarProperties := Map(
  "sonar.organization" -> "dedis",
  "sonar.projectKey" -> "dedis_student_21_pop_be2",
  "sonar.sources" -> "src/main/scala",
  "sonar.sourceEncoding" -> "UTF-8",
  "sonar.scala.version" -> "2.13.5",
  "sonar.scala.coverage.reportPaths" -> "./target/scala-2.13/scoverage-report/scoverage.xml",
  "sonar.scala.scapegoat.reportPaths" -> "./target/scala-2.13/scapegoat-report/scapegoat.xml"
)


// For websockets
val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.0"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion)

// Logging for akka
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

// distributed pub sub cluster
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion

// Akka actor test kit
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test

// For LevelDB database
// https://mvnrepository.com/artifact/org.iq80.leveldb/leveldb
libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.12"
libraryDependencies += "org.xerial.snappy" % "snappy-java" % "1.1.7.3"
// missing binary dependency, leveldbjni
//libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % AkkaVersion


// Json Parser (https://github.com/spray/spray-json)
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"

// Encryption
libraryDependencies += "com.google.crypto.tink" % "tink" % "1.5.0"

// Scala unit tests
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test

// Jackson Databind (for Json Schema Validation)
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.0-RC3"

// Json Schema Validator
libraryDependencies += "com.networknt" % "json-schema-validator" % "1.0.60"



conflictManager := ConflictManager.latestCompatible
