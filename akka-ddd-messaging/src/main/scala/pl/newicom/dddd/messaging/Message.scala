package pl.newicom.dddd.messaging

import pl.newicom.dddd.aggregate.EntityId
import pl.newicom.dddd.delivery.protocol.{Receipt, Processed, alod}
import pl.newicom.dddd.messaging.MetaData._

import scala.util.{Success, Try}

object MetaData {
  val DeliveryId          = "_deliveryId"
  val CausationId         = "causationId"
  val CorrelationId       = "correlationId"
  // contains ID of a message that the recipient of this message should process before it can process this message
  val MustFollow          = "_mustFollow"
  val SessionId           = "sessionId"

  def empty: MetaData = MetaData(Map.empty)


  def apply(content0: Map[String, String]): MetaData = new MetaData {
    override def content: Map[String, String] = content0
  }
}

trait MetaData {

  def content: Map[String, String]

  def mergeWithMetadata(metadata: Option[MetaData]): MetaData = {
    metadata.map(_.content).map(addContent).getOrElse(this)
  }

  def addContent(content: Map[String, String]): MetaData = {
    MetaData(this.content ++ content)
  }

  def contains(attrName: String) = content.contains(attrName)

  def get[B](attrName: String) = tryGet[B](attrName).get

  def tryGet[B](attrName: String): Option[B] = content.get(attrName).asInstanceOf[Option[B]]

  def exceptDeliveryAttributes: Option[MetaData] = {
    val resultMap = this.content.filterKeys(a => !a.startsWith("_"))
    if (resultMap.isEmpty) None else Some(MetaData(resultMap))
  }

  override def toString: String = content.toString()
}

trait Message extends Serializable {

  def id: String

  type MessageImpl <: Message

  def causedBy(msg: Message): MessageImpl =
    withMetaData(msg.metadataExceptDeliveryAttributes)
      .withCausationId(msg.id).asInstanceOf[MessageImpl]

  def metadataExceptDeliveryAttributes: Option[MetaData] = {
    metadata.flatMap(_.exceptDeliveryAttributes)
  }

  def withMetaData(metadata: Option[MetaData]): MessageImpl = {
    copyWithMetaData(this.metadata.map(_.mergeWithMetadata(metadata)).orElse(metadata))
  }

  def withMetaData(metadataContent: Map[String, String]): MessageImpl = {
    withMetaData(Some(MetaData(metadataContent)))
  }

  def copyWithMetaData(m: Option[MetaData]): MessageImpl

  def metadata: Option[MetaData]

  def withMetaAttribute(attrName: String, value: String): MessageImpl = withMetaData(Map(attrName -> value))
  def withMetaAttribute(attrName: String, value: Long): MessageImpl = withMetaData(Map(attrName -> value.toString))

  def hasMetaAttribute(attrName: String) = metadata.exists(_.contains(attrName))

  def getMetaAttribute[B](attrName: String) = tryGetMetaAttribute[B](attrName).get

  def tryGetMetaAttribute[B](attrName: String): Option[B] = if (metadata.isDefined) metadata.get.tryGet[B](attrName) else None

  def deliveryReceipt(result: Try[Any] = Success("OK")): Receipt = {
    deliveryId.map(id => alod.Processed(id, result)).getOrElse(Processed(result))
  }

  def withDeliveryId(deliveryId: Long) = withMetaAttribute(DeliveryId, deliveryId)

  def withCorrelationId(correlationId: EntityId) = withMetaAttribute(CorrelationId, correlationId)

  def withCausationId(causationId: EntityId) = withMetaAttribute(CausationId, causationId)

  def withMustFollow(mustFollow: Option[String]) = mustFollow.map(msgId => withMetaAttribute(MustFollow, msgId)).getOrElse(this.asInstanceOf[MessageImpl])

  def withSessionId(sessionId: EntityId) = withMetaAttribute(SessionId, sessionId)

  def deliveryId: Option[Long] = tryGetMetaAttribute[String](DeliveryId).map(_.toLong)

  def correlationId: Option[EntityId] = tryGetMetaAttribute[EntityId](CorrelationId)

  def causationId: Option[EntityId] = tryGetMetaAttribute[EntityId](CausationId)

  def mustFollow: Option[String] = tryGetMetaAttribute[String](MustFollow)
}