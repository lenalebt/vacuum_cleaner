package de.lenabrueder.vacuumcleaner.web

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import de.lenabrueder.vacuumcleaner.simulation.WorldActor.{ GetStatus, SimulatorState, StatusUpdate, WorldState }
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.lenabrueder.vacuumcleaner.simulation._
import spray.json.{ DefaultJsonProtocol, JsNumber, JsObject, JsString, JsValue, RootJsonFormat }

import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Success
import scala.concurrent.duration._
import scala.language.postfixOps

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object RoomFormat extends RootJsonFormat[Room] {
    override def read(json: JsValue): Room = ???

    override def write(obj: Room): JsValue = obj match {
      case RectangleRoom(w, h) => JsObject(
        "w" -> JsNumber(w),
        "h" -> JsNumber(h)
      )
    }
  }
  implicit object SimulatorStateFormat extends RootJsonFormat[SimulatorState] {
    override def read(json: JsValue): SimulatorState = ???

    override def write(obj: SimulatorState): JsValue = obj match {
      case RobotSimulation.State(simulator, position, heading, velocity) =>
        JsObject(
          "simulator" -> JsString(simulator.name.filter(_.isDigit)),
          "position" -> JsObject("x" -> JsNumber(position(0)), "y" -> JsNumber(position(1))),
          "heading" -> JsObject("x" -> JsNumber(heading(0)), "y" -> JsNumber(heading(1))),
          "velocity" -> JsNumber(velocity)
        )
    }
  }
  implicit val worldStateFormat = jsonFormat2(WorldState)
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
}

/**
  * http request router
  */
class Router(worldActor: ActorRef)(implicit ec: ExecutionContext) extends Directives with JsonSupport {
  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    } ~ path("") {
      getFromResource("web/index.html")
    } ~ path("update") {
      get {
        implicit val timeout = Timeout(2 seconds)
        onComplete(worldActor ? GetStatus) {
          case Success(StatusUpdate(worldState)) =>
            complete(worldState)
        }
      }
    } ~ path("updates") {
      get {
        val source: Source[WorldState, NotUsed] = Source.fromIterator(() => new Iterator[WorldState] {
          override def hasNext: Boolean = true
          override def next(): WorldState = {
            Thread.sleep(250)
            implicit val timeout = Timeout(2 seconds)
            Await.result((worldActor ? GetStatus).map {
              case StatusUpdate(worldState) => worldState
            }, 3 seconds)
          }
        })
        complete(source)
      }
    }
}
