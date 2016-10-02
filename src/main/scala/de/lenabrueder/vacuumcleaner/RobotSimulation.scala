package de.lenabrueder.vacuumcleaner

import akka.actor.{ Actor, ActorRef, Props }
import breeze.linalg.{ DenseVector, norm }
import de.lenabrueder.vacuumcleaner.RobotBrain._
import de.lenabrueder.vacuumcleaner.RobotSimulation.State
import de.lenabrueder.vacuumcleaner.WorldActor.{ DenseVectorOps, SimulatorState, Tick, TickDone, WorldState }

import scalaz._
import Scalaz._

/**
  * A simulation of a robot. Reacts to both changes of the world, or changes triggered through its brain (if any).
  */
class RobotSimulation extends Actor {
  val brain: ActorRef = context.actorOf(Props[RobotBrain])

  var state: RobotSimulation.State = null

  def partitionMeAndOthers(worldState: WorldState): (SimulatorState, Iterable[SimulatorState]) = {
    worldState.states.partition(_.simulator == self) |> { case (myState, otherStates) => (myState.head, otherStates) }
  }

  def simulateTick(worldState: WorldState): RobotSimulation.State = {
    val (myState, others) = partitionMeAndOthers(worldState)
    var myNewState = simulateMovement(myState) //state after movement (if any)

    //bump the wall case
    if (!worldState.room.isInside(myNewState.position)) {
      self ! RobotBrain.BumpWall
      myNewState = recreate(myState)
    }

    //bump another robot case
    val contactAfterMovement = others.filter(_.position.contact(RobotSimulation.robotSize)(myNewState.position))
    for { (contactedRobot, contactedRobotPosition) <- contactAfterMovement.map(robot => (robot.simulator, robot.position)) } {
      contactedRobot ! RobotBrain.BumpRobot(self)
      self ! RobotBrain.BumpRobot(contactedRobot)
      myNewState = myNewState.copy(position = {
          def difference = contactedRobotPosition - myNewState.position
        //this is the position where the robots contact each other, from the new projected position, moving myself but not the other robot.
        contactedRobotPosition + (difference / norm(difference)) * (2.0 * RobotSimulation.robotSize)
      })
    }

    myNewState
  }

  def simulateMovement(state: WorldActor.SimulatorState): RobotSimulation.State = state match {
    case robotSimulationState: RobotSimulation.State => robotSimulationState
    case other                                       => RobotSimulation.State(other.simulator, other.position + other.heading, other.heading, state.velocity)
  }

  def recreate(state: WorldActor.SimulatorState): RobotSimulation.State = RobotSimulation.State(state.simulator, state.position, state.heading, state.velocity)

  override def receive: Receive = {
    case Tick(worldState) => {
      state = simulateTick(worldState)
      sender() ! TickDone(state)
    }
    case Faster            => state = state.copy(velocity = (state.velocity + (RobotSimulation.maxVelocity / 10.0)) max RobotSimulation.maxVelocity)
    case Slower            => state = state.copy(velocity = (state.velocity - (RobotSimulation.maxVelocity / 10.0)) min -RobotSimulation.maxVelocity)
    case Stop              => state = state.copy(velocity = 0.0)
    case TurnRight(amount) => //TODO: vektor drehen
    case TurnLeft(amount)  => //TODO: vektor drehen
    case other             => brain ! other
  }
}

object RobotSimulation {
  val robotSize = 1.0
  val maxVelocity = robotSize / 4

  case class State(
    simulator: ActorRef,
    position:  DenseVector[Double],
    heading:   DenseVector[Double],
    velocity:  Double
  ) extends WorldActor.SimulatorState
}
