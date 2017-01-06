package pl.newicom.dddd.eventhandling.reliable

import pl.newicom.dddd.messaging.event.EventMessage

case class RedeliveryFailedException(event: EventMessage) extends RuntimeException
