import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform._
import spray.revolver.RevolverPlugin._
import wartremover._
import sbtassembly.AssemblyPlugin.autoImport._

object BuildSettings {
  val buildOrganization = "cfpb"
  val buildVersion      = "0.0.1"
  val buildScalaVersion = "2.11.7"

  val buildSettings = Defaults.coreDefaultSettings ++
    scalariformSettings ++
    wartremoverSettings ++
    Defaults.itSettings ++
    Seq(
      organization  := buildOrganization,
      version       := buildVersion,
      scalaVersion  := buildScalaVersion,
      //wartremoverWarnings ++= Warts.allBut(Wart.NoNeedForMonad, Wart.NonUnitStatements),
      scalacOptions ++= Seq(
        "-Xlint",
        "-deprecation",
        "-unchecked",
        "-feature")
    )
}

object GrasshopperBuild extends Build {
  import Dependencies._
  import BuildSettings._

  val commonDeps = Seq(logback, scalaLogging, scalaTest, scalaCheck)

  val akkaDeps = commonDeps ++ Seq(akkaActor, akkaStreams, akkaTestKit, akkaStreamsTestkit)

  val jsonDeps = commonDeps ++ Seq(akkaHttpJson)

  val akkaHttpDeps = akkaDeps ++ jsonDeps ++ Seq(akkaHttp, akkaHttpCore, akkaHttpTestkit)

  val esDeps = commonDeps ++ Seq(es, scaleGeoJson)

  val scaleDeps = Seq(scaleGeoJson)

  val metricsDeps = Seq(config, metricsScala, metricsJvm, influxDbReporter)

  val geocodeDeps = akkaHttpDeps ++ esDeps ++ scaleDeps ++ metricsDeps

  val asyncDeps = Seq(async)

  val mfgLabs = Seq(mfglabs)

    
  lazy val grasshopper = (project in file("."))
    .settings(buildSettings: _*)
    .aggregate(geocoder, model, client, test_harness)


  lazy val elasticsearch = (project in file("elasticsearch"))
    .configs( IntegrationTest )
    .settings(buildSettings: _*)
    .settings(
      Seq(
        libraryDependencies ++= esDeps,
        resolvers ++= repos
      )
    )

  lazy val metrics = (project in file("metrics"))
    .configs(IntegrationTest)
    .settings(buildSettings: _*)
    .settings(
      Revolver.settings ++
      Seq(
        assemblyJarName in assembly := {s"grasshopper-${name.value}.jar"},
        libraryDependencies ++= metricsDeps,
        resolvers ++= repos
      )
    )


  lazy val client = (project in file("client"))
    .configs( IntegrationTest )
    .settings(buildSettings: _*)
    .settings(
        Seq(
          assemblyJarName in assembly := {s"grasshopper-${name.value}.jar"},
          libraryDependencies ++= akkaHttpDeps ++ scaleDeps ++ asyncDeps
        )
    ).dependsOn(model)

  lazy val geocoder = (project in file("geocoder"))
    .configs( IntegrationTest )
    .settings(buildSettings: _*)
    .settings(
      Revolver.settings ++
      Seq(
        assemblyJarName in assembly := {s"grasshopper-${name.value}.jar"},
        assemblyMergeStrategy in assembly := {
          case "application.conf" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        },
        libraryDependencies ++= geocodeDeps,
        resolvers ++= repos
      )
    ).dependsOn(client, metrics, elasticsearch)


  lazy val model = (project in file("model"))
    .configs(IntegrationTest)
    .settings(buildSettings: _*)
    .settings(
      Seq(
         assemblyJarName in assembly := {s"grasshopper-${name.value}.jar"},
         assemblyMergeStrategy in assembly := {
          case "application.conf" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        },
        libraryDependencies ++= jsonDeps ++ scaleDeps
      )
    )

  lazy val hmdaGeo = ProjectRef(uri("git://github.com/cfpb/hmda-geo.git"), "client")

  lazy val test_harness = (project in file("test-harness"))
    .configs(IntegrationTest)
    .settings(buildSettings: _*)
    .settings(
      Seq(
        assemblyJarName in assembly := {s"grasshopper-${name.value}.jar"},
        assemblyMergeStrategy in assembly := {
          case "application.conf" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        },
        libraryDependencies ++= akkaHttpDeps ++ scaleDeps ++ mfgLabs,
        resolvers ++= repos
      )
    )
    .dependsOn(geocoder, hmdaGeo)

}
