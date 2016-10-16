import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import de.lenabrueder.vacuumcleaner.simulation.{ RectangleRoom, WorldActor }
import de.lenabrueder.vacuumcleaner.web.Router

import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object Main extends App {
  implicit val system = ActorSystem("ActorSystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val world = system.actorOf(Props[WorldActor], "world")

  implicit val timeout = Timeout(2 seconds)
  world ? WorldActor.StartSimulation(new RectangleRoom(20, 20), 9)

  private val router = new Router(world)
  val bindingFuture = Http().bindAndHandle(router.route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  //StdIn.readLine() // let it run until user presses return
  Thread.sleep(30000)
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete{ _ =>
      world ! PoisonPill
      system.terminate()
    }
}
