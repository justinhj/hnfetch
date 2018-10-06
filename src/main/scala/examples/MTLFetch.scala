package examples

import cats._
import cats.data._
import cats.mtl._
import cats.implicits._
import cats.mtl.implicits._
import cats.effect.IO

object MTLFetch {

  case class Cat(name: String, yearOfBirth: Int)
  val sampleCat1 = Cat("Garfield", 1965)
  val sampleCat2 = Cat("Winky", 1973)

  type Log = Vector[String]

  implicit val showLog: Show[Log] = Show.show{
    l =>
      "Log\n===\n" ++
      l.foldLeft("") {_ + _ + "\n"}
  }

  def program[F[_] : Monad](implicit ft : FunctorTell[F, Log], aa: ApplicativeAsk[F, Cat]) : F[Int] = {

    for(
      name <- aa.ask.map(cat => cat.name);
      year <- aa.ask.map(cat => cat.yearOfBirth);
      _ <- ft.tell(Vector("I did it!"));
      _ <- ft.tell(Vector("You did what?"));
      _ <- ft.tell(Vector(s"Found the cat called ${name}'s born in $year"))
    ) yield (2018 - year)
  }

  def main(args: Array[String]): Unit = {

    // Materialize the program with monad transformer instances

    val w = program[ReaderT[WriterT[Id, Log, ?], Cat, ?]].run(sampleCat2)

    val (log, result) = w.run

    println(s"Program result was $result")
    println(log.show)

    // (53,Vector(I did it!, You did what?, Found the cat called Garfield's age is 1965))

  }
}
