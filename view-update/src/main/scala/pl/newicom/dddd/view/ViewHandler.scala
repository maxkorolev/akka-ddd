package pl.newicom.dddd.view

import akka.Done
import pl.newicom.dddd.messaging.event.EventMessage
import scala.concurrent.Future

abstract class ViewHandler(val vuConfig: ViewUpdateConfig) {

  def handle(eventMessage: EventMessage, eventNumber: Long): Future[Done]

  def lastEventNumber: Future[Option[Long]]

  protected def viewName = vuConfig.viewName

}
