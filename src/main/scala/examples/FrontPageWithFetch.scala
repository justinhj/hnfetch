package examples

import cats.effect.ConcurrentEffect
import fetch.Fetch
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._

import scala.io.StdIn.readLine
import scala.util.Try
import scalaz.zio.{DefaultRuntime, Task}
import scalaz.zio.interop.catz.implicits._
import scalaz.zio.interop.catz._

object FrontPageWithFetch {

  // Fetch a page of Hacker News items with optional cache from a previous call
//  def fetchPage[F[_]](startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataCache[F]] = None) :
//    Task[(FetchEnv, List[HNItem])] = {
//
//    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)
//
//    //val ass = Fetch.runCache[Task](fetchUser)
//
//    val fetchItems = pageOfItems.map(HNDataSources.getItem)
//
//    cache match {
//      case Some(c) =>
//        fetchItems.run[Task](c)
//      case None =>
//        fetchItems.runF[Task]
//    }
//
//  }

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

  def getNumericInput: Task[Option[Int]] = Task {
    Try {
      val input = readLine()
      input.toInt
    }.toOption

  }

  def printTopItemCount(topItems: HNItemIDList) = Task {
    println(s"Got ${topItems.size} items")
  }

  def printError(err: String) = Task {
    println(s"Error: $err")
  }

  val numItemsPerPage = 10

  def getUserPage: Task[Option[Int]] = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

//  def showPagesLoop(topItems: HNItemIDList, cache: Option[DataSourceCache]): Task[Option[DataSourceCache]] =
//
//  // Here we will show the page of items or exit if the user didn't enter a number
//    getUserPage.flatMap {
//
//      case Some(page) =>
//        println(s"fetch page $page")
//
//        for (
//          fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
//          (env, items) = fetchResult;
//          _ = println(s"${env.rounds.size} fetch rounds");
//          _ <- printPageItems(page, numItemsPerPage, items);
//          newCache <- showPagesLoop(topItems, Some(env.cache))
//        ) yield newCache
//
//
//      case None =>
//        Task.now(cache)
//    }

  // Set a fixed size pool with a small number of threads so we can be nice to the Hacker News servers by
  // limiting the number of concurrent requests
  //val scheduler = monix.execution.Scheduler.fixedPool("monix-pool", 4, true)

  def main(args : Array[String]) : Unit = {

    // Finally the main program consists of getting the list of top item IDs and then calling the loop ...

    implicit val runtime = new DefaultRuntime {}

    val itemID = 13867316

    implicit val ec = runtime.Platform.executor

    val ass : Task[Unit] = runtime.Environment.console.putStrLn("ass")

    //val ass2 = ass.concurrentEffect

    //scalaz.zio.interop-cats.instances

    runtime.unsafeRun(ass)

    val fetchItem = HNDataSources.getItem[Task](itemID)
//
    val run = Fetch.run[Task](fetchItem)

    runtime.unsafeRun(run)

//    val program = getTopItems().flatMap {
//      case Right(items) =>
//        showPagesLoop(items, None)
//      case Left(err) =>
//        printError(err)
//    }
//
//    val ran = program.runAsync(scheduler)
//    Await.result(ran, Duration.Inf)

  }

}
