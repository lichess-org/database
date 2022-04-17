scalaVersion := "3.1.2"
name         := "lichess-db"
organization := "org.lichess"
version      := "2.0"
resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"

val akkaVersion          = "2.6.19"
val reactivemongoVersion = "1.1.0-RC3"

libraryDependencies += "org.typelevel"      %% "cats-core"                % "2.7.0"
libraryDependencies += "org.typelevel"      %% "alleycats-core"           % "2.7.0"
libraryDependencies += "org.reactivemongo"  %% "reactivemongo"            % reactivemongoVersion
libraryDependencies += ("org.reactivemongo" %% "reactivemongo-akkastream" % reactivemongoVersion)
  .exclude("com.typesafe.akka", "akka-actor_2.13")
libraryDependencies += "com.github.ornicar" %% "scalalib"         % "8.0.2"
libraryDependencies += "org.lichess"        %% "scalachess"       % "11.0.1"
libraryDependencies += "com.typesafe.akka"  %% "akka-actor-typed" % akkaVersion
// libraryDependencies += "com.typesafe.akka"  %% "akka-stream"              % akkaVersion
// libraryDependencies += "com.typesafe.akka"  %% "akka-slf4j"               % akkaVersion
/* libraryDependencies += "ch.qos.logback"     % "logback-classic"           % "1.2.3" */
libraryDependencies += "joda-time"    % "joda-time"   % "2.10.14"
libraryDependencies += "org.lichess" %% "compression" % "1.7"

scalacOptions := Seq(
  "-encoding",
  "utf-8",
  "-rewrite",
  "-source:future-migration",
  "-indent",
  "-explaintypes",
  "-feature",
  "-language:postfixOps"
  // Warnings as errors!
  // "-Xfatal-warnings",
)
