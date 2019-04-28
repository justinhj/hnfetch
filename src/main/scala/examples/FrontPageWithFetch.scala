package examples

import fetch.{DataCache, Fetch, InMemoryCache}
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._

import scala.io.StdIn.readLine
import scala.util.Try
import scalaz.zio.{DefaultRuntime, Task}
import scalaz.zio.interop.catz.implicits._
import scalaz.zio.interop.catz._
import scalaz.zio.console.{putStrLn, _}
import scalaz._, Scalaz._

object FrontPageWithFetch {

  implicit val runtime = new DefaultRuntime {}

  // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: DataCache[Task]) :
    Task[(DataCache[Task], List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    //val ass = Fetch.runCache[Task](fetchUser)

    val fetchItems: List[Fetch[Task, HNItem]] = pageOfItems.map(HNDataSources.getItem[Task])
    val fetchItem: Fetch[Task, HNItem] = HNDataSources.getItem[Task](12)

    // G[_], B => ItemID -> G[B] : G[List[B]] needs applicative in List[B]
    // Fetch[Task, HNItem]

    // F[A], 	A => G[B], 	G[F[B]]
    // List[Option[Int]], Option[List[Int]]

    // Sample traverse
//    val t1 = List(1.some,2.some,3.some).traverse{
//      case Some(a) => Some(a + 1)
//      case None => None
//    }
//    println(t1)

//    object ApplicativeFetch extends Applicative[F[_]] {
//
//    }


    def intTask(n: Int) = Task.succeed(n)

    val l1 = List(1,2,3).traverse(intTask)



    //val fetchItems2 = pageOfItems.traverse(id => HNDataSources.getItem[Task](id))

    val ass = Fetch.runCache[Task](fetchItem, cache)

    ???
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

  def showPagesLoop(topItems: HNItemIDList, cache: DataCache[Task]): Task[DataCache[Task]] =

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




  // Set a fixed size pool with a small number of threads so we can be nice to the Hacker News servers by
  // limiting the number of concurrent requests
  //val scheduler = monix.execution.Scheduler.fixedPool("monix-pool", 4, true)

  def main(args : Array[String]) : Unit = {

    // Finally the main program consists of getting the list of top item IDs and then calling the loop ...

    val itemID = 13867316

    //implicit val ec = runtime.Platform.executor

    val fetchItem = HNDataSources.getItem[Task](itemID)

    val cache = InMemoryCache.from[Task, HNItemID, HNItem]()

    val f1 = Fetch.runCache[Task](fetchItem, cache).flatMap {
      case (_, item) =>
        putStrLn(s"Item author ${item.by}")
    }

    runtime.unsafeRun(f1)

    val program = getTopItems().flatMap {
      case Right(items) =>
        showPagesLoop(items, cache)
      case Left(err) =>
        printError(err)
    }

    runtime.unsafeRun(program)
//
//    val ran = program.runAsync(scheduler)
//    Await.result(ran, Duration.Inf)

  }

}
