package de.lenabrueder.vacuumcleaner

import akka.actor.{ Actor, ActorRef }
import breeze.linalg.DenseVector

/**
  * A simulation of a robot. Reacts to both changes of the world, or changes triggered through its brain (if any).
  */
class RobotSimulation extends Actor {
  override def receive: Receive = ???
}

object RobotSimulation {
  case class State(
    simulator: ActorRef,
    position:  DenseVector[Double]
  ) extends WorldActor.SimulatorState
}
