package examples

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import fs2.util.Async
import fs2.{Scheduler, Strategy, Stream, Task, time}
import justinhj.hnfetch.HNFetch
import justinhj.hnfetch.HNFetch.HNItemIDList
import monix.execution.Scheduler
import scodec.bits.ByteVector
import spinoco.fs2.kafka
import spinoco.fs2.kafka.Logger.Level
import spinoco.fs2.kafka.network.BrokerAddress
import spinoco.fs2.kafka.{KafkaClient, Logger, partition, topic}
import spinoco.protocol.kafka.ProtocolVersion
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

// Periodically get Hacker news top items and publish to kafka using FS2 streams

object KafkaFS2Streams {


  implicit val S: Strategy = Strategy.fromFixedDaemonPool(8,"fs2-kafka-spec")
  implicit val Sch: fs2.Scheduler =  fs2.Scheduler.fromFixedDaemonPool(4, "fs2-kafka-spec-scheduler")
  implicit val AG: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(8, Strategy.daemonThreadFactory("fs2-kafka-spec-acg")))
  implicit val F: Async[fs2.Task] = fs2.Task.asyncInstance
  implicit lazy val logger: Logger[Task] = new Logger[Task] {
    def log(level: Logger.Level.Value, msg: => String, throwable: Throwable): Task[Unit] =
      Task.delay { println(s"LOGGER: $level: $msg"); if (throwable != null) throwable.printStackTrace() }
  }

  val topicId = topic("items")
  val part0 = partition(0)

  case class TopItemPayload(time: Long, items: HNItemIDList)

  val brokers = Set(BrokerAddress("0.0.0.0", 9092))

  /**
    * Given a kafka client Stream publish a single item
    * @param client Kafka client stream
    * @param item String item (will be encoded and sent)
    * @return items offset in the topic or throws an error
    */
  def publishItem(client : Stream[Task, KafkaClient[Task]], item: String): Stream[Task, Long] = {

    client.flatMap {
      clientTask =>

        //val timeMillis = System.currentTimeMillis()

        ByteVector.encodeUtf8(item) match {
          case Right(message) =>
            val pub: Task[Long] = clientTask.publish1(topicId, part0,
              ByteVector.empty, message,
              requireQuorum = false,
              serverAckTimeout = 3 seconds)

            Stream.eval(pub)

          case Left(err) =>
            // Not being able to encode the message using utf8 is a fatal error
            throw err
        }
    }
  }

  def blockingPublishItem(client : Stream[Task, KafkaClient[Task]], item: String) : Seq[Long] = {
    // publish a message

    val publishTask =publishItem(client, item)

    publishTask.runLog.unsafeRun()
  }

  def main(args : Array[String]) : Unit = {

    logger.log(Level.Info, "Starting", null)

    // TODO config
    // config time between fetches
    // config kafka host and port
    // config topic name

    val pauseTime = 10 minutes

    val client: Stream[Task, KafkaClient[Task]] = kafka.client(
      ensemble = brokers
      , protocol = ProtocolVersion.Kafka_0_10_2
      , clientName = "fs2-client"
    )

    // query offsets

    val queryOffsets = client.flatMap {
      kc =>
        Stream.eval(kc.offsetRangeFor(topicId, part0))
    }

    queryOffsets.map {

      case (head, next) =>
        logger.log(Level.Info, s"First $head Next $next", null)
        println(s"First $head Next $next")
    }.run.unsafeRun()

    // get the top items on Hacker News

    val scheduler = monix.execution.Scheduler.Implicits.global

    val runFetch = HNFetch.getTopItems()

    //val getTopItems = Stream.eval(runFetch)

    // delay a specified period

    def delay(): Stream[Task, Unit] = {
      logger.log(Level.Info, s"Delaying $pauseTime", null)
      time.sleep(pauseTime)
    }
//
    val getAndPublishTopItems : Stream[Task, Long] = getTopItems.flatMap {
      (items: Either[String, HNItemIDList]) =>

        items match {
          case Right(items) =>1

            val payload = TopItemPayload(System.currentTimeMillis(), items)

            val serializedItems = write(payload)

            logger.log(Level.Info, "Writing items", null)

            publishItem(client, serializedItems)

          case Left(err) =>
            throw new Exception(err)
        }

    }

//    val withDelay = (getAndPublishTopItems ++ delay).repeat

    // end of the world

//    val result = withDelay.run.unsafeRun()
//
//    logger.log(Level.Info, "Done", null)

  }



}