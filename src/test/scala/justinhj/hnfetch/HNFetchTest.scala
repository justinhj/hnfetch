package justinhj.hnfetch

import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest._

class HNFetchTest extends AsyncFlatSpec with Matchers with OptionValues with Inside with Inspectors {

  implicit val scheduler = Scheduler.Implicits.global


  "HNFetch" should "be able to fetch the top news stories" in {

    HNFetch.getTopItems().runAsync.map {
      case Right(items) => assert(items.size > 0)
      case Left(err) => fail
    }

  }

  it should "be able to fetch a specific user" in {
    val testUser = "justinhj"

    Task(HNFetch.getUser(testUser)).runAsync.map {
      case Right(user) => assert(user.id == testUser)
      case Left(err) => fail
    }
  }

  it should "be able to fetch a specific item" in {
    val itemID = 13867316

    Task(HNFetch.getItem(itemID)).runAsync.map {
      case Right(item) => assert(item.id == itemID)
      case Left(err) => fail
    }
  }


}
