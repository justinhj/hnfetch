package justinhj.hnfetch

import cats.effect.ConcurrentEffect
import fetch.{Data, DataSource, Fetch}
import justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNUser, HNUserID}
import scala.language.postfixOps

object HNDataSources {

  // Some constants to control the behaviour of Fetch executions
  // These could be moved to a config file in a real application

  //val batchSize = Some(1) // max concurrent requests of each data source, we don't support batching so set to 1
  //val executionType : ExecutionType = Parallel // whether to do batches concurrently or sequentially

  //import cats.data.NonEmptyList

  object HNUserSource extends Data[HNUserID, HNUser] {
    override def name = "user"

    def source[F[_] : ConcurrentEffect] = new DataSource[F, HNUserID, HNUser] {

      override def data = HNUserSource

      override def CF = ConcurrentEffect[F]

      override def fetch(id: HNUserID): F[Option[HNUser]] = {
          CF.delay(HNFetch.getUserSync(id).toOption)
        }
    }
  }

  object HNItemSource extends Data[HNItemID, HNItem] {
    override def name = "item"

    def source[F[_] : ConcurrentEffect] = new DataSource[F, HNItemID, HNItem] {

      override def data = HNItemSource

      override def CF = ConcurrentEffect[F]

      override def fetch(id: HNItemID): F[Option[HNItem]] = {
        CF.delay(HNFetch.getItemSync(id).toOption)
      }
    }
  }

      //      override def maxBatchSize : Option[Int] = batchSize

      //override def batchExecution : ExecutionType = executionType

//    override def fetchOne(id: HNUserID): Query[Option[HNUser]] = {
//
//      Query.sync(HNFetch.getUserSync(id) match {
//        case Right(a) => Some(a)
//        case Left(_) => None
//      })
//    }
//
//    // If the data source supports multiple queries (the HN API does not) you can implement it here
//    // otherwise you can just tell it to use the single one using this built in function...
//    override def fetchMany(ids: NonEmptyList[HNUserID]): Query[Map[HNUserID, HNUser]] = {
//      batchingNotSupported(ids)
//    }
//  }

//  implicit object HNItemSource extends DataSource[HNItemID, HNItem]{
//    override def name = "item"
//
//    override def maxBatchSize : Option[Int] = batchSize
//    override def batchExecution : ExecutionType = executionType
//
//    override def fetchOne(id: HNItemID): Query[Option[HNItem]] = {
//      Query.sync(HNFetch.getItemSync(id) match {
//        case Right(a) => Some(a)
//        case Left(_) => None
//      })
//    }

//    // If the data source supports multiple queries (the HN API does not) you can implement it here
//    // otherwise you can just tell it to use the single one using this built in function...
//    override def fetchMany(ids: NonEmptyList[HNItemID]): Query[Map[HNItemID, HNItem]] = {
//      batchingNotSupported(ids)
//    }
//  }

  def getUser[F[_] : ConcurrentEffect](id: HNUserID): Fetch[F, HNUser] = Fetch(id, HNUserSource.source)
  def getItem[F[_] : ConcurrentEffect](id: HNItemID): Fetch[F, HNItem] = Fetch(id, HNItemSource.source)

}
