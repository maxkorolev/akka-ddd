package pl.newicom.dddd.messaging.event

import org.joda.time.DateTime

case class EventMessageEntry(msg: EventMessage, position: Long, created: Option[DateTime])
