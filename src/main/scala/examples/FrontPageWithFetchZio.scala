package examples

import fetch._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._
import zio._
import zio.console._
import zio.interop.cats.implicits._

object FrontPageWithFetchZio {

  implicit val runtime: DefaultRuntime = new DefaultRuntime {}

  import HNDataSources._

  // Fetch a page of Hacker News items using the provided cache
  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: DataCache[Task]) : Task[(DataCache[Task], List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    getMultipleItems(pageOfItems, cache)
  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items: List[HNItem]) = {
    ZIO.foreach(items.zipWithIndex){
      case (item, n) =>
        val itemNum = (startPage * numItemsPerPage) + n + 1
        putStrLn(s"$itemNum. ${item.title} ${Util.getHostName(item.url)}\n  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
    }
  }

  // Simple input and output is encoded as Monix Task so we can compose all the pieces
  // to get the final
  def promptInput = putStrLn("Enter a page number to fetch a page of news items or anything else to quit: ")

  def getNumericInput: ZIO[Console, Nothing, Option[HNItemID]] = {
    (for (
      input <- getStrLn;
      num <- Task.effect(input.toInt)
    ) yield num).fold(err => None, succ => Some(succ))
  }

  def printTopItemCount(topItems: HNItemIDList) =
    putStrLn(s"Got ${topItems.size} items")

  def printError(err: String) =
    putStrLn(s"Error: $err")

  val numItemsPerPage = 10

  def getUserPage: ZIO[Console, Throwable, Option[Int]] = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

  def showPagesLoop(topItems: HNItemIDList, cache: DataCache[Task]): ZIO[Console, Throwable, DataCache[Task]] =

    // Here we will show the page of items or exit if the user didn't enter a number
    getUserPage.flatMap {

      case Some(page) =>

        for (
          _ <- putStrLn(s"fetch page $page");
          fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
          (cache, items) = fetchResult;
          //_ = println(s"${env.rounds.size} fetch rounds");
          _ <- printPageItems(page, numItemsPerPage, items);
          newCache <- showPagesLoop(topItems, cache)
        ) yield newCache

      case None =>
        Task.succeed(cache)
    }

  def main(args : Array[String]) : Unit = {

    val cache = InMemoryCache.from[Task, HNItemID, HNItem]()

    val program = (for(
      items <- ZIO.absolve(getTopItems().mapError(_.getMessage));
      _ <- showPagesLoop(items, cache)
    ) yield ()).foldM(err => printError(err.toString), _ => ZIO.succeed(()))

    runtime.unsafeRun(program)
  }
}