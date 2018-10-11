package examples

import cats._
import cats.data._
import cats.effect.IO
import cats.mtl._
import cats.implicits._
import cats.mtl.implicits._

// Demo of a simple monad stack with Writer, Reader and Either

object MTLCatSample {

  case class Cat(name: String, yearOfBirth: Int)

  val sampleCat1 = Cat("Binky", 2012)
  val sampleCat2 = Cat("Winky", 1973)

  type Log = Vector[String]

  implicit val showLog: Show[Log] = Show.show{
    l =>
      "Log\n===\n" ++
      l.foldLeft("") {_ + _ + "\n"}
  }

  def program[F[_] : Monad](implicit ft : FunctorTell[F, Log], aa: ApplicativeAsk[F, Cat], fr: FunctorRaise[F, String]) : F[Int] = {

    for(
      _ <- ft.tell(Vector("Reading cat details"));
      name <- aa.ask.map(cat => cat.name);
      year <- aa.ask.map(cat => cat.yearOfBirth);
      _ <- ft.tell(Vector("Calculating age"));
      age = 2018 - year;
      _ <- ft.tell(Vector(s"Found the cat called $name, born in $year"));
      _ <- if(age > 25) fr.raise(s"Cat appears to be too old ($age)") else age.pure[F]
    ) yield age
  }

  def main(args: Array[String]): Unit = {

    // Materialize the program with monad transformer instances

    // Note the effect type is IO, it could easily be Id or something else

    type IOLogWriter[A] = WriterT[IO, Log, A]
    type IOStringEither[A] = EitherT[IOLogWriter, String, A]
    type IOCatReader[A] = ReaderT[IOStringEither, Cat, A]

    // Can be written inline as ReaderT[EitherT[WriterT[IO, Log, ?], String, ?], Cat, ?]

    val p = program[IOCatReader]

    val (log, eitherResult) = p.run(sampleCat1).value.run.unsafeRunSync

     println(s"Program result was $eitherResult")
     println(log.show)

    val p1 = List(sampleCat1, sampleCat2).traverse(catP => p.run(catP).value.run)

    p1.unsafeRunSync.foreach {
      case (log, result) =>
        println(s"Program result was $result")
        println(log.show)
        
    }


  }
}
