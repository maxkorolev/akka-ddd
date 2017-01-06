package pl.newicom.dddd.messaging.command

import java.util.Date

import org.joda.time.DateTime
import pl.newicom.dddd.aggregate.{Command, EntityId}
import pl.newicom.dddd.messaging.{AddressableMessage, Message}
import pl.newicom.dddd.utils.UUIDSupport.uuid

case class CommandMessage(
                  command: Command,
                       id: String = uuid,
                timestamp: DateTime = new DateTime,
              causationId: Option[EntityId] = None,
            correlationId: Option[EntityId] = None,
               deliveryId: Option[Long] = None,
               mustFollow: Option[EntityId] = None
                         ) extends Message with AddressableMessage {

  type MessageImpl = CommandMessage

  override def destination: Option[EntityId] = Some(command.aggregateId)
  override def payload: Any = command

  override def toString: String = {
    s"CommandMessage(command = $command, id = $id, timestamp = $timestamp)"
  }

  def withDeliveryId(deliveryId: Long) = copy(deliveryId = Option(deliveryId))
  def withCorrelationId(correlationId: EntityId) = copy(correlationId = Option(correlationId))
  def withCausationId(causationId: EntityId) = copy(causationId = Option(causationId))
  def withMustFollow(mustFollow: Option[String]) = copy(mustFollow = mustFollow)

  def causedBy(msg: Message): CommandMessage =
    copy(correlationId = msg.correlationId, causationId = Option(msg.id))
}