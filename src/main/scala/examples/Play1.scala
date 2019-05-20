package examples

import scalaz._, Scalaz._
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.random.Random
import scalaz.zio.system.System
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.DefaultRuntime
import scalaz.zio._

object Play1 {
    type Env = Clock with Console with System with Random with Blocking 

    // A runtime instance
    implicit val rts: Runtime[Env] = new DefaultRuntime {
    override val Platform: Platform = PlatformLive.makeDefault().withReportFailure(_ => ())
    } 

    // Our own monoid typeclass. Use a trait to describe the operations that make up a monoid...
    trait Monoid[A, F[_]] extends Any {
        def zero : F[A]
        def mappend(a : F[A]) : F[A]
    }

    //
    implicit class ListMonoidOps[A](val l : List[A]) extends AnyVal with Monoid[A, List] {
        def zero : List[A] = List.empty[A]

        def |+|(app : List[A]) : List[A] = mappend(app)

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

    // This play is about Scalaz Zip
    // An implementation is needed for the Zip.zip function, you can see the sig
    // obviously this is closely related to Apply2

    // Zip for Task
    implicit object TaskZip extends Zip[Task] {
        def zip[A, B](a: => Task[A], b: => Task[B]): Task[(A, B)] = 
            a.flatMap(a2 => b.flatMap(b2 => Task((a2,b2))))
    }
    //implicit val taskZipper = new TaskZip 

    def saySomething(something: String) : Task[Int] = Task {
        println(s"Saying: $something")
        something.length
      } 

    // Apply
    // Apply[List].ap(List[Int](1,2,3))(List({(a : Int) => a + 1}, {(a : Int) => a - 1})) 

    // With angry parent operator
    // List[Int](1,2,3) <*> List({(a : Int) => a + 1}, {(a : Int) => a - 1}) 
    // res3: List[Int] = List(2, 3, 4, 0, 1, 2)

    // A sample product
    case class Foo(s: String, i: Int)

    // Divide splits a product (covariant)
    implicit val fooEqual: Equal[Foo] =
           Divide[Equal].divide2(Equal[String], Equal[Int]) {
             (foo: Foo) => (foo.s, foo.i)
    }

    val e1 = Equal[String]


    def main(args: Array[String]) : Unit = {

        def io(s: String) = UIO{println(s"Hello! $s")} 

        val l1 = List(1,2,3)
        val l2 = List(4,5,6)

        val l3 = l1 mappend l2

        val l4 = l1 |+| l3

        val hello = Zip[List].zip(List("Hello"),List("I like cats")) 
        val test : Task[Int] = Zip[Task].zip(saySomething("Hello"),saySomething("I like dogs")).flatMap(a => 
            saySomething(s"Zipper ${a._1.toString} and ${a._2.toString}")
        )

        rts.unsafeRunSync(io(l3.toString) *> io(l4.toString) *> io(hello.toString) *> test)

    }
}