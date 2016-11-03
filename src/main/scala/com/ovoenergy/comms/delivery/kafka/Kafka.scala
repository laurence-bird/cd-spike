package com.ovoenergy.comms.delivery.kafka

import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.util.Try

object Kafka {
  private val log = LoggerFactory.getLogger("Kafka")

  def buildSource[Command, Event](consumerSettings: ConsumerSettings[String, Option[Command]],
                                  incomingTopic: String,
                                  outgoingTopic: String)(
      processCommand: Command => Option[Event]): Source[ProducerRecord[String, Event], Control] = {
    // Note: we're doing at-most-once delivery here, committing the offset before we send the email
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(incomingTopic))
      .map { msg =>
        log.info(s"Received a command! ${msg.record.value}")

        msg.committableOffset.commitScaladsl()

        msg.record.value.flatMap { command =>
          val t = Try(processCommand(command))
          t.failed.foreach(e => log.warn(s"Ignoring exception: $e"))
          t.toOption.flatten
        }
      }
      .recover {
        case e =>
          log.warn(s"Exception: $e")
          None
      }
      .mapConcat(_.toList)
      .map { event =>
        new ProducerRecord[String, Event](outgoingTopic, event)
      }
  }

}
