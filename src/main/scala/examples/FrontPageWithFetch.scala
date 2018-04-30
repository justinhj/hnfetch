package examples

import cats.instances.list._
import cats.syntax.traverse._
import fetch._
import fetch.monixTask.implicits._
import fetch.syntax._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn.readLine

object FrontPageWithFetch {

  // Default Monix scheduler
  private val scheduler = Scheduler.Implicits.global

  // Fetch a page of Hacker News items with optional cache from a previous call
  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache] = None) :
    Task[(FetchEnv, List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)

    cache match {
      case Some(c) =>
        fetchItems.runF[Task](c)
      case None =>
        fetchItems.runF[Task]
    }

  }

  // Print a page of fetched items
  def printPageItems(startPage: Int, numItemsPerPage: Int, items : List[HNItem]): Task[Unit] = {
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
  def promptInput = Task { println("Enter page # or q to quit: ") }
  def getInput = Task{ readLine }
  def printTopItemCount(topItems : HNItemIDList) = Task { println(s"Got ${topItems.size} items") }
  def printError(err: String) = Task { println(s"Error: $err") }

  def main(args : Array[String]) : Unit = {

    val numItemsPerPage = 10

    // Here we compose all the tasks together

    // This first task composes a bunch of smaller tasks together to let the user show a page of news items
    // by entering the page number at the command line

    def showPagesLoop(topItems: HNItemIDList, cache: Option[DataSourceCache]) : Task[(String, DataSourceCache)] =
      for (
        _ <- promptInput;
        input <- getInput;
        page = input.toInt;
        _ = println(s"fetch page $page");
        fetchResult <- fetchPage(page, numItemsPerPage, topItems, cache);
        (env, items) = fetchResult;
        _ <- printPageItems(page, numItemsPerPage, items)
    ) yield (input, env.cache)

    // This loops until the user enters "q", calling the showPagesLoop above

    def loop(topItems: HNItemIDList, cache: Option[DataSourceCache] = None) : Task[(String, DataSourceCache)] =
      showPagesLoop(topItems, cache).flatMap {
        case (input, newCache) =>
          println(s"input is $input")
          if(input == "q") Task.pure("Quit", newCache)
          else loop(topItems, Some(newCache))
      }

    // Finally the main program consists of getting the list of top item IDs and then calling the loop ...

    val program = getTopItems().flatMap {
      case Right(topItems) =>
        loop(topItems, None)
      case Left(err) =>
        printError(err)
     }

    val ran = program.runAsync(scheduler)
    Await.result(ran, Duration.Inf)

  }

}
