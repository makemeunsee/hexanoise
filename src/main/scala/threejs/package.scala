import geometry.Matrix4
import org.denigma.threejs._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Created by markus on 09/01/2016.
  */
package object threejs {

  @JSName("THREE.PlaneBufferGeometry")
  @js.native
  class PlaneBufferGeometry(width: Float, height: Float) extends Geometry

  // BufferGeometry from org.denigma.threejs does not extends Geometry, has to be redefined
  @JSName("THREE.BufferGeometry")
  @js.native
  class MyBufferGeometry extends Geometry {
    override def clone(): MyBufferGeometry = js.native

    var attributes: js.Array[BufferAttribute] = js.native
    var drawcalls: js.Any = js.native
    var offsets: js.Any = js.native

    def setIndex(attribute: BufferAttribute): js.Dynamic = js.native

    def addAttribute(name: String, attribute: BufferAttribute): js.Dynamic = js.native

    def addAttribute(name: String, array: js.Any, itemSize: Double): js.Dynamic = js.native

    def getAttribute(name: String): js.Dynamic = js.native

    def addDrawCall(start: Double, count: Double, index: Double): Unit = js.native

    def applyMatrix(matrix: Matrix4): Unit = js.native

    def fromGeometry(geometry: Geometry, settings: js.Any = js.native): BufferGeometry = js.native

    def computeVertexNormals(): Unit = js.native

    def computeOffsets(indexBufferSize: Double): Unit = js.native

    def merge(): Unit = js.native

    def normalizeNormals(): Unit = js.native

    def reorderBuffers(indexBuffer: Double, indexMap: js.Array[Double], vertexCount: Double): Unit = js.native
  }

  @JSName("whocares")
  @js.native
  object ReadableWebGLRendererParameters extends WebGLRendererParameters {
    preserveDrawingBuffer = false
  }

  def assembleMesh( geometry: Geometry, material: Material, name: String ): Mesh = {
    val mesh = new Mesh
    mesh.geometry = geometry
    mesh.material = material
    mesh.frustumCulled = false
    mesh.name = name
    mesh
  }
}