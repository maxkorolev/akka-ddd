package pl.newicom.dddd.eventhandling

import akka.actor.{Actor, ActorRef}
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.messaging.event.EventMessage

import scala.util.Success

trait LocalPublisher extends EventPublisher {
  this: Actor =>

  override abstract def handle(senderRef: ActorRef, event: EventMessage): Unit = {
    publish(event)
    senderRef ! Processed(Success(event.payload))
  }

  override def publish(em: EventMessage) {
    context.system.eventStream.publish(em.event)
  }


}
