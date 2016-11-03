package com.ovoenergy.comms.delivery

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import okhttp3.OkHttpClient
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import com.ovoenergy.comms.delivery.kafka.{Kafka, Protobuf}
import com.ovoenergy.comms.delivery.mailgun.Mailgun
import com.ovoenergy.comms.delivery.mailgun.Mailgun.MailgunContext
import org.slf4j.LoggerFactory

object Main extends App {
  private val log = LoggerFactory.getLogger("Main")

  val config = ConfigFactory.load()

  val mailgunContext = {
    val mailgunApiKey = config.getString("mailgun.apiKey")
    val mailgunDomain = config.getString("mailgun.domain")
    val httpClient = new OkHttpClient()
    MailgunContext(mailgunApiKey, mailgunDomain, httpClient)
  }

  implicit val actorSystem = ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  val source = {
    val consumerSettings =
      ConsumerSettings(actorSystem, new StringDeserializer, Protobuf.commandDeserializer)
        .withBootstrapServers(config.getString("kafka.bootstrap.servers"))
        .withGroupId(config.getString("kafka.group.id"))
    val incomingTopic = config.getString("kafka.incoming.topic")
    val outgoingTopic = config.getString("kafka.outgoing.topic")
    Kafka.buildSource(consumerSettings, incomingTopic, outgoingTopic) {
      Mailgun.send(_).run(mailgunContext).leftMap(err => { log.warn(s"Failed to queue email: $err"); err }).toOption
    }
  }

  val producerSettings =
    ProducerSettings(actorSystem, new StringSerializer, Protobuf.deliveryEventSerializer)
      .withBootstrapServers(config.getString("kafka.bootstrap.servers"))

  source.runWith(Producer.plainSink(producerSettings))
  log.info("Started source.")

}
