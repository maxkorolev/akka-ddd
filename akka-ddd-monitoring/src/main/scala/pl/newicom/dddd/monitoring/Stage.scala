package pl.newicom.dddd.monitoring

import pl.newicom.dddd.aggregate.BusinessEntity
import pl.newicom.dddd.messaging.AddressableMessage


class Stage(val position: Integer, val shortName: String) {

  def traceContextName(observed: BusinessEntity, msg: AddressableMessage): String =
    s"$position-${observed.department.capitalize}-$shortName-${msg.payloadName}"
}

case object ReceptionOfCommand extends Stage(1, "reception")
case object HandlingOfCommand extends Stage(2, "handling")
case object ReceptionOfEvent extends Stage(3, "reception")
case object ReactionOnEvent extends Stage(4, "reaction")
