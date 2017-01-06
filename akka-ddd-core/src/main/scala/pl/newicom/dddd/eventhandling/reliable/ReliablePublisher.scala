package pl.newicom.dddd.eventhandling.reliable

import akka.actor._
import akka.persistence.AtLeastOnceDelivery.{UnconfirmedDelivery, UnconfirmedWarning}
import akka.persistence._
import pl.newicom.dddd.aggregate.AggregateRootBase
import pl.newicom.dddd.delivery.protocol.alod.Processed
import pl.newicom.dddd.eventhandling.EventPublisher
import pl.newicom.dddd.messaging.event.EventMessage

import scala.collection.immutable.Seq
import scala.concurrent.duration._

trait ReliablePublisher extends PersistentActor with EventPublisher with AtLeastOnceDelivery {
  this: AggregateRootBase =>

  implicit def system: ActorSystem = context.system

  def target: ActorPath

  override def redeliverInterval = 30.seconds
  override def warnAfterNumberOfUnconfirmedAttempts = 15

  override def publish(em: EventMessage) =
    deliver(target)(em.withDeliveryId)

  abstract override def receiveRecover: Receive = {
    case event: EventMessage =>
      super.receiveRecover(event)
      publish(event)

    case Processed(deliveryId, _) =>
      confirmDelivery(deliveryId)
  }

  abstract override def receiveCommand: Receive = {
    case receipt @ Processed(deliveryId, _) =>
      persist(receipt) {
        _ => confirmDelivery(deliveryId)
      }
    case UnconfirmedWarning(unconfirmedDeliveries) =>
      receiveUnconfirmedDeliveries(unconfirmedDeliveries)
    case c => super.receiveCommand(c)
  }

  def receiveUnconfirmedDeliveries(deliveries: Seq[UnconfirmedDelivery]): Unit = {
    // TODO it should be possible define compensation action that will be triggered from here
    // If compensation applied, unconfirmed deliveries should be confirmed: 
    //unconfirmedDeliveries.foreach(ud => confirmDelivery(ud.deliveryId))
  }
}
