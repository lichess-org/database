scalaVersion := "2.13.2"
name := "lichess-db"
organization := "org.lichess"
version := "1.3"
resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"

val akkaVersion = "2.6.4"

libraryDependencies += "org.reactivemongo"  %% "reactivemongo"            % "0.20.3"
libraryDependencies += "org.reactivemongo"  %% "reactivemongo-akkastream" % "0.20.3"
libraryDependencies += "org.scalaz"         %% "scalaz-core"              % "7.2.30"
libraryDependencies += "com.github.ornicar" %% "scalalib"                 % "6.8"
libraryDependencies += "org.lichess"        %% "scalachess"               % "9.2.1"
libraryDependencies += "com.typesafe.akka"  %% "akka-actor"         % akkaVersion
libraryDependencies += "com.typesafe.akka"  %% "akka-stream"         % akkaVersion
libraryDependencies += "com.typesafe.akka"  %% "akka-slf4j"               % akkaVersion
/* libraryDependencies += "ch.qos.logback"     % "logback-classic"           % "1.2.3" */
libraryDependencies += "joda-time"          % "joda-time"                 % "2.10.5"
libraryDependencies += "org.lichess"        %% "compression"              % "1.5"
