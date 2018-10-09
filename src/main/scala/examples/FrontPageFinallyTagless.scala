package examples

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.Executors

import cats._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import justinhj.hnfetch.HNFetch.{HNUser, HNUserID}
import justinhj.hnfetch._

import scala.concurrent.ExecutionContext

object FrontPageFinallyTagless {

  trait Logging[F[_]] {
    def log(s : String) : F[Unit]
  }

  trait HttpClient[F[_]] {
    def get(url: String) : F[String]
  }

  trait Parsing[F[_]] {
    def parse[A](json: String)(implicit D: Decoder[A]) : F[Either[String, A]]
  }

  // Implement parsing using Circe

  val circeParser = new Parsing[IO] {
    def parse[A](json: String)(implicit D: Decoder[A]) : IO[Either[String, A]] = {

      decode[A](json) match {
        case Right(a) =>
          IO.pure(Right(a))
        case Left(err) =>
          IO.pure(Left(err.toString))
      }

    }
  }

  // A full Hacker News API Client using tagless final style
  class HNApi[F[_] : Monad](L : Logging[F], H : HttpClient[F], P : Parsing[F]) {

    // All functions are written in terms of this one
    def exec[A](url: String)(implicit D: Decoder[A]) : F[Either[String, A]] = {

      for (
        _ <- L.log(s"Fetching $url");
        result <- H.get(url);
        _ <- L.log(s"Parsing ${result.size}  bytes");
        parsed <- P.parse[A](result)
      ) yield parsed

    }

    def getUser(userID: HNUserID) : F[Either[String, HNUser]] = {
      exec[HNUser](HNFetch.getUserURL(userID))
    }

  }

  def fetch[F[_] : Monad](url : String)(F: HttpClient[F], L: Logging[F]) : F[String] = {

    for(
      _ <- L.log(s"Fetching $url");
      result <- F.get(url)
    ) yield result

  }

  // An implementation of fetch API to string that uses Cats IO and calls the actual API
  def fetchAPI(ec: ExecutionContext) = new HttpClient[IO] {

    def get(url: String) : IO[String] = {
      val result = for (
        _ <- IO.shift(ec);
        response <- IO(HNFetch.customHttp(url).asString);
        body = if(response.is2xx) response.body else "error" // TODO better error handling
      ) yield body

      result
    }

  }

  // This second implementation is used for mocking. The developer maintains a map of url to expected output
  // so we can write unit tests around it
  val mockFetchAPI = new HttpClient[IO] {

    def get(url: String) : IO[String] = {

      val reqResponseMap = Map[String, String](
        HNFetch.getMaxItemURL -> "1000"
      )

      IO(reqResponseMap.getOrElse(url, "not found"))

    }

  }

  // A third implementation uses Id instead IO
  val idMockFetchAPI = new HttpClient[Id] {

    def get(url: String) : Id[String] = {

      val reqResponseMap = Map[String, String](
        HNFetch.getMaxItemURL -> "1000"
      )

      (reqResponseMap.getOrElse(url, "not found")).pure[Id]

    }

  }

  val printlnLogging : Logging[IO] = new Logging[IO] {

    def log(s: String): IO[Unit] = {
      val now : java.util.Date = Calendar.getInstance.getTime
      val timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

      IO(println(timeFormat.format(now) + ": " + s))
    }

  }

  def main(args: Array[String]): Unit = {

    // Fetch from the real API using IO Monad

    val threadPool = Executors.newFixedThreadPool(4)
    val ec = ExecutionContext.fromExecutor(threadPool)
    val fetchReal = fetchAPI(ec)

    val fetchFromReal = fetch[IO](HNFetch.getMaxItemURL)(fetchReal, printlnLogging)

    // Now run it

    val result = fetchFromReal.unsafeRunSync()

    println(s"Result:\n$result")

    // Run with the IO Monad still but this time use the mock fetch

    val fetchFromMock = fetch[IO](HNFetch.getMaxItemURL)(mockFetchAPI, printlnLogging)

    // Now run it

    val result2 = fetchFromMock.unsafeRunSync()

    println(s"Result 2:\n$result2")


    // Get user by ID real API

    val fetchRealProgram = new HNApi[IO](printlnLogging, fetchReal, circeParser).getUser("justinhj")

    println(fetchRealProgram.unsafeRunSync())

    threadPool.shutdown()

  }


}
