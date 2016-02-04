package demo.webapp

import geometry.Matrix4
import org.denigma.threejs._
import rendering.shaders.ShadersPack
import rendering.shaders.ShaderModule

import threejs._
import models.DefaultGridModel
import world2d.HexaGrid._
import world2d.LivingHexagon
import world2d.Point

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
 * Created by markus on 17/06/2015.
 */
object ThreeScene {

  private val textureW = 256
  private val textureH = 128

  def colorCode( faceId: Int ) = faceId +1

  def revertColorCode( colorCode: Int ): Int = {
    colorCode -1
  }

  import scala.language.implicitConversions
  implicit def toJsMatrix( m: Matrix4 ): org.denigma.threejs.Matrix4 = {
    val r = new org.denigma.threejs.Matrix4()
    r.set( m.a00, m.a01, m.a02, m.a03
         , m.a10, m.a11, m.a12, m.a13
         , m.a20, m.a21, m.a22, m.a23
         , m.a30, m.a31, m.a32, m.a33
    )
    r
  }

  val t0 = System.currentTimeMillis
}

import demo.webapp.ThreeScene._

class ThreeScene( config: Config ) {

  // ******************** three scene basics ********************

  // dummy cam for texture rendering
  private val dummyCam = new Camera

  private var camera = new OrthographicCamera
  camera.position.x = 0
  camera.position.y = 0
  camera.position.z = 1
  camera.scale.x = 2
  camera.scale.y = 2
  camera.lookAt( new Vector3(0,0,0) )

  private val scene = new Scene
  private val rtScene = new Scene

  @JSExport
  val renderer = new WebGLRenderer( ReadableWebGLRendererParameters )
  ( setBackgroundColor _ ).tupled( demo.JsColors.jsStringToFloats( config.`Background color` ) )

  private var shaderModule: ShaderModule[LivingHexagon] = ShadersPack( config.`Shader` )

  // ******************** view management ********************

  private var innerWidth: Int = 0
  private var innerHeight: Int = 0

  @JSExport
  def updateViewport( width: Int, height: Int ): Unit = {
    innerWidth = width
    innerHeight = height

    val oldCamera = camera
    camera = new OrthographicCamera(-width/2, width/2, height/2, -height/2, -0.1, 0.1)
    camera.position.x = oldCamera.position.x
    camera.position.y = oldCamera.position.y
    camera.scale.x = oldCamera.scale.x
    camera.scale.y = oldCamera.scale.y

    adjustTexturing( innerWidth, innerHeight )
  }

  @JSExport
  def dragView( deltaX: Int, deltaY: Int ): Unit = {
    camera.position.x -= deltaX.toFloat / innerWidth * 2 * camera.scale.x
    camera.position.y += deltaY.toFloat / innerHeight * 2 * camera.scale.y
  }

  private val zoomSpeed = 1.1f

  @JSExport
  def zoom( inOrOut: Int ): Unit = math.signum(inOrOut) match {
    case -1 =>
      camera.scale.x = camera.scale.x * zoomSpeed
      camera.scale.y = camera.scale.y * zoomSpeed
    case 1 =>
      camera.scale.x = camera.scale.x / zoomSpeed
      camera.scale.y = camera.scale.y / zoomSpeed
    case _ =>
      ()
  }

  // ******************** mesh management ********************

  private val gridModel = new DefaultGridModel
  private val worldHexas = gridModel.window(Point(-1600, -900), Point(1600, 900))._1
  println(s"worldHexas: ${worldHexas.size}")
  private val backgroundMesh: Mesh = createBackground( worldHexas )

  private def createBackground( hexagons: Seq[LivingHexagon] ): Mesh = {
    val mesh = shaderModule.makeMesh( hexagons )
    shaderModule.update( mesh )
    scene.add( mesh )
    mesh
  }

  private def makeScreenMesh: Mesh = {
    val plane = new PlaneBufferGeometry( 2, 2 )

    val customUniforms = js.Dynamic.literal(
      "u_texture" -> js.Dynamic.literal("type" -> "t", "value" -> renderingTexture )
    )
    val shaderMaterial = new ShaderMaterial
    shaderMaterial.uniforms = customUniforms
    shaderMaterial.vertexShader =
      """varying vec2 vUv;
        |void main() {
        |    vUv = position.xy;
        |    gl_Position = vec4( 2.0 * position-1.0, 1.0 );
        |}
      """.stripMargin
    shaderMaterial.fragmentShader =
      """uniform sampler2D u_texture;
        |varying vec2 vUv;
        |void main(){
        |    gl_FragColor = texture2D( u_texture, vUv );
        |}
      """.stripMargin
    shaderMaterial.depthWrite = false

    assembleMesh( plane, shaderMaterial, "screenMesh" )
  }

  // ******************** special effects ********************

  def now: Float = System.currentTimeMillis - t0

  val maxTextureSize: Int = {
    val gl = renderer.getContext().asInstanceOf[scala.scalajs.js.Dynamic]
    gl.getParameter( gl.MAX_TEXTURE_SIZE ).asInstanceOf[Int]
  }

  def setShaders( shaderModule: ShaderModule[LivingHexagon] ): Unit = {
    this.shaderModule = shaderModule
    shaderModule.update( backgroundMesh )
  }

  def setBackgroundColor( r: Float, g: Float, b: Float ): Unit = {
    renderer.setClearColor( new org.denigma.threejs.Color( r, g, b ) )
  }

  def udpateDownsampling(): Unit = {
    adjustTexturing( innerWidth, innerHeight )
  }

  private val screenMesh: Mesh = makeScreenMesh
  rtScene.add( screenMesh )

  private var renderingTexture = makeTexture( textureW, textureH )

  private def makeTexture( w: Int, h: Int ): WebGLRenderTarget = {
    val t = new WebGLRenderTarget( w, h )
    //    t.asInstanceOf[js.Dynamic].updateDynamic( "format" )( 1020d ) // RGB
    t.asInstanceOf[js.Dynamic].updateDynamic( "minFilter" )( 1006d ) // Linear, needed for non power of 2 sizes
    t.asInstanceOf[js.Dynamic].updateDynamic( "magFilter" )( 1003d ) // Nearest. Comment for smoother rendering
    t
  }

  private def adjustTexturing( w: Int, h: Int ): Unit = {
    val downsampling = config.safeDownsamplingFactor
    renderingTexture.dispose()
    renderingTexture = makeTexture( w / downsampling, h / downsampling )
  }

  // ******************** rendering ********************

  @JSExport
  def render(): Unit = {
    ShaderModule.uniformLoader( backgroundMesh )( "u_time", now )
    renderer.clearColor()

    val applyDownsampling = config.safeDownsamplingFactor > 1
    if ( applyDownsampling ) {
      renderer.render( scene, camera, renderingTexture )
      ShaderModule.uniformLoader( screenMesh )( "u_texture", renderingTexture )
      renderer.render( rtScene, dummyCam )
    } else {
      renderer.render( scene, camera )
    }
  }
}
