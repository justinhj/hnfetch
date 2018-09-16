package examples

//import cats.instances.list._
//import cats.syntax.traverse._
import fetch._
import fetch.syntax._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._

import scalaz._
import Scalaz._
import scalaz.syntax.traverse._
import scalaz.std.list._
import scalaz.syntax.traverse.ToTraverseOps

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn.readLine
import scala.util.Try

import scalaz.zio._

import scala.concurrent.ExecutionContext

object FrontPageFetchScalaz {


  // Scalaz ZIO implemetation of FetchMonadError

  type FetchIO[A] = IO[FetchException, A]

  implicit val applicativeFetch : Applicative[fetch.Fetch] = ???

  implicit def zioFetchMonadError : FetchMonadError[FetchIO] = ???

  //      new FetchMonadError.FromMonadError[Future] {


 // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache] = None):
    IO[Exception, (FetchEnv, List[HNItem])] = {

    hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage).toNel match {

      case Some(pageOfItems) =>
        val fetchItems = pageOfItems.traverse(HNDataSources.getItem)

        cache match {
          case Some(c) =>
            fetchItems.runF[FetchIO](c)
          case None =>
            fetchItems.runF[FetchIO]
        }

      case None =>
        ???
    }


  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items: List[HNItem]): FetchIO[Unit] = {
    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    IO[FetchException, Unit] {

      items.zipWithIndex.foreach {
        case (item, n) =>
          println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)}")
          println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
      }
    }
  }

  // Simple input and output is encoded as FetchIO so we can compose all the pieces
  // to get the final
  def promptInput = IO[FetchException, Unit] {
    println("Enter a page number to fetch a page of news items or anything else to quit: ")
  }

  def getNumericInput: FetchIO[Option[Int]] = IO[FetchException, Option[Int]] {
    Try {
      val input = readLine()
      input.toInt
    }.toOption

  }

  def printTopItemCount(topItems: HNItemIDList) = IO[FetchException, Unit] {
    println(s"Got ${topItems.size} items")
  }

  def printError(err: String) = IO[FetchException, Unit] {
    println(s"Error: $err")
  }

  val numItemsPerPage = 10

  def getUserPage: IO[FetchException, Option[Int]] = for (
    _ <- promptInput;
    page <- getNumericInput
  ) yield page

  def showPagesLoop(topItems: HNItemIDList, cache: Option[DataSourceCache]): FetchIO[Option[DataSourceCache]] =

  // Here we will show the page of items or exit if the user didn't enter a number
    getUserPage.flatMap {

      case Some(page) =>
        println(s"fetch page $page")

        for (
          fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
          (env, items) = fetchResult;
          _ = println(s"${env.rounds.size} fetch rounds");
          _ <- printPageItems(page, numItemsPerPage, items);
          newCache <- showPagesLoop(topItems, Some(env.cache))
        ) yield newCache


      case None =>
        FetchIO.now(cache)
    }

  // Set a fixed size pool with a small number of threads so we can be nice to the Hacker News servers by
  // limiting the number of concurrent requests
//  val scheduler = monix.execution.Scheduler.fixedPool("monix-pool", 4, true)

  def main(args : Array[String]) : Unit = {

    // Finally the main program consists of getting the list of top item IDs and then calling the loop ...

//    val program = getTopItems().flatMap {
//      case Right(items) =>
//        showPagesLoop(items, None)
//      case Left(err) =>
//        printError(err)
//    }
//
//    val ran = program.runAsync // (scheduler)
//    Await.result(ran, Duration.Inf)

  }

}
