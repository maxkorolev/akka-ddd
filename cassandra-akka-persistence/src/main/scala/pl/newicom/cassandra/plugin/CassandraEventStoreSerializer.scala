package pl.newicom.cassandra.plugin

import akka.serialization.SerializerWithStringManifest
import pl.newicom.dddd.messaging.event.MessageProto
import pl.newicom.dddd.messaging.command.CommandMessage
import pl.newicom.dddd.messaging.event.EventMessage


class CassandraEventStoreSerializer extends SerializerWithStringManifest {
  override def identifier: Int = 9001

  // Event <- **Deserializer** <- Serialized(Event) <- Journal
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case CommandMessage => MessageProto.parseFrom(bytes)
      case EventMessage => Subtracted.parseFrom(bytes)
    }

  // We use the manifest to determine the event (it is called for us during serializing)
  // Akka will call manifest and attach it to the message in the event journal/snapshot database
  // when toBinary is being invoked
  override def manifest(o: AnyRef): String = o.getClass.getName
  final val CommandMessage = classOf[CommandMessage].getName
  final val EventMessage = classOf[EventMessage].getName

  // Event -> **Serializer** -> Serialized(Event) -> Journal
  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case m: CommandMessage => a.toByteArray
      case m: EventMessage => s.toByteArray
    }
  }
}