package pl.newicom.dddd.monitoring

import akka.actor.ActorRef
import akka.contrib.pattern.ReceivePipeline.Inner
import pl.newicom.dddd.aggregate.AggregateRootBase
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage

trait AggregateRootMonitoring extends AggregateRootBase with TraceContextSupport {
  myself =>

  override abstract def handle(senderRef: ActorRef, event: EventMessage): Unit = {
    super.handle(senderRef, event)
    finishCurrentTraceContext()
    log.debug("Event stored: {}", event.payload)
  }

  pipelineOuter {
    case cm: CommandMessage =>
      /**
        * Record elapsed time since the command was created (by write-front)
        */

      log.debug("Received: {}", cm)

      newTraceContext(ReceptionOfCommand.traceContextName(myself, cm), cm.timestamp.getMillis).foreach(_.finish())

      setNewCurrentTraceContext(HandlingOfCommand.traceContextName(myself, cm))

      Inner(cm)
  }


  def commandTraceContext = currentTraceContext
}
