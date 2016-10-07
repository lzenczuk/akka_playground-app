package com.github.lzenczuk.apa.web.crawler.akka

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.github.lzenczuk.apa.web.crawler.{CrawlerRequest, CrawlerRequestMethod, FailureResponse}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by dev on 07/10/16.
  */
class CrawlerActorSpec extends TestKit(ActorSystem("CrawlerActorSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll{

  override protected def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An crawler actor" should {

    "when validating crawler request" must {

      val crawlerActor = system.actorOf(CrawlerActor.props())

      "send back FailureResponse when url is null" in {

        crawlerActor ! CrawlerRequest(CrawlerRequestMethod.GET, null)

        expectMsg(FailureResponse(List("Null url value.")))
      }

      "send back FailureResponse when url is empty" in {

        crawlerActor ! CrawlerRequest(CrawlerRequestMethod.GET, "")

        expectMsg(FailureResponse(List("Empty url.")))
      }

      "send back FailureResponse when url is not in correct format" in {

        crawlerActor ! CrawlerRequest(CrawlerRequestMethod.GET, "no url string")

        expectMsg(FailureResponse(List("Incorrect url format. Can't parse it.")))
      }

      "send back FailureResponse when missing request method" in {

        crawlerActor ! CrawlerRequest(null, "http://www.wikipedia.org")

        expectMsg(FailureResponse(List("Null method")))
      }

      "send back FailureResponse with multiple error messages when receive request with multiple errors" in {

        crawlerActor ! CrawlerRequest(null, "invalid url")

        expectMsgPF(){
          case f:FailureResponse =>
            f.errors should have size 2
            f.errors should contain("Null method")
            f.errors should contain("Incorrect url format. Can't parse it.")
          case _ =>
            println("-----------------------> fail")
            fail()
        }
      }
    }
  }
}
