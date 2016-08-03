name := "akka-stream-java8"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.6"

//resolvers += "Gamlor-Repo" at "https://github.com/gamlerhart/gamlor-mvn/raw/master/snapshots"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.6",
  "com.typesafe.akka" % "akka-kernel_2.11" % "2.4.8",


  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,

  "org.mongodb" % "mongo-java-driver" % "2.10.1",
  "org.bitbucket.dollar" % "dollar" % "1.0-beta2"
//  "info.gamlor.akkaasync"  %% "akka-io_2.9.1" % "1.0-SNAPSHOT"
)

fork in run := true
