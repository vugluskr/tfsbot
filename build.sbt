name := "tfs"

version := "1.1"
maintainer := "denis@danilin.name"

lazy val `tfs` = (project in file(".")).enablePlugins(PlayMinimalJava)

javacOptions ++= Seq("-Xlint:all")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  guice, javaJdbc, javaWs, ws,
  "org.postgresql" % "postgresql" % "42.1.4",
  "org.mybatis" % "mybatis" % "3.5.2",
  "org.mybatis" % "mybatis-guice" % "3.10+",
  "com.google.inject.extensions" % "guice-multibindings" % "4.1.0",
)

updateOptions := updateOptions.value.withCachedResolution(true)
routesGenerator := InjectedRoutesGenerator

unmanagedResourceDirectories in Compile += (baseDirectory.value / "conf")

// tests

libraryDependencies += "org.assertj" % "assertj-core" % "3.6.2" % Test
libraryDependencies += "org.awaitility" % "awaitility" % "2.0.0" % Test
fork in Test := false

testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))
javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
