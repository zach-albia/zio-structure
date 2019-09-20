name := "zio-structure"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "com.rms.miu"        %% "slick-cats"       % "0.9.1",
  "com.typesafe.slick" %% "slick"            % "3.3.0",
  "dev.zio"            %% "zio"              % "1.0.0-RC12-1",
  "dev.zio"            %% "zio-interop-cats" % "2.0.0.0-RC3",
  "org.typelevel"      %% "cats-effect"      % "2.0.0-RC2",
)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
