package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.esri.core.geometry.Polygon
import models.{DataSet, QueryResult}
import org.joda.time.{DateTime, Interval}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
  * Each user is an actor.
  */
class UserActor(out: ActorRef)(implicit val cachesActor: ActorRef) extends Actor with ActorLogging {

  import akka.pattern.ask

  implicit val timeout = Timeout(5.seconds)

  def receive() = {
    case json: JsValue =>
      val parsedQuery = parseQuery(json)
      cachesActor ! parsedQuery
    //      (cachesActor ? parsedQuery).mapTo[QueryResult] onComplete {
    //        case Success(result) => out ! Json.toJson(result)
    //        case Failure(t) => out ! Json.toJson(QueryResult.Failure)
    //      }
    case result: QueryResult =>
      log.info("send to out:" + result)
      out ! Json.toJson(result)
    case other =>
      log.info("received:" + other)
  }

  def parseQuery(json: JsValue) = {
    //    val rESTFulQuery = json.as[RESTFulQuery]
    val rESTFulQuery = RESTFulQuery.Sample.copy(keyword = (json \ "keyword").as[String])
    val entities = Knowledge.geoTag(new Polygon(), rESTFulQuery.level)
    ParsedQuery(DataSet.Twitter, rESTFulQuery.keyword, new Interval(rESTFulQuery.timeStart, rESTFulQuery.timeEnd), entities)
  }

}

object UserActor {
  def props(out: ActorRef)(implicit cachesActor: ActorRef) = Props(new UserActor(out))
}

case class RESTFulQuery(dataset: String,
                        keyword: String,
                        timeStart: Long,
                        timeEnd: Long,
                        leftBottomLog: Double,
                        leftBottomLat: Double,
                        rightTopLog: Double,
                        rightTopLat: Double,
                        level: Int,
                        seconds: Long = 5
                       )

object RESTFulQuery {

  val Sample = RESTFulQuery(DataSet.Twitter.name, "rain",
    new DateTime(2012, 1, 1, 0, 0).getMillis(),
    new DateTime(2016, 3, 1, 0, 0).getMillis(),
    -146.95312499999997,
    7.798078531355303,
    -45.703125,
    61.3546135846894,
    1
  )
  implicit val restfulQueryWriter = Json.writes[RESTFulQuery]
  implicit val restfulQueryFormat = Json.format[RESTFulQuery]
}

