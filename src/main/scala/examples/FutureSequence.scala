import java.util.concurrent.TimeoutException
import java.util.{Timer, TimerTask}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object FutureUtil {

  // All Future's that use futureWithTimeout will use the same Timer object
  // it is thread safe and scales to thousands of active timers
  // The true parameter ensures that timeout timers are daemon threads and do not stop
  // the program from shutting down

  val timer: Timer = new Timer(true)

  /**
    * Returns the result of the provided future within the given time or a timeout exception, whichever is first
    * This uses Java Timer which runs a single thread to handle all futureWithTimeouts and does not block like a
    * Thread.sleep would
    * @param future Caller passes a future to execute
    * @param timeout Time before we return a Timeout exception instead of future's outcome
    * @return Future[T]
    */
  def futureWithTimeout[T](future : Future[T], timeout : FiniteDuration)(implicit ec: ExecutionContext): Future[T] = {

    // Promise will be fulfilled with either the callers Future or the timer task if it times out
    val p = Promise[T]

    // and a Timer task to handle timing out

    val timerTask = new TimerTask() {
      def run() : Unit = {
            p.tryFailure(new TimeoutException())
        }
      }

    // Set the timeout to check in the future
    timer.schedule(timerTask, timeout.toMillis)

    future.map {
      a =>
        if(p.trySuccess(a)) {
          timerTask.cancel()
        }
    }
    .recover {
      case e: Exception =>
        if(p.tryFailure(e)) {
          timerTask.cancel()
        }
    }

    p.future
  }

}

object Test {

  import FutureUtil._

  import ExecutionContext.Implicits.global

  def goodone(n: Int) : Future[Int] = Future{Thread.sleep(n); println(s"good $n"); n}
  def badone(whytho: String, n: Int) = Future{
    Thread.sleep(n)
    throw new Exception(whytho + n.toString)
  }

  def tryFuture[A](f : Future[A]) : Future[Try[A]] = f.map(Success(_)).recover{case e => Failure(e)}

  def main(args: Array[String]) : Unit = {

    val futures = List(goodone(1000), goodone(3000), badone("oops ", 2000))
    val timeoutFutures = futures.map{f =>
      tryFuture(futureWithTimeout(f, 2000 milliseconds))}

    val sequenced = Future.sequence(timeoutFutures)

    val result = Await.result(sequenced, 5 seconds)

    println(result)
  }

}
