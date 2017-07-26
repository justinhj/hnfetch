package justinhj.hnfetch

import java.net.URL
import java.util.Date

import justinhj.hnfetch.HNFetch._
import org.ocpsoft.prettytime.PrettyTime
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object FrontPage {

  def getItems(itemIDs : Seq[HNItemID]) : Future[Seq[Either[String, HNItem]]] = {

    val f = itemIDs.map { itemID => getItem(itemID) }
    Future.sequence(f)
  }

  // Let's display the time like "2 minutes ago" using the PrettyTime library
  // ts is epoch time in seconds
  def timestampToPretty(ts: Int) : String = {

    val epochTimeMS = ts * 1000L

    val p = new PrettyTime()
    p.format(new Date(epochTimeMS))
  }

  // We will display just the hostname of the URL
  // this returns close to what we want but not exactly...
  def getHostName(url: String) : String = {
    if(url.isEmpty) ""
    else {
      Try(new URL(url)) match {
        case Success(u) =>
          "(" + u.getHost + ")"
        case Failure(e) =>
          ""
      }
    }
  }

  // Print a simple list of the Hacker News front page
  // By default it will start at the first item and show N items
  // But by passing the page number 0-N it will show that page
  // Only a certain number of top items are returned by the API so you will run out of
  // pages eventually
  // The Hacker News Homepage has 30 items but for politeness we'll just do a smaller number of requests at a time

  def main(args : Array[String]) : Unit = {

    val numItemsPerPage = 5

    val startPage = Try(args(0).toInt).getOrElse(0)

    // helper to show the article rank
    def itemNum(n: Int) = (startPage * numItemsPerPage) + n + 1

    println(s"start page $startPage")

    val futureItems = getTopItems().flatMap {
      case Right(items) =>
        val pageOfItems = items.slice(startPage * numItemsPerPage, startPage * numItemsPerPage + numItemsPerPage)
        getItems(pageOfItems)

      case Left(err) =>
        Future.failed(new Exception(err))
    }

    val printItems = futureItems.map {
      _.zipWithIndex.foreach {
          case (Right(item), n) =>
            println(s"${itemNum(n)}. ${item.title} ${getHostName(item.url)}")
            println(s"  ${item.score} points by ${item.by} at ${timestampToPretty(item.time)} ${item.descendants} comments\n")
          case (Left(err), n) => println(s"${itemNum(n)}. No item (err $err)")
        }
    }

    Await.ready(printItems, 10 seconds)

  }



}
