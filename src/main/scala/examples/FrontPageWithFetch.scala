package examples

import scala.io.StdIn.readLine
import cats.instances.list._
import cats.syntax.traverse._
import fetch._
import fetch.implicits._
import fetch.syntax._
import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._
import monix.eval.Task
import monix.execution.Scheduler
import fetch.monixTask.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object FrontPageWithFetch {

  val scheduler = Scheduler.Implicits.global

  def fetchPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache]) :
    Task[(FetchEnv, List[HNItem])] = {

    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)

    val fetch: Task[(FetchEnv, List[HNItem])] = cache match {
      case Some(c) =>
        fetchItems.runF[Task](c)
      case None =>
        fetchItems.runF[Task]
    }

    fetch

  }

  def printPage2(startPage: Int, numItemsPerPage: Int, items : List[HNItem]): Task[Unit] = {
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
//
//  def printPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache])
//    : Future[Option[FetchEnv]] = {
//
//    // helper to show the article rank
//    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1
//
//    // Get the items
//    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)
//
//    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)
//
//    val fetchResult: Future[(FetchEnv, List[HNItem])] =
//      if(cache.isDefined) fetchItems.runF[Future](cache.get)
//      else fetchItems.runF[Future]
//
//    fetchResult.map {
//      case (env, items) =>
//        items.zipWithIndex.foreach {
//          case (item, n) =>
//            println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)}")
//            println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
//        }
//        Some(env)
//    }.recover {
//      case e : Exception =>
//        None
//    }
//  }

//  def blockingPrintPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache]) :
//      Option[FetchEnv] = {
//
//    val f = printPage(startPage, numItemsPerPage, hNItemIDList, cache)
//
//    Await.result(f, 15 seconds)
//  }

  def promptInput = Task { println("Enter page # or q to quit: ") }
  def getInput = Task{ readLine }
  def printTopItemCount(topItems : HNItemIDList) = Task { println(s"Got ${topItems.size} items") }
  def printError(err: String) = Task { println(s"Error: $err") }

  def main(args : Array[String]) : Unit = {

    val numItemsPerPage = 10

    //println("Getting top items")

    //var cache = None

    val simpleProgram = for (
      topItemsResult <- getTopItems();
      items = topItemsResult.right.get;
      _ <- printTopItemCount(items);
      _ <- promptInput;
      input <- getInput;
      page = input.toInt;
      _ = println(s"fetch page $page");
      fetchResult <- fetchPage(page, numItemsPerPage, items, None);
      (_, items) = fetchResult;
      _ <- printPage2(page, numItemsPerPage, items)
    ) yield input


    // Run the program until it returns "q"

    def loop : Task[String] = simpleProgram.flatMap {
      input =>
        println(s"input is $input")
        if(input == "q") Task.pure("Quit")
        else loop
    }

    val ran = loop.runAsync(scheduler)

//    val ran = simpleProgram.runAsync(scheduler)
    val result = Await.result(ran, Duration.Inf)

    println(result)

    //Thread.sleep(10000)

//    var x = 1
//    x = 2
//
//    // Main interactive loop
//    def inputLoop(topItems: HNItemIDList, cache: Option[DataSourceCache] = None) : Task[Unit] = {
//
//      promptInput.flatMap { _ =>
//        val res : Task[Unit] = getInput.map {
//          case "q" =>
//            Task.pure(())
//
//          case "" =>
//            Task.pure(())
//
//          case s: String =>
//
//            val what = Try(s.toInt) match {
//              case Success(page) =>
//
//                fetchPage(page, numItemsPerPage, topItems, cache).flatMap {
//                  case (env, items) =>
//                    printPage2(page, numItemsPerPage, items)
//                }
//
////                fetchPage(n, numItemsPerPage, topItems, cache).flatMap {
////                  result =>
////                    inputLoop(topItems, result.map(_.cache))
////
////                }
//
//              case Failure(_) =>
//                Task.pure(())
//
//            }
//
//           what
//
//        }
//        res
//      }
//
//    }
//
//     val program = getTopItems().flatMap {
//       case Right(topItems) =>
//         printTopItemCount(topItems).flatMap{_ => inputLoop(topItems)}
//       case Left(err) =>
//         printError(err)
//     }
//
//    Await.result(program.runAsync(scheduler), Duration.Inf)

//    Await.result(topItemsF.runAsync(scheduler), 10 seconds) match {
//
//      case Right(topItems) =>
//        println(s"Got ${topItems.size} items")
//
//        var cache : Option[DataSourceCache] = None
//
//        def inputLoop() : Unit = {
//          println("Enter page # or q to quit: ")
//
//          io.StdIn.readLine match {
//            case "q" =>
//            case "" =>
//            case s: String =>
//              val page = Try(s.toInt) match {
//                case Success(n) =>
//                  println(s"Printing page $n")
//
//                  val result = blockingPrintPage(n, numItemsPerPage, topItems, cache)
//
//                  if(result.isDefined) cache = Some(result.get.cache)
//
//                  inputLoop()
//                case Failure(_) =>
//              }
//            case _ =>
//
//          }
//        }
//
//        inputLoop()
//
//      case Left(err) =>
//        println(s"Failed to get top items because $err")
//    }

  }

}
