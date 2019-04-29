package examples

import fetch._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._

import scala.concurrent._
import java.util.concurrent._

import scala.concurrent.duration._
import cats._
import cats.effect._
import cats.instances.list._
import cats.data.NonEmptyList
import cats.syntax.all._
import fetch._

import scala.util.Try

import scala.language.higherKinds


object One extends Data[Int, Int] {
  def name = "One"

  def source[F[_] : ConcurrentEffect]: DataSource[F, Int, Int] = new DataSource[F, Int, Int] {
    override def data = One

    override def CF = ConcurrentEffect[F]

    override def fetch(id: Int): F[Option[Int]] =
      CF.pure(Option(id))

    override def batch(ids: NonEmptyList[Int]): F[Map[Int, Int]] =
      CF.pure(
        ids.toList.map((v) => (v, v)).toMap
      )
  }
}


object FrontPageWithFetchCats {

  import HNDataSources._

  val executor = new ScheduledThreadPoolExecutor(4)
  val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)

  implicit val timer: Timer[IO] = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  def one[F[_] : ConcurrentEffect](id: Int): Fetch[F, Int] =
    Fetch(id, One.source)


//  def fetchTraverse[F[_] : ConcurrentEffect](items : List[HNItemID]) : Fetch[F, List[HNItem]] =
//    items.traverse(getItem[F])

  def fetch[F[_] : ConcurrentEffect]: Fetch[F, List[Int]] =
    List(1,2,3).traverse(a => one(a))


  // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage[F[_] : ConcurrentEffect](startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: DataCache[F]) : F[(DataCache[F], List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

//    val fetchItems = pageOfItems.traverse(getItem[F])
//
//    Fetch.runCache(fetchItems, cache)

    ???
  }

  def printError(err: String) = IO {
    println(s"Error: $err")
  }

  // Print a page of fetched items
  def printPageItems[F[_] : ConcurrentEffect](startPage: Int, numItemsPerPage: Int, items: List[HNItem]) = {
    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    Sync[F].delay {

      items.zipWithIndex.foreach {
        case (item, n) =>
          println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)}")
          println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
      }
    }
  }

  // Simple input and output is encoded as Monix Task so we can compose all the pieces
  // to get the final
  def promptInput[F[_] : ConcurrentEffect] = Sync[F].delay {
    println("Enter a page number to fetch a page of news items or anything else to quit: ")
  }

  def getNumericInput[F[_] : ConcurrentEffect] = Sync[F].delay {
    Try {
      val input = readLine()
      input.toInt
    }.toOption
  }

  def printTopItemCount[F[_] : ConcurrentEffect](topItems: HNItemIDList) = Sync[F].delay {
    println(s"Got ${topItems.size} items")
  }

  val numItemsPerPage = 10

  def getUserPage[F[_] : ConcurrentEffect] : F[Option[Int]] = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

  def showPagesLoop(topItems: HNItemIDList, cache: DataCache[IO]) : IO[DataCache[IO]] =

  // Here we will show the page of items or exit if the user didn't enter a number
    getUserPage[IO].flatMap {

      case Some(page) =>

        for (
          //_ <- IO.pure(println(s"fetch page $page"));
          fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
          (cache, items) = fetchResult;
          //_ = println(s"${env.rounds.size} fetch rounds");
          _ <- printPageItems(page, numItemsPerPage, items);
          newCache <- showPagesLoop(topItems, cache)
        ) yield newCache

      case None =>
        IO.pure(cache)
    }

  // Set a fixed size pool with a small number of threads so we can be nice to the Hacker News servers by
  // limiting the number of concurrent requests
  //val scheduler = monix.execution.Scheduler.fixedPool("monix-pool", 4, true)

  def main(args : Array[String]) : Unit = {

    // Finally the main program consists of getting the list of top item IDs and then calling the loop ...

    val itemID = 13867316

    //implicit val ec = runtime.Platform.executor

    val cache = InMemoryCache.from[IO, HNItemID, HNItem]()

//    val f1 = Fetch.runCache[Task](fetchItem, cache).flatMap {
//      case (_, item) =>
//        putStrLn(s"Item author ${item.by}")
//    }

    val wassup = Fetch.runCache[IO](HNDataSources.getItem(itemID), cache).unsafeRunTimed(5.seconds)
    wassup.map{case (c, a) => println(s"got it ${a}")}

    def program[IO[_] : ConcurrentEffect] = getTopItems().flatMap {
      case Right(items) =>
        showPagesLoop(items, cache)
      case Left(err) =>
        printError(err)
    }
//
//    runtime.unsafeRun(program)
    //
    //    val ran = program.runAsync(scheduler)
    //    Await.result(ran, Duration.Inf)

  }

}
