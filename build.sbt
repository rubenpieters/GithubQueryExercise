scalaVersion := "2.11.8"

val circeVersion = "0.5.4"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.7.2"
  ,"com.chuusai" %% "shapeless" % "2.3.2"

  // HTTP
  ,"com.typesafe.play" %% "play-ws" % "2.4.3"

  // JSON
  ,"io.circe" %% "circe-core" % circeVersion
  ,"io.circe" %% "circe-generic" % circeVersion
  ,"io.circe" %% "circe-parser" % circeVersion

  // LOGGING
  ,"ch.qos.logback" % "logback-core" % "1.1.7"
  ,"ch.qos.logback" % "logback-classic" % "1.1.7"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.0.1" cross CrossVersion.full)