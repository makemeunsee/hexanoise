package demo.webapp

/**
 * Created by markus on 16/02/2015.
 */

import org.scalajs.dom
import org.scalajs.dom.screen
import datgui.DatGUI
import demo.JsColors
import rendering.shaders.ShadersPack

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSName, JSExport}
import scala.scalajs.js.{JSApp, JSON}
import scala.util.Try

@JSName("jQuery")
@js.native
object JQuery extends js.Object {
  def apply(x: String): js.Dynamic = js.native
}

object ScramblMain extends JSApp {

  def main(): Unit = {}

  private val DEFAULT_MAX_HEXAGONS = 65536

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

    config.`Shader` = args
      .get("shader")
      .filter( ShadersPack.values.map(_.name).toSet.contains )
      .getOrElse(config.`Shader`)

    val maxHexagons = args
      .get("maxhexas")
      .flatMap( str => Try{ Integer.parseInt( str ) }.toOption )
      .getOrElse(DEFAULT_MAX_HEXAGONS)

    new ScramblMain(config, maxHexagons)
  }
}

class ScramblMain(config: Config, maxHexagons:Int ) {

  private val datGUI = new DatGUI( js.Dynamic.literal( "load" -> JSON.parse( Config.presets ), "preset" -> "Default" ) )

  // ******************** actual three.js scene ********************

  @JSExport
  val scene = new ThreeScene( config,
    screen.width.toInt,
    screen.height.toInt,
    maxHexagons )

  // ******************** init code ********************

  def setupDatGUI( jsCfg: js.Dynamic ): Unit = {

    import js.JSConverters._
    datGUI
      .addList( jsCfg, "Shader", ShadersPack.values.map(_.name).toJSArray )
      .onFinishChange { string: String =>
        val shaders = ShadersPack( string )
        scene.setShaders( shaders )
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

    datGUI.open()
  }

  @JSExport
  def loadModel(): Unit = {
    val jsCfg = config.asInstanceOf[js.Dynamic]
    setupDatGUI( jsCfg )
    JQuery( "#progressbar" ).hide()
  }

  @JSExport
  def shareLink(): String = {
    var path = dom.location.pathname + "?"
    path += "bg=" + config.`Background color`.substring(1)
    path += "&downsampling=" + config.`Downsampling`
    path += "&shader=" + config.`Shader`
    path
  }
}
