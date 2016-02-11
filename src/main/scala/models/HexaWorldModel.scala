package models

import world2d.Hexagon

// hexagon coordinates
case class HexaBlock(x0: Int, y0: Int, x1: Int, y1: Int) {
  def hexagons[H <: Hexagon](gridModel: GridModel[H]): Iterable[H] =
    for( i <- x0 to x1; j <- y0 to y1 ) yield { gridModel.atGridCoords(i, j) }

  def contains[H <: Hexagon]( hexagon: H ): Boolean = {
  	val x = hexagon.x
  	val y = hexagon.y
    x >= x0 && x <= x1 &&
    y >= y0 && y <= y1 &&
    x >= x0 && x <= x1 &&
    y >= y0 && y <= y1
  }
}

trait HexaWorldModel {
  // using real coordinates
  def updateBounds(x0: Float, y0: Float, x1: Float, y1: Float): HexaWorldModel
  def blocks: Set[HexaBlock]
}