package de.lenabrueder.vacuumcleaner.simulation

import akka.actor.{ Actor, ActorLogging, ActorPath, ActorRef, Props }
import breeze.linalg.{ DenseMatrix, DenseVector, norm }
import de.lenabrueder.vacuumcleaner.simulation.WorldActor.{ Done, GetStatus, Simulator, StartSimulation, StatusUpdate, Tick, TickDone, WorldState, WorldTick }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

/**
  * The world actor that holds state of the world that contains all simulated objects.
  *
  * The world sends a "tick" message to all simulated objects that contains the current state of the world, such
  * that the simulated objects can then update themselves according to the current state of the world.
  */
class WorldActor extends Actor with ActorLogging {
  var robots: Seq[ActorRef] = Seq.empty
  var state: WorldState = null

  override def receive: Receive = {
    case StartSimulation(newRoom, numRobots) =>
      startSimulation(newRoom, numRobots)
      sender ! Done
      println(s"simulation started for $numRobots robots.")
    case TickDone(status) =>
      state = state.copy(states = status :: state.states.filterNot(_.simulator == sender().path).toList)
    case WorldTick =>
      //printState()
      tick()
    case GetStatus => sender() ! StatusUpdate(state)
  }

  def startSimulation(newRoom: Room, numRobots: Int): Unit = {
    val room = newRoom
    robots = for { i <- 1 to numRobots } yield {
      context.actorOf(Props[Simulator], s"robot$i")
    }
    state = WorldState(room, for { robot <- robots } yield {
      Simulator.State(robot.path, room.randomPosition, { val v = DenseVector.rand(Room.dimension); v / norm(v) }, Simulator.maxVelocity * Math.random())
    })
    context.system.scheduler.schedule(500.milliseconds, 20.milliseconds, self, WorldTick)
  }

  def tick(): Unit = {
    for { robot <- robots } {
      robot ! Tick(state)
    }
  }
  def printState(): Unit = {
    val statePlane = DenseMatrix.zeros[Int](state.room.scaling.toInt + 3, state.room.scaling.toInt + 3)
    for (robotState <- state.states) {
      Try(statePlane.update(robotState.position(0).floor.toInt, robotState.position(1).floor.toInt, robotState.simulator.name.filter(_.isDigit).toInt))
    }
    for (i <- 1 to 30) { println() }
    println(statePlane.toString.replaceAll(" ", "").replaceAll("0", " "))
  }
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

    require(position.length == Room.dimension)

    val heading: DenseVector[Double]
    require(heading.length == Room.dimension)

    val velocity: Double

    val simulator: ActorPath
  }
  case class WorldState(room: Room, states: Seq[SimulatorState])

  /**
    * Every simulated object will get this message for each simulation step and must respond with TickDone. There is a
    * timeout after which it is assumed that nothing has changed.
    *
    * @param worldState the state of the world this object should take into account for the simulation
    */
  case class Tick(worldState: WorldState /*TODO: Need to find out how to hold the state of the world*/ )
  case object WorldTick

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
