package examples

import cats._
import cats.data._
import cats.mtl._
import cats.implicits._
import cats.mtl.implicits._
import cats.effect.IO
import upickle.default.{Reader => uPickleReader}

object MTLFetch {

  type Log = Vector[String]

    implicit val showLog: Show[Log] = Show.show{
    l =>
      "Log\n===\n" ++
      l.foldLeft("") {_ + _ + "\n"}
  }

  case class APIConfig(url: String)

  trait URLGetter[F[_]] {
    def getUrl(url: String) : F[String]
  }

  // Get a thing of the appropriate type
  // We can log to the tell
  // We can raise errors
  // We can read a config

  def hnRequest[F[_] : Monad, ResponseType](implicit ft : FunctorTell[F, Log],
    aa: ApplicativeAsk[F, APIConfig],
    r : uPickleReader[ResponseType],
    ug : URLGetter[F]) : F[ResponseType] = {

    for (
      url <- aa.ask.map(_.url);
      _ <- ft.tell(Vector(s"Getting $url"));
      data <- ug.getUrl(url);
      _ <- ft.tell(Vector(s"Parsing response (${data.size} chars)"))

    ) yield true

    ???

  }

  def main(args: Array[String]): Unit = {

    val sampleLog = Vector("Sample log 1", "Sample log 2")

    println(sampleLog.show)

  }
}
