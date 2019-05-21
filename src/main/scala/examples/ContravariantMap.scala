package examples

import scalaz._, Scalaz._

object ContravariantMap {

// Example of contravariant using Scalaz
  trait Printable[A] {
    def format(value: A): String
  }

  // Contravariant printable
  // This implements a contravariant for type Printable (providing the contramap)
  // Contramap let's us map over an A when we have a B and a function to map B to A
  object Printable {
    implicit val contraPrintable = new Divide[Printable] {
      def contramap[A, B](fa: Printable[A])(f: B => A): Printable[B] = new Printable[B] {
        def format(b: B): String =
          fa.format(f(b))
      }

      def divide2[A1, A2, Z](fa1: =>Printable[A1], fa2: =>Printable[A2])(f: Z => (A1, A2)): Printable[Z] = new Printable[Z] {
        def format(z: Z): String = {
          val (a1,a2) = f(z)
          val a1f = fa1.format(a1)
          val a2f = fa2.format(a2)
          s"Divided! $a1f $a2f"
        }
      }

    }
  }

  def format[A](value: A)(implicit p: Printable[A]): String =
    p.format(value)

  implicit val stringPrintable =
    new Printable[String] {
      def format(value: String): String =
        s"String! >$value<"
    }

  implicit val booleanPrintable =
    new Printable[Boolean] {
      def format(value: Boolean): String =
        if (value) "yes" else "no"
    }

  implicit val intPrintable =
    new Printable[Int] {
      def format(value: Int): String =
        s"Int! >$value<"
    }  

  // Example for contramap
  final case class Box[A](value: A)

  // Example for divide needs two things...
  final case class TwoBox[A,B](a: A, b: B)

  // Generic printables for boxes 
  implicit def boxPrintable[A](implicit p: Printable[A]): Printable[Box[A]] =
    p.contramap(_.value)

  implicit def twoBoxPrintable[A,B](implicit p: Printable[A], q: Printable[B]): Printable[TwoBox[A,B]] =
    Printable.contraPrintable.divide2(p,q)(twoBox => (twoBox.a, twoBox.b))

  def main(args: Array[String]): Unit = {
    println(format(Box("Hello, World!")))
    println(format(Box(true)))
    println(format(Box(3117)))
    println(format(TwoBox("Justin", 1871)))
    println(format(TwoBox(false, "Jimmy")))
  }
}
