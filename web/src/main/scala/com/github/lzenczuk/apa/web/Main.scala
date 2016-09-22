package com.github.lzenczuk.apa.web

import java.util.Date

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.Future

/**
  * Created by dev on 22/09/16.
  */

object Main extends App{

  implicit private val system: ActorSystem = ActorSystem("web")
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val route =
    path("") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello akka web app</h1"))
      }
    }

  private val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(route, "localhost", 8099)
}
