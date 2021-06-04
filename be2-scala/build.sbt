name := "pop"

version := "0.1"

scalaVersion := "2.13.5"

coverageEnabled := true

scapegoatVersion in ThisBuild := "1.4.8"
scapegoatReports := Seq("xml")

// temporarily report scapegoat errors as warnings, to avoid broken builds
scalacOptions in Scapegoat += "-P:scapegoat:overrideLevels:all=Warning"



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


conflictManager := ConflictManager.latestCompatible
