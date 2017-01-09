package pl.newicom.cassandra

import akka.actor._
import pl.newicom.dddd.messaging.event.DefaultEventStreamSubscriber

trait CassandraSubscriber extends DefaultEventStreamSubscriber with CassandraEventSourceProvider {
  this: Actor =>

}