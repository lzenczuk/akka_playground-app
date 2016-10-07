package com.github.lzenczuk.apa.web

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream._
import akka.util.Timeout
import com.github.lzenczuk.apa.web.crawler.akka.CrawlerActor
import com.github.lzenczuk.apa.web.crawler.{CrawlerRequest, CrawlerRequestMethod, FailureResponse, SuccessResponse}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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

  private val crawlerActor: ActorRef = system.actorOf(CrawlerActor.props())

  implicit val timeout = Timeout(5 seconds)

  crawlerActor.ask(CrawlerRequest(CrawlerRequestMethod.GET, "http://google.com"))
    .andThen{
    case Success(sr:SuccessResponse) => println(s"Successful response ${sr.code}:${sr.message}")
    case Success(fr:FailureResponse) => println(s"Failure response ${fr.errors}")
    case Failure(ex) => println(s"Main failure ${ex.getMessage}")
  }

  crawlerActor.ask(CrawlerRequest(CrawlerRequestMethod.GET, "http://www.wikipedia.org"))
    .andThen{
      case Success(sr:SuccessResponse) => println(s"Successful response ${sr.code}:${sr.message}")
      case Success(fr:FailureResponse) => println(s"Failure response ${fr.errors}")
      case Failure(ex) => println(s"Main failure ${ex.getMessage}")
    }

  crawlerActor.ask(CrawlerRequest(CrawlerRequestMethod.GET, "http://www.wykop.pl"))
    .andThen{
      case Success(sr:SuccessResponse) => println(s"Successful response ${sr.code}:${sr.message}")
      case Success(fr:FailureResponse) => println(s"Failure response ${fr.errors}")
      case Failure(ex) => println(s"Main failure ${ex.getMessage}")
    }

  crawlerActor.ask(CrawlerRequest(CrawlerRequestMethod.GET, "http://gazeta.pl"))
    .andThen{
      case Success(sr:SuccessResponse) => println(s"Successful response ${sr.code}:${sr.message}")
      case Success(fr:FailureResponse) => println(s"Failure response ${fr.errors}")
      case Failure(ex) => println(s"Main failure ${ex.getMessage}")
    }
}
