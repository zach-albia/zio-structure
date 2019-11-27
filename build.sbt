name := "zio-structure"

version := "0.2"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "com.h2database"     % "h2"                % "1.4.187",
  "com.rms.miu"        %% "slick-cats"       % "0.9.1",
  "com.typesafe.slick" %% "slick"            % "3.3.0",
  "dev.zio"            %% "zio"              % "1.0.0-RC17",
  "dev.zio"            %% "zio-streams"      % "1.0.0-RC17",
)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
