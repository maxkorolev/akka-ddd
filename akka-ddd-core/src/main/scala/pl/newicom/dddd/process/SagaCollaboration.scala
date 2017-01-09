package pl.newicom.dddd.process

import akka.actor.{ActorPath, ActorSystem}
import org.joda.time.DateTime.now
import org.joda.time.{DateTime, Period}
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.delivery.protocol.DeliveryHandler
import pl.newicom.dddd.messaging.Message
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.office.{Office, RemoteOfficeId}
import pl.newicom.dddd.scheduling.ScheduleEvent

trait SagaCollaboration {
  this: SagaBase =>

  protected def processCollaborators: List[RemoteOfficeId[_]]

  protected def deliverMsg(target: ActorPath, msg: Message): Unit =
    deliver(target)(deliveryId => msg.withDeliveryId(deliveryId))

  protected def deliverCommand(target: ActorPath, command: Command): Unit =
    deliverMsg(target, CommandMessage(command).causedBy(currentEventMsg))


  protected def schedule(officeId: RemoteOfficeId[_], event: DomainEvent, deadline: DateTime, correlationId: EntityId = sagaId): Unit = {
    val command = ScheduleEvent("global", officePath, deadline, event)
    office(officeId) deliver CommandMessage(command).withCorrelationId(correlationId)
  }

  //
  // DSL helpers
  //

  def ⟶[C <: Command](officeId: RemoteOfficeId[C], command: C): Unit =
    office(officeId) deliver command

  def ⟵(officeId: RemoteOfficeId[_], event: DomainEvent): ToBeScheduled = schedule(officeId, event)

  implicit def deliveryHandler: DeliveryHandler = {
    (ap: ActorPath, msg: Any) => msg match {
      case c: Command => deliverCommand(ap, c)
      case m: Message => deliverMsg(ap, m)
    }
  }.tupled


  def schedule(officeId: RemoteOfficeId[_], event: DomainEvent) = new ToBeScheduled(officeId, event)

  class ToBeScheduled(officeId: RemoteOfficeId[_], event: DomainEvent) {
    def on(dateTime: DateTime): Unit = schedule(officeId, event, dateTime)
    def at(dateTime: DateTime): Unit = on(dateTime)
    def in(period: Period): Unit = on(now.plus(period))
    def asap(): Unit = on(now)
  }


  //
  // Private members
  //

  private implicit val as: ActorSystem = context.system
}
