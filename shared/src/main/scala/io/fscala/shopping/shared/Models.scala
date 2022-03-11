package io.fscala.nusPay.shared

case class ItemService(name: String, code : String, promotion : String, price: Double)

abstract class CartKey {
  def user: String
  def items_serviceCode: String
}

case class ItemServiceInCart(user:String, items_serviceCode: String) extends CartKey

case class Cart(user:String, items_serviceCode: String, quantity: Int) extends CartKey

case class User(sessionID: String)
