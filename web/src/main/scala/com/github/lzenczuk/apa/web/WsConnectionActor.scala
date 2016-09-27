package com.github.lzenczuk.apa.web

import java.util.Date

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}

import scala.collection.mutable

/**
  * Created by dev on 27/09/16.
  */
class WsConnectionActor extends ActorPublisher[AppMessage] {

  val queue:mutable.Queue[AppMessage] = mutable.Queue()

  override def receive = {
    case CorrectMessage(time, content) => println(s"Receive message '$content' at $time")
      queue.enqueue(CorrectMessage(System.currentTimeMillis(), s"Response to:$content"))
      publishIfNeeded()
    case Request(cnt) =>
      publishIfNeeded()
    case Cancel => println("Receive cancel")
      context.stop(self)
    case ConnectionClosed => println("Connection closed")
      context.stop(self)
    case _ => println("Unknown message")
  }


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    println(s"WsConnectionActor path: ${self.path}")
  }

  def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0){
      println("On next")
      onNext(queue.dequeue())
    }
  }
}
