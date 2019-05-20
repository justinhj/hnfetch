package examples

import scalaz._, Scalaz._

object ContravariantMap {

// Example of contravariant

  trait Printable[A] {

    self =>

    def format(value: A): String

    //def contramap[A,B](r: Printable[A])(f: B => A): Printable[B] = ???

    def contramap[B](f: B => A): Printable[B] =
      new Printable[B] {
        def format(b: B): String = {
          val a = f(b)
          self.format(a)
        }
      }
  }

  //implicit val printable[A,B](implicit p: Printable[A]) = p.contramap[B](b => b.value)

//   implicit class printableContravariant[A,B](b : B)(implicit fa: Printable[a])
//   new Contravariant[Printable] {
//     def contramap[A, B](fa: Printable[A])(f: B => A) = {
//         new Printable[B] {
//             b => fa.format(f(b))
//         }
//     }
//   }

  def format[A](value: A)(implicit p: Printable[A]): String =
    p.format(value)

  implicit val stringPrintable =
    new Printable[String] {
      def format(value: String): String =
        "\"" + value + "\""
    }

  implicit val booleanPrintable =
    new Printable[Boolean] {
      def format(value: Boolean): String =
        if (value) "yes" else "no"
    }

  final case class Box[A](value: A)

  implicit val boxPrintableString = stringPrintable.contramap[Box[String]](b => b.value)
  implicit val boxPrintableBool   = booleanPrintable.contramap[Box[Boolean]](b => b.value)

  def main(args: Array[String]): Unit = {
    println(format(Box("Hello, World!")))
    println(format(Box(true)))
  }

}
