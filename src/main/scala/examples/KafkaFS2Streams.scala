package examples

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import fs2.{Scheduler, Strategy, Stream, Task}
import fs2.util.Async
import justinhj.hnfetch.HNFetch
import justinhj.hnfetch.HNFetch.HNItemIDList
import scodec.bits.ByteVector
import spinoco.fs2.kafka
import spinoco.fs2.kafka.Logger.Level
import spinoco.fs2.kafka.network.BrokerAddress
import spinoco.fs2.kafka.{KafkaClient, Logger, partition, topic}
import spinoco.protocol.kafka.ProtocolVersion
import upickle.default._
import scala.concurrent.Await
import scala.concurrent.duration._

// Periodically get Hacker news top items and publish to kafka using FS2 streams

object KafkaFS2Streams {


  implicit val S: Strategy = Strategy.fromFixedDaemonPool(8,"fs2-kafka-spec")
  implicit val Sch: Scheduler =  Scheduler.fromFixedDaemonPool(4, "fs2-kafka-spec-scheduler")
  implicit val AG: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(8, Strategy.daemonThreadFactory("fs2-kafka-spec-acg")))
  implicit val F: Async[fs2.Task] = fs2.Task.asyncInstance
  implicit lazy val logger: Logger[Task] = new Logger[Task] {
    def log(level: Logger.Level.Value, msg: => String, throwable: Throwable): Task[Unit] =
      Task.delay { println(s"LOGGER: $level: $msg"); if (throwable != null) throwable.printStackTrace() }
  }

  val topicId = topic("items")
  val part0 = partition(0)

  val brokers = Set(BrokerAddress("0.0.0.0", 9092))

  def blockingPublishItem(client : Stream[Task, KafkaClient[Task]], item: String) : Seq[Long] = {
    // publish a message

    val streamTask: Stream[Task, Long] = client.flatMap {

      clientTask =>

        val timeMillis = System.currentTimeMillis()

        // TODO error handling on encode
        val message = ByteVector.encodeUtf8(item).getOrElse(ByteVector.empty)

        val pub: Task[Long] = clientTask.publish1(topicId, part0,
          ByteVector.empty, message,
          requireQuorum = false,
          serverAckTimeout = 3 seconds)

        Stream.eval(pub)
    }

    streamTask.runLog.unsafeRun()
  }

  def main(args : Array[String]) : Unit = {

    logger.log(Level.Info, "Starting", null)

    // TODO config
    // config time between fetches
    // config kafka host and port
    // config topic name

    val pauseTime = 5 seconds

    val client: Stream[Task, KafkaClient[Task]] = kafka.client(
      ensemble = brokers
      , protocol = ProtocolVersion.Kafka_0_10_2
      , clientName = "fs2-client"
    )

    import cats.effect.IO

    val eff = Stream.eval(IO { println("BEING RUN!!"); 1 + 1 })
    val er = eff.runLog

    (1 to 10).foreach{
      item =>
        Thread.sleep(pauseTime.toMillis)

        logger.log(Level.Info, "Getting top items", null)

        val topItemsF = HNFetch.getTopItems()

        val topItems: Either[String, HNItemIDList] = Await.result(topItemsF, 10 seconds)

        topItems.map{
          items =>
            val serializedItems = write(items)

            logger.log(Level.Info, "Writing items", null)

            blockingPublishItem(client, serializedItems)

        }
    }

    logger.log(Level.Info, "Done", null)

  }



}