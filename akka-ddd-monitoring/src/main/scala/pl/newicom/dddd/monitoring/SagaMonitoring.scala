package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.process.{Saga, SagaAbstractStateHandling}

trait SagaMonitoring extends SagaAbstractStateHandling with TraceContextSupport {
  this: Saga =>

  override abstract def updateState(event: DomainEvent): Unit = {
    super.updateState(event)

    if (!recoveryRunning) {
      // finish 'reaction' record
      newLocalTraceContext(
        name            = ReactionOnEvent.traceContextName(officeId, currentEventMsg),
        startedOnNanos = currentEventMsg.timestamp.getMillis / 1000
      ).foreach(
        _.finish()
      )
    }
  }

}
