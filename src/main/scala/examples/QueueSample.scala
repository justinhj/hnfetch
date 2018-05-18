package examples

import scala.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration._
import monix.execution.Cancelable
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

object QueueSample {

  val r = Random

  case class Job(name: String, id: Long)

  val jobs = new ConcurrentLinkedQueue[Job]()

  def makeAJob(n : Long) = {
    Job(s"Job $n", n.hashCode().toLong)
  }

  def startJobs() : Cancelable = {

    Observable.intervalAtFixedRate(1 second, 100 milliseconds).filter{
      x =>
        r.nextBoolean()
    }.map {
      n => makeAJob(n)
    }//.dump("job")
    .foreach {
      job =>
        //println(s"queing job $job")
        jobs.add(job)
    }


  }

  // We're gonna print the queue size, remove a batch of jobs

  def startWorker() : Cancelable = {

    val batch = 10

    Observable.interval(3 seconds).foreach {

      _ =>
        val size = jobs.size()
        val takeJobs = Math.min(batch, size)

        println(s"removing $takeJobs jobs of $size")
        (1 to takeJobs).foreach {
          _ =>
            jobs.remove()
        }

    }

  }


  def main(args: Array[String]): Unit = {

    println("Beginning with amazing queue thing")






  }




}
