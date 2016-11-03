import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID

import com.ovoenergy.comms.protobuf.command.SendTransactionalEmail
import com.ovoenergy.comms.protobuf.common.Metadata
import com.trueaccord.scalapb.json.JsonFormat
import org.json4s.jackson.JsonMethods

object PrintProtobufJson extends App {
  val metadata = Metadata(timestampIso8601 = OffsetDateTime.now().toString,
                          uuid = UUID.randomUUID().toString,
                          source = "Chris's brain")
  val command = SendTransactionalEmail(
    Some(metadata),
    from = "OVO Energy <no-reply@ovoenergy.com>",
    to = "Chris <chris.birchall@ovoenergy.com>",
    subject = "Hello",
    body = "World"
  )
  println(JsonMethods.pretty(JsonFormat.toJson(command)))
  /*
  {
    "body" : "World",
    "subject" : "Hello",
    "to" : "Chris <chris.birchall@ovoenergy.com>",
    "from" : "OVO Energy <no-reply@ovoenergy.com>",
    "metadata" : {
      "timestampIso8601" : "2016-10-25T11:40:01.876+01:00",
      "uuid" : "ff0cde89-fa2b-44be-95c1-f18c34a0345e",
      "source" : "Chris's brain"
    }
  }
 */
}
