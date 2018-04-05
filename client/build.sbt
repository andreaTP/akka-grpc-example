
enablePlugins(AkkaGrpcPlugin)

name := "akka-grpc-client-example"

scalaVersion := "2.12.4"

organization := "org.akka-js"

import akka.grpc.gen.scaladsl.ScalaBothCodeGenerator
(akkaGrpcCodeGenerators in Compile) := Seq(GeneratorAndSettings(ScalaBothCodeGenerator, (akkaGrpcCodeGeneratorSettings in Compile).value))
