package io.coral.actors.transform

// scala

import spray.http.HttpHeaders.RawHeader

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scalaz.OptionT
import scala.concurrent.duration._

// akka
import akka.actor.{ActorLogging, Props}

//json goodness
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.render
import org.json4s.native.JsonMethods._

// coral
import io.coral.actors.CoralActor

// spray client
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}

object HttpClientActor {
  implicit val formats = org.json4s.DefaultFormats

  def apply(json: JValue): Option[Props] = {
    Some(Props(classOf[HttpClientActor], json))
  }
}

class HttpClientActor(json: JObject) extends CoralActor with ActorLogging {
  private val TimeOut = 5.seconds

  def jsonDef = json
  def state   = Map.empty
  def timer   = noTimer

  var answer: HttpResponse = _

  def trigger: (JObject) => OptionT[Future, Unit] = {
    json: JObject =>
      try {
        // from trigger data
        val url: String = (json \ "url").extract[String]
        val methodString = (json \ "method").extract[String]
        val payload: JObject = (json \ "payload").extractOrElse[JObject](JObject())
        val headers = (json \ "headers").extractOrElse[JObject](JObject())

        val method: RequestBuilder = methodString match {
          case "POST" => Post
          case "GET" => Get
          case "PUT" => Put
          case "DELETE" => Delete
          case _ => throw new IllegalArgumentException("method unknown " + methodString)
        }

        import io.coral.api.JsonConversions._
        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
        val rawHeaders = headers.values.map{case (key, value) => RawHeader(key, value.asInstanceOf[String])}.toList
        val response: Future[HttpResponse] = pipeline(method(url, payload).withHeaders(rawHeaders))

        response onComplete {
          case Success(resp) =>
            answer = resp
            log.info("HTTP " + method + " " + url + " " + payload + " successful.")
          case Failure(error) =>
            log.error("Failure: " + error)
        }
        Await.result(response, TimeOut)
      } catch {
        case e: Exception =>
          answer = null
          log.error(e.getMessage)
      }

      OptionT.some(Future.successful({}))
  }

  def emit = {
    json: JObject =>
      if (answer != null) {
        val headers = JObject(answer.headers.map(header => JField(header.name, header.value)))
        val contentType = (headers \ "Content-Type").extractOpt[String] getOrElse ""
        val json = contentType == "application/json"
        val body = if (json) parse(answer.entity.asString) else JString(answer.entity.asString)
        val result = render(
            ("status" -> answer.status.value)
          ~ ("headers" -> headers)
          ~ ("body" -> body))
        answer = null
        result
      } else {
        JNothing
      }
}
}