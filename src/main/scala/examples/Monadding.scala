package org.example

object Monadding {

  // A monad interface

  trait JMonad[F[_]] extends Any {
    def unit[A](a : A) : F[A]
    def flatMap[A,B](fa : F[A])(f: A => F[B]) : F[B]

    // Implemented in terms of above

    def map2[A,B,C](ma: F[A], mb: F[B])(f: (A, B) => C): F[C] = {

      flatMap(ma){
        a =>
          flatMap(mb) {
            b =>
              unit(f(a,b))
          }
      }

    }

    def sequence[A](lma: List[F[A]]): F[List[A]] =
      lma.foldRight(unit(List[A]()))((ma, mla) => map2(ma, mla)(_ :: _))

    def traverse[A,B](la: List[A])(f: A => F[B]): F[List[B]] =
      la.foldRight(unit(List[B]()))((a, mlb) => map2(f(a), mlb)(_ :: _))

    // Using `sequence` and the `List.fill` function of the standard library:
    def replicateM[A](n: Int, ma: F[A]): F[List[A]] =
      sequence(List.fill(n)(ma))


    def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
      a => flatMap(f(a))(g)

    def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(ma => ma)

  }

  // Functor
  trait JFunctor[F[_]] extends Any {

    def map[A,B](fa: F[A])(f: A => B) : F[B]

  }

  implicit class ListOps[A](val fa : List[A]) extends AnyVal {

    // Does it make sense for this to be here?
    def unit(a: A)(implicit M : JMonad[List]): List[A] = M.unit(a)
  
    def flatMap[B](f: A => List[B])(implicit M : JMonad[List]): List[B] = {
      println("Justin flatMap!")
      M.flatMap(fa)(f)
    }
  
    def hello = "Hello from ListOps"
  
    def map[B](f: A => B)(implicit F : JFunctor[List]): List[B] = {
      println("Justin map!")
      F.map(fa)(f)
    }
  }

  // Temp
  // implicit class JFunctorOps[A, F[A]](t: F[A])(implicit fm: JFunctor[F]) {

  //   def hello = "Hello JFunctorOps"
  //   def map[B](f: A => B) : F[B] = {
  //     fm.map(t)(f)
  //   }
  // }

  // List implementation as an object
  // JListmonadfunctor.map(List(1,2,3)){_ + 1}

  implicit val listMonadFunctor = new JMonad[List] with JFunctor[List] {

    def unit[A](a: A): List[A] = List(a)

    def flatMap[A, B](fa: List[A])(f: A => List[B]): List[B] = {

      fa.foldLeft(List.empty[B]) {
        case (acc, a) =>
          f(a).head +: acc
      }.reverse

    }

    def map[A, B](fa: List[A])(f: A => B): List[B] = {

      fa.foldLeft(List.empty[B]) {
        case (acc, a) =>
          f(a) +: acc
      }.reverse

    }
  }


  // Implement for Option

  object JOptionMonadFunctor extends JMonad[Option] with JFunctor[Option] {

    def unit[A](a : A) = Some(a)

    def flatMap[A,B](fa: Option[A])(f: A => Option[B]) : Option[B] = {

      fa match {

        case Some(a) =>
          f(a)

        case None =>
          None

      }

    }

    // Define map in terms of flatMap
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = {

      flatMap(fa)(a => unit(f(a)))

    }
  }

  // Functor over a map with key type K and value type V

  // Victory: Using type lambda
  // We want to specialize the functor to be a map with keys of type A and values of another type
  // then we can make a functor implementation for any type of key -> value without making a monad for
  // every key type (Functor takes only one type param)
  //type JMapTL[V] = JFunctor[({type IntMap[K] = Map[K, V]})#IntMap]

  trait JMapFunctor[K] extends JFunctor[Map[K, ?]] {

    def map[A, B](fa: Map[K,A])(f: A => B): Map[K,B] = {

      fa.foldLeft(Map.empty[K,B]) {

        case (acc, (k,a)) =>
            acc updated (k, f(a))
      }

    }

  }

  object JLongMap extends JMapFunctor[Long]

  // JReader Monad
  // Note JReader[R, ?] is kind projector syntax and equivalent to ({type LT[V] = JReader[R, V]})#LT

  case class JReader[R,A](run : R => A)

  class JReaderMonadFunctor[R] extends JMonad[JReader[R, ?]] with JFunctor[JReader[R, ?]] {

      def unit[A](a: A): JReader[R, A] = {
        JReader(r => a)
      }

      def flatMap[A,B](st: JReader[R,A])(f: A => JReader[R,B]): JReader[R,B] = {

        JReader {

          s =>
            val reader = f(st.run(s))
            reader.run(s)

        }

      }

      def map[A, B](fa: JReader[R,A])(f: A => B): JReader[R,B] = {

        flatMap(fa)(a => unit(f(a)))

      }
    }

  // State data type

  case class JState[S, A](run: S => (A, S)) {

    def runS(s: S) : S = run(s)._2

    def runA(s: S) : A = run(s)._1

  }

  class JStateImp[S] extends JMonad[JState[S, ?]] with JFunctor[JState[S, ?]] {

    def unit[A](a: A) : JState[S,A] = JState{
      s =>
        (a, s)
    }

    def _map[A,B](fa: JState[S, A])(f : A => B)  : JState[S, B] = {

      JState {
        is => {
          val (a, s) = fa.run(is)
          (f(a), s)
        }
      }

    }

    def map[A,B](fa: JState[S, A])(f : A => B)  : JState[S, B] = {

        flatMap(fa)(a => unit(f(a)))

    }

    def flatMap[A,B](fa : JState[S, A])(f : A => JState[S, B]) : JState[S, B] = {

      JState[S, B](run =
        s => {

          val (a, s2) = fa.run(s)
          f(a).run(s2)
        })

    }

  }

  implicit class JStateOps[S,A](val fa : JState[S,A]) {

    def map[B](f : A => B)(implicit F : JFunctor[JState[S, ?]])  : JState[S, B] =
      F.map(fa)(f)

    def flatMap[B](f : A => JState[S, B])(implicit M : JMonad[JState[S, ?]]) : JState[S, B] =
      M.flatMap(fa)(f)

  }

  def stateFunctorMonad[S] = new JStateImp[S]

  // Examples

  case class Cat(name: String, yearOfBirth: Int)

  object sm extends JStateImp[Cat] 

  // implicit class 

  def ageCat(n : Int) = JState[Cat, Int]{s =>
    val newAge = s.yearOfBirth + n
    (newAge, s.copy(yearOfBirth = newAge))
  } 

  def catName() = JState[Cat, String](
   s => 
     (s.name, s)
  ) 

  val cats = List(Cat("Garfield", 1965), Cat("Winky", 1974))

  // Using the State monad in a for comprehension

  // Create an instance of the State Monad where Cat is the State
  implicit val sfmCat = stateFunctorMonad[Cat]

  val ageAndName = for (
    _ <- ageCat(10);
    r <- catName()
  ) yield  r




}
