package io.fscala.nusPay.client


import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.fscala.nusPay.shared._
import org.querki.jquery._
import org.scalajs.dom
import org.scalajs.dom.html.Document
import org.scalajs.dom.raw.{CloseEvent, Event, MessageEvent, WebSocket}

import scala.scalajs.js.UndefOr
import scala.util.{Random, Try}


object UIManager {

  val origin: UndefOr[String] = dom.document.location.origin
  val cart: CartDiv = CartDiv(Set.empty[CartLine])

  val webSocket: WebSocket = getWebSocket

  val dummyUserName = s"user-${Random.nextInt(1000)}"


  def main(args: Array[String]): Unit = {
    val settings = JQueryAjaxSettings.url(s"$origin/nuspay/login").data(dummyUserName).contentType("text/plain")
    $.post(settings._result).done((_: String) => {
      initUI(origin)
    })
  }

  private def initUI(origin: UndefOr[String]) = {

    $.get(url = s"$origin/nuspay/items_services", dataType = "text")
      .done((answers: String) => {
        val items_services = decode[Seq[ItemService]](answers)
        items_services.right.map { seq =>
          seq.foreach(p => {
            $("#items_services").append(ItemServiceDiv(p).content)
          })
          initCartUI(origin, seq)
        }
      })
      .fail((xhr: JQueryXHR, textStatus: String, textError: String) =>
        println(s"call failed: $textStatus with status code: ${xhr.status} $textError")
      )
  }

  private def initCartUI(origin: UndefOr[String], items_services: Seq[ItemService]) = {
    $.get(url = s"$origin/nuspay/cart/items_services", dataType = "text")
      .done((answers: String) => {
        val carts = decode[Seq[Cart]](answers)
        carts.right.map { cartLines =>
          cartLines.foreach { cartDao =>
            val items_service = items_services.find(_.code == cartDao.items_serviceCode)
            items_service match {
              case Some(p) =>
                val cartLine = CartLine(cartDao.quantity, p)
                val cartContent = UIManager.cart.addItemService(cartLine).content
                $("#cartPanel").append(cartContent)
              case None =>
                println(s"items_service code ${cartDao.items_serviceCode} doesn't exists in the catalog")
            }
          }
        }
      })
      .fail((xhr: JQueryXHR, textStatus: String, textError: String) =>
        println(s"call failed: $textStatus with status code: ${xhr.status} $textError")
      )
  }

  def addOneItemService(items_service: ItemService): JQueryDeferred = {
    val quantity = 1

    def onDone = () => {
      val cartContent = cart.addItemService(CartLine(quantity, items_service)).content
      $("#cartPanel").append(cartContent)
      println(s"ItemService $items_service added in the cart")
      webSocket.send(CartEvent(dummyUserName, items_service, Add).asJson.noSpaces)
    }

    postInCart(items_service.code, quantity, onDone)
  }

  def updateItemService(items_service: ItemService): JQueryDeferred = {
    putInCart(items_service.code, quantity(items_service.code))
  }

  def deleteItemService(items_service: ItemService): JQueryDeferred = {
    def onDone = () => {
      val cartContent = $(s"#cart-${items_service.code}-row")
      cartContent.remove()
      webSocket.send(CartEvent(dummyUserName, items_service, Remove).asJson.noSpaces)
      println(s"ItemService ${items_service.code} removed from the cart")
    }

    deletefromCart(items_service.code, onDone)
  }

  private def getWebSocket: WebSocket = {
    val ws = new WebSocket(getWebsocketUri(dom.document, "nuspay/cart/events"))
    ws.onopen = { (event: Event) ⇒
      println(s"webSocket.onOpen '${event.`type`}'")
      event.preventDefault()
    }

    ws.onerror = { (event: Event) =>
      System.err.println(s"webSocket.onError '${event.getClass}'")
    }

    ws.onmessage = { (event: MessageEvent) =>
      println(s"[webSocket.onMessage] '${event.data.toString}'...")
      val msg = decode[Alarm](event.data.toString)
      msg match {
        case Right(alarm) =>
          println(s"[webSocket.onMessage]  Got alarm event : $alarm)")
          notify(alarm)
        case Left(e) =>
          println(s"[webSocket.onMessage] Got a unknown event : $msg)")
      }
    }

    ws.onclose = { (event: CloseEvent) ⇒
      println(s"webSocket.onClose '${event.`type`}'")
    }
    ws
  }

  private def notify(alarm: Alarm): Unit = {
    val notifyClass = if (alarm.action == Add) "info" else "warn"
    NotifyJS.notify(alarm.message, new Options {
      className = notifyClass
      globalPosition = "right bottom"
    })
  }

  private def quantity(items_serviceCode: String) = Try {
    val inputText = $(s"#cart-$items_serviceCode-qty")
    if (inputText.length != 0)
      Integer.parseInt(inputText.`val`().asInstanceOf[String])
    else 1
  }.getOrElse(1)

  private def postInCart(items_serviceCode: String, quantity: Int, onDone: () => Unit) = {
    val url = s"${UIManager.origin}/nuspay/cart/items_services/$items_serviceCode/quantity/$quantity"
    $.post(JQueryAjaxSettings.url(url)._result)
      .done(onDone)
      .fail(() => println("cannot add a items_service twice"))
  }

  private def putInCart(items_serviceCode: String, updatedQuantity: Int) = {
    val url = s"${UIManager.origin}/nuspay/cart/items_services/$items_serviceCode/quantity/$updatedQuantity"
    $.ajax(JQueryAjaxSettings.url(url).method("PUT")._result)
      .done()
  }

  private def deletefromCart(items_serviceCode: String, onDone: () => Unit) = {
    val url = s"${UIManager.origin}/nuspay/cart/items_services/$items_serviceCode"
    $.ajax(JQueryAjaxSettings.url(url).method("DELETE")._result)
      .done(onDone)
  }

  private def getWebsocketUri(document: Document, context: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"

    s"$wsProtocol://${
      dom.document.location.host
    }/$context"
  }

}
