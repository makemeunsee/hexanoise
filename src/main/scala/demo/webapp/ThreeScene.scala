package demo.webapp

import geometry.Matrix4
import org.denigma.threejs._
import rendering.shaders.ShadersPack.ShaderPair
import rendering.shaders.ShadersPack

import threejs._
import world2d.HexaGrid._
import world2d.LivingHexagon

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

class ThreeScene( cfg: Config ) {

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
  ( setBackgroundColor _ ).tupled( demo.JsColors.jsStringToFloats( cfg.`Background color` ) )

  private var shaderModule: ShaderPair = ShadersPack( cfg.`Shader` )

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

  private var flakeMesh: Option[Mesh] = None

  def setFlake( flake: Flake[LivingHexagon] ): Unit = {
    flakeMesh.foreach { case oldM =>
      scene.remove( oldM )
      oldM.geometry.dispose()
    }
    flakeMesh = None

    val later = now + 250 // arbitrary, hopefully later than mesh making time
    val mesh = shaderModule.blockShader.makeMesh( new LivingHexagon(0,0) +: flake.transpose.flatten,
      birthOffset = later,
      limit = Some( cfg.safeFlakeSize * 6 ) )
    flakeMesh = Some( mesh )

    if ( cfg.`Draw flake` ) {
      shaderModule.blockShader.loadUniforms(updateMeshUniform( mesh ))
      scene.add( mesh )
    }
  }

  def killFlake( deathFct: () => Float ): Unit = {
    flakeMesh.foreach { case mesh =>
      val deaths = mesh.geometry.asInstanceOf[threejs.MyBufferGeometry].getAttribute("a_deathtime")
      for( i <- 0 until deaths.count.asInstanceOf[Int] / 7 ) {
        val deathTime = deathFct()
        val oldDeathTime = deaths.array.asInstanceOf[js.Array[Float]]( 7 * i )
        if ( oldDeathTime > deathTime ) {
          for( j <- 0 until 7 ) {
            deaths.array.asInstanceOf[js.Array[Float]].update(7 * i + j, deathTime)
          }
        }
      }
      deaths.needsUpdate = true
    }
  }

  def updateFlake(): Unit = flakeMesh.foreach { mesh =>
    val n = now
    val limit = cfg.safeFlakeSize * 6
    val death = n + LivingHexagon.agonyDuration // give some time for death animation

    val deaths = mesh.geometry.asInstanceOf[threejs.MyBufferGeometry].getAttribute("a_deathtime")
    val births = mesh.geometry.asInstanceOf[threejs.MyBufferGeometry].getAttribute("a_birthtime")

    val length = deaths.count.asInstanceOf[Int] / 7

    for ( i <- 0 until length ) {
      for ( j <- 0 until 7 ) {
        val id = i*7 + j
        val oldDeathTime = deaths.array.asInstanceOf[js.Array[Float]]( id )
        if (i > limit) {
          deaths.array.asInstanceOf[js.Array[Float]].update( id, math.min( death, oldDeathTime ) )
        } else {
          if (oldDeathTime < n) {
            births.array.asInstanceOf[js.Array[Float]].update( id, n )
          } else if (oldDeathTime < death) {
            births.array.asInstanceOf[js.Array[Float]].update( id, 2*n - oldDeathTime )
          }
          deaths.array.asInstanceOf[js.Array[Float]].update( id, Float.MaxValue )
        }
      }
    }
    
    deaths.needsUpdate = true
    births.needsUpdate = true
  }

  def toggleFlake(): Unit = {
    if ( cfg.`Draw flake` ) {
      flakeMesh.foreach { mesh =>
        shaderModule.blockShader.loadUniforms(updateMeshUniform( mesh ))
        scene.add(mesh)
      }
    } else {
      flakeMesh.foreach { scene.remove }
    }
  }

  private var backgroundMesh: Option[Mesh] = None

  def setBackground( hexagons: Seq[LivingHexagon] ): Unit = {
    backgroundMesh.foreach { case oldM =>
      scene.remove( oldM )
      oldM.geometry.dispose()
    }

    val mesh = shaderModule.backgroundShader.makeMesh( hexagons )
    backgroundMesh = Some( mesh )

    shaderModule.backgroundShader.loadUniforms(updateMeshUniform( mesh ))
    scene.add( mesh )
  }

  private def makeScreenMesh: Mesh = {
    val plane = new PlaneBufferGeometry( 2, 2 )

    val customUniforms = js.Dynamic.literal(
      "texture" -> js.Dynamic.literal("type" -> "t", "value" -> renderingTexture )
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
      """uniform sampler2D texture;
        |varying vec2 vUv;
        |void main(){
        |    gl_FragColor = texture2D( texture, vUv );
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

  def setShaders( shaders: ShaderPair ): Unit = {
    shaderModule = shaders
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
    val downsampling = cfg.safeDownsamplingFactor
    renderingTexture.dispose()
    renderingTexture = makeTexture( w / downsampling, h / downsampling )
  }

  // ******************** rendering ********************

  private def updateMeshUniform( mesh: Mesh )( field: String, value: js.Any ): Unit = {
    mesh.material.asInstanceOf[ShaderMaterial].uniforms.asInstanceOf[scala.scalajs.js.Dynamic]
      .selectDynamic( field )
      .updateDynamic( "value" )( value )
  }

  @JSExport
  def renderNoTexture(): Unit = {
    renderToTexture( None )
  }

  @JSExport
  def render(): Unit = {
    renderToTexture( Some( renderingTexture ) )
    updateMeshUniform( screenMesh )( "texture", renderingTexture )
    renderer.render( rtScene, dummyCam )
  }

  private def renderToTexture( texture: Option[WebGLRenderTarget] ): Unit = {

    for( m <- backgroundMesh ) {
      val updateThis = updateMeshUniform( m ) _
      updateThis( "u_time", now )
    }

    if ( cfg.`Draw flake` ) {
      for( m <- flakeMesh ) {
        val updateThis = updateMeshUniform( m ) _
        updateThis( "u_time", now )
      }
    }

    renderer.clearColor()
    texture match {
      case Some( t ) => renderer.render( scene, camera, t )
      case None      => renderer.render( scene, camera )
    }
  }
}