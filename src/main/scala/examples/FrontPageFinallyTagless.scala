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
import justinhj.hnfetch.HNFetch._

import scala.concurrent.ExecutionContext

object FrontPageFinallyTagless {

  trait Logging[F[_]] {
    def log(s : String) : F[Unit]
  }

  trait HttpClient[F[_]] {
    def get(url: String)(L : Logging[F]) : F[String]
  }

  trait Parsing[F[_]] {
    def parse[A](json: String)(implicit D: Decoder[A]) : F[Either[String, A]]
  }

  // Implement parsing using Circe

  val circeParser: Parsing[IO] = new Parsing[IO] {
    def parse[A](json: String)(implicit D: Decoder[A]) : IO[Either[String, A]] = {

      if(json == "null") Left("Object not found (server returned null)").pure[IO]
      else {
        decode[A](json) match {
          case Right(a) =>
            IO.pure(Right(a))
          case Left(err) =>
            IO.pure(Left(err.toString))
        }
      }
    }
  }

  // A full Hacker News API Client using tagless final style
  case class HNApi[F[_] : Monad](L : Logging[F], H : HttpClient[F], P : Parsing[F]) {

    // All functions are written in terms of this one
    def exec[A](url: String)(implicit D: Decoder[A]) : F[Either[String, A]] = {

      for (
        _ <- L.log(s"Fetching $url");
        body <- H.get(url)(L);
        _ <- L.log(s"Parsing ${body.length}  bytes");
        parsed <- P.parse[A](body)
      ) yield parsed

    }

    def getUser(userID: HNUserID) : F[Either[String, HNUser]] = exec[HNUser](getUserURL(userID))

    def getItem(itemID: HNItemID) : F[Either[String, HNItem]] = exec[HNItem](getItemURL(itemID))

    def getTopItems() : F[Either[String, HNItemIDList]] = exec[HNItemIDList](getTopItemsURL)


  }

  def fetch[F[_] : Monad](url : String)(F: HttpClient[F], L: Logging[F]) : F[String] = {

    for(
      _ <- L.log(s"Fetching $url");
      result <- F.get(url)(L)
    ) yield result

  }

  // An implementation of fetch API to string that uses Cats IO and calls the actual API
  def fetchAPI(ec: ExecutionContext) : HttpClient[IO] = new HttpClient[IO] {

    def get(url: String)(L: Logging[IO]) : IO[String] = {
      val result = for (
        _ <- IO.shift(ec);
        response <- IO(customHttp(url).asString);
        body = if(response.is2xx) response.body else "error" // TODO better error handling, need the error monad here
      ) yield body

      result
    }

  }

  // This second implementation is used for mocking. The developer maintains a map of url to expected output
  // so we can write unit tests around it
  val mockFetchAPI : HttpClient[IO] = new HttpClient[IO] {

    def get(url: String)(L: Logging[IO]) : IO[String] = {

      val reqResponseMap = Map[String, String](
        getMaxItemURL -> "1000"
      )

      IO(reqResponseMap.getOrElse(url, "not found"))

    }

  }

  // A third implementation uses Id instead IO
  val idMockFetchAPI : HttpClient[Id] = new HttpClient[Id] {

    def get(url: String)(L: Logging[Id]) : Id[String] = {

      val reqResponseMap = Map[String, String](
        getMaxItemURL -> "1000"
      )

      reqResponseMap.getOrElse(url, "not found").pure[Id]

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

    val fetchFromReal = fetch[IO](getMaxItemURL)(fetchReal, printlnLogging)

    // Now run it

    val result = fetchFromReal.unsafeRunSync()

    println(s"Result:\n$result")

    // Run with the IO Monad still but this time use the mock fetch

    val fetchFromMock = fetch[IO](getMaxItemURL)(mockFetchAPI, printlnLogging)

    // Now run it

    val result2 = fetchFromMock.unsafeRunSync()

    println(s"Result 2:\n$result2")

    // Show how we can mix our monads and use the Id logger alongside an IO program
//
//    implicit val idToIO: FunctionK[Id, IO] = new FunctionK[Id, IO] {
//      def apply[A](id: Id[A]): IO[A] = id.pure[IO]
//    }
//
//    val fetchFromMockIdLogging = fetch[IO](getMaxItemURL)(idMockFetchAPI, printlnLogging)
//
//    // Now run it
//
//    val result3 = fetchFromMockIdLogging.unsafeRunSync()
//
//    println(s"Result 3:\n$result3")

    // Get user by ID real API

    val hnAPI = HNApi[IO](printlnLogging, fetchReal, circeParser)

    val getJustin = hnAPI.getUser("justinhj")
    val getJustinItem = hnAPI.getItem(11498534)
    val getTopItems = hnAPI.getTopItems()

    val fetchRealProgram: IO[(Either[String, HNUser], Either[String, HNItem], Either[String, HNItemIDList])] = for (
      topItems <- getTopItems;
      user <- getJustin;
      item <- getJustinItem
    ) yield (user, item, topItems)

    val results = fetchRealProgram.unsafeRunSync()

    val ops = List(getJustin, getJustinItem, getTopItems)

    //val what: IO[List[Either[String, Any]]] = ops.sequence[IO, Either[String, Any]]
    //val runList: List[Either[String, Any]] = what.unsafeRunSync()

    println(s"Found ${results._3.map(_.size)}")

    threadPool.shutdown()

  }


}
