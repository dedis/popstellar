lazy val V = _root_.scalafix.sbt.BuildInfo
lazy val rulesCrossVersions = Seq(V.scala213)
lazy val scala3Version = "3.3.1"

inThisBuild(
  List(
    organization := "io.github.dedis",
      organizationName := "dedis",
    organizationHomepage := Some(url("https://dedis.epfl.ch")),
    homepage := Some(url("https://github.com/dedis/popstellar")),
    licenses := List("AGPL 3.0" -> url("https://www.gnu.org/licenses/agpl-3.0.en.html")),
      developers := List(Developer("t1b00", "Thibault Czarniak", "thibault.czarniak@epfl.ch", url("https://www.linkedin.com/in/thcz/"))),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
      scmInfo := Some(ScmInfo(url("https://github.com/dedis/popstellar"), "scm:git@github:dedis/popstellar.git")),
      version := "1.0",
      versionScheme := Some("pvp"),
  )
)

lazy val `popstellar` = (project in file("."))
  .aggregate(
    rules.projectRefs ++
      input.projectRefs ++
      output.projectRefs ++
      tests.projectRefs: _*
  )
  .settings(
    publish / skip := true
  )
lazy val rules = projectMatrix
  .settings(
    moduleName := "scapegoat-scalafix",
    libraryDependencies += "ch.epfl.scala" % "scalafix-core_2.13" % V.scalafixVersion,
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions :+ scala3Version)

lazy val input = projectMatrix
  .settings(
    publish / skip := true
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

lazy val output = projectMatrix
  .settings(
    publish / skip := true
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

lazy val testsAggregate = Project("tests", file("target/testsAggregate"))
  .aggregate(tests.projectRefs: _*)
  .settings(
    publish / skip := true
  )

lazy val tests = projectMatrix
  .settings(
    publish / skip := true,
      scalaVersion := V.scala213,
    scalafixTestkitOutputSourceDirectories :=
      TargetAxis
        .resolve(output, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputSourceDirectories :=
      TargetAxis
        .resolve(input, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputClasspath :=
      TargetAxis.resolve(input, Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions :=
      TargetAxis.resolve(input, Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      TargetAxis.resolve(input, Compile / scalaVersion).value
  )
  .defaultAxes(
    rulesCrossVersions.map(VirtualAxis.scalaABIVersion) :+ VirtualAxis.jvm: _*
  )
  .jvmPlatform(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(V.scala213)),
    settings = Seq()
  )
  .jvmPlatform(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(scala3Version)),
    settings = Seq()
  )

  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
