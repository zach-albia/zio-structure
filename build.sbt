name := "zio-structure"

version := "0.1"

scalaVersion := "2.12.10"

val zioVersion = "1.0.0-RC17+430-2e51f657-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.h2database"     % "h2"                % "1.4.200",
  "com.rms.miu"        %% "slick-cats"       % "0.10.1",
  "com.typesafe.slick" %% "slick"            % "3.3.2",
  "dev.zio"            %% "zio"              % zioVersion,
  "dev.zio"            %% "zio-streams"      % zioVersion,
  "dev.zio"            %% "zio-interop-cats" % "2.0.0.0-RC10",
  "org.typelevel"      %% "cats-core"        % "2.0.0"
)

resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
