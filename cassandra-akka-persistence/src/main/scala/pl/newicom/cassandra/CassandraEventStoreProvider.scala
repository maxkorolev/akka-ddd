package pl.newicom.cassandra

import akka.actor.ActorSystem
import eventstore.EsConnection
import eventstore.EventStream.System
import pl.newicom.dddd.messaging.event.EventStoreProvider

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CassandraEventStoreProvider extends EventStoreProvider {
    def system: ActorSystem

    type EventStore = EsConnection

    override def eventStore: EsConnection = EsConnection(system)

    override def ensureEventStoreAvailable(): Future[EsConnection] = {
        val esConn = eventStore
        esConn.getStreamMetadata(System("test")).map(_ => esConn)
    }

}