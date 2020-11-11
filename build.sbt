name := """flimey-core"""

organization := "io.flimey"
version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-slick" % "4.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",

  "mysql" % "mysql-connector-java" % "8.0.15",

  "org.mindrot" % "jbcrypt" % "0.4"

)