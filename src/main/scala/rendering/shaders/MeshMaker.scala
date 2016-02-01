package rendering.shaders

import org.denigma.threejs.Mesh
import world2d.Hexagon

object MeshMaker {
  // standards mesh constants
  
  val verticePerHexa = 7 // 7 points to define the triangles in a hexagon
  val indicePerHexa = 18 // 6 explicit triangles
}

trait MeshMaker[H <: Hexagon] {
  def makeMesh(hexas: Seq[_ <: H] ): Mesh
}