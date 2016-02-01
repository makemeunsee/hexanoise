package demo.webapp

/**
 * Created by markus on 16/02/2015.
 */

import org.scalajs.dom
import datgui.DatGUI
import demo.JsColors
import models.DefaultGridModel
import rendering.shaders.ShadersPack
import world2d.HexaGrid.{Flake, FlakeGenerationStopper, NeighbourDistribution}
import world2d._

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSName, JSExport}
import scala.scalajs.js.{JSApp, JSON}
import scala.scalajs.js.timers.setTimeout
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.util.Try

@JSName("jQuery")
@js.native
object JQuery extends js.Object {
  def apply(x: String): js.Dynamic = js.native
}

object ScramblMain extends JSApp {

  def main(): Unit = {}

  @JSExport
  def init(): ScramblMain = {
    val search = dom.location.search.substring(1)
    val args = search
      .split("&")
      .map(_.split("="))
      .map(array => (array(0), array(1)))
      .toMap

    val config = new Config()

    config.`Background color` = args
      .get("bg")
      .map("#"+_)
      .getOrElse(config.`Background color`)

    config.`Downsampling` = args
      .get("downsampling")
      .flatMap( str => Try{ Integer.parseInt( str ) }.toOption )
      .getOrElse(config.`Downsampling`)

    config.`Draw flake` = args
      .get("flake")
      .map( _ == "true" )
      .getOrElse(config.`Draw flake`)

    config.`Flake size` = args
      .get("size")
      .flatMap( str => Try{ Integer.parseInt( str ) }.toOption )
      .getOrElse(config.safeFlakeSize)

    config.`Flake spacing` = args
      .get("spacing")
      .flatMap( str => Try{ Integer.parseInt( str ) }.toOption )
      .getOrElse(config.safeFlakeSpacing)

    config.`Seed` = args
      .getOrElse("seed", config.`Seed`)

    config.`Shader` = args
      .get("shader")
      .filter( ShadersPack.values.map(_.name).toSet.contains )
      .getOrElse(config.`Shader`)

    new ScramblMain(config, config.`Seed`)
  }
}

class ScramblMain(config: Config, val seed0: String) extends FlakeGenerator {

  private val datGUI = new DatGUI( js.Dynamic.literal( "load" -> JSON.parse( Config.presets ), "preset" -> "Default" ) )

  // ******************** actual three.js scene ********************

  @JSExport
  val scene = new ThreeScene( config )

  // ******************** stuff ********************

  def flakeSpacing = config.safeFlakeSpacing
  def distri: NeighbourDistribution = HexaGrid.unbiasedDistribution
  def stopper: FlakeGenerationStopper = (id: Int, hex: Hexagon) => id < FlakeGenerator.sizeMax / flakeSpacing // grow until size is reached

  private val gridModel = new DefaultGridModel

  private val worldHexas = gridModel.window(Point(-3200, -1800), Point(3200, 1800))._1

  // ******************** init code ********************

  def setupDatGUI( jsCfg: js.Dynamic ): Unit = {

    datGUI
      .addString( jsCfg, "Seed" )
      .onFinishChange { forceNext _ }
      .listen()

    import js.JSConverters._
    datGUI
      .addList( jsCfg, "Shader", ShadersPack.values.map(_.name).toJSArray )
      .onFinishChange { string: String =>
        val shaders = ShadersPack( string )
        scene.setShaders( shaders )
        scene.setBackground( worldHexas )
        scene.setFlake( flake )
      }

    datGUI
      .addColor( jsCfg, "Background color" )
      .onChange { str: String =>
        val color = JsColors.jsStringToFloats( str )
        ( scene.setBackgroundColor _ ).tupled(color)
      }

    datGUI
      .addRange( jsCfg, "Downsampling", 0, 7 ).step( 1 )
      .onChange { _: Float => scene.udpateDownsampling() }

    datGUI
      .addRange( jsCfg, "Flake spacing", 1, 5 ).step( 1 )
      .onChange { _: Float =>
        forceNext( config.`Seed` )
      }

    datGUI
      .addBoolean( jsCfg, "Draw flake" )
      .onChange { _: Boolean => scene.toggleFlake() }

    datGUI
      .addRange( jsCfg, "Flake size", 0, FlakeGenerator.sizeMax ).step( 1 )
      .onChange { _: Float => scene.updateFlake() }

    datGUI.open()
  }

  @JSExport
  def loadModel( uiToggler: js.Function1[Boolean, _]
               , frameHandler: js.Function1[js.Function0[_], _]
               , continuation: js.Function0[_]
               , initDatGUI: Boolean = true  ): Unit = {

    scene.setBackground( worldHexas )
    val jsCfg = config.asInstanceOf[js.Dynamic]
    setupDatGUI( jsCfg )
    uiToggler( true )
    JQuery( "#progressbar" ).hide()

    next()

    frameHandler( continuation )
  }

  @JSExport
  def shareLink(): String = {
    var path = dom.location.pathname + "?"
    path += "bg=" + config.`Background color`.substring(1)
    path += "&downsampling=" + config.`Downsampling`
    path += "&flake=" + config.`Draw flake`
    path += "&size=" + config.`Flake size`
    path += "&spacing=" + config.`Flake spacing`
    path += "&seed=" + config.`Seed`
    path += "&shader=" + config.`Shader`
    path
  }

  @JSExport
  override def next() = super.next()

  override protected def nextWithSeed(seed: Option[String]): Unit = {
    val wait = if ( flake.isEmpty ) 0 else LivingHexagon.agonyDuration
    val death = scene.now + wait
    scene.killFlake( () => death )
    setTimeout( Duration( wait, MILLISECONDS ) ) {
      super.nextWithSeed( seed )
    }
  }

  private var flake: Flake[LivingHexagon] = Seq.empty

  def onFlakeComplete(flake: Flake[LivingHexagon], flakeSeed: String): Unit = {
    println(s"showing flake of seed: $flakeSeed")
    config.updateSeed(flakeSeed)
    scene.setFlake( flake )
    this.flake = flake
  }
}
