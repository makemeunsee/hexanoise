package demo.webapp

import geometry.Matrix4
import org.denigma.threejs._
import rendering.shaders._

import threejs._
import models.DefaultGridModel
import models.HexaBlock
import world2d.{Hexagon, LivingHexagon, Point}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
 * Created by markus on 17/06/2015.
 */
object ThreeScene {

  private val textureW = 256
  private val textureH = 128

  import scala.language.implicitConversions
  implicit private def toJsMatrix( m: Matrix4 ): org.denigma.threejs.Matrix4 = {
    val r = new org.denigma.threejs.Matrix4()
    r.set( m.a00, m.a01, m.a02, m.a03
         , m.a10, m.a11, m.a12, m.a13
         , m.a20, m.a21, m.a22, m.a23
         , m.a30, m.a31, m.a32, m.a33
    )
    r
  }

  private val zoomSpeed = 1.01f

  private val t0 = System.currentTimeMillis

  private val cameraShift = 0 // 25000

  private def makeCamera( width: Int, height: Int, maybeOldCamera: Option[Camera] = None ): OrthographicCamera = {
    val camera = new OrthographicCamera( -width/2, width/2, height/2, -height/2, -0.1, 0.1 )
    camera.position.x = cameraShift
    camera.position.y = 0
    camera.position.z = 1
    camera.scale.x = 1
    camera.scale.y = 1
    camera.lookAt( new Vector3(cameraShift,0,0) )

    for( oldCamera <- maybeOldCamera ) {
      camera.position.x = oldCamera.position.x
      camera.position.y = oldCamera.position.y
      camera.scale.x = oldCamera.scale.x
      camera.scale.y = oldCamera.scale.y
    }

    camera
  }

  private def computeHexablocks( spanHorizontal: Int, spanVertical: Int ): Seq[HexaBlock] = {
    for( i <- -1 until 1 ;
         j <- -1 until 1 ) yield {
      HexaBlock(spanHorizontal * i, spanVertical * j, ( spanHorizontal * (i+1) ) -1, ( spanVertical * (j+1) ) -1 )
    }
  }

  private val hexaRatio = Hexagon.ySpacing / Hexagon.xSpacing
}

import demo.webapp.ThreeScene._

class ThreeScene( config: Config, maxWidth: Int, maxHeight: Int, maxHexagons: Int ) {

  private val screenRatio = maxWidth.toFloat / maxHeight
  private val spanHorizontal = math.sqrt(maxHexagons.toFloat / 4 / screenRatio * hexaRatio).toInt
  private val spanVertical = (spanHorizontal * screenRatio / hexaRatio).toInt

  private val maxScale = {
    val maxVertical = LivingHexagon.scaling * Hexagon.xSpacing * spanHorizontal.toFloat - 1
    val maxHorizontal = LivingHexagon.scaling * Hexagon.ySpacing * (spanVertical - 1)

    math.min( maxHorizontal / maxWidth, maxVertical / maxHeight )
  }

  private val gridModel = new DefaultGridModel

  private val scene = new Scene
  private val rtScene = new Scene

  // dummy cam for texture rendering
  private val dummyCam = new Camera
  private var camera = makeCamera( maxWidth, maxHeight )
  camera.scale.x = math.min( maxScale, 1.0 )
  camera.scale.y = math.min( maxScale, 1.0 )

  @JSExport
  val renderer = new WebGLRenderer( ReadableWebGLRendererParameters )
  ( setBackgroundColor _ ).tupled( demo.JsColors.jsStringToFloats( config.`Background color` ) )

  private var shaderModule: ShaderModule[LivingHexagon] = ShadersPack( config.`Shader` )

  def setCubic(cubic: Boolean): Unit = {
    shaderModule = shaderModule.copy(cubic = cubic)
    setShader( shaderModule )
  }

  private val backgrounds: Seq[Mesh] = computeHexablocks( spanHorizontal, spanVertical ) map createBackground

  private def updateBounds( x0: Float, y0: Float, x1: Float, y1: Float ): Unit = {
    val hexa0 = gridModel.at(Point(x0, y0))
    val i = 1+math.floor( hexa0.x.toFloat / spanHorizontal ).toInt
    val j = 1+math.floor( hexa0.y.toFloat / spanVertical ).toInt
    val displacement = new LivingHexagon(spanHorizontal * i, spanVertical * j).center - new LivingHexagon(0, 0).center
    for( mesh <- backgrounds ) {
      ShaderModule.uniformLoader( mesh )( "u_xDisplacement", displacement.x )
      ShaderModule.uniformLoader( mesh )( "u_yDisplacement", displacement.y )
    }
  }

  private def createBackground( hexablock: HexaBlock ): Mesh = {
    val mesh = ShaderModule.makeMesh( hexablock.hexagons( gridModel ) )
    shaderModule.update( mesh )
    scene.add( mesh )
    mesh
  }

  // ******************** view management ********************

  private var innerWidth: Int = 1
  private var innerHeight: Int = 1

  private def cameraChanged(): Unit = {
//    camera.lookAt( new Vector3(camera.position.x,camera.position.y,0) )
    val x0 = camera.left   * camera.scale.x + camera.position.x * innerWidth  / 2 
    val x1 = camera.right  * camera.scale.x + camera.position.x * innerWidth  / 2 
    val y0 = camera.bottom * camera.scale.y + camera.position.y * innerHeight / 2
    val y1 = camera.top    * camera.scale.y + camera.position.y * innerHeight / 2
    updateBounds( x0.toFloat, y0.toFloat, x1.toFloat, y1.toFloat )
  }

  @JSExport
  def updateViewport( width: Int, height: Int ): Unit = {
    innerWidth = width
    innerHeight = height

    camera = makeCamera( width, height, Some(camera) )

    adjustTexturing( innerWidth, innerHeight )

    cameraChanged()
  }

  @JSExport
  def dragView( deltaX: Int, deltaY: Int ): Unit = {
    camera.position.x -= deltaX.toFloat / innerWidth * 2 * camera.scale.x
    camera.position.y += deltaY.toFloat / innerHeight * 2 * camera.scale.y
    cameraChanged()
  }

  @JSExport
  def zoom( inOrOut: Int ): Unit = math.signum(inOrOut) match {
    case -1 =>
      // zooming out
      camera.scale.x = math.min( maxScale, camera.scale.x * zoomSpeed )
      camera.scale.y = math.min( maxScale, camera.scale.y * zoomSpeed )
      cameraChanged()
    case 1 =>
      // zooming in
      camera.scale.x = camera.scale.x / zoomSpeed
      camera.scale.y = camera.scale.y / zoomSpeed
      cameraChanged()
    case _ =>
      ()
  }

  // ******************** mesh management ********************

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

  def setShader( shaderModule: ShaderModule[LivingHexagon] ): Unit = {
    this.shaderModule = shaderModule
    backgrounds foreach shaderModule.update
    println(shaderModule.vertexShader)
    println(shaderModule.fragmentShader)
    cameraChanged()
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
    for( backgroundMesh <- backgrounds ) {
      ShaderModule.uniformLoader( backgroundMesh )( "u_time", now )
    }
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
