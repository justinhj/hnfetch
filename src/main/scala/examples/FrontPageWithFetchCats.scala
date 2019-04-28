package examples

import fetch._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._
import java.util.concurrent._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn.readLine
import scala.util.Try
import cats.effect._
import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._


object FrontPageWithFetchCats {

  val executor = new ScheduledThreadPoolExecutor(4)
  val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)

  implicit val timer: Timer[IO] = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)


  /*
  // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage[F[_] : ConcurrentEffect](startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: DataCache[F]) : F[(DataCache[F], List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)

    Fetch.runCache(fetchItems, cache)
  }

  // Print a page of fetched items
  def printPageItems[F[_]](startPage: Int, numItemsPerPage: Int, items: List[HNItem]) = {
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
  def promptInput[F[_]] = Sync[F].delay {
    println("Enter a page number to fetch a page of news items or anything else to quit: ")
  }

  def getNumericInput[F[_]] = Sync[F].delay {
    Try {
      val input = readLine()
      input.toInt
    }.toOption
  }

  def printTopItemCount[F[_]](topItems: HNItemIDList) = Sync[F].delay {
    println(s"Got ${topItems.size} items")
  }

  def printError[F[_]](err: String) = Sync[F].delay {
    println(s"Error: $err")
  }

  val numItemsPerPage = 10

  def getUserPage[F[_]] = Sync[F].delay( for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page)

  def showPagesLoop[F[_] : ConcurrentEffect](topItems: HNItemIDList, cache: DataCache[F]) =

  // Here we will show the page of items or exit if the user didn't enter a number
    getUserPage[F].flatMap {

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
        Sync.pure(cache)
    }


*/

  // Set a fixed size pool with a small number of threads so we can be nice to the Hacker News servers by
  // limiting the number of concurrent requests
  //val scheduler = monix.execution.Scheduler.fixedPool("monix-pool", 4, true)

  def main(args : Array[String]) : Unit = {

    // Finally the main program consists of getting the list of top item IDs and then calling the loop ...

    val itemID = 13867316

    //implicit val ec = runtime.Platform.executor

    val fetchItem = HNDataSources.getItem(itemID)

    val cache = InMemoryCache.from[IO, HNItemID, HNItem]()

//    val f1 = Fetch.runCache[Task](fetchItem, cache).flatMap {
//      case (_, item) =>
//        putStrLn(s"Item author ${item.by}")
//    }

    Fetch.run(fetchItem).unsafeRunTimed(5.seconds)

//    val program = getTopItems().flatMap {
//      case Right(items) =>
//        showPagesLoop(items, cache)
//      case Left(err) =>
//        printError(err)
//    }
//
//    runtime.unsafeRun(program)
    //
    //    val ran = program.runAsync(scheduler)
    //    Await.result(ran, Duration.Inf)

  }

}
