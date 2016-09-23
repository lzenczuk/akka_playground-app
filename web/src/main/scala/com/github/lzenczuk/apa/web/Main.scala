package com.github.lzenczuk.apa.web

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.impl.StreamLayout.Atomic
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}

import scala.concurrent.Future
import scala.concurrent.duration._

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

  val pairsSource: Source[(Int, Int), NotUsed] = Source.fromGraph(
    GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>

        import GraphDSL.Implicits._

        val source1: Source[Int, NotUsed] = Source(1 to 20)
        val source2: Source[Int, NotUsed] = Source(30 to 50)

        val zip: FanInShape2[Int, Int, (Int, Int)] = builder.add(Zip[Int, Int]())

        source1.filter(_ % 2 == 0) ~> zip.in0
        source2.filter(_ % 3 == 0) ~> zip.in1

        SourceShape(zip.out)
    })

  pairsSource.to(Sink.foreach(p => println(s"Pair: $p"))).run()


  val broadcastSink: Sink[Int, NotUsed] = Sink.fromGraph[Int, NotUsed](GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>

      import GraphDSL.Implicits._

      val sink1: Sink[Int, Future[Done]] = Sink.foreach[Int](n => println(s"Sink 1: $n"))
      val sink2: Sink[Int, Future[Done]] = Sink.foreach[Int](n => println(s"Sink 2: $n"))

      val brodcast: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))

      brodcast.out(0) ~> sink1
      brodcast.out(1) ~> sink2

      SinkShape(brodcast.in)
  })

  Source(1 to 20).to(broadcastSink).run()

  val tickFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph[Int, Int, NotUsed](GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>

      import GraphDSL.Implicits._

      val latest: AtomicInteger = new AtomicInteger()
      val latestSink: SinkShape[Int] = builder.add(Sink.foreach[Int](n => latest.set(n)))

      val tickSource: Source[Int, Cancellable] = Source.tick(0 second, 1 second, 0)
      val getLatestFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(n => latest.intValue())

      val latestTickSource: SourceShape[Int] = builder.add(tickSource.via(getLatestFlow))

      FlowShape(latestSink.in, latestTickSource.out)
  })

  val numberTickSource: Source[Int, NotUsed] = Source.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>

      import GraphDSL.Implicits._

      val tickSource = Source.tick(5 second, 5 second, 0)
      val numbersSource = Source(5 to 11)

      val zipFlow: FanInShape2[Int, Int, (Int, Int)] = builder.add(Zip[Int, Int]())
      val skipTickFlow: FlowShape[(Int, Int), Int] = builder.add(Flow[(Int, Int)].map(n => n._2))

      tickSource ~> zipFlow.in0
      numbersSource ~> zipFlow.in1

      zipFlow.out ~> skipTickFlow

      SourceShape(skipTickFlow.out)
  })

  println("---------------> go")
  numberTickSource.via(tickFlow).to(Sink.foreach(n => println(s"Number: $n"))).run()

  //private val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(route, "localhost", 8099)
}
