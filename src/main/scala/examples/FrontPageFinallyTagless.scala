package examples

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.Executors

import cats._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import justinhj.hnfetch.HNFetch.{getUserURL, _}

import scala.concurrent.ExecutionContext

object FrontPageFinallyTagless {

  // Define the error type for the application, we'll just use string
  type Error = String
  type EitherError[A] = Either[String, A]

  trait Logging[F[_]] {
    def log(s : String)(implicit evApp : Applicative[F]) : F[Unit]
  }

  trait HttpClient[F[_]] {
    def get(url: String)(L : Logging[F]) : F[String]
  }

  trait Parsing[F[_]] {
    def parse[A](json: String)(implicit D: Decoder[A], evApp : Applicative[F]) : F[EitherError[A]]
  }

  // Implement parsing using Circe

  def circeParser[F[_]]: Parsing[F] = new Parsing[F] {
    def parse[A](json: String)(implicit D: Decoder[A], evApp : Applicative[F]) : F[EitherError[A]] = {

      if(json == "null") evApp.pure(Left("Object not found (server returned null)"))
      else {
        decode[A](json) match {
          case Right(a) =>
            evApp.pure(Right(a))
          case Left(err) =>
            evApp.pure(Left(err.toString))
        }
      }
    }
  }

  // A full Hacker News API algebra using tagless final style
  // Note that nothing is implemented here but the methods needed in our
  // Hacker News API domain
  class HNApi[F[_] : Monad](L : Logging[F], H : HttpClient[F], P : Parsing[F]) {

    // All functions are written in terms of this one
    def exec[A](url: String)(implicit D: Decoder[A]) : F[EitherError[A]] = {

      for (
        _ <- L.log(s"Fetching $url");
        body <- H.get(url)(L);
        _ <- L.log(s"Parsing ${body.length}  bytes");
        parsed <- P.parse[A](body)
      ) yield parsed

    }

    def getUser(userID: HNUserID) : F[Either[String, HNUser]] = exec[HNUser](getUserURL(userID))

    def getItem(itemID: HNItemID) : F[Either[String, HNItem]] = exec[HNItem](getItemURL(itemID))

    def getTopItems() : F[Either[String, HNItemIDList]] = exec[HNItemIDList](getTopItemsURL)


  }

  def fetch[F[_] : Monad](url : String)(F: HttpClient[F], L: Logging[F]) : F[String] = {

    for(
      _ <- L.log(s"Fetching $url");
      result <- F.get(url)(L)
    ) yield result

  }

  // An implementation of fetch API to string that uses Cats IO and calls the actual API
  def httpClientIO(ec: ExecutionContext) : HttpClient[IO] = new HttpClient[IO] {

    def get(url: String)(L: Logging[IO]) : IO[String] = {
      val result = for (
        _ <- IO.shift(ec);
        response <- IO(customHttp(url).asString);
        body = if(response.is2xx) response.body else "error" // TODO better error handling, need the error monad here
      ) yield body

      result
    }

  }

  // HttpClient implementation uses Id instead IO and can be used for testing and offline development
  object IdMockFetchAPI extends HttpClient[Id] {

    def get(url: String)(L: Logging[Id]) : Id[String] = {

      val reqResponseMap = Map[String, String](
        getMaxItemURL -> "1000",
        getUserURL("justinhj") -> """{"about":"A software engineer currently working in Scala and Akka. Interested in app development, game development, distributed systems and functional programming, emacs, Common Lisp and Clojure.","created":1249966944,"id":"justinhj","karma":1432,"submitted":[18185046,18123331,18122043,18102560,18101235,18019158,18016014,17989088,17989076,17979893,17978966,17928154,17650505,17649307,17649292,17648818,17647934,17647878,17647614,17647332,17646676,17646064,17607267,17607260,17511554,17511541,17443115,17265787,17220215,17217772,17165275,17049569,16885882,16885593,16885507,16879277,16826005,16825635,16825563,16821272,16797330,16797318,16755142,16755004,16658014,16624712,16612746,16575990,16539609,16466008,16446555,16438112,16421629,16420555,16399195,16385175,16377115,16356380,16355982,16354708,16344474,16344112,16295450,16292358,16290702,16242096,16223465,16223447,16180648,16180628,16064927,16064912,16055120,16052668,16047029,16047019,16039195,15991471,15988032,15952157,15929304,15915649,15873530,15818258,15771097,15713480,15636149,15601547,15525089,15483642,15483215,15465470,15401004,15360719,15323151,15316431,15239180,15227645,15195356,15058585,15057966,14936408,14928967,14881336,14804575,14752857,14745974,14706658,14673488,14663850,14620064,14612924,14573672,14542675,14518625,14512413,14512412,14512411,14476455,14441549,14439214,14428063,14428062,14426153,14424357,14417012,14243913,14219427,14204639,14184212,14184092,14106771,14074743,14067432,14029876,14029853,13973438,13917442,13889014,13852442,13846447,13818018,13689059,13688110,13648008,13619195,13598889,13567749,13494517,13481829,13481726,13481697,13448834,13433267,13424580,13391265,13369473,13357007,13341900,13286677,13286592,13278303,13177915,13172201,13168095,13168094,13167918,13133545,13126498,13121066,13090173,13079882,13050021,13011900,13011899,13009711,13009689,13009670,13009093,12967347,12941780,12896111,12873344,12796765,12789514,12788648,12744785,12710191,12708909,12708764,12706311,12706307,12706270,12705395,12669235,12664506,12664495,12661247,12661156,12658492,12656242,12654834,12652790,12602499,12598073,12589636,12576471,12576470,12558538,12543203,12543151,12543132,12535115,12535109,12531801,12531765,12531747,12497232,12497231,12492761,12488684,12450260,12441998,12354125,12352751,12348777,12323898,12320807,12315868,12305616,12301613,12254366,12236534,12235879,12233028,12218262,12218261,12206029,12203557,12203555,12190411,12190386,12186736,12158650,12158595,12156680,12156024,12136852,12122012,12121994,12121989,12118361,12118359,12116891,12116837,12049867,12046867,11957102,11950130,11943251,11943235,11933531,11933514,11917657,11917655,11912839,11882288,11869175,11869129,11860380,11812253,11808760,11807040,11803180,11792527,11771704,11768297,11721953,11596680,11592379,11591832,11583041,11577389,11575251,11574157,11573352,11568179,11567687,11567255,11550805,11549076,11536207,11536156,11498534,11480848,11473222,11471470,11435025,11426807,11404778,11389610,11383214,11355372,11353949,11306509,11298087,11298041,11292902,11268563,11267995,11228051,11215137,11203203,11174646,11167781,11164210,11115677,11099654,11095477,11088632,11088631,11038325,11037015,11036032,10977094,10975282,10973915,10970252,10952892,10927768,10927763,10916324,10888942,10859466,10839258,10834186,10829937,10807839,10807768,10802490,10732198,10719041,10711724,10704201,10672134,10671851,10629556,10622551,10601722,10590459,10585216,10575438,10574780,10539962,10508483,10507463,10497119,10483158,10462391,10457830,10313327,10272671,10213478,10195098,10151062,10126645,10126076,10123960,10104751,10070439,10022767,10014424,10010221,9930159,9891772,9886314,9784382,9779242,9771586,9751926,9739214,9715381,9711720,9700811,9692623,9692580,9629204,9594920,9594791,9594781,8803209,7928252,7836115,7810376,7796571,7668658,7653592,7651928,7649948,7633931,7599209,7595542,7595525,7591747,7582498,7488162,7488102,7474021,7463938,7439552,7428806,7415456,7407419,7394824,7342997,7260003,7171603,7171372,7110641,7087421,7076193,7054455,7053009,7027044,7025616,6981341,6975491,6973402,6967278,6967233,6939391,6929414,6924687,6910257,6896206,6892385,6849206,6849187,6848493,6847726,6843273,6821389,6764977,6740313,6716566,6699284,6651114,6650887,6648702,6615766,6583409,6577887,6575673,6575576,6546319,6517962,6512050,6508204,6492233,6462634,6460993,6445177,6419270,6418392,6399558,6347152,6346879,6328797,6328782,6324893,6311781,6311752,6305727,6305310,6268857,6203574,6147605,6125593,6123869,6123855,6108771,6096095,6086923,6086920,6086908,6086886,6084072,6073938,6011428,6007028,5893447,5875478,5840006,5792975,5783841,5780448,5756885,5752534,5750868,5749275,5739472,5734397,5733217,5730758,5730222,5729122,5721172,5691319,5669106,5648842,5640096,5638444,5636506,5624132,5614010,5610242,5601488,5579200,5552067,5525160,5499881,5488067,5465612,5461298,5461006,5438671,5433130,5426555,5416688,5413122,5222682,5189157,5189148,5185833,5171057,5164849,5155491,5085960,5085956,5068986,4989208,4879101,4832595,4786384,4752629,4700770,4699350,4698304,4698261,4655518,4649495,4619557,4618605,4604502,4604196,4564863,4562380,4562004,4554758,4554662,4552877,4540168,4518081,4517850,4516491,4508548,4467435,4444208,4439183,4431087,4393911,4361831,4359361,4358954,4358889,4339247,4305761,4298021,4251357,4249267,4243078,4241428,4238475,4231295,4209735,4208008,4167495,4167427,4148337,4135098,4131979,4123153,4089579,4084385,4074636,4028774,3957068,3938076,3920060,3822300,3812924,3793327,3792956,3791202,3788495,3788493,3788474,3771182,3769594,3762042,3739821,3729714,3727056,3718956,3647953,3643591,3577614,3576678,3576663,3534882,3511934,3505802,3475061,3475009,3465467,3454543,3453802,3440913,3435616,3435069,3430310,3430198,3429200,3416645,3341373,3321047,3320980,3288087,3277597,3268364,3268363,3226041,3222925,3197770,3157190,3126997,3093759,3068701,3067780,3067611,3062874,3057268,2989524,2977413,2875859,2875513,2868633,2868589,2855775,2855463,2854466,2844192,2761919,2744745,2743065,2547626,2538894,2126457,1143918,1036068,1013444,1011729,1010780,1010547,971362,971174,965374,963508,962991,942045,930265,924557,877019,877018,875892,874357,872581,872578,858487,857025,856455,856409,848627,847139,847008,844721,842135,828673,800418,799459,798499,785397,782703]}""",
        getItemURL(11498534) -> """{"by":"justinhj","id":11498534,"parent":11498393,"text":"I&#x27;m an emacs user but I often use a GUI IDE when it makes sense. I find I&#x27;d rather use JetBrains products (PHPStorm, PyCharm and IntelliJ) even though the actual text editing part is nowhere near as powerful and efficient as my Emacs setup.  Sometimes if I need to do some repetitive or monumental editing task that I could script in emacs or use the macro system, I&#x27;ll switch over for a moment. The benefits of being in an environment with a debugger and many other tools as well as project management and auto-completion that is consistent across the languages I use outweigh the cons of leaving emacs. \nI&#x27;m considering switching to Visual Studio just for a React Native project I&#x27;m working on because it seems to handle Javascript&#x2F;JMX much better than anything else I&#x27;ve tried.","time":1460653869,"type":"comment"}"""
      )

      reqResponseMap.getOrElse(url, "not found").pure[Id]

    }

  }

  // Logging implementation that logs the time and the provided string
  def printlnLogging[F[_]] : Logging[F] = new Logging[F] {

    def log(s: String)(implicit evApp : Applicative[F]): F[Unit] = {
      val now : java.util.Date = Calendar.getInstance.getTime
      val timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

      println(timeFormat.format(now) + ": " + s).pure[F]
    }

  }

  def main(args: Array[String]): Unit = {

    // Fetch from the real API using IO Monad

    // Requires a threadpool
    val threadPool = Executors.newFixedThreadPool(4)
    val ec = ExecutionContext.fromExecutor(threadPool)

    val fetchAPIInstance = httpClientIO(ec)

    // Get top items and a user with the Id API

    val hnAPIId = new HNApi[Id](printlnLogging, IdMockFetchAPI, circeParser)

    val getJustin1 = hnAPIId.getUser("justinhj")
    val getJustinItem1 = hnAPIId.getItem(11498534)
    val getTopItems1 = hnAPIId.getTopItems()

    val fetchMockProgram = for (
      topItems <- getTopItems1;
      user <- getJustin1;
      item <- getJustinItem1
    ) yield (user, item, topItems)

    println(fetchMockProgram)

    // Get user by ID real API
    // Note the parser and the logger are the same and we've swapped out the http API to use our mock instead

    val hnAPI = new HNApi[IO](printlnLogging, fetchAPIInstance, circeParser)

    val getJustin = hnAPI.getUser("justinhj")
    val getJustinItem = hnAPI.getItem(11498534)
    val getTopItems = hnAPI.getTopItems()

    val fetchRealProgram = for (
      topItems <- getTopItems;
      user <- getJustin;
      item <- getJustinItem
    ) yield (user, item, topItems)

    val results = fetchRealProgram.unsafeRunSync()

    println(s"Found ${results._3.map(_.size)}")

    threadPool.shutdown()

  }


}
