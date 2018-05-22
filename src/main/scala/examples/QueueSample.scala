package examples

import scala.util.Random
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch}

import scala.concurrent.duration._
import monix.execution.Cancelable
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

object QueueSample {

  // This is a pretend job processor

  val r = Random

  // Job with a unique Id and a name
  // This would be the full job details, for example
  // in a DB writer this would be the data to write, the ID
  // and possibly a callback
  case class Job(name: String, id: Long)

  // Job queue is a concurrent linked queue
  val jobs = new ConcurrentLinkedQueue[Job]()

  def makeAJob(n : Long) = {
    Job(s"Job $n", n.hashCode().toLong)
  }

  val testJobs = 500 // how many jobs to process before shutting down

  // create limit jobs over a period
  def startJobs(limit: Int) : Cancelable = {

    Observable.intervalAtFixedRate(1 second, 10 milliseconds)
    .filter{
      x =>
        // Randomly filter half the jobs to create some jitter
        r.nextBoolean()
    }
    .take(limit)
    .map {
      n => makeAJob(n)
    }//.dump("job")
    .foreach {
      job =>
        //println(s"queing job $job")
        jobs.add(job)
    }


  }

  // An Observable that periodically batches jobs from the concurrent queue

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

  def processJobs(latch: CountDownLatch)= {

    jobsObservable(batchSize = 10, 200 milliseconds).map{
      batch =>
        if(batch.size > 0) {
          // do pretend work
          Thread.sleep(200)

          (1 to batch.size).foreach {_ => latch.countDown()}

          s"Completed ${batch.size} jobs, starting at job ID ${batch(0).id}"

        }
        else
          "no jobs"
    }.
    dump("job output").subscribe

  }


  def main(args: Array[String]): Unit = {

    println("Beginning amazing queue thing")

    val latch = new CountDownLatch(testJobs)

    startJobs(testJobs) // make some jobs start happening to a public concurrent queue

    processJobs(latch) // process the jobs

    latch.await()

  }

}
