package com.github.lzenczuk.apa.web

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.actor.ActorPublisher
import akka.stream.{FlowShape, OverflowStrategy, SinkShape, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import org.reactivestreams.Publisher

/**
  * Created by dev on 23/09/16.
  */

trait AppMessage
case class CorrectMessage(time:Long, content:String) extends AppMessage
case class UnsupportedMessage(time:Long) extends AppMessage
case object ConnectionClosed extends AppMessage

object WebSocketService {

  def flowHandler()(implicit system:ActorSystem):Flow[Message, Message, NotUsed] = {

    val connectionActor: ActorRef = system.actorOf(Props[WsConnectionActor], s"wsConnectionActor_${System.currentTimeMillis()}")
    val connectionPublisher: Publisher[AppMessage] = ActorPublisher[AppMessage](connectionActor)

    val actorSink = Flow[Message].map{
      case TextMessage.Strict(txt) => CorrectMessage(System.currentTimeMillis(), txt)
      case _ => UnsupportedMessage(System.currentTimeMillis())
    }.to(Sink.actorRef[AppMessage](connectionActor, ConnectionClosed))

    val actorSource = Source.fromPublisher(connectionPublisher).via(Flow[AppMessage].map{
      case CorrectMessage(_, content) => TextMessage.Strict(content)
    })

    Flow.fromSinkAndSource(actorSink, actorSource)
  }

}
