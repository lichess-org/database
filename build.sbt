inThisBuild(
  Seq(
    scalaVersion  := "3.8.4",
    versionScheme := Some("early-semver"),
    version       := "2.0",
    run / javaOptions += "-Dconfig.override_with_env_vars=true"
  )
)
lazy val app = project
  .in(file("."))
  .settings(
    name         := "lichess-db",
    organization := "org.lichess",
    scalacOptions -= "-Xfatal-warnings",
    scalacOptions ++= Seq(
      "-source:future",
      "-rewrite",
      "-new-syntax",
      "-explain",
      "-Wunused:all",
      "-release:21"
    ),
    resolvers ++= Seq(
      "lila-maven".at("https://raw.githubusercontent.com/lichess-org/lila-maven/master"),
      "jitpack".at("https://jitpack.io")
    ),
    // resolvers += Resolver.sonatypeOssRepo("public"),
    libraryDependencies ++= Seq(
      "org.reactivemongo"                 %% "reactivemongo"            % "1.1.0-RC15",
      "org.reactivemongo"                 %% "reactivemongo-akkastream" % "1.1.0-RC15",
      "com.github.lichess-org.scalalib"   %% "scalalib-core"            % "11.10.9",
      "com.github.lichess-org.scalachess" %% "scalachess"               % "17.16.0",
      "com.typesafe.akka"                 %% "akka-actor"               % "2.6.21",
      "com.typesafe.akka"                 %% "akka-stream"              % "2.6.21",
      "com.typesafe.akka"                 %% "akka-slf4j"               % "2.6.21",
      "org.playframework"                 %% "play-json"                % "3.0.6",
      "com.github.lichess-org"             % "compression"              % "3.2.1",
      "org.slf4j"                          % "slf4j-nop"                % "2.0.18"
      // "ch.qos.logback"      % "logback-classic"          % "1.4.14"
    )
  )
