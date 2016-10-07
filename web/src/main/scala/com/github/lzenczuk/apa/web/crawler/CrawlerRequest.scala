package com.github.lzenczuk.apa.web.crawler

/**
  * Created by dev on 06/10/16.
  */

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

final case class SuccessResponse(code:Int, message:String) extends CrawlerResponse {
  override def isFailure: Boolean = false
  override def isSuccess: Boolean = true
}

final case class FailureResponse(errors:List[String]) extends CrawlerResponse {
  override def isFailure: Boolean = true
  override def isSuccess: Boolean = false
}

