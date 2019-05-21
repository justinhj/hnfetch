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
    implicit val contraPrintable = new Contravariant[Printable] {
      def contramap[A, B](fa: Printable[A])(f: B => A): Printable[B] = new Printable[B] {
        def format(b: B): String =
          fa.format(f(b))
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

  final case class Box[A](value: A)

  implicit def boxPrintable[A](implicit p: Printable[A]): Printable[Box[A]] =
    p.contramap(_.value)

  def main(args: Array[String]): Unit = {
    println(format(Box("Hello, World!")))
    println(format(Box(true)))
    println(format(Box(3117)))
  }
}
