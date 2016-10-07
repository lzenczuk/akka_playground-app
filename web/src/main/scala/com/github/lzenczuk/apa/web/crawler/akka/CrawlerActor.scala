package com.github.lzenczuk.apa.web.crawler.akka

import akka.NotUsed
import akka.actor.ActorRef
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.actor.ActorPublisher
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.lzenczuk.apa.web.crawler.akka.CrawlerActor.CrawlerHttpClientFlow
import com.github.lzenczuk.apa.web.crawler._

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 07/10/16.
  */
object CrawlerActor {

  type CrawlerHttpClientFlow = Flow[(HttpRequest, ActorRef), (Try[HttpResponse], ActorRef), NotUsed]

  case object HttpStreamClosed

  def props():Props = Props(new CrawlerActor(null))

  def extractHttpMethod(cr: CrawlerRequest): Either[HttpMethod, String] = {
    cr.method match {
      case CrawlerRequestMethod.GET => Left(HttpMethods.GET)
      case CrawlerRequestMethod.POST => Left(HttpMethods.POST)
      case null => Right("Null method")
      case _ => Right("Unsupported method")
    }
  }

  def extractUri(cr: CrawlerRequest): Either[Uri, String] = {
    try {
      val uri: Uri = Uri(cr.url)

      if(uri.isEmpty) Right("Empty url.")
      else Left(uri)

    } catch {
      case ex: NullPointerException => Right("Null url value.")
      case ex: IllegalUriException => Right("Incorrect url format. Can't parse it.")
    }
  }

  def crawlerRequestToHttpRequest(cr: CrawlerRequest): Either[HttpRequest, CrawlerResponse] = {
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

  def httpResponseToCrawlerResponse(hr: HttpResponse): CrawlerResponse = {
    SuccessResponse(hr.status.intValue(), hr.status.reason())
  }
}

class CrawlerActor(private var httpFlow:CrawlerHttpClientFlow) extends ActorPublisher[(HttpRequest, ActorRef)] {

  import CrawlerActor._

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  if(httpFlow==null){
    httpFlow = Http(context.system).superPool[ActorRef]()
  }

  val queue: mutable.Queue[(HttpRequest, ActorRef)] = mutable.Queue()

  Source.fromPublisher(ActorPublisher(self)).via(httpFlow).to(Sink.actorRef(self, HttpStreamClosed)).run()

  override def receive = {

    // External request
    case cr: CrawlerRequest =>
      crawlerRequestToHttpRequest(cr) match {
        case Left(request) =>
          queue.enqueue((request, sender()))
          publishIfNeeded()
        case Right(response) =>
          sender() ! response
      }

    case (response:Try[HttpResponse], requestSender:ActorRef) =>
      response match {
        case Success(httpResponse) => requestSender ! httpResponseToCrawlerResponse(httpResponse)
        case Failure(ex) => requestSender ! FailureResponse(List(ex.getMessage))
      }

    // ActorPublisher messages
    case Request(cnt) =>
      publishIfNeeded()
    case Cancel =>
      context.stop(self)

    // Stream end message
    case HttpStreamClosed =>
      context.stop(self)
    case _ => println("Unknown message")
  }

  def publishIfNeeded() = {
    while (queue.nonEmpty && isActive && totalDemand > 0) {
      onNext(queue.dequeue())
    }
  }
}
