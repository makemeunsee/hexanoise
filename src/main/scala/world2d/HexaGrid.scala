package world2d

import scala.util.Random
import Hexagon.{Route, Neighbour, NeighbourN, NeighbourS, NeighbourSE, NeighbourSW, NeighbourNE, NeighbourNW}
import scala.annotation.tailrec

object HexaGrid {
  
  val xStep = Hexagon.xSpacing
  val yStep = Hexagon.ySpacing / 2
  
  def at(p: Point): Hexagon = {
    val col = (p.x / xStep).toInt - (if (p.x < 0) 1 else 0)
    val row = (p.y / yStep).toInt - (if (p.y < 0) 1 else 0)
    // depending on row and column modulo, the hexagons to choose from differ
    val (h1, h2) = (col % 2, row % 2) match {
      case (0, 0) => (Hexagon(col, row/2),     Hexagon(col+1, row/2+1))
      case (0, _) => (Hexagon(col, (row+1)/2), Hexagon(col+1, (row+1)/2))
      case (_, 0) => (Hexagon(col, row/2+1),   Hexagon(col+1, row/2))
      case (_, _) => (Hexagon(col, (row+1)/2), Hexagon(col+1, (row+1)/2))
    }
    // check distance to center of possible hexagons
    val v1 = h1.center - p
    val v2 = h2.center - p
    if ( v1.x*v1.x + v1.y*v1.y < v2.x*v2.x + v2.y*v2.y ) h1
    else h2
  }

  def window[T: Numeric](n1: T, n2: T, n3: T, n4: T): Seq[Hexagon] = {
    window(Point(n1, n2), Point(n3, n4))
  }
  def window(p1: Point, p2: Point) = {
    val h1 = at(p1)
    val h2 = at(p2)
    (math.min(h1.x, h2.x)-1 to math.max(h1.x, h2.x)+1)
    .flatMap(x =>
      (math.min(h1.y, h2.y)-1 to math.max(h1.y, h2.y)+1)
      .map(y => Hexagon(x,y) ) )
  }

  // indicate if a hexagon (toLocate) is inside the arc of neighbours of center, starting at initialDir(center), stopping at stopAt
  // 'stopAt' and 'toLocate' must be neighbours of 'center'.
  // indicates if 'toLocate' is found among the neighbours of 'center',
  // looking from 'center(initialDir)' up to 'stopAt' (excluded).
  // 'direction' indicates clockwise (<0) or counterclockwise (>=0) search
  @tailrec
  def locate(center: Hexagon, initialDir: Neighbour, stopAt: Hexagon, direction: Int, toLocate: Hexagon): Boolean = {
    if ( initialDir(center) == stopAt ) false
    else if ( initialDir(center) == toLocate ) true
    else if ( direction < 0 ) locate(center, initialDir.previous, stopAt, direction, toLocate)
    else locate(center, initialDir.next, stopAt, direction, toLocate)
  }

  private def djikstra(rem: Set[Hexagon],
                       currentFront: Map[Hexagon, Route],
                       currentDistance: Int,
                       end: Hexagon): Option[Route] = {
    if (currentFront.isEmpty) None
    else if (currentFront contains end) currentFront.get(end)
    else if (rem.isEmpty) None
    else {
      val newRem = rem -- currentFront.keySet
      val newFront = currentFront
      .flatMap {
        case (h ,r) =>
          h.neighboursWithDir
            .map { case (n, nextH) => (nextH, r :+ n)} }
      .filter {
        case (h, _) =>
          rem contains h
      }
      djikstra(newRem, newFront, currentDistance+1, end)
    }
  }
  
  def path(set: Set[Hexagon], start: Hexagon, end: Hexagon): Option[Route] = {
    if (!(set contains start) || !(set contains end)) None
    else djikstra(set, Map((start, Hexagon.EmptyRoute)), 0, end)
  }
  
  private def djikstraClosest(rem: Set[Hexagon],
                              currentFront: Map[Hexagon, Route],
                              currentDistance: Int,
                              closest: (Hexagon, Route), end: Hexagon): (Hexagon, Route) = {
    val newClosest = currentFront.foldLeft(closest){
      case ((oldH, oldR), (h, r)) =>
        val oldVec = oldH.center - end.center
        val vec = h.center - end.center
        if ((oldVec dot oldVec) > (vec dot vec)) (h, r)
        else (oldH, oldR)
    }
    if (currentFront.isEmpty) newClosest
    else if (currentFront contains end) (end, currentFront(end))
    else if (rem.isEmpty) newClosest
    else {
      val newRem = rem -- currentFront.keySet
      val newFront = currentFront
      .flatMap {
        case (h ,r) =>
          h.neighboursWithDir
            .map {
              case (n, nextH) =>
                (nextH, r :+ n)
            }
      }
      .filter {
        case (h, _) =>
          rem contains h
      }
      djikstraClosest(newRem, newFront, currentDistance+1, newClosest, end)
    }
  }

  def pathToClosest(set: Set[Hexagon], start: Hexagon, end: Hexagon): (Hexagon, Route) = {
    val defaultSolution = (start, Hexagon.EmptyRoute)
    if (!(set contains start)) defaultSolution
    else djikstraClosest(set, Map(defaultSolution), 0, defaultSolution, end)
  }
}