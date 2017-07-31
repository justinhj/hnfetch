package examples

import justinhj.hnfetch.HNFetch._

import scala.util.Try

object FrontPageWithFetch {

  def main(args: Array[String]): Unit = {

    val topItemsF = getTopItems()

    val numItemsPerPage = 10

    val startPage = Try(args(0).toInt).getOrElse(0)





  }

}
