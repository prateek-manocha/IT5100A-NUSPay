package io.fscala.nusPay.shared

object SharedMessages {
  def itWorks = "It works!"
}


sealed trait ActionOnCart

case object Add extends ActionOnCart

case object Remove extends ActionOnCart

sealed trait WebsocketMessage

case class CartEvent(user: String, items_service: ItemService, action: ActionOnCart) extends WebsocketMessage

case class Alarm(message: String, action: ActionOnCart)