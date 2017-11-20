package examples

import justinhj.hnfetch.HNDataSources
import justinhj.hnfetch.HNFetch._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import fetch._
import fetch.implicits._
import cats._, cats.data._, cats.implicits._
import fetch.syntax._

object FrontPageWithFetch {

  def printPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache])
    : Future[Option[FetchEnv]] = {

    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    // Get the items
    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    val fetchItems = pageOfItems.traverse(HNDataSources.getItem)

    val fetchResult: Future[(FetchEnv, List[HNItem])] =
      if(cache.isDefined) fetchItems.runF[Future](cache.get)
      else fetchItems.runF[Future]

    fetchResult.map {
      case (env, items) =>
        items.zipWithIndex.foreach {
          case (item, n) =>
            println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)}")
            println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
        }
        Some(env)
    }.recover {
      case e : Exception =>
        None
    }
  }

  def blockingPrintPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList, cache: Option[DataSourceCache]) :
      Option[FetchEnv] = {

    val f = printPage(startPage, numItemsPerPage, hNItemIDList, cache)

    Await.result(f, 15 seconds)
  }


  def main(args : Array[String]) : Unit = {

    val numItemsPerPage = 10

    println("Getting top items")

    var cache = None

    val topItemsF = getTopItems()

    Await.result(topItemsF, 10 seconds) match {

      case Right(topItems) =>
        println(s"Got ${topItems.size} items")

        var cache : Option[DataSourceCache] = None

        def inputLoop() : Unit = {
          println("Enter page # or q to quit: ")

          io.StdIn.readLine match {
            case "q" =>
            case "" =>
            case s: String =>
              val page = Try(s.toInt) match {
                case Success(n) =>
                  println(s"Printing page $n")

                  val result = blockingPrintPage(n, numItemsPerPage, topItems, cache)

                  if(result.isDefined) cache = Some(result.get.cache)

                  inputLoop()
                case Failure(_) =>
              }
            case _ =>

          }
        }

        inputLoop()

      case Left(err) =>
        println(s"Failed to get top items because $err")
    }

  }

}
