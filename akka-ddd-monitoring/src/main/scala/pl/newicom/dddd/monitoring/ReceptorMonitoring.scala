package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.event.EventSourceProvider

trait ReceptorMonitoring extends EventSourceProvider with TraceContextSupport {

  override abstract def eventSource(es: EventStore, observable: BusinessEntity, fromPosExcl: Option[Long]): EventSource =
    super.eventSource(es, observable, fromPosExcl) map {
      entry =>

        /**
          * Record elapsed time since the event was persisted in the event store
          */
        newTraceContext(
          name = ReceptionOfEvent.traceContextName(observable, entry.msg),
          startedOnMillis = entry.created.get.getMillis
        ).foreach(
          _.finish()
        )
        entry
    }

}
