package pl.newicom.dddd.messaging.event

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.{DomainEvent, EntityId}
import pl.newicom.dddd.messaging.{AddressableMessage, Message}
import pl.newicom.dddd.utils.UUIDSupport._

case class EventMessage(
                  event: DomainEvent,
                     id: String = uuid,
              timestamp: DateTime = new DateTime,
            causationId: Option[EntityId] = None,
          correlationId: Option[EntityId] = None,
             deliveryId: Option[Long] = None,
             mustFollow: Option[EntityId] = None
                       ) extends Message with AddressableMessage {

  type MessageImpl = EventMessage

  override def destination = correlationId
  override def payload = event

  override def toString: String = {
    s"EventMessage(event = $event, id = $id, timestamp = $timestamp)"
  }

  def withDeliveryId(deliveryId: Long) = copy(deliveryId = Option(deliveryId))
  def withCorrelationId(correlationId: EntityId) = copy(correlationId = Option(correlationId))
  def withCausationId(causationId: EntityId) = copy(causationId = Option(causationId))
  def withMustFollow(mustFollow: Option[String]) = copy(mustFollow = mustFollow)

  def causedBy(msg: Message): EventMessage =
    copy(correlationId = msg.correlationId, causationId = Option(msg.id))
}