package pl.newicom.dddd.writefront

import java.util.Date

import akka.actor.Actor
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import pl.newicom.dddd.aggregate.Command
import pl.newicom.dddd.http.JsonMarshalling
import pl.newicom.dddd.office.RemoteOfficeId
import pl.newicom.dddd.streams.ImplicitMaterializer
import pl.newicom.dddd.delivery.protocol.Processed
import io.circe.{ Decoder, Encoder }

import scala.util.{Failure, Success, Try}

trait HttpCommandHandler extends GlobalOfficeClientSupport with Directives with JsonMarshalling with ImplicitMaterializer {
  this: Actor =>

  type OfficeResponseToClientResponse = (Try[Any]) => ToResponseMarshallable

  import context.dispatcher
  implicit def timeout: Timeout

  def inOffice[A <: Command: Encoder: Decoder](officeId: RemoteOfficeId[A]): Route = {
    post {
      entity(as[A]) { command =>
        complete {
          (officeActor(officeId) ? command).mapTo[Processed].map(_.result).map(toClientResponse)
        }
      }
    }
  }

  def toClientResponse: OfficeResponseToClientResponse = {
    case Success(result) =>
      StatusCodes.OK -> result.toString

    case Failure(ex) =>
      StatusCodes.InternalServerError -> ex.getMessage
  }
}
