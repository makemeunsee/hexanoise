package models

import world2d.Hexagon

// hexagon coordinates
case class HexaBlock(x0: Int, y0: Int, x1: Int, y1: Int) {
  def hexagons[H <: Hexagon](gridModel: GridModel[H]): Iterable[H] =
    for( i <- x0 to x1; j <- y0 to y1 ) yield { gridModel.atGridCoords(i, j) }
}

trait HexaWorldModel {
  // using real coordinates
  def updateBounds(x0: Double, y0: Double, x1: Double, y1: Double): HexaWorldModel
  def blocks: Set[HexaBlock]
}