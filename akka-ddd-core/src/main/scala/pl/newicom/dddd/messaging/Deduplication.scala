package pl.newicom.dddd.messaging

import akka.contrib.pattern.ReceivePipeline
import akka.contrib.pattern.ReceivePipeline.{Inner, HandledCompletely}
import scala.collection.mutable

/**
  * Designed to be used by persistent actors. Allows detecting duplicated messages sent to the actor.
  * Keeps a set of message IDs that were received by the actor.
  *
  * Provides messageProcessed(Message) method that should be called during the "update-state" stage.
  * The given message must contain [[pl.newicom.dddd.messaging.MetaData.CausationId]] attribute
  * referring to the ID of the received message.
  */
trait Deduplication {
  myself: ReceivePipeline =>

  private val ids: mutable.Set[String] = mutable.Set.empty

  pipelineInner {
    case msg: Message if wasReceived(msg) =>
        handleDuplicated(msg)
        HandledCompletely

    case msg: Message =>
        Inner(msg)
  }

  def handleDuplicated(msg: Message)

  def messageProcessed(msg: Message): Unit =
    msg.causationId.foreach(messageReceived)

  def wasReceived(msgId: String): Boolean =
    ids.contains(msgId)

  private def wasReceived(msg: Message): Boolean =
    wasReceived(msg.id)

  private def messageReceived(msgId: String): Unit =
    ids += msgId

}
