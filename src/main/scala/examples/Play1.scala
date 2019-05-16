package examples

import scalaz.zio._

import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.random.Random
import scalaz.zio.system.System
import scalaz.zio.internal.PlatformLive
import scalaz.zio.DefaultRuntime 
import scalaz.zio._

object Play1 {
    type Env = Clock with Console with System with Random with Blocking 

    // A runtime instance
    implicit val rts: Runtime[Env] = new DefaultRuntime {
    override val Platform = PlatformLive.makeDefault().withReportFailure(_ => ())
    } 

    // Our own monoid typeclass
    trait Monoid[A, F[_]] extends Serializable {
        def zero : F[A]
        def mappend(a : F[A]) : F[A]
    }

    class ListMonoid[A](l : List[A]) extends Monoid[A, List] {
        def zero : List[A] = List.empty[A]

        def mappend(app : List[A]) : List[A] = {
            def append(in : List[A], acc: List[A]) : List[A] = {
                in match {
                    case head :: tl => append(tl, head +: acc)
                    case Nil => acc.reverse
                }
            }
            append(app, l.reverse)
         }
    }

    // Implement for list
    object ListMonoidOps {
        implicit def listMonoid[A](l: List[A]) = new ListMonoid(l)
    }

    def main(args: Array[String]) : Unit = {

        import ListMonoidOps._

        def io(s: String) = UIO{println(s"Hello! $s")} 

        val l1 = List(1,2,3)
        val l2 = List(4,5,6)

        val l3 = l1.mappend(l2)

        rts.unsafeRunSync(io(l3.toString))



    }
}