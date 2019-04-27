package justinhj.hnfetch

import org.scalatest._
//import scalaz.zio.Task

class HNFetchTest extends AsyncFlatSpec with Matchers with OptionValues with Inside with Inspectors {

//  implicit val scheduler = Scheduler.Implicits.global


//  "HNFetch" should "be able to fetch the top news stories" in {
//
//    HNFetch.getTopItems().runAsync.map {
//      case Right(items) => assert(items.size > 0)
//      case Left(err) => fail
//    }
//
//  }
//
//  it should "be able to fetch a specific user" in {
//    val testUser = "justinhj"
//
//    Task(HNFetch.getUserSync(testUser)).runAsync.map {
//      case Right(user) => assert(user.id == testUser)
//      case Left(err) => fail
//    }
//  }
//
//  it should "be able to fetch a specific item" in {
//    val itemID = 13867316
//
//    Task(HNFetch.getItemSync(itemID)).runAsync.map {
//      case Right(item) => assert(item.id == itemID)
//      case Left(err) => fail
//    }
//  }


}
