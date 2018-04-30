name := "hnfetch"

version := "1.0"

scalaVersion := "2.12.2"

val fetchVersion = "0.7.2"

libraryDependencies ++= Seq(
  "com.47deg" %% "fetch" % fetchVersion,
  "com.47deg" %% "fetch-monix" % fetchVersion,
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.spinoco" %% "fs2-kafka" % "0.2.1-SNAPSHOT",
  "com.spinoco" %% "fs2-crypto" % "0.2.1-SNAPSHOT",
  "com.lihaoyi" %% "upickle" % "0.4.4",
  "org.typelevel" %% "cats-core" % "1.1.0",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC",
  "org.ocpsoft.prettytime" % "prettytime" % "3.2.7.Final",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.lihaoyi" % "ammonite" % "1.0.0" % "test" cross CrossVersion.full
)

sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue