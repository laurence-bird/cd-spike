package com.ovoenergy.comms.delivery.kafka

import java.nio.charset.StandardCharsets
import java.util

import com.ovoenergy.comms.protobuf.command.SendTransactionalEmail
import com.ovoenergy.comms.protobuf.event.EmailDeliveryEvent
import com.trueaccord.scalapb.json.JsonFormat
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

object Protobuf {
  private val log = LoggerFactory.getLogger("Protobuf")

  val commandDeserializer = new Deserializer[Option[SendTransactionalEmail]] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
    override def close(): Unit = {}
    override def deserialize(topic: String, data: Array[Byte]): Option[SendTransactionalEmail] = {
      try {
        Some(JsonFormat.fromJsonString[SendTransactionalEmail](new String(data, StandardCharsets.UTF_8)))
      } catch {
        case NonFatal(e) =>
          log.warn(s"Failed to parse message as JSON-encoded protobuf: ${util.Arrays.toString(data)}")
          None
      }
    }
  }

  val deliveryEventSerializer = new Serializer[EmailDeliveryEvent] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
    override def close(): Unit = {}
    override def serialize(topic: String, data: EmailDeliveryEvent): Array[Byte] =
      JsonFormat.toJsonString(data).getBytes(StandardCharsets.UTF_8)
  }

}
