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

    Observable.intervalAtFixedRate(1 second, 500 milliseconds).filter{
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

  def jobsObservable(batchSize: Integer, interval: FiniteDuration)  = {

    Observable.interval(interval).map {

      _ =>
        val size = jobs.size()

        if(size > 0) {
          val takeJobs = Math.min(batchSize, size)

          (1 to takeJobs).map {
            _ =>
              jobs.remove()
          }
        }
        else {
          IndexedSeq.empty[Job]
        }


    }

  }

  def processJobs()= {

    jobsObservable(10, 200 milliseconds).map{
      batch =>
        if(batch.size > 0) {
          s"BEGIN ${batch.size} jobs starting at ${batch(0).id}"

          // do pretend work
          Thread.sleep(200)

          s"END ${batch.size} jobs starting at ${batch(0).id}"
        }
        else
          "no jobs"
    }.
    dump("job output").subscribe

  }


  def main(args: Array[String]): Unit = {

    println("Beginning with amazing queue thing")






  }




}
