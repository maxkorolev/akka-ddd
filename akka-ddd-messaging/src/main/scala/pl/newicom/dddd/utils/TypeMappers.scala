package pl.newicom.dddd.utils


import java.time.Instant

import akka.actor.{ ActorRef, ActorSystem, ExtendedActorSystem }
import akka.serialization.Serialization
import com.google.protobuf.wrappers.{ BytesValue, Int64Value }
import com.google.protobuf.ByteString
import com.trueaccord.scalapb.TypeMapper
import org.joda.time.DateTime

abstract class ActorSystemMapper[BaseType, CustomType](implicit val system: ActorSystem) extends TypeMapper[BaseType, CustomType]

object TypeMappers extends MessageMapper

trait MessageMapper {
  def get[E, A](xor: Either[E, A]): A = xor match {
    case Right(res) ⇒ res
    case Left(e)    ⇒ throw new Exception(s"Parse error: $e")
  }

  private def applyArrayBytes(bytes: ByteString): Array[Byte] = bytes.toByteArray

  private def unapplyArrayBytes(message: Array[Byte]): ByteString = ByteString.copyFrom(message)

  private def applyDateTime(millis: Long): DateTime = new DateTime(millis)

  private def unapplyDateTime(dt: DateTime): Long = dt.getMillis

  private def applyInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

  private def unapplyInstant(dt: Instant): Long = dt.toEpochMilli

  private def applyInstantOpt(millis: Int64Value): Instant = Instant.ofEpochMilli(millis.value)

  private def unapplyInstantOpt(dt: Instant): Int64Value = Int64Value(dt.toEpochMilli)

  implicit val dateTimeMapper: TypeMapper[Long, DateTime] = TypeMapper(applyDateTime)(unapplyDateTime)

  implicit val instantMapper: TypeMapper[Long, Instant] = TypeMapper(applyInstant)(unapplyInstant)

  implicit val instantOptMapper: TypeMapper[Int64Value, Instant] = TypeMapper(applyInstantOpt)(unapplyInstantOpt)

  implicit val arrayBytesMapper: TypeMapper[ByteString, Array[Byte]] = TypeMapper(applyArrayBytes)(unapplyArrayBytes)

  implicit def actorRefMapper(implicit system: ActorSystem): TypeMapper[String, ActorRef] =
    new ActorSystemMapper[String, ActorRef]() {
      override def toCustom(base: String): ActorRef =
        system.asInstanceOf[ExtendedActorSystem].provider.resolveActorRef(base)

      override def toBase(custom: ActorRef): String = Serialization.serializedActorPath(custom)
    }
}
