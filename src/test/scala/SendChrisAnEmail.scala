import com.ovoenergy.comms.delivery.mailgun.Mailgun
import com.ovoenergy.comms.delivery.mailgun.Mailgun.MailgunContext
import com.ovoenergy.comms.protobuf.command.SendTransactionalEmail
import com.typesafe.config.ConfigFactory
import okhttp3.OkHttpClient

object SendChrisAnEmail extends App {
  val config = ConfigFactory.load()
  val mailgunApiKey = config.getString("mailgun.apiKey")
  val mailgunDomain = config.getString("mailgun.domain")
  val httpClient = new OkHttpClient()
  val context = MailgunContext(mailgunApiKey, mailgunDomain, httpClient)

  println("Let's send Chris an email! He'll love that.")
  val subject = scala.io.StdIn.readLine("Subject: ")
  val body = scala.io.StdIn.readLine("Body: ")

  val eventPayload = SendTransactionalEmail(
    from = "OVO Energy <no-reply@ovoenergy.com>",
    to = "Chris <chris.birchall@ovoenergy.com>",
    subject = subject,
    body = body
  )
  val result = Mailgun.send(eventPayload).run(context)
  println(result)
}
