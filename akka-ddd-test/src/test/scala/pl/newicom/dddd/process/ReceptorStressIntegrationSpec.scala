package pl.newicom.dddd.process

import akka.actor._
import akka.testkit.TestProbe
import pl.newicom.dddd.actor.PassivationConfig
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.coordination.ReceptorConfig
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.eventhandling.LocalPublisher
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.office.OfficeFactory._
import pl.newicom.dddd.office.OfficeListener
import pl.newicom.dddd.office.SimpleOffice._
import pl.newicom.dddd.process.ReceptorIntegrationSpec._
import pl.newicom.dddd.persistence.{RegularSnapshottingConfig, SaveSnapshotRequest}
import pl.newicom.dddd.process.ReceptorSupport.ReceptorFactory
import pl.newicom.dddd.saga.CoordinationOffice
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.{ChangeValue, CreateDummy, ValueChanged}
import pl.newicom.dddd.test.dummy.DummySaga.{DummySagaActorFactory, DummySagaConfig, EventApplied}
import pl.newicom.dddd.test.dummy.{DummyAggregateRoot, DummySaga, dummyOfficeId}
import pl.newicom.dddd.test.support.IntegrationTestConfig.integrationTestSystem
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.eventstore.EventstoreSubscriber

import scala.concurrent.duration._

object ReceptorStressIntegrationSpec {

  case object GetNumberOfUnconfirmed

  implicit def actorFactory(implicit it: Duration = 1.minute): AggregateRootActorFactory[DummyAggregateRoot] =
    new AggregateRootActorFactory[DummyAggregateRoot] {
      override def props(pc: PassivationConfig): Props = Props(new DummyAggregateRoot with LocalPublisher)
      override def inactivityTimeout: Duration = it
    }

}

/**
  * Requires EventStore to be running on localhost!
  */
class ReceptorStressIntegrationSpec extends OfficeSpec[DummyAggregateRoot](Some(integrationTestSystem("ReceptorStressSpec"))) {

  def dummyId: EntityId = aggregateId

  implicit lazy val testSagaConfig = new DummySagaConfig(s"${dummyOfficeId.id}-$dummyId")

  implicit val _ = new OfficeListener[DummySaga]

  implicit val receptorFactory: ReceptorFactory = (config: ReceptorConfig) => {
    new Receptor(config.copy(capacity = 1000)) with EventstoreSubscriber {

      override def receiveCommand: Receive = myReceive.orElse(super.receiveCommand)

      def myReceive: Receive = {
        case GetNumberOfUnconfirmed => sender() ! numberOfUnconfirmed
      }

      override val snapshottingConfig = RegularSnapshottingConfig(receiveEvent, interval = 50)
    }
  }


  val sagaProbe = TestProbe()
  system.eventStream.subscribe(sagaProbe.ref, classOf[EventApplied])
  ignoreMsg({ case EventMessage(Processed(_), _, _, _, _, _, _) => true })

  "Receptor" should {

    var receptor: ActorRef = null
    var coordinationOffice: CoordinationOffice[DummySaga] = null

    val changes = 2 to 101

    "deliver 100 events to the receiver" in {
      val co = office[DummySaga].asInstanceOf[CoordinationOffice[DummySaga]]
      val sm = ReceptorSupport.receptor(co.receptorConfig)
      receptor = sm; coordinationOffice = co

      given {
        List(
          CreateDummy(dummyId, "name", "description", 0),
          ChangeValue(dummyId, 1)
        )
      }
      .when {
        changes.map(v => ChangeValue(dummyId, v))
      }
      .expectEvents(
        changes.map(v => ValueChanged(dummyId, v, v.toLong)): _*
      )

      expectNumberOfEventsAppliedBySaga(changes.size + 1)
      expectNoUnconfirmedMessages(receptor)
    }

  }

  def expectNumberOfEventsAppliedBySaga(expectedNumberOfEvents: Int): Unit = {
    for (i <- 1 to expectedNumberOfEvents) {
      sagaProbe.expectMsgClass(classOf[EventApplied])
    }
  }

  def expectNoUnconfirmedMessages(receptor: ActorRef): Unit = {
    expectNumberOfUnconfirmedMessages(receptor, 0)
  }

  def expectNumberOfUnconfirmedMessages(receptor: ActorRef, expectedNumberOfMessages: Int): Unit = within(3.seconds) {
    receptor ! SaveSnapshotRequest
    awaitAssert {
      receptor ! GetNumberOfUnconfirmed
      expectMsg(expectedNumberOfMessages)
    }
  }
}
