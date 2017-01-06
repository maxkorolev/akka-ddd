package pl.newicom.dddd.saga

import akka.actor.ActorRef
import pl.newicom.dddd.aggregate.{BusinessEntity => Observable}
import pl.newicom.dddd.coordination.{ReceptorBuilder, ReceptorConfig}
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.Office
import pl.newicom.dddd.saga.ProcessConfig.CorrelationIdResolver

import scala.reflect.ClassTag

class CoordinationOffice[E: ClassTag](val config: ProcessConfig[E], actor: ActorRef) extends Office(config, actor) {

  def receptorConfig: ReceptorConfig =
    ReceptorBuilder()
      .reactTo(new Observable {
        def id = config.bpsName
        def department = config.department
      })
      .applyTransduction {
        case em @ EventMessage(event, _, _, _, _, _, _) if correlationIdResolver.isDefinedAt(event) =>
          em.withCorrelationId(correlationIdResolver(event))
      }
      .propagateTo(actorPath)

  def correlationIdResolver: CorrelationIdResolver =
    config.correlationIdResolver

}
