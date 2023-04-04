name := "zio-sampler"

version := "0.1.0-SNAPSHOT"
scalaVersion := "3.3.0-RC3"
scalacOptions ++= Seq(
  "-Wunused:imports",
  "-Xfatal-warnings"
)

//scalaVersion := "2.13.10"
//scalacOptions ++= Seq(
//  "-Ymacro-annotations",
//  "-Xfatal-warnings"
//)

val `zio-version` = "2.0.10"
val `zio-logging-version` = "2.1.11"
val `zio-http-version` = "0.0.5"
val `zio-redis-version` = "0.1.0"
val `zio-schema-version` = "0.3.1"
val `zio-prelude-version` = "1.0.0-RC18"

val executionRulesByScala213 = Seq(
  ExclusionRule("dev.zio", "zio-schema_2.13"),
  ExclusionRule("dev.zio", "zio-schema-json_2.13"),
)

val executionRulesByScala3 = Seq(
  ExclusionRule("dev.zio", "zio-schema_3"),
  ExclusionRule("dev.zio", "zio-schema-json_3"),
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % `zio-version`,
  "dev.zio" %% "zio-streams" % `zio-version`,
  "dev.zio" %% "zio-macros" % `zio-version`,
  "dev.zio" %% "zio-test" % `zio-version` % Test,
  "dev.zio" %% "zio-test-sbt" % `zio-version` % Test,

  "dev.zio" %% "zio-logging" % `zio-logging-version`,
  "dev.zio" %% "zio-logging-slf4j2" % `zio-logging-version`,

  "dev.zio" %% "zio-metrics-connectors" % "2.0.7" excludeAll (executionRulesByScala3 *),

  "dev.zio" %% "zio-http" % `zio-http-version` excludeAll (executionRulesByScala3 *),
  "dev.zio" %% "zio-http-testkit" % `zio-http-version` % Test excludeAll (executionRulesByScala3 *),

  "dev.zio" %% "zio-redis" % `zio-redis-version`,
  "dev.zio" %% "zio-redis-embedded" % `zio-redis-version`,

  "dev.zio" %% "zio-schema" % `zio-schema-version`,
  "dev.zio" %% "zio-schema-derivation" % `zio-schema-version`,
  "dev.zio" %% "zio-schema-json" % `zio-schema-version`,
  "dev.zio" %% "zio-schema-macros" % `zio-schema-version`,

  "dev.zio" %% "zio-prelude" % `zio-prelude-version`,

  //  "org.mockito" %% "mockito-scala" % "1.17.14",
  "org.mockito" % "mockito-core" % "5.2.0" % Test,
  "dev.zio" %% "zio-mock" % "1.0.0-RC9" % Test,
//  "org.scalamock" %% "scalamock" % "5.1.0" % Test,
)