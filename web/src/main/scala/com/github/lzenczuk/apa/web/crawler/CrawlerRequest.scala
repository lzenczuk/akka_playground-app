package com.github.lzenczuk.apa.web.crawler

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.lzenczuk.apa.web.crawler.CrawlerActor.CrawlerHttpClientFlow

import scala.collection.mutable
import scala.util.Try

/**
  * Created by dev on 06/10/16.
  */

// ---------- DOMAIN ----------------
object CrawlerRequestMethod extends Enumeration {
  val GET = Value("GET")
  val POST = Value("POST")
}

case class CrawlerRequest(method: CrawlerRequestMethod.Value, url: String) {

}

sealed abstract class CrawlerResponse {
  def isSuccess:Boolean
  def isFailure:Boolean
}

final case class SuccessResponse() extends CrawlerResponse {
  override def isFailure: Boolean = false
  override def isSuccess: Boolean = true
}

final case class FailureResponse(errors:List[String]) extends CrawlerResponse {
  override def isFailure: Boolean = true
  override def isSuccess: Boolean = false
}

// ---------- FRAMEWORK ----------------

object CrawlerActor {

  type CrawlerHttpClientFlow = Flow[(HttpRequest, ActorRef), (Try[HttpResponse], ActorRef), NotUsed]

  case object HttpStreamClosed

  def extractHttpMethod(cr: CrawlerRequest): Either[HttpMethod, String] = {
    cr.method match {
      case CrawlerRequestMethod.GET => Left(HttpMethods.GET)
      case CrawlerRequestMethod.POST => Left(HttpMethods.POST)
      case _ => Right("Unsupported method")
    }
  }

  def extractUri(cr: CrawlerRequest): Either[Uri, String] = {
    try {
      Left(Uri(cr.url))
    } catch {
      case ex: IllegalUriException => Right("Incorrect url format. Can't parse it.")
    }
  }

  def mapToHttpRequest(cr: CrawlerRequest): Either[HttpRequest, CrawlerResponse] = {
    val method = extractHttpMethod(cr)
    val uri = extractUri(cr)

    val httpRequestOption = for {
      m <- method.left.toOption
      u <- uri.left.toOption
    } yield HttpRequest(method = m, uri = u)

    httpRequestOption match {
      case Some(httpRequest) => Left(httpRequest)
      case None =>
        val errorMessages = List(method.right.toOption, uri.right.toOption).filter(_.isDefined).map(_.get)
        Right(FailureResponse(errorMessages))
    }
  }

  def defaultCrawlerHttpClientFlow() = Http().superPool[ActorRef]()
}

class CrawlerActor(private val httpFlow:CrawlerHttpClientFlow = CrawlerActor.defaultCrawlerHttpClientFlow()) extends ActorPublisher[(HttpRequest, ActorRef)] {

  import CrawlerActor._

  val queue: mutable.Queue[(HttpRequest, ActorRef)] = mutable.Queue()

  Source.fromPublisher(ActorPublisher(self)).via(httpFlow).to(Sink.actorRef(self, HttpStreamClosed))

  override def receive = {

    // External request
    case cr: CrawlerRequest =>
      println(s"Crawler request $cr")
      mapToHttpRequest(cr) match {
        case Left(request) =>
          queue.enqueue((request, sender()))
          publishIfNeeded()
        case Right(response) =>
          sender() ! response
      }

    case (response:Try[HttpResponse], requestSender:ActorRef) =>


    // ActorPublisher messages
    case Request(cnt) =>
      publishIfNeeded()
    case Cancel => println("Receive cancel")
      context.stop(self)

    // Stream end message
    case HttpStreamClosed => println("Connection closed")
      context.stop(self)
    case _ => println("Unknown message")
  }

  def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      println("On next")
      onNext(queue.dequeue())
    }
  }
}