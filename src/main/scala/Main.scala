import akka.actor.{ ActorSystem, Props }
import de.lenabrueder.vacuumcleaner.{ RectangleRoom, WorldActor }

object Main extends App {
  val system = ActorSystem("ActorSystem")

  val world = system.actorOf(Props[WorldActor])

  world ! WorldActor.StartSimulation(new RectangleRoom(100, 100), 5)
}
