name := """flimey-core"""

organization := "io.flimey"
version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-slick" % "4.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",

  "mysql" % "mysql-connector-java" % "8.0.15",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.1"
)