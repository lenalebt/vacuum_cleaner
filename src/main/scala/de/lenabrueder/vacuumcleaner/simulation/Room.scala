package de.lenabrueder.vacuumcleaner.simulation

import breeze.linalg.DenseVector

/** The room in which the simulation takes place */
trait Room {
  /**
    * the scaling of the room. Normal rooms have all coordinates in the [0,1]-cube. If not, you need to adjust the
    * scaling to the maximum that is possible in one dimension.
    */
  val scaling: Double

  def isInside(vec: DenseVector[Double]): Boolean
  /**a stream of random positions inside the [0,1]-cube*/
  private def randomPositions: Stream[DenseVector[Double]] =
    (DenseVector.rand[Double](Room.dimension) * scaling) #:: randomPositions
  /**a stream of positions inside this room*/
  def randomInsidePositions: Stream[DenseVector[Double]] = randomPositions filter isInside
  /**a random position inside the room*/
  def randomPosition: DenseVector[Double] = randomPositions.head
}
object Room {
  val dimension = 2
}

case class RectangleRoom(w: Double, h: Double) extends Room {
  override val scaling: Double = Seq(w, h).max

  override def isInside(vec: DenseVector[Double]): Boolean = {
    vec(0) >= 0.0 && vec(1) >= 0.0 &&
      vec(0) <= w && vec(1) <= h
  }
}
