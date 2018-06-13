name := "auth-api"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf-8",
  "-target:jvm-1.8",
  "-feature"
)

assemblyJarName in assembly := "auth-api.jar"

scalastyleFailOnError := true

coverageEnabled in(Test, compile) := true

coverageEnabled in(Compile, compile) := false

coverageFailOnMinimum := true

libraryDependencies ++= Seq(
  "com.tresata" %% "akka-http-spnego" % "0.3.0",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "ch.megard" %% "akka-http-cors" % "0.3.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.14.0",
  "io.github.twonote" % "radosgw-admin4j" % "1.1.0" exclude("com.google.guava", "guava"),
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test)


assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
