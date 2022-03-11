import io.circe.generic.auto._
import io.circe.parser._
import io.fscala.nusPay.shared.Cart
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.{DefaultWSCookie, WSClient}
import play.api.test.Helpers._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class APISpec extends PlaySpec with ScalaFutures with GuiceOneServerPerSuite {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(100, Millis))

  val baseURL = s"localhost:$port/nuspay"
  val items_servicesURL = s"http://$baseURL/items_services"
  val addItemServicesURL = s"http://$baseURL/items_services/add"
  val items_servicesInCartURL = s"http://$baseURL/cart/items_services"

  def deleteItemServiceInCartURL(items_serviceID: String) = s"http://$baseURL/cart/items_services/$items_serviceID"

  def actionItemServiceInCartURL(items_serviceID: String, quantity: Int) = s"http://$baseURL/cart/items_services/$items_serviceID/quantity/$quantity"

  val login = s"http://$baseURL/login"


  "The API" should {
    val wsClient = app.injector.instanceOf[WSClient]


    "list all the items_service" in {

      val response = wsClient.url(items_servicesURL).get().futureValue
      println(response.body)
      response.status mustBe OK
      response.body must include("DECK")
      response.body must include("FineFoods")
      response.body must include("Flavours")

    }

    "add a items_service" in {

      val newItemService =
        """
                    {
                         "name" : "NewOne",
                         "code" : "New",
                         "promotion" : "The brand new items_service",
                         "price" : 100.0
                    }
      """

      val posted = wsClient.url(addItemServicesURL).post(newItemService).futureValue
      posted.status mustBe OK

      val response = wsClient.url(items_servicesURL).get().futureValue
      println(response.body)
      response.body must include("NewOne")
    }

    lazy val defaultCookie = {
      val loginCookies = Await.result(wsClient.url(login).post("me")
        .map(p => p.headers.get("Set-Cookie").map(
          _.head.split(";").head)), 1 seconds)
      val play_session = loginCookies.get.split("=").tail.mkString("")

      DefaultWSCookie("PLAY_SESSION", play_session)
    }

    "list all the items_services in a cart" in {
      val response = wsClient.url(items_servicesInCartURL)
        .addCookies(defaultCookie).get().futureValue
      println(response)
      response.status mustBe OK

      val listOfItemService = decode[Seq[Cart]](response.body)
      listOfItemService.right.get mustBe empty
    }

    "add a items_service in the cart" in {
      val items_serviceID = "SOC1"
      val quantity = 1
      val posted = wsClient.url(actionItemServiceInCartURL(items_serviceID, quantity))
        .addCookies(defaultCookie).post("").futureValue
      posted.status mustBe OK

      val response = wsClient.url(items_servicesInCartURL)
        .addCookies(defaultCookie).get().futureValue
      println(response)
      response.status mustBe OK
      response.body must include("SOC1")
    }

    "delete a items_service from the cart" in {
      val items_serviceID = "SOC1"
      val posted = wsClient.url(deleteItemServiceInCartURL(items_serviceID))
        .addCookies(defaultCookie).delete().futureValue
      posted.status mustBe OK

      val response = wsClient.url(items_servicesInCartURL)
        .addCookies(defaultCookie).get().futureValue
      println(response)
      response.status mustBe OK
      response.body mustNot include("SOC1")
    }

    "update a items_service quantity in the cart" in {
      val items_serviceID = "SOC1"
      val quantity = 1
      val posted = wsClient.url(actionItemServiceInCartURL(items_serviceID, quantity))
        .addCookies(defaultCookie).post("").futureValue
      posted.status mustBe OK

      val newQuantity = 99
      val update = wsClient.url(actionItemServiceInCartURL(items_serviceID, newQuantity))
        .addCookies(defaultCookie).put("").futureValue
      update.status mustBe OK

      val response = wsClient.url(items_servicesInCartURL)
        .addCookies(defaultCookie).get().futureValue
      println(response)
      response.status mustBe OK
      response.body must include(items_serviceID)
      response.body must include(newQuantity.toString)
    }

    "return a cookie when a user logins" in {
      val cookieFuture = wsClient.url(login).post("myID").map {
        response =>
          response.headers.get("Set-Cookie").map(
            header => header.head.split(";")
              .filter(_.startsWith("PLAY_SESSION")).head)
      }

      val play_session_Key = cookieFuture.futureValue.get.split("=").head
      play_session_Key must equal("PLAY_SESSION")
    }
  }

}
