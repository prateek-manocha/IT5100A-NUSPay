package io.fscala.nusPay.client

import io.fscala.nusPay.shared.ItemService
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._


case class ItemServiceDiv(items_service: ItemService) {
  def content: Div = div(`class` := "col")(getItemServicePromotion, getButton).render

  private def getItemServicePromotion =
    div(
      p(items_service.name),
      p(items_service.promotion),
      p(items_service.price))


  private def getButton = button(`type` := "button", onclick := addToCart)("Add to Cart")

  private def addToCart() = () => UIManager.addOneItemService(items_service)
}
