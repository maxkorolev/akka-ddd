package pl.newicom.dddd.process

import akka.persistence.RecoveryCompleted
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.alod._
import pl.newicom.dddd.messaging.event.EventMessage

case object EventDroppedMarkerEvent extends DomainEvent

sealed trait SagaAction

case class  RaiseEvent(e: DomainEvent)  extends SagaAction
case object DropEvent                   extends SagaAction

trait SagaAbstractStateHandling {
  type ReceiveEvent = PartialFunction[DomainEvent, SagaAction]

  def receiveEvent: ReceiveEvent

  def updateState(event: DomainEvent): Unit

  def initialized: Boolean
}


abstract class Saga extends SagaBase {
  this: SagaAbstractStateHandling =>

  override def receiveRecover: Receive = {
    case rc: RecoveryCompleted =>
      // do nothing
    case msg: Any =>
      _updateState(msg)
  }

  override def receiveCommand: Receive = {
    case em: EventMessage =>
      val event = em.event
	    val actionMaybe: Option[SagaAction] =
        em.mustFollow.fold(Option(receiveEvent(event))) { mustFollow =>
          if (wasReceived(mustFollow))
            Option(receiveEvent(event))
          else
            None
        }

      if (actionMaybe.isEmpty) {
        log.debug("Message out of order detected: {}", em.id)
      } else {
        val action  = actionMaybe.get
        val eventToPersist = action match {
          case RaiseEvent(raisedEvent) => raisedEvent
          case DropEvent => EventDroppedMarkerEvent
        }

        val emToPersist = em.copy(event = eventToPersist, causationId = Option(em.id))

        persist(emToPersist) { persisted =>
          log.debug("Event message persisted: {}", persisted)
          _updateState(persisted)
          acknowledgeEvent(persisted)
        }

        onEventReceived(em, action)
      }

    case receipt: Delivered if initialized =>
      persist(EventMessage(receipt))(_updateState)
  }

  private def _updateState(msg: Any): Unit = {
    msg match {
      case EventMessage(receipt: Delivered, _, _, _, _, _, _ ) =>
        confirmDelivery(receipt.deliveryId)
        updateState(receipt)

      case em @ EventMessage(EventDroppedMarkerEvent, _, _, _, _, _, _ ) =>
        messageProcessed(em)

      case em @ EventMessage(event, _, _, _, _, _, _ ) =>
        messageProcessed(em)
        updateState(event)
    }
  }

  def onEventReceived(em: EventMessage, appliedAction: SagaAction): Unit = {
    appliedAction match {
      case DropEvent =>
        log.debug(s"Event dropped: ${em.event}")
      case RaiseEvent(e) =>
        log.debug(s"Event raised: $e")
    }
  }

}