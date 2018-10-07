package examples

import cats._
import cats.data._
import cats.mtl._
import cats.implicits._
import cats.mtl.implicits._
import cats.effect._
import upickle.default.{Reader => uPickleReader}
import justinhj.hnfetch.HNFetch._
import scala.concurrent.ExecutionContext
import scala.util.Try
import cats.syntax._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object MTLFetch {

  type Log = Vector[String]

  type Error = String

  implicit val showLog: Show[Log] = Show.show{
    l =>
      "Log\n===\n" ++
      l.foldLeft("") {_ + _ + "\n"}
  }

  case class APIConfig(url: String)

  //type IOProgram = ReaderT[EitherT[WriterT[IO, Log, ?], Error, ?], APIConfig, ?]

  trait URLGetter[F[_]] {
    def getUrl(url: String)(implicit ft : FunctorTell[IO, Log],
                            fr: FunctorRaise[IO, Error],
                            aa: ApplicativeAsk[IO, APIConfig]) : F[String]
  }

  // We want to test the API both against the real site and against a simple virtual site for testing, for this
  // purpose we need two implementations of URLGetter. Note that the effect class F for the real one uses IO
  // whilst the test one can use any effect (Id for example)

//  class HNURLGetter(implicit ec: ExecutionContext) extends URLGetter[ReaderT[EitherT[WriterT[IO, Log, ?], Error, ?], APIConfig, ?]] {
//
//    def getUrl2(url: String)(implicit ft : FunctorTell[IO, Log],
//      fr: FunctorRaise[IO, Error],
//      aa: ApplicativeAsk[IO, APIConfig]): IO[String] = {
//
//      for (
//        _ <- IO.shift(ec);
//        response <- IO(customHttp(url).asString);
//        result <- if(response.is2xx) response.body.pure[IO]
//                  else fr.raise(s"Didn't get code 200. Got (${response.code})")
//
//      ) yield result
//
//    }
//
//    def getUrl(url: String)(implicit ft: FunctorTell[IO, Log], fr: FunctorRaise[IO, Error], aa: ApplicativeAsk[IO, APIConfig]):
//    ReaderT[EitherT[WriterT[IO, Log, _], Error, _], APIConfig, String] = ???
//  }

  // Get a thing of the appropriate type
  // We can log to the tell
  // We can raise errors
  // We can read a config

//  def hnRequest[F[_] : Monad, ResponseType](ug: URLGetter[F])(implicit ft : FunctorTell[F, Log],
//    fr: FunctorRaise[F, Error],
//    aa: ApplicativeAsk[F, APIConfig],
//    //r : uPickleReader[ResponseType],
//    //ug : URLGetter[F]
//                                                       ) : F[Boolean] = {
//
//    for (
//      url <- aa.ask.map(_.url);
//      _ <- ft.tell(Vector(s"Getting $url"));
//      data <- ug.getUrl(url);
//      _ <- ft.tell(Vector(s"Parsing response (${data.size} chars)"))
//
//    ) yield true
//
//
//
//  }

  def main(args: Array[String]): Unit = {

    val threadPool = Executors.newFixedThreadPool(4)

    val ec = ExecutionContext.fromExecutor(threadPool)

    //val urlGetter = new HNURLGetter()(ec)

    //val result = hnRequest[ReaderT[EitherT[WriterT[IO, Log, ?], Error, ?], APIConfig, ?], Boolean](urlGetter)

    //    val sampleLog = Vector("Sample log 1", "Sample log 2")
//
//    println(sampleLog.show)

  }
}



