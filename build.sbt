name := "hnfetch"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.47deg" %% "fetch" % "0.7.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "co.fs2" %% "fs2-core" % "0.10.0-M8",
  "com.spinoco" %% "fs2-kafka" % "0.1.2",
  "com.lihaoyi" %% "upickle" % "0.4.4",
  "org.typelevel" %% "cats-core" % "1.0.0-RC1",
  "org.typelevel" %% "cats-effect" % "0.5",
  "org.ocpsoft.prettytime" % "prettytime" % "3.2.7.Final",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.lihaoyi" % "ammonite" % "1.0.0" % "test" cross CrossVersion.full
)

sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue