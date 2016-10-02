package de.lenabrueder.vacuumcleaner

import akka.actor.{ Actor, ActorRef }

/**
  * Contains the "brain" of the robot. The brain gets messages from the simulation from time to time, such
  * as collisions with other robots, or sensor updates. The brain can then react on what happened.
  */
class RobotBrain extends Actor {
  override def receive: Receive = ???
}

object RobotBrain {
  case object BumpWall
  case class BumpRobot(otherBrain: ActorRef)
  case class FoundDirt(amount: Double)
  case object Faster
  case object Slower
  case object Stop
  case class TurnRight(amount: Double)
  case class TurnLeft(amount: Double)
}
