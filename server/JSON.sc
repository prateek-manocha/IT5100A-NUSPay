import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import io.fscala.nusPay.shared.ItemService

val newItemService = ItemService("NewOne","New","The brand new items_service", 100.0)

newItemService.asJson

val json = """{
                |  "name" : "NewOne",
                |  "code" : "New",
                |  "promotion" : "The brand new items_service",
                |  "price" : 100.0
                |}""".stripMargin

decode[ItemService](json)