package justinhj.hnfetch

import cats.effect.{ConcurrentEffect, Sync}
import scalaj.http.{ BaseHttp, HttpConstants, HttpOptions }
import upickle.default._
//import cats.effect._

import scala.util.{ Failure, Success, Try }

// Get Hacker News items

object HNFetch {

  def customOptions: Seq[HttpOptions.HttpOption] = Seq(
    HttpOptions.connTimeout(10000),
    HttpOptions.readTimeout(10000),
    HttpOptions.followRedirects(true)
  )

  object customHttp
      extends BaseHttp(
        proxyConfig = None,
        options = customOptions,
        charset = HttpConstants.utf8,
        sendBufferSize = 4096,
        userAgent = "justinhj/hnfetch/1.0",
        compress = false
      )

  private val baseHNURL = "https://hacker-news.firebaseio.com/v0/"

  type HNUserID = String
  type HNItemID = Int

  val HNMissingItemID: HNItemID = -1
  val HNMissingUserID: HNUserID = ""

  case class HNUser(
    id: HNUserID, // The user's unique username. Case-sensitive. Required.
    //delay : Int, // Delay in minutes between a comment's creation and its visibility to other users.
    created: Int, // Creation date of the user, in Unix Time.
    karma: Int, // The user's karma.
    about: String, // The user's optional self-description. HTML.
    submitted: List[HNItemID]
  ) // List of the user's stories, polls and comments.

  // These functions construct the url for various api queries
  private def getUserURL(userId: HNUserID) = s"${baseHNURL}user/$userId.json"

  private def getItemURL(itemId: HNItemID) = s"${baseHNURL}item/$itemId.json"

  private val getTopItemsURL = s"${baseHNURL}topstories.json"

  private val getMaxItemURL = s"${baseHNURL}maxitem.json"

  case class HNItem(
    id: HNItemID, // The item's unique id.
    deleted: Boolean = false, // true if the item is deleted.
    `type`: String, // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
    by: HNUserID = HNMissingUserID, // The username of the item's author.
    time: Int, // Creation date of the item, in Unix Time.
    text: String = "", // The comment, story or poll text. HTML.
    dead: Boolean = false, // true if the item is dead.
    parent: HNItemID = HNMissingItemID, // The comment's parent: either another comment or the relevant story.
    poll: HNItemID = HNMissingItemID, // The pollopt's associated poll.
    kids: List[HNItemID] = List(), // The ids of the item's comments, in ranked display order.
    url: String = "", // The URL of the story.
    score: Int = -1, // The story's score, or the votes for a pollopt.
    title: String = "", // The title of the story, poll or job.
    parts: List[HNItemID] = List(), // A list of related pollopts, in display order.
    descendants: Int = 0 // In the case of stories or polls, the total comment count.
  )

  // Get user details. This is a blocking call
  def getUserSync(userID: HNUserID): Either[String, HNUser] = {
    val url = getUserURL(userID)

    hnRequestSync[HNUser](url)
  }

  def getUser[F[_]: ConcurrentEffect](userID: HNUserID): F[Either[String, HNUser]] =
    Sync[F].delay(getUserSync(userID))

  // Get Item details. This is a blocking call
  def getItemSync(itemId: HNItemID): Either[String, HNItem] = {
    val url = getItemURL(itemId)

    hnRequestSync[HNItem](url)
  }

  // Todo don't bury this error, rather than an Either it should be a Task or something
  // similar compatible with Cats Effect
  def getItem[F[_]: ConcurrentEffect](itemId: HNItemID): F[HNItem] =
    Sync[F].delay{
      getItemSync(itemId) match {
        case Right(a) => a
        case Left(err) => throw new Exception(err)
      }
    }

  type HNItemIDList = List[HNItemID]

  // Get the ids of the top items. This is a blocking call
  def getTopItemsSync(): Either[String, HNItemIDList] =
    hnRequestSync[HNItemIDList](getTopItemsURL)

  def getTopItems[F[_]: ConcurrentEffect](): F[Either[String, HNItemIDList]] = Sync[F].delay {
    hnRequestSync[HNItemIDList](getTopItemsURL)
  }

  // Top item is the latest item posted to the site

  // Get max item. This is a blocking call
  def getMaxItemSync(): Either[String, HNItemID] =
    hnRequestSync[HNItemID](getMaxItemURL)

  def getMaxItem[F[_]: ConcurrentEffect](): F[Either[String, HNItemID]] = Sync[F].delay {
    hnRequestSync[HNItemID](getMaxItemURL)
  }

  // Executes the API request. We need a upickle.Reader for the type T so it can be parsed
  // on receipt
  def hnRequestSync[T](url: String)(implicit r: Reader[T]): Either[String, T] = {

    println(s"url get on thread ${Thread.currentThread().getName}")

    Try(customHttp(url).asString) match {

      case Success(response) =>
        if (response.code == 200) {
          Try(read[T](response.body)) match {
            case Success(good) if good == null =>
              //println("got empty")
              Left("Not found")
            case Success(good) =>
              println(s"got url on thread ${Thread.currentThread().getName}")
              Right(good)
            case Failure(e) =>
              //println(s"got parse error ${response.body}")
              Left(s"Failed to read ${e.getMessage}")
          }
        } else {
          //println("got no response")
          Left(s"Failed to retrieve $url code: ${response.code}")
        }
      case Failure(err) =>
        Left(s"Failed to retrieve $url error: ${err.getMessage}")

    }
  }

}
