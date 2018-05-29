
enablePlugins(AkkaGrpcPlugin)

name := "akka-grpc-server-example"

scalaVersion := "2.12.4"

organization := "org.akka-js"

akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server)

libraryDependencies ++= Seq()

inConfig(Compile)(Seq(
  PB.protoSources += sourceDirectory.value / "protobuf"
))
