package examples

import justinhj.hnfetch.HNFetch._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Try

object FrontPage {

  def getItems(itemIDs : Seq[HNItemID]) : Future[Seq[Either[String, HNItem]]] = {

    val f = itemIDs.map { itemID => getItem(itemID) }
    Future.sequence(f)
  }

  // Print a simple list of the Hacker News front page
  // By default it will start at the first item and show N items
  // But by passing the page number 0-N it will show that page
  // Only a certain number of top items are returned by the API so you will run out of
  // pages eventually
  // The Hacker News Homepage has 30 items but for politeness we'll just do a smaller number of requests at a time

  def printPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList) : Future[Unit] = {

    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    // Get the items
    val pageOfItems = hNItemIDList.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)

    getItems(pageOfItems).map {
      _.zipWithIndex.foreach {
        case (Right(item), n) =>
          println(s"${itemNum(n)}. ${item.title} ${Util.getHostName(item.url)}")
          println(s"  ${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments\n")
        case (Left(err), n) => println(s"${itemNum(n)}. No item (err $err)")
      }
    }

  }


  def main(args : Array[String]) : Unit = {

    val numItemsPerPage = 10

    val startPage = Try(args(0).toInt).getOrElse(0)

    println("Getting top items")

    val printItems = getTopItems().flatMap {
      case Right(items) =>
        println(s"Got items")
        printPage(startPage, numItemsPerPage, items)
      case Left(err) =>
        Future.failed(new Exception(err))
    }

    Await.ready(printItems, 30 seconds)

  }

}
