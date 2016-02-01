package models

/**
 * Created by mgalilee on 11/01/2016.
 */
object DefaultGridModel {
  // means the world is about 100 * 100 hexas
  val xMax = 50
  val yMax = 50
}

import DefaultGridModel._
import world2d.HexaGrid
import world2d.LivingHexagon
import world2d.Point

class DefaultGridModel extends GridModel[LivingHexagon] {

  def worldLimits = (new LivingHexagon(-xMax, -yMax).center, new LivingHexagon(xMax, yMax).center)

  def worldSize = {
    val limits = worldLimits
    (limits._2.x - limits._1.x, limits._2.y - limits._1.y)
  }

  // methods modifying the state of the model

  def at(p: Point): LivingHexagon = {
    // living hexa are rotated, so switch x and y to match
    HexaGrid.at(Point(p.y, p.x) / LivingHexagon.scaling)
  }

  def atGridCoords(x: Int, y: Int) = new LivingHexagon(x, y)
}