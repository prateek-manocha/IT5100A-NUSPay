package io.fscala.nusPay.client

import org.scalajs.dom.html.Div
import scalatags.JsDom.all._
import io.fscala.nusPay.shared.ItemService

case class CartDiv(lines: Set[CartLine]) {
  def content: Div = lines.foldLeft(div.render) { (a, b) =>
    a.appendChild(b.content).render
    a
  }

  def addItemService(line: CartLine): CartDiv = {
    CartDiv(this.lines + line)
  }

}

case class CartLine(qty: Int, items_service: ItemService) {
  def content: Div = div(`class` := "row", id := s"cart-${items_service.code}-row")(
    div(`class` := "col-1")(getDeleteButton),
    div(`class` := "col-2")(getQuantityInput),
    div(`class` := "col-6")(getItemServiceLabel),
    div(`class` := "col")(getPriceLabel)
  ).render

  private def getQuantityInput =  input(id := s"cart-${items_service.code}-qty", onchange := changeQty, value := qty.toString, `type` := "text", style := "width: 100%;").render

  private def getItemServiceLabel = label(items_service.name).render

  private def getPriceLabel = label(items_service.price * qty).render

  private def getDeleteButton = button(`type` := "button", onclick := removeFromCart)("X").render

  private def changeQty() = () => UIManager.updateItemService(items_service)

  private def removeFromCart() = () => UIManager.deleteItemService(items_service)
}