package de.lenabrueder.vacuumcleaner

import akka.actor.{ Actor, ActorRef, Props }
import breeze.linalg.{ DenseVector, norm }
import de.lenabrueder.vacuumcleaner.WorldActor.{ Done, Simulator, StartSimulation, WorldState }

/**
  * The world actor that holds state of the world that contains all simulated objects.
  *
  * The world sends a "tick" message to all simulated objects that contains the current state of the world, such
  * that the simulated objects can then update themselves according to the current state of the world.
  */
class WorldActor extends Actor {
  var room: Room = null
  var robots: Seq[ActorRef] = Seq.empty
  var state: WorldState = null

  override def receive: Receive = {
    case StartSimulation(newRoom, numRobots) =>
      startSimulation(newRoom, numRobots)
      sender ! Done
  }

  def startSimulation(newRoom: Room, numRobots: Int): Unit = {
    room = newRoom
    robots = for { i <- 1 to numRobots } yield {
      context.actorOf(Props[Simulator])
    }
    state = WorldState(for { robot <- robots } yield {
      Simulator.State(robot, room.randomPosition)
    })
  }
}

object WorldActor {
  type Simulator = RobotSimulation
  val Simulator = RobotSimulation

  implicit class DenseVectorOps(val a: DenseVector[Double]) extends AnyVal {
    def distanceTo(b: DenseVector[Double]): Double = norm(a - b)
  }

  trait SimulatorState {
    val position: DenseVector[Double]
    lazy val x = position(0)
    lazy val y = position(1)
    lazy val z = position(2)

    require(position.length == 3)

    val simulator: ActorRef
  }
  case class WorldState(states: Iterable[SimulatorState])

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
