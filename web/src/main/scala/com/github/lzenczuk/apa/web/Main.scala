package com.github.lzenczuk.apa.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.stream._

import scala.concurrent.Future

/**
  * Created by dev on 22/09/16.
  */

object Main extends App {

  implicit private val system: ActorSystem = ActorSystem("web")
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val route =
    path("") {
      get {
        system.actorSelection("/user/wsConnectionActor_*") ! CorrectMessage(System.currentTimeMillis(), "REST message")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello akka web app</h1"))
      }
    } ~
      path("ws") {
        get {
          handleWebSocketMessages(WebSocketService.flowHandler())
        }
      }

  private val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(route, "localhost", 8099)
}
