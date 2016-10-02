package de.lenabrueder.vacuumcleaner

import akka.actor.{ Actor, ActorRef, Props }
import breeze.linalg.{ DenseVector, norm }
import de.lenabrueder.vacuumcleaner.WorldActor.{ Done, Simulator, StartSimulation, Tick, TickDone, WorldState }

/**
  * The world actor that holds state of the world that contains all simulated objects.
  *
  * The world sends a "tick" message to all simulated objects that contains the current state of the world, such
  * that the simulated objects can then update themselves according to the current state of the world.
  */
class WorldActor extends Actor {
  var robots: Map[ActorRef, Option[Any]] = Map.empty
  var state: WorldState = null

  override def receive: Receive = {
    case StartSimulation(newRoom, numRobots) =>
      startSimulation(newRoom, numRobots)
      sender ! Done
    case TickDone(status) =>
      robots = robots.updated(sender(), Some(TickDone(status)))
      state = state.copy(states = status :: state.states.filterNot(_.simulator != sender()).toList)
      if (tickDone()) { //reset last message seen - we wait for the last to finish before ticking again.
        robots = robots.map{ case (robot, _) => (robot, None) }
      }
  }

  def startSimulation(newRoom: Room, numRobots: Int): Unit = {
    val room = newRoom
    robots = (for { i <- 1 to numRobots } yield {
      context.actorOf(Props[Simulator]) -> None
    }).toMap
    state = WorldState(room, for { (robot, lastMessage) <- robots } yield {
      Simulator.State(robot, room.randomPosition, DenseVector.zeros(3), 0.0)
    })
  }

  def tick(): Unit = {
    for { (robot, lastMessage) <- robots } {
      robot ! Tick(state)
    }
  }
  def tickDone(): Boolean = robots.forall(_._2.exists(_.isInstanceOf[TickDone]))
}

object WorldActor {
  type Simulator = RobotSimulation
  val Simulator = RobotSimulation

  implicit class DenseVectorOps(val a: DenseVector[Double]) extends AnyVal {
    def distanceTo(b: DenseVector[Double]): Double = norm(a - b)
    def contact(size: Double)(b: DenseVector[Double]): Boolean = distanceTo(b) < 2 * size
  }

  trait SimulatorState {
    val position: DenseVector[Double]
    lazy val x = position(0)
    lazy val y = position(1)
    lazy val z = position(2)

    require(position.length == 3)

    val heading: DenseVector[Double]
    require(heading.length == 3)

    val velocity: Double

    val simulator: ActorRef
  }
  case class WorldState(room: Room, states: Iterable[SimulatorState])

  /**
    * Every simulated object will get this message for each simulation step and must respond with TickDone. There is a
    * timeout after which it is assumed that nothing has changed.
    *
    * @param worldState the state of the world this object should take into account for the simulation
    */
  case class Tick(worldState: WorldState /*TODO: Need to find out how to hold the state of the world*/ )

  /**
    * Informs the world that this actor has finished simulating this tick.
    *
    * @param status the new status of the object after the simulation tick is done
    */
  case class TickDone(status: SimulatorState /*TODO: Need to find out how to represent the state*/ )

  case object GetStatus
  case class StatusUpdate(worldState: WorldState)

  case class StartSimulation(room: Room, numRobots: Int)
  case object StopSimulation

  case object Done
}
