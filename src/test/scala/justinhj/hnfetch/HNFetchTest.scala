package justinhj.hnfetch

import org.scalatest._

class HNFetchTest extends AsyncFlatSpec with Matchers with OptionValues with Inside with Inspectors {

  "HNFetch" should "be able to fetch the top news stories" in {

    HNFetch.getTopItems().map {
      case Right(items) => assert(items.size > 0)
      case Left(err) => fail
    }

  }

  it should "be able to fetch a specific user" in {
    val testUser = "justinhj"

    HNFetch.getUser(testUser).map {
      case Right(user) => assert(user.id == testUser)
      case Left(err) => fail
    }
  }

  it should "be able to fetch a specific item" in {
    val itemID = 13867316

    HNFetch.getItem(itemID).map {
      case Right(item) => assert(item.id == itemID)
      case Left(err) => fail
    }
  }


}
