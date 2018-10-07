package examples

import java.util.concurrent.Executors

import cats._
import cats.effect._
import cats.implicits._
import justinhj.hnfetch._

import scala.concurrent.ExecutionContext

object FrontPageFinallyTagless {

  trait Logging[F[_]] {
    def log(s : String) : F[Unit]
  }

  trait FetchUrl[F[_]] {
    def fetch(url: String) : F[String]
  }

  def fetch[F[_] : Monad](url : String)(F: FetchUrl[F])(L: Logging[F]) : F[String] = {

    for(
      _ <- L.log(s"Fetching $url");
      result <- F.fetch(url)
    ) yield result

  }

  // An implementation of fetch API to string that uses Cats IO and calls the actual API
  def fetchAPI(ec: ExecutionContext) = new FetchUrl[IO] {

    def fetch(url: String) : IO[String] = {
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
  def mockFetchAPI = new FetchUrl[IO] {

    def fetch(url: String) : IO[String] = {

      val reqResponseMap = Map[String, String](
        HNFetch.getMaxItemURL -> "1000"
      )

      IO(reqResponseMap.getOrElse(url, "not found"))

    }

  }

  val printlnLogging = new Logging[IO] {

    def log(s: String): IO[Unit] = {
      IO(println(s))
    }

  }

  def main(args: Array[String]): Unit = {

    // Fetch from the real API using IO Monad

    val threadPool = Executors.newFixedThreadPool(4)
    val ec = ExecutionContext.fromExecutor(threadPool)
    val fetchReal = fetchAPI(ec)

    val fetchFromReal = fetch[IO](HNFetch.getMaxItemURL)(fetchReal)(printlnLogging)

    // Now run it

    val result = fetchFromReal.unsafeRunSync()

    println(s"Result:\n$result")

    // Run with the IO Monad still but this time use the mock fetch

    val fetchFromMock = fetch[IO](HNFetch.getMaxItemURL)(mockFetchAPI)(printlnLogging)

    // Now run it

    val result2 = fetchFromMock.unsafeRunSync()

    println(s"Result 2:\n$result2")

  }


}
