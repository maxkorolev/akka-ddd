package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.messaging.event.EventMessage

trait LoggingEventPublisher extends EventPublisher {
  this: Actor =>

  override def publish(em: EventMessage) {
    println("Published: " + em)
  }

}
