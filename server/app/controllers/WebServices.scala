package controllers

import dao.{CartDao, ItemServiceDao}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.fscala.nusPay.shared.{Cart, ItemService, ItemServiceInCart}
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.circe.Circe
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
@Api(value = "Student Cart and NUS provided services and items related API documentation")
class WebServices @Inject()(cc: ControllerComponents, items_serviceDao: ItemServiceDao, cartsDao: CartDao) extends AbstractController(cc) with Circe {


  val recoverError: PartialFunction[Throwable, Result] = {
    case e: org.h2.jdbc.JdbcSQLException =>
      Logger.error("Inserting duplicate in the database", e)
      BadRequest("Cannot insert duplicates in the database")
    case e: Throwable =>
      Logger.error("Error while writing in the database", e)
      InternalServerError("Cannot write in the database")
  }

  // *********** User Controler ******** //
  @ApiOperation(value = "Login to the service", consumes = "text/plain")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "Create a session for this user",
      required = true,
      dataType = "java.lang.String", // complete path
      paramType = "body"
    )
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "login success"), new ApiResponse(code = 400, message = "Invalid user name supplied")))
  def login() = Action { request =>
    request.body.asText match {
      case None => BadRequest
      case Some(user) => Ok.withSession("user" -> user)
    }
  }

  // *********** CART Controler ******** //
  @ApiOperation(value = "List the items_service in the cart", consumes = "text/plain")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "ItemService added"),
    new ApiResponse(code = 401, message = "unauthorized, please login before to proceed"),
    new ApiResponse(code = 500, message = "Internal server error, database error")))
  def listCartItemServices(): Action[AnyContent] = Action.async { request =>
    val userOption = request.session.get("user")
    userOption match {
      case Some(user) =>
        Logger.info(s"User '$user' is asking for the list of items_service in the cart")
        val futureInsert = cartsDao.cart4(user)

        futureInsert.map(items_services => Ok(items_services.asJson)).recover(recoverError)
      case None => Future.successful(Unauthorized)
    }
  }

  @ApiOperation(value = "Delete a items_service from the cart", consumes = "text/plain")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "ItemService delete from the cart"),
    new ApiResponse(code = 401, message = "unauthorized, please login before to proceed"),
    new ApiResponse(code = 500, message = "Internal server error, database error")))
  def deleteCartItemService(@ApiParam(name = "id", value = "The items_service code", required = true) id: String): Action[AnyContent] = Action.async { request =>
    val userOption = request.session.get("user")
    userOption match {
      case Some(user) =>
        Logger.info(s"User '$user' is asking to delete the items_service '$id' from the cart")
        val futureInsert = cartsDao.remove(ItemServiceInCart(user, id))
        futureInsert.map(_ => Ok).recover(recoverError)
      case None => Future.successful(Unauthorized)
    }
  }

  @ApiOperation(value = "Add a items_service in the cart", consumes = "text/plain")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "ItemService added in the cart"),
    new ApiResponse(code = 400, message = "Cannot insert duplicates in the database"),
    new ApiResponse(code = 401, message = "unauthorized, please login before to proceed"),
    new ApiResponse(code = 500, message = "Internal server error, database error")))
  def addCartItemService(@ApiParam(name = "id", value = "The items_service code", required = true) id: String, @ApiParam(name = "quantity", value = "The quantity to add", required = true) quantity: String): Action[AnyContent] = Action.async { request =>
    val userOption = request.session.get("user")
    userOption match {
      case Some(user) =>
        Logger.info(s"User '$user' is adding $quantity times the items_service'$id' in it's cart")
        val futureInsert = cartsDao.insert(Cart(user, id, quantity.toInt))
        futureInsert.map(_ => Ok).recover(recoverError)
      case None => Future.successful(Unauthorized)
    }
  }

  @ApiOperation(value = "Update a items_service quantity in the cart", consumes = "text/plain")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "ItemService updated in the cart"),
    new ApiResponse(code = 401, message = "unauthorized, please login before to proceed"),
    new ApiResponse(code = 500, message = "Internal server error, database error")))
  def updateCartItemService(@ApiParam(name = "id", value = "The items_service code", required = true, example = "SOC1") id: String, @ApiParam(name = "quantity", value = "The quantity to update", required = true) quantity: String): Action[AnyContent] = Action.async { request =>
    val userOption = request.session.get("user")
    userOption match {
      case Some(user) =>
        Logger.info(s"User '$user' is updating the items_service'$id' in it's cart with a quantity of $quantity")
        val futureInsert = cartsDao.update(Cart(user, id, quantity.toInt))
        futureInsert.map(_ => Ok).recover(recoverError)
      case None => Future.successful(Unauthorized)
    }
  }

  // *********** ItemService Controler ******** //
  @ApiOperation(value = "List all the items_services")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "The list of all the items_service")))
  def listItemService(): Action[AnyContent] = Action.async { _ =>
    val futureItemServices = items_serviceDao.all()
    for (
      items_services <- futureItemServices
    ) yield Ok(items_services.asJson)
  }

  @ApiOperation(value = "Add a items_service", consumes = "text/plain")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "The items_service to add",
      required = true,
      dataType = "io.fscala.nusPay.shared.ItemService", // complete path
      paramType = "body"
    )
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "ItemService added"),
    new ApiResponse(code = 400, message = "Invalid body supplied"),
    new ApiResponse(code = 500, message = "Internal server error, database error")))
  def addItemService(): Action[AnyContent] = Action.async { request =>
    val items_serviceOrNot = decode[ItemService](request.body.asText.getOrElse(""))
    items_serviceOrNot match {
      case Right(items_service) =>
        val futureInsert = items_serviceDao.insert(items_service)
        futureInsert.map(_ => Ok).recover(recoverError)
      case Left(error) =>
        Logger.error("Error while adding a items_service", error)
        Future.successful(BadRequest)
    }
  }

}
