package com.github.lzenczuk.apa.web

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}

/**
  * Created by dev on 23/09/16.
  */

trait AppMessage
case class CorrectMessage(time:Long, content:String) extends AppMessage
case class UnsupportedMessage(time:Long) extends AppMessage

object WebSocketService {

  def flowHandler():Flow[Message, Message, NotUsed] = {
    Flow.fromGraph(GraphDSL.create(){
      implicit builder =>

        import GraphDSL.Implicits._

        val wsMessageToAppMessageFlow = builder.add(Flow[Message].map{
          case TextMessage.Strict(txt) => CorrectMessage(System.currentTimeMillis(), txt)
          case _ => UnsupportedMessage(System.currentTimeMillis())
        })

        val appMessageToToMessageFlow = builder.add(Flow[AppMessage].map{
          case CorrectMessage(_, content) => TextMessage.Strict(content)
        })

        // Not for production - println is synchronous
        val appMessageLogFlow = builder.add(Flow[AppMessage].map(am => {
          println(s"Message: $am")
          am
        }))

        // In practice this is echo
        wsMessageToAppMessageFlow.out ~> appMessageLogFlow ~> appMessageToToMessageFlow.in

        FlowShape(wsMessageToAppMessageFlow.in, appMessageToToMessageFlow.out)

    })
  }

}
