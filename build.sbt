name := "tractor-poc"
organization := "ru.ownrobot"
description := "Trafic analysis with MapReduce based on akka actors"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.6"

mainClass := Some("ru.ownrobot.tractor.ApplicationMain")

//resolvers += "Gamlor-Repo" at "https://github.com/gamlerhart/gamlor-mvn/raw/master/snapshots"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.6",

//  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.6",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.6",
//  "com.typesafe.akka" %% "akka-http-jackson-experimental" % "2.4.6",

  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.6",
"com.typesafe.akka" %% "akka-http-jackson-experimental" % "2.4.6",
"com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.6",
"com.typesafe.akka" %% "akka-http-xml-experimental" % "2.4.6",

  "com.typesafe.akka" % "akka-kernel_2.11" % "2.4.8",


  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,

  "org.mongodb" % "mongo-java-driver" % "2.10.1",
  "org.bitbucket.dollar" % "dollar" % "1.0-beta2",
  "com.google.protobuf" % "protobuf-java" % "2.5.0",
//  "org.rythmengine" % "rythm-engine" % "1.1.5"
//  "com.github.spullara.mustache.java" % "compiler" % "0.9.3"
//  "org.thymeleaf" % "thymeleaf" % "3.0.1.RELEASE"
  "org.apache.velocity" % "velocity" % "1.7"
)

fork in run := true


