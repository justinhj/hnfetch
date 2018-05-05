package examples

import justinhj.hnfetch.HNFetch._
import monix.eval.Task
import monix.execution.Scheduler
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object FrontPage {

  def getItems(itemIDs : Seq[HNItemID]) : Task[Seq[Either[String, HNItem]]] = {

    val f = itemIDs.map { itemID => getItem(itemID) }
    Task.sequence(f)
  }

  // Print a simple list of the Hacker News front page
  // By default it will start at the first item and show N items
  // But by passing the page number 0-N it will show that page
  // Only a certain number of top items are returned by the API so you will run out of
  // pages eventually
  // The Hacker News Homepage has 30 items but for politeness we'll just do a smaller number of requests at a time

  def printPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList) : Task[Unit] = {

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

  def blockingPrintPage(startPage: Int, numItemsPerPage: Int, hNItemIDList: HNItemIDList) : Unit = {

    val scheduler = Scheduler.Implicits.global

    val task: Task[Unit] = printPage(startPage, numItemsPerPage, hNItemIDList)

    Await.result(task.runAsync(scheduler), Duration.Inf)
  }


  def main(args : Array[String]) : Unit = {

    val numItemsPerPage = 10

    val startPage = Try(args(0).toInt).getOrElse(0)

    println("Getting top items")

    val scheduler = Scheduler.Implicits.global

    val topItemsF = getTopItems()

    Await.result(topItemsF.runAsync(scheduler), 10 seconds) match {

      case Right(topItems) =>
        println(s"Got items")

        def inputLoop() : Unit = {
          println("Enter page # or q to quit: ")

          io.StdIn.readLine match {
            case "q" =>
            case "" =>
            case s: String =>
              val page = Try(s.toInt) match {
                case Success(n) =>
                  println(s"Printing page  $n")

                  blockingPrintPage(n, numItemsPerPage, topItems)

                  inputLoop()
                case Failure(_) =>
              }
            case _ =>

          }
        }

        inputLoop()

      case Left(err) =>
        Future.failed(new Exception(err))
    }

  }

}
