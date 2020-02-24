name := "hnfetch"

version := "1.0"

scalaVersion := "2.12.10"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding",
  "UTF-8",
  "-Xlint",
  "-Xverify",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
  //,"-optimise"
  //"-Xlog-implicit-conversions"
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")

val FetchVersion      = "1.2.0"
val ZIOVersion        = "1.0.0-RC17"
val ScalaZVersion     = "7.3.0-M28"
val CatsVersion       = "2.1.0"
val CatsEffectVersion = "2.1.1"

libraryDependencies ++= Seq(
  "org.scalaz"             %% "scalaz-core"      % ScalaZVersion,
  "dev.zio"                %% "zio"              % ZIOVersion,
  "dev.zio"                %% "zio-streams"      % ZIOVersion,
  "dev.zio"                %% "zio-interop-cats" % "2.0.0.0-RC9",
  "com.47deg"              %% "fetch"            % FetchVersion,
  "org.typelevel"          %% "cats-core"        % CatsVersion,
  "org.typelevel"          %% "cats-effect"      % CatsEffectVersion,
  "org.scalaj"             %% "scalaj-http"      % "2.3.0",
  "com.lihaoyi"            %% "upickle"          % "1.0.0",
  "org.ocpsoft.prettytime" % "prettytime"        % "3.2.7.Final",
  "org.scalatest"          %% "scalatest"        % "3.0.1" % "test"
)

libraryDependencies += {
  val version = scalaBinaryVersion.value match {
    case "2.10" => "1.0.3"
    case _ ⇒ "2.0.4"
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
  (updateClassifiers in Test).value
    .configurations
    .find(_.configuration == Test.name)
    .get
    .modules
    .flatMap(_.artifacts)
    .collect{case (a, f) if a.classifier == Some("sources") => f}
}

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
