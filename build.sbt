name := "hnfetch"

version := "1.0"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding",
  "UTF-8",
  "-Xlint",
  "-Xverify",
  "-feature",
  "-Ypartial-unification",
  //,"-Xfatal-warnings"
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  //"-language:_",
  //,"-optimise"
  "-Xlog-implicit-conversions"
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")

val fetchVersion  = "1.0.0"
val ScalaZVersion = "7.3.0-M28"
val ZIOVersion    = "1.0-RC4"

libraryDependencies ++= Seq(
  "org.scalaz"             %% "scalaz-core"             % ScalaZVersion,
  "org.scalaz"             %% "scalaz-zio"              % ZIOVersion,
  "org.scalaz"             %% "scalaz-zio-interop-cats" % ZIOVersion,
  "org.scalaz"             %% "scalaz-zio-interop-scalaz7x" % ZIOVersion,
  "com.47deg"              %% "fetch"                   % fetchVersion,
  "org.scalaj"             %% "scalaj-http"             % "2.3.0",
  "com.lihaoyi"            %% "upickle"                 % "0.4.4",
  "org.ocpsoft.prettytime" % "prettytime"               % "3.2.7.Final",
  "org.scalatest"          %% "scalatest"               % "3.0.1" % "test"
)

libraryDependencies += {
  val version = scalaBinaryVersion.value match {
    case "2.10" => "1.0.3"
    case _      ⇒ "1.6.6"
  }
  "com.lihaoyi" % "ammonite" % version % "test" cross CrossVersion.full
}

sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main.main(args) }""")
  Seq(file)
}.taskValue

// Optional, required for the `source` command to work
(fullClasspath in Test) ++= {
  (updateClassifiers in Test).value.configurations
    .find(_.configuration == Test.name)
    .get
    .modules
    .flatMap(_.artifacts)
    .collect { case (a, f) if a.classifier == Some("sources") => f }
}

sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.0")

// if your project uses multiple Scala versions, use this for cross building
addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.0" cross CrossVersion.binary)

// if your project uses both 2.10 and polymorphic lambdas
libraryDependencies ++= (scalaBinaryVersion.value match {
  case "2.10" =>
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full) :: Nil
  case _ =>
    Nil
})