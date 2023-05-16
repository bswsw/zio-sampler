val scala33 = "3.3.0-RC3"
val scala213 = "2.13.10"

name := "zio-sampler"

version := "0.1.0-SNAPSHOT"
scalaVersion := scala33
scalacOptions ++= Seq(
//  "-Xlint:unused",
  "-Wunused:imports",
  "-Xfatal-warnings",
)

//crossScalaVersions := List(scala33, scala213)

val `zio-version` = "2.0.13"
val `zio-mock-version` = "1.0.0-RC11"
val `zio-logging-version` = "2.1.12"
val `zio-http-version` = "3.0.0-RC1"
val `zio-kafka-version` = "2.2"
val `zio-redis-version` = "0.2.0"
val `zio-schema-version` = "0.4.10"
val `zio-prelude-version` = "1.0.0-RC18"
val `zio-metrics-connector-version` = "2.0.8"
val `mongo4cats-zio-version` = "0.6.11"

val executionRulesByScala213 = Seq(
  ExclusionRule("dev.zio", "zio-schema_2.13"),
  ExclusionRule("dev.zio", "zio-schema-json_2.13"),
)

val executionRulesByScala3 = Seq(
  ExclusionRule("dev.zio", "zio-schema_3"),
  ExclusionRule("dev.zio", "zio-schema-json_3"),
)

libraryDependencies ++= Seq(
  // zio
  "dev.zio" %% "zio" % `zio-version`,
  "dev.zio" %% "zio-streams" % `zio-version`,
  "dev.zio" %% "zio-macros" % `zio-version`,
  "dev.zio" %% "zio-test" % `zio-version` % Test,
  "dev.zio" %% "zio-test-sbt" % `zio-version` % Test,
  "dev.zio" %% "zio-mock" % `zio-mock-version` % Test,

  // logging
  "dev.zio" %% "zio-logging" % `zio-logging-version`,
  "dev.zio" %% "zio-logging-slf4j" % `zio-logging-version`,
  "ch.qos.logback" % "logback-classic" % "1.4.6",

  // metrics
  "dev.zio" %% "zio-metrics-connectors" % `zio-metrics-connector-version` excludeAll (executionRulesByScala3*),

  // http
  "dev.zio" %% "zio-http" % `zio-http-version`,
  "dev.zio" %% "zio-http-testkit" % `zio-http-version` % Test,

  // kafka
  "dev.zio" %% "zio-kafka" % `zio-kafka-version`,

  // redis
  "dev.zio" %% "zio-redis" % `zio-redis-version`,
  "dev.zio" %% "zio-redis-embedded" % `zio-redis-version`,

  // schema
  "dev.zio" %% "zio-schema" % `zio-schema-version`,
  "dev.zio" %% "zio-schema-derivation" % `zio-schema-version`,
  "dev.zio" %% "zio-schema-json" % `zio-schema-version`,
  "dev.zio" %% "zio-schema-macros" % `zio-schema-version`,

  // prelude
  "dev.zio" %% "zio-prelude" % `zio-prelude-version`,

  // mongo
  "io.github.kirill5k" %% "mongo4cats-zio" % `mongo4cats-zio-version`,
  "io.github.kirill5k" %% "mongo4cats-zio-json" % `mongo4cats-zio-version`,
  "io.github.kirill5k" %% "mongo4cats-zio-embedded" % `mongo4cats-zio-version` % Test
)
