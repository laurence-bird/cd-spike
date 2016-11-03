package com.ovoenergy.comms.delivery.mailgun

import java.time.OffsetDateTime
import java.util.UUID

import cats.data.{Reader, Xor}
import com.ovoenergy.comms.protobuf.command.SendTransactionalEmail
import com.ovoenergy.comms.protobuf.common.Metadata
import com.ovoenergy.comms.protobuf.event.EmailDeliveryEvent
import com.ovoenergy.comms.protobuf.event.EmailDeliveryEvent.EventType
import okhttp3._
import io.circe.{Decoder, Error => CirceError}
import io.circe.parser._
import io.circe.generic.auto._

import scala.util.{Failure, Success, Try}

object Mailgun {

  case class MailgunContext(
      apiKey: String,
      domain: String,
      httpClient: OkHttpClient
  )

  sealed abstract class MailgunError
  object MailgunError {
    case class FailedToQueue(status: Int, message: String) extends MailgunError
    case class InvalidResponse(status: Int, responseBody: String) extends MailgunError
    case class UnexpectedError(exception: Throwable) extends MailgunError
  }

  import MailgunError._

  def send(command: SendTransactionalEmail) = Reader[MailgunContext, Xor[MailgunError, EmailDeliveryEvent]] {
    context =>
      val requestBody = buildRequestBody(command)
      val request = new Request.Builder()
        .url(s"https://api.mailgun.net/v3/${context.domain}/messages")
        .post(requestBody)
        .addHeader("Authorization", Credentials.basic("api", context.apiKey))
        .build
      Try(context.httpClient.newCall(request).execute()) match {
        case Success(response) => interpretResponse(response, command)
        case Failure(t) => Xor.left(UnexpectedError(t))
      }
  }

  private case class MailgunErrorResponse(message: String)
  private case class MailgunSuccessResponse(message: String, id: String)

  private def buildRequestBody(payload: SendTransactionalEmail): FormBody =
    new FormBody.Builder()
      .add("from", payload.from)
      .add("to", payload.to)
      .add("subject", payload.subject)
      .add("html", payload.body)
      .add("v:custom", s"""{"friendlyDescription": "${payload.friendlyDescription}"}""")
      .build

  private def interpretResponse(response: Response,
                                command: SendTransactionalEmail): Xor[MailgunError, EmailDeliveryEvent] = {
    val body = response.body().string()

    // See https://documentation.mailgun.com/api-intro.html#errors for details of error codes
    response.code() match {
      case 200 =>
        parseResponse[MailgunSuccessResponse](body)
          .leftMap(circeError => InvalidResponse(200, body))
          .map(resp => buildEmailDeliveryEvent(resp.id, command))
      case code =>
        Xor.left(
          parseResponse[MailgunErrorResponse](body)
            .leftMap(circeError => InvalidResponse(code, body))
            .map(resp => FailedToQueue(code, resp.message))
            .merge)

    }
  }

  private def parseResponse[T: Decoder](body: String): Xor[CirceError, T] =
    parse(body).flatMap(_.as[T])

  private def buildEmailDeliveryEvent(messageId: String, command: SendTransactionalEmail): EmailDeliveryEvent =
    EmailDeliveryEvent(
      metadata = Some(Metadata(OffsetDateTime.now().toString, UUID.randomUUID().toString, "delivery-service")),
      eventType = EventType.QUEUED,
      messageId = messageId,
      recipient = command.to,
      friendlyDescription = command.friendlyDescription
    )
}
