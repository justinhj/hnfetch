package examples

import cats.effect._
import cats.instances.list._
import cats.syntax.all._
import fetch._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._

import scala.concurrent._
import scala.io.StdIn._
import scala.language.higherKinds
import scala.util.Try
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object FrontPageWithFetchCats {

  val executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
    override def newThread(r: Runnable): Thread = {
      val t = Executors.defaultThreadFactory.newThread(r)
      t.setDaemon(true)
      t
    }
  })

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)

  implicit val timer: Timer[IO]     = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  import HNDataSources._

  // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage(
    startPage: Int,
    numItemsPerPage: Int,
    hNItemIDList: HNItemIDList,
    cache: DataCache[IO]
  ): IO[(DataCache[IO], List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    val fetchItems: Fetch[IO, List[HNItem]] = pageOfItems.traverse(getItem[IO])

    Fetch.runCache[IO](fetchItems, cache)
  }

  def printError(err: String) = IO {
    println(s"Error: $err")
  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items: List[HNItem]) = {
    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    IO(items.zipWithIndex.foreach {
      case (item, n) =>
        println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)} ${item.url}")
        println(
          s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n"
        )
    })
  }

  // Simple input and output is encoded as Monix Task so we can compose all the pieces
  // to get the final
  def promptInput = IO {
    println("Enter a page number to fetch a page of news items or anything else to quit: ")
  }

  def getNumericInput = IO {
    Try {
      val input = readLine()
      input.toInt
    }.toOption
  }

  def printTopItemCount[F[_]: ConcurrentEffect](topItems: HNItemIDList) = IO {
    println(s"Got ${topItems.size} items")
  }

  def getUserPage: IO[Option[HNItemID]] =
    for (_    <- promptInput;
         page <- getNumericInput) yield page

  val numItemsPerPage = 10

  def showPagesLoop(topItems: HNItemIDList, cache: DataCache[IO]): IO[DataCache[IO]] =
    // Here we will show the page of items or exit if the user didn't enter a number
    getUserPage.flatMap {

      case Some(page) =>
        val what = for (//_ <- IO.pure(println(s"fetch page $page"));
                        fetchResult    <- fetchPage(page, numItemsPerPage, topItems, cache);
                        (cache, items) = fetchResult;
                        //_ = println(s"${env.rounds.size} fetch rounds");
                        _        <- printPageItems(page, numItemsPerPage, items);
                        newCache <- showPagesLoop(topItems, cache)) yield newCache

        what

      case None =>
        IO.pure(cache)
    }
0
  def main(args: Array[String]): Unit = {

    val cache = InMemoryCache.from[IO, HNItemID, HNItem]()

    val program = getTopItems[IO]().flatMap {
      case Right(items) =>
        showPagesLoop(items, cache)
      case Left(err) =>
        printError(err)
    }

    program.unsafeRunSync()
  }

}
