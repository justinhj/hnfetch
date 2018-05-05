package justinhj.hnfetch

import fetch.{DataSource, ExecutionType, Fetch, Query, Parallel}
import justinhj.hnfetch.HNFetch.{HNItem, HNItemID, HNUser, HNUserID}
import scala.language.postfixOps

object HNDataSources {

  // Some constants to control the behaviour of Fetch executions
  // These could be moved to a config file in a real application

  val batchSize = Some(1) // max concurrent requests of each data source, we don't support batching so set to 1
  val executionType : ExecutionType = Parallel // whether to do batches concurrently or sequentially

  import cats.data.NonEmptyList

  implicit object HNUserSource extends DataSource[HNUserID, HNUser]{
    override def name = "user"

    override def maxBatchSize : Option[Int] = batchSize
    override def batchExecution : ExecutionType = executionType

    override def fetchOne(id: HNUserID): Query[Option[HNUser]] = {

      Query.sync(HNFetch.getUser(id) match {
        case Right(a) => Some(a)
        case Left(_) => None
      })
    }

    // If the data source supports multiple queries (the HN API does not) you can implement it here
    // otherwise you can just tell it to use the single one using this built in function...
    override def fetchMany(ids: NonEmptyList[HNUserID]): Query[Map[HNUserID, HNUser]] = {
      batchingNotSupported(ids)
    }
  }

  implicit object HNItemSource extends DataSource[HNItemID, HNItem]{
    override def name = "item"

    override def maxBatchSize : Option[Int] = batchSize
    override def batchExecution : ExecutionType = executionType

    override def fetchOne(id: HNItemID): Query[Option[HNItem]] = {
      Query.sync(HNFetch.getItem(id) match {
        case Right(a) => Some(a)
        case Left(_) => None
      })
    }

    // If the data source supports multiple queries (the HN API does not) you can implement it here
    // otherwise you can just tell it to use the single one using this built in function...
    override def fetchMany(ids: NonEmptyList[HNItemID]): Query[Map[HNItemID, HNItem]] = {
      batchingNotSupported(ids)
    }
  }

  def getUser(id: HNUserID): Fetch[HNUser] = Fetch(id)
  def getItem(id: HNItemID): Fetch[HNItem] = Fetch(id)

}
