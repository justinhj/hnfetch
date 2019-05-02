package justinhj.hnfetch

import cats.effect.ConcurrentEffect
import fetch.{ Data, DataSource, Fetch }
import justinhj.hnfetch.HNFetch.{ HNItem, HNItemID, HNUser, HNUserID }
import scala.language.{ higherKinds, postfixOps }

object HNDataSources {

  object HNUserSource extends Data[HNUserID, HNUser] {
    override def name = "user"

    def source[F[_]: ConcurrentEffect]: DataSource[F, HNUserID, HNUser] {
      def fetch(id: HNUserID): F[Option[HNUser]]

      def CF: ConcurrentEffect[F]

      def data: HNUserSource.type
    } = new DataSource[F, HNUserID, HNUser] {

      override def data = HNUserSource

      override def CF = ConcurrentEffect[F]

      override def fetch(id: HNUserID): F[Option[HNUser]] =
        CF.delay(HNFetch.getUserSync(id).toOption)
    }
  }

  object HNItemSource extends Data[HNItemID, HNItem] {
    override def name = "item"

    def source[F[_]: ConcurrentEffect] = new DataSource[F, HNItemID, HNItem] {

      override def data = HNItemSource

      override def CF = ConcurrentEffect[F]

      override def fetch(id: HNItemID): F[Option[HNItem]] =
        CF.delay(HNFetch.getItemSync(id).toOption)
    }
  }

  def getUser[F[_]: ConcurrentEffect](id: HNUserID): Fetch[F, HNUser] = Fetch(id, HNUserSource.source)
  def getItem[F[_]: ConcurrentEffect](id: HNItemID): Fetch[F, HNItem] = Fetch(id, HNItemSource.source)

}
