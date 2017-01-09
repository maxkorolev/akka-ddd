package pl.newicom.dddd.http

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller }
import akka.util.ByteString
import io.circe.{ Decoder, Encoder, Json, Printer, jawn }

trait JsonMarshalling {

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller.
      forContentTypes(`application/json`).
      mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset)       => data.decodeString(charset.nioCharset.name)
      }

  private val jsonStringMarshaller =
    Marshaller.stringMarshaller(`application/json`)

  /**
    * HTTP entity => `A`
    *
    * @param decoder decoder for `A`, probably created by `circe.generic`
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */
  implicit def circeUnmarshaller[A](implicit decoder: Decoder[A]): FromEntityUnmarshaller[A] =
    jsonStringUnmarshaller.map(jawn.decode(_).fold(throw _, identity))

  /**
    * `A` => HTTP entity
    *
    * @param encoder encoder for `A`, probably created by `circe.generic`
    * @param printer pretty printer function
    * @tparam A type to encode
    * @return marshaller for any `A` value
    */
  implicit def circeToEntityMarshaller[A](implicit encoder: Encoder[A], printer: Json => String = Printer.noSpaces.pretty): ToEntityMarshaller[A] =
    jsonStringMarshaller.compose(printer).compose(encoder.apply)
}