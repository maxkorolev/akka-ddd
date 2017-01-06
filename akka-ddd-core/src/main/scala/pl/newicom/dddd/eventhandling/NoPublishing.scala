package pl.newicom.dddd.eventhandling

import akka.actor.Actor
import pl.newicom.dddd.messaging.event.EventMessage

trait NoPublishing extends EventPublisher {
  this: Actor =>

  override def publish(em: EventMessage): Unit = ()

}
