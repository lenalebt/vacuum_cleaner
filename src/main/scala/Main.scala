import akka.actor.{ ActorSystem, PoisonPill, Props }
import de.lenabrueder.vacuumcleaner.{ RectangleRoom, WorldActor }
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  val system = ActorSystem("ActorSystem")

  val world = system.actorOf(Props[WorldActor], "world")

  implicit val timeout = Timeout(2 seconds)
  world ? WorldActor.StartSimulation(new RectangleRoom(20, 20), 9)
  Thread.sleep(20000)
  world ! PoisonPill
  println("killed actors, shutting down system")
  system.terminate()
}
