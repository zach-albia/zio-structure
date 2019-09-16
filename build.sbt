name := "zio-structure"

version := "0.1"

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.0-RC12-1",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC3",
  "org.typelevel" %% "cats-effect" % "2.0.0-RC2"
)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
