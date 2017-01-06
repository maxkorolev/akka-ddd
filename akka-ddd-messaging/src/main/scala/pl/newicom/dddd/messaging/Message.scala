package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.{Receipt, Processed, alod}

import scala.util.{Success, Try}


//val DeliveryId          = "_deliveryId"
//val CausationId         = "causationId"
//val CorrelationId       = "correlationId"
// contains ID of a message that the recipient of this message should process before it can process this message
//val MustFollow          = "_mustFollow"
//val SessionId           = "sessionId"



trait Message {

  type MessageImpl <: Message

  def id: String
  def deliveryId: Option[Long]
  def causationId: Option[EntityId]
  def correlationId: Option[EntityId]
  def mustFollow: Option[EntityId]       // contains ID of a message that the recipient of this message should process before it can process this message

  def withDeliveryId(deliveryId: Long): MessageImpl
  def withCorrelationId(correlationId: EntityId): MessageImpl
  def withCausationId(causationId: EntityId): MessageImpl
  def withMustFollow(mustFollow: Option[String]): MessageImpl

  def causedBy(msg: Message): MessageImpl

  def deliveryReceipt(result: Try[Any] = Success("OK")): Receipt = {
    deliveryId.map(id => alod.Processed(id, result)).getOrElse(Processed(result))
  }


}