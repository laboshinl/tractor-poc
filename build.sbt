name := "tractor-poc"
organization := "ru.ownrobot"
description := "Trafic analysis with MapReduce based on akka actors"

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.6"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,


  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-jackson-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaVersion,

  "com.typesafe.akka" % "akka-kernel_2.11" % "2.4.8",


  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,

  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "junit" % "junit" % "4.12",

  "org.mongodb" % "mongo-java-driver" % "2.10.1",
  "org.bitbucket.dollar" % "dollar" % "1.0-beta2",
  "com.google.protobuf" % "protobuf-java" % "2.5.0",
  "com.google.guava" % "guava" % "19.0",
  "org.apache.velocity" % "velocity" % "1.7",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",

"com.lowagie" % "itext" % "2.1.7",
"org.jfree" % "jfreechart" % "1.0.19",
"jfree" % "jcommon" % "1.0.16",
"com.itextpdf" % "itextpdf" % "5.5.9"
)

fork in run := true


