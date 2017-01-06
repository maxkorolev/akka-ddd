package pl.newicom.dddd.eventhandling

import akka.actor.ActorRef
import pl.newicom.dddd.messaging.event.EventMessage

trait EventPublisher extends EventHandler {

  override abstract def handle(senderRef: ActorRef, event: EventMessage): Unit = {
    publish(event)
    super.handle(senderRef, event)
  }

  def publish(event: EventMessage)
}
