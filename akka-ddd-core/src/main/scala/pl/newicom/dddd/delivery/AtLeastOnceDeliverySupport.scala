package pl.newicom.dddd.delivery

import akka.actor.ActorPath
import akka.persistence.AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot
import akka.persistence._
import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.alod.Delivered
import pl.newicom.dddd.messaging.{AddressableMessage, Message}
import pl.newicom.dddd.persistence.{PersistentActorLogging, SaveSnapshotRequest}

case class DeliveryStateSnapshot(state: DeliveryState, alodSnapshot: AtLeastOnceDeliverySnapshot)

trait AtLeastOnceDeliverySupport extends PersistentActor with AtLeastOnceDelivery with PersistentActorLogging {

  type DeliverableMessage = Message with AddressableMessage

  private var deliveryState: DeliveryState = InitialState

  def destination(msg: Message): ActorPath

  def recoveryCompleted(): Unit

  def lastSentDeliveryId: Option[Long] = deliveryState.lastSentOpt

  def unconfirmedNumber: Int = deliveryState.unconfirmedNumber

  def deliver(msg: Message, deliveryId: Long): Unit =
    persist(msg.withDeliveryId(deliveryId))(updateState)

  def updateState(msg: Any): Unit = msg match {
    case message: DeliverableMessage =>

      message.destination getOrElse log.warning(s"No entityId. Skipping $message")
      for {
        destinationId <- message.destination
        deliveryId <- message.deliveryId
      } yield {
        val dest = destination(message)
        deliver(dest) { internalDeliveryId =>
          val lastSentToDestinationMsgId: Option[EntityId] = deliveryState.lastSentToDestinationMsgId(destinationId)
          deliveryState = deliveryState.withSent(message.id, internalDeliveryId, deliveryId, destinationId)

          val msgToDeliver = message.withMustFollow(lastSentToDestinationMsgId)

          log.debug(s"[DELIVERY-ID: $deliveryId] Delivering: $msgToDeliver to $dest")
          msgToDeliver
        }
      }


    case receipt: Delivered =>
      val deliveryId = receipt.deliveryId
      for {
        internalDeliveryId <- deliveryState.internalDeliveryId(deliveryId)
        if confirmDelivery(internalDeliveryId)
      } yield {
        log.debug(s"[DELIVERY-ID: $deliveryId] - Delivery confirmed")
        deliveryState = deliveryState.withDelivered(deliveryId)
        deliveryConfirmed(internalDeliveryId)
      }
  }

  def deliveryStateReceive: Receive = {
    case receipt: Delivered =>
      persist(receipt)(updateState)

    case SaveSnapshotRequest =>
      val snapshot = DeliveryStateSnapshot(deliveryState, getDeliverySnapshot)
      saveSnapshot(snapshot)

    case SaveSnapshotSuccess(metadata) =>
      log.debug("Snapshot saved successfully with metadata: {}", metadata)

    case f @ SaveSnapshotFailure(metadata, reason) =>
      log.error(s"$f")
      throw reason

  }

  override def receiveRecover: Receive = {
    case RecoveryCompleted  =>
      log.debug("Recovery completed")
      recoveryCompleted()

    case SnapshotOffer(metadata, DeliveryStateSnapshot(dState, alodSnapshot)) =>
      setDeliverySnapshot(alodSnapshot)
      deliveryState = dState
      log.debug(s"Snapshot restored: $deliveryState")

    case msg =>
      updateState(msg)
  }

  def deliveryConfirmed(deliveryId: Long): Unit = {
    // do nothing
  }
}
