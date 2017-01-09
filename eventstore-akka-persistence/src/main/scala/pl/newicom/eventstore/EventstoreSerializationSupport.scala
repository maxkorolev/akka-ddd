package pl.newicom.eventstore

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.persistence.eventstore.snapshot.EventStoreSnapshotStore.SnapshotEvent
import akka.util.ByteString
import eventstore.Content._
import eventstore.{Content, ContentType, EventData}
import io.circe.{ Decoder, Encoder, Json, Printer, jawn }
import org.joda.time.DateTime
import pl.newicom.dddd.aggregate._
import pl.newicom.dddd.messaging.event.EventMessage

import scala.util.{Failure, Success, Try}

/**
 * Contains methods for converting akka.persistence.PersistentRepr from/to eventstore.EventData.
 * Payload of deserialized (in-memory) PersistentRepr is EventMessage.
 * During serialization of PersistentRepr, its payload is replaced with payload of EventMessage (actual event) while
 * metadata of EventMessage is stored in metadata of eventstore.EventData.
 * EventType of eventstore.EventData is set to class of actual event.
 * </br>
 * During deserialization original (in-memory) PersistentRepr is reconstructed.
 */
trait EventstoreSerializationSupport {

  def system: ActorSystem

  def toEventData(x: AnyRef, contentType: ContentType): EventData = {
    def toContent(o: AnyRef, eventType: Option[String] = None) =
      Content(ByteString(serialize(o, eventType)), contentType)

    x match {
      case x: PersistentRepr =>
        x.payload match {
          case em: EventMessage =>
            val event = toPayloadAndMetadata(em)
            val eventType = classFor(event).getName
            EventData(
              eventType = eventType,
              data = toContent(x.withPayload(event), Some(eventType))
            )
          case _ =>
            EventData(eventType = classFor(x).getName, data = toContent(x))
        }

      case x: SnapshotEvent =>
        EventData(eventType = classFor(x).getName, data = toContent(x))

      case _ => sys.error(s"Cannot serialize $x")
    }
  }

  def fromEvent[A](event: EventData, manifest: Class[A]): Try[A] = {
    val result = deserialize(event.data.value.toArray, manifest, Some(event.eventType))
    if (manifest.isInstance(result)) {
      Success((result match {
        case pr: PersistentRepr =>
          pr.withPayload(fromPayloadAndMetadata(pr.payload.asInstanceOf[AnyRef]))
        case _ => result
      }).asInstanceOf[A])
    } else
      Failure(sys.error(s"Cannot deserialize event as $manifest, event: $event"))
  }

  def toOfficeEventMessage(eventData: EventData): Try[EventMessage] =
    fromEvent(eventData, classOf[PersistentRepr]).map {
      _.payload.asInstanceOf[EventMessage]
    }

    private def toPayloadAndMetadata(em: EventMessage) = em.event
    private def fromPayloadAndMetadata(payload: AnyRef): EventMessage = EventMessage(payload)

//  private def toPayloadAndMetadata(em: EventMessage): (DomainEvent, Option[MetaData]) =
//    (em.event, em.withMetaData(Map("id" -> em.id, "timestamp" -> em.timestamp.getMillis.toString)).metadata)
//
//  private def fromPayloadAndMetadata(payload: AnyRef, maybeMetadata: Option[MetaData]): EventMessage = {
//    if (maybeMetadata.isDefined) {
//      val metadata = maybeMetadata.get
//      val id: EntityId = metadata.get("id")
//      val timestamp = DateTime.parse(metadata.get("timestamp"))
//      EventMessage(payload, id, timestamp).withMetaData(Some(metadata))
//    } else {
//      EventMessage(payload)
//    }
//  }

  private def deserialize[T](bytes: Array[Byte], clazz: Class[T], eventType: Option[String] = None): T = {
    null.asInstanceOf[T]
  }

  private def serialize(o : AnyRef, eventType: Option[String] = None): Array[Byte] =
    Array()

  private def classFor(x: AnyRef) = x match {
    case x: PersistentRepr => classOf[PersistentRepr]
    case _                 => x.getClass
  }

}
