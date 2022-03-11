

import dao.ItemServiceDao
import io.fscala.nusPay.shared.ItemService
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application


class ItemServiceDaoSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {
  "ItemServiceDao" should {
    "Have default rows on database creation" in {
      val app2dao = Application.instanceCache[ItemServiceDao]
      val dao: ItemServiceDao = app2dao(app)

      val expected = Set(
        ItemService("DECK", "SOC1", "Deck is a one stop food court catering the SOC fraternity!", 5),
        ItemService("FineFoods", "UT1", "Lucky DRAW! First 20 users get additional promo. Fine video is waiting to serve you!", 8),
        ItemService("Flavours", "UT2", "Hungry? Try the mouth watering flavours. Limted 10% discount available on everything.", 10)
      )

      dao.all().futureValue should contain theSameElementsAs (expected)
    }
  }
}
