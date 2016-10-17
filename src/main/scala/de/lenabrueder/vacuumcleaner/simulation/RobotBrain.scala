package de.lenabrueder.vacuumcleaner.simulation

import akka.actor.Actor
import de.lenabrueder.vacuumcleaner.simulation.RobotBrain._

/**
  * Contains the "brain" of the robot. The brain gets messages from the simulation from time to time, such
  * as collisions with other robots, or sensor updates. The brain can then react on what happened.
  */
class RobotBrain extends Actor {
  override def receive: Receive = {
    case BumpWall      =>
    //sender() ! TurnRight(10.0)
    case BumpRobot     =>
    //sender() ! TurnRight(5.0)
    case BumpedByRobot => //???
  }
}

object RobotBrain {
  //Nachrichten der Sensoren an das Gehirn
  case object BumpWall //wird von den Sensoren gesendet, wenn man gegen die Wand fährt
  case object BumpRobot //wenn man gegen einen anderen Robotr fährt
  case object BumpedByRobot //wenn man von einem anderen Roboter angefahren wird

  //Befehle für die Aktoren
  case object Faster //Geschwindigkeit erhöhen
  case object Slower //Geschwindigkeit verringern
  case object Stop //Anhalten
  case class TurnRight(amount: Double = 90.0) //Rechts drehen um Winkel in Grad
  case class TurnLeft(amount: Double = 90.0) //Links drehen um Winkel in Grad
  case object TurnAround //Umdrehen (180° Drehung)

  //Zusätzliche Befehle
  case class FoundDirt(amount: Double)
  case object LogState
}
