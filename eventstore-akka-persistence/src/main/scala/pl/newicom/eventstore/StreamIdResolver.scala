package pl.newicom.eventstore

import eventstore.EventStream.{Plain, System}
import eventstore.{EventStream => ESEventStream}
import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.office.{LocalOfficeId, RemoteOfficeId}
import pl.newicom.dddd.saga.ProcessConfig

object StreamIdResolver {

  def streamId(observable: BusinessEntity): ESEventStream.Id = observable match {

    case o: ProcessConfig[_] =>
      Plain(s"${o.id}")

    case LocalOfficeId(id, _) =>
      System(s"ce-$id")

    case RemoteOfficeId(id, _) =>
      System(s"ce-$id")

    case clerk: BusinessEntity =>
      Plain(s"${clerk.id}")
  }
}
