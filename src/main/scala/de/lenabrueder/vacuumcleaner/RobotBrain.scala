package de.lenabrueder.vacuumcleaner

import akka.actor.{ Actor, ActorPath }
import de.lenabrueder.vacuumcleaner.RobotBrain._

/**
  * Contains the "brain" of the robot. The brain gets messages from the simulation from time to time, such
  * as collisions with other robots, or sensor updates. The brain can then react on what happened.
  */
class RobotBrain extends Actor {
  override def receive: Receive = {
    case BumpWall =>
      //println(s"${self.path.name} bumped into a wall. turning right!")
      sender() ! TurnRight()
    // case message => println(s"${self.path.name}: $message")
    case BumpRobot(_) =>
      TurnRight(10.0)
    //      if (Math.random() < 0.01) { sender() ! Stop; sender ! Faster }
    //      if (Math.random() < 0.01) sender() ! Faster
  }
}

object RobotBrain {
  case object BumpWall
  case class BumpRobot(otherBrain: ActorPath)
  case class FoundDirt(amount: Double)
  case object Faster
  case object Slower
  case object Stop
  case class TurnRight(amount: Double = 90.0)
  case class TurnLeft(amount: Double = 90.0)
  case object LogState
}
