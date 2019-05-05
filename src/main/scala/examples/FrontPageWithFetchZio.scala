package examples

import fetch._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._
import scalaz.zio.console.Console
import scalaz.zio.console.{getStrLn, putStrLn}
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.implicits._
import scalaz.zio.{DefaultRuntime, Task, ZIO}


object FrontPageWithFetchZio {

  implicit val runtime: DefaultRuntime = new DefaultRuntime {}

  import HNDataSources._

  // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: DataCache[Task]) : Task[(DataCache[Task], List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    getMultipleItems(pageOfItems, cache)
  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items: List[HNItem]): Task[Unit] = {
    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    Task {

      items.zipWithIndex.foreach {
        case (item, n) =>
          println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)}")
          println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
      }
    }
  }

  // Simple input and output is encoded as Monix Task so we can compose all the pieces
  // to get the final
  def promptInput = Task {
    println("Enter a page number to fetch a page of news items or anything else to quit: ")
  }

  def getNumericInput: ZIO[Console, Nothing, Option[Int]] =
    getStrLn.map{_.toInt}.fold (
      _ => None,
      succ => Some(succ)
    )

  def printTopItemCount(topItems: HNItemIDList) = Task {
    println(s"Got ${topItems.size} items")
  }

  def printError(err: String) = Task {
    println(s"Error: $err")
  }

  val numItemsPerPage = 10

  def getUserPage: ZIO[Console, Throwable, Option[Int]] = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

  def showPagesLoop(topItems: HNItemIDList, cache: DataCache[Task]): ZIO[Console, Throwable, DataCache[Task]] =

    // Here we will show the page of items or exit if the user didn't enter a number
    getUserPage.flatMap {

      case Some(page) =>
        println(s"fetch page $page")

        val t1 = for (
          fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
          (cache, items) = fetchResult;
          //_ = println(s"${env.rounds.size} fetch rounds");
          _ <- printPageItems(page, numItemsPerPage, items);
          newCache <- showPagesLoop(topItems, cache)
        ) yield newCache

        t1

      case None =>
        Task.succeed(cache)
    }

  def main(args : Array[String]) : Unit = {

    val cache = InMemoryCache.from[Task, HNItemID, HNItem]()

    val program = getTopItems().flatMap {
      case Right(items) =>
        showPagesLoop(items, cache)
      case Left(err) =>
        printError(err)
    }

    runtime.unsafeRun(program)
  }

}
