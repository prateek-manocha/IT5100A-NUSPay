package dao

import io.fscala.nusPay.shared
import io.fscala.nusPay.shared.{Cart, CartKey, ItemService, ItemServiceInCart}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


class ItemServiceDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def all(): Future[Seq[shared.ItemService]] = db.run(items_services.result)

  def insert(items_service: shared.ItemService): Future[Unit] = db.run(items_services insertOrUpdate items_service).map { _ => () }

  private class ItemServiceTable(tag: Tag) extends Table[shared.ItemService](tag, "PRODUCT") {
    def name = column[String]("NAME")

    def code = column[String]("CODE")

    def promotion = column[String]("PROMOTION")

    def price = column[Double]("PRICE")

    override def * = (name, code, promotion, price) <> (ItemService.tupled, ItemService.unapply)
  }

  private val items_services = TableQuery[ItemServiceTable]
}

class CartDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._


  def cart4(usr: String): Future[Seq[Cart]] = db.run(carts.filter(_.user === usr).result)

  def insert(cart: Cart): Future[_] = db.run(carts += cart)

  def remove(cart: ItemServiceInCart): Future[Int] = db.run(carts.filter(c => matchKey(c, cart)).delete)

  def update(cart: Cart): Future[Int] = {
    val q = for {
      c <- carts if matchKey(c, cart)
    } yield c.quantity
    db.run(q.update(cart.quantity))
  }

  private def matchKey(c: CartTable, cart: CartKey): Rep[Boolean] = {
    c.user === cart.user && c.items_serviceCode === cart.items_serviceCode
  }

  def all(): Future[Seq[Cart]] = db.run(carts.result)

  private class CartTable(tag: Tag) extends Table[Cart](tag, "CART") {

    def user = column[String]("USER")

    def items_serviceCode = column[String]("CODE")

    def quantity = column[Int]("QTY")

    override def * = (user, items_serviceCode, quantity) <> (Cart.tupled, Cart.unapply)
  }

  private val carts = TableQuery[CartTable]

}


