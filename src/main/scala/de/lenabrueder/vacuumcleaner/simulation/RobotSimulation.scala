package de.lenabrueder.vacuumcleaner.simulation

import akka.actor.{ Actor, ActorPath, ActorRef, Props }
import breeze.linalg.{ DenseMatrix, DenseVector, norm }
import de.lenabrueder.vacuumcleaner.simulation.RobotBrain._
import de.lenabrueder.vacuumcleaner.simulation.WorldActor.{ SimulatorState, Tick, TickDone, WorldState }
import scalaz._
import Scalaz._
import de.lenabrueder.vacuumcleaner.simulation.WorldActor.DenseVectorOps

/**
  * A simulation of a robot. Reacts to both changes of the world, or changes triggered through its brain (if any).
  */
class RobotSimulation extends Actor {
  val brain: ActorRef = context.actorOf(Props[RobotBrain], s"robotbrain${self.path.name.filter(_.isDigit)}")

  var state: RobotSimulation.State = null

  def partitionMeAndOthers(worldState: WorldState): (SimulatorState, Seq[SimulatorState]) = {
    worldState.states.partition(_.simulator == self.path) |> { case (myState, otherStates) => (myState.head, otherStates) }
  }

  def simulateTick(worldState: WorldState): RobotSimulation.State = {
    val (myState, others) = partitionMeAndOthers(worldState)
    val myInitialState = Option(state) getOrElse myState
    var myNewState = simulateMovement(myInitialState) //state after movement (if any)

    //bump the wall case
    if (!worldState.room.isInside(myNewState.position)) {
      brain ! RobotBrain.BumpWall
      myNewState = recreate(myInitialState)
    }

    //bump another robot case
    val contactAfterMovement = others.filter(_.position.contact(RobotSimulation.robotSize)(myNewState.position))
    for { (contactedRobot, contactedRobotPosition) <- contactAfterMovement.map(robot => (robot.simulator, robot.position)) } {
      context.actorSelection(contactedRobot) ! RobotBrain.BumpedByRobot(self.path)
      brain ! RobotBrain.BumpRobot(contactedRobot)
      myNewState = myNewState.copy(position = {
          def difference = contactedRobotPosition - myNewState.position
        //this is the position where the robots contact each other, from the new projected position, moving myself but not the other robot.
        contactedRobotPosition - (difference / norm(difference)) * (2.0 * RobotSimulation.robotSize)
      })
    }

    myNewState
  }

  def simulateMovement(state: WorldActor.SimulatorState): RobotSimulation.State = state match {
    case state2: RobotSimulation.State => state2.copy(position = state2.position + state2.velocity * state2.heading)
    case other                         => RobotSimulation.State(other.simulator, other.position + other.velocity * other.heading, other.heading, state.velocity)
  }

  def recreate(state: WorldActor.SimulatorState): RobotSimulation.State = RobotSimulation.State(state.simulator, state.position, state.heading, state.velocity)

  import Math.{ cos, sin }
  def deg2rad(deg: Double): Double = deg * 3.14159265358979 / 180.0
  def rot(degree: Double): DenseMatrix[Double] = DenseMatrix((cos(degree), -sin(degree)), (sin(degree), cos(degree)))

  override def receive: Receive = {
    case Tick(worldState) =>
      state = simulateTick(worldState)
      sender() ! TickDone(state)
    case Faster            => state = state.copy(velocity = (state.velocity + (RobotSimulation.maxVelocity / 10.0)) max RobotSimulation.maxVelocity)
    case Slower            => state = state.copy(velocity = (state.velocity - (RobotSimulation.maxVelocity / 10.0)) min -RobotSimulation.maxVelocity)
    case Stop              => state = state.copy(velocity = 0.0)
    case TurnRight(amount) => state = state.copy(heading = rot(deg2rad(amount)) * state.heading)
    case TurnLeft(amount)  => state = state.copy(heading = rot(deg2rad(-amount)) * state.heading)
    case LogState          => println(state)
    case other             => brain ! other
  }
}

object RobotSimulation {
  val robotSize = 0.5
  val maxVelocity = robotSize / 16

  case class State(
    simulator: ActorPath,
    position:  DenseVector[Double],
    heading:   DenseVector[Double],
    velocity:  Double
  ) extends WorldActor.SimulatorState
}
