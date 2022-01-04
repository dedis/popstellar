import scala.util.{Try, Success, Failure}
import sbtsonar.SonarPlugin.autoImport.sonarProperties
import sbt.IO._

name := "pop"

version := "0.1"

scalaVersion := "2.13.7"

parallelExecution in ThisBuild := false

//Create task to copy the protocol folder to resources
lazy val copyProtocolTask = taskKey[Unit]("Copy protocol to resources")
copyProtocolTask := {
    val log = streams.value.log
    log.info("Executing Protocol folder copy...")
    val scalaDest = "be2-scala"
    baseDirectory.value.name
    if(! baseDirectory.value.name.equals(scalaDest)){
        log.error(s"Please make sure you working dir is $scalaDest !")
    }else{
        val source = new File("../protocol")
        val dest   = new File("./src/main/resources/protocol")
        Try(IO.copyDirectory(source,dest, true)) match {
            case Success(_) => log.info("Copied !!")
            case Failure(exception) =>
                log.error("Could not copy protocol to ressource folder")
                exception.printStackTrace()
        }
    }
}
//Add the copyProtocolTask to compile and test scopes
(Compile/ compile) := ((Compile/ compile) dependsOn copyProtocolTask).value
(Test/ test) := ((Test/ test) dependsOn copyProtocolTask).value

//Setup resource directory for jar assembly
resourceDirectory in (Compile, packageBin) := file(".") / "./src/main/resources"
//Make resourceDirectory setting global to remove sbt warning
(Global / excludeLintKeys) += resourceDirectory

//Setup main calass task context/confiuration
mainClass in (Compile, run) := Some("ch.epfl.pop.Server")
mainClass in (Compile, packageBin) := Some("ch.epfl.pop.Server")

lazy val scoverageSettings = Seq(
  coverageEnabled in Compile := true,
  coverageEnabled in Test := true,
  coverageEnabled in packageBin := false,
)

scapegoatVersion in ThisBuild := "1.4.11"
scapegoatReports := Seq("xml")

// temporarily report scapegoat errors as warnings, to avoid broken builds
scalacOptions in Scapegoat += "-P:scapegoat:overrideLevels:all=Warning"

// Configure Sonar
sonarProperties := Map(
  "sonar.organization" -> "dedis",
  "sonar.projectKey" -> "dedis_student_21_pop_be2",

  "sonar.sources" -> "src/main/scala",
  "sonar.tests" -> "src/test/scala",

  "sonar.sourceEncoding" -> "UTF-8",
  "sonar.scala.version" -> "2.13.7",
  // Paths to the test and coverage reports
  "sonar.scala.coverage.reportPaths" -> "./target/scala-2.13/scoverage-report/scoverage.xml",
  "sonar.scala.scapegoat.reportPaths" -> "./target/scala-2.13/scapegoat-report/scapegoat.xml"
)

assemblyMergeStrategy in assembly := {
    case PathList("module-info.class") => MergeStrategy.discard
    case PathList("reference.conf") => MergeStrategy.concat
    case PathList("META-INF","MANIFEST.MF") => MergeStrategy.discard
    case _ => MergeStrategy.defaultMergeStrategy("")
}

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
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.9" % Test

// Jackson Databind (for Json Schema Validation)
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.0-RC3"

// Json Schema Validator
libraryDependencies += "com.networknt" % "json-schema-validator" % "1.0.60"

conflictManager := ConflictManager.latestCompatible
