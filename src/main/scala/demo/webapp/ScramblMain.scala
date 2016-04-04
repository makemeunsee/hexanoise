package demo.webapp

/**
 * Created by markus on 16/02/2015.
 */

import org.scalajs.dom
import org.scalajs.dom.screen
import datgui.DatGUI
import demo.JsColors

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

  private val DEFAULT_MAX_HEXAGONS = 0xffff

  @JSExport
  def init(): ScramblMain = {
    val search = dom.location.search.substring(1)
    val args = search
      .split("&")
      .map(_.split("="))
      .map(array => (array(0), array(1)))
      .toMap

    val configs = JSON.parse( Configs.examples )

    val maxHexagons = args
      .get( "maxhexas" )
      .flatMap( str => Try{ Integer.parseInt( str ) }.toOption )
      .getOrElse(DEFAULT_MAX_HEXAGONS)

    val devMode = args
      .get( "devmode" )
      .exists( _ => true )

    new ScramblMain( configs, maxHexagons, devMode )
  }
}

class ScramblMain( configs: js.Dynamic, maxHexagons:Int, devMode: Boolean ) {

  import js.JSConverters._

  private val datGUI = new DatGUI( js.Dynamic.literal( "load" -> JSON.parse( Config.presets ), "preset" -> "Default" ) )

  private val config = Config.fromJson( configs.selectDynamic( Config.defaultName ) )
  private val configNames = js.Object.getOwnPropertyNames( configs.asInstanceOf[js.Object] )

  // ******************** actual three.js scene ********************

  @JSExport
  val scene = new ThreeScene( config,
    screen.width.toInt,
    screen.height.toInt,
    maxHexagons )

  scene.setShader( config.toShader )

  // ******************** init code ********************

  private def simpleSetupDatGUI( jsCfg: js.Dynamic ): Unit = {
    datGUI
      .addList( jsCfg, "Name", configNames )
      .onFinishChange { string: String =>
        Config.updateConfigWithJson( config, configs.selectDynamic( string ) )
        scene.setShader( config.toShader )
        DatGUI.updateDisplay( datGUI )
      }

    datGUI.open()
  }

  private def setupDatGUI( jsCfg: js.Dynamic ): Unit = {

    val commonFolder = datGUI.addFolder("...")
    val borderFolder = datGUI.addFolder("Border")
    val colorFolder = datGUI.addFolder("Color")
    val colorModeFolder = datGUI.addFolder("Color mode")
    val highlightingFolder = datGUI.addFolder("Effect")
    val highlightingSubFolder = highlightingFolder.addFolder( "Effect params" )

    commonFolder
      .addList( jsCfg, "Name", configNames )
      .onFinishChange { string: String =>
        Config.updateConfigWithJson( config, configs.selectDynamic( string ) )
        scene.setShader( config.toShader )
        DatGUI.updateDisplay( datGUI )

        if( config.`Border size` == 0 ) {
          borderFolder.close()
        } else {
          borderFolder.open()
        }

        if( config.`Highlighting` == Config.noHighlighting ) {
          highlightingSubFolder.close()
        } else {
          highlightingSubFolder.open()
        }

        ()
      }

    commonFolder
      .addColor( jsCfg, "Background color" )
      .onChange { str: String =>
        val color = JsColors.jsStringToFloats( str )
        ( scene.setBackgroundColor _ ).tupled(color)
      }

    def shaderUpdateFunction[T]: T => Unit = _ => {
      scene.setShader( config.toShader )
    }

    commonFolder
      .addBoolean( jsCfg, "Cubic" )
      .onChange { shaderUpdateFunction }

    commonFolder
      .addBoolean( jsCfg, "Shade center" )
      .onChange { shaderUpdateFunction }

    commonFolder.open()

    borderFolder
      .addRange( jsCfg, "Border size", 0.0f, 3.5f, 0.1f )
      .onChange { shaderUpdateFunction }

    borderFolder
      .addColor( jsCfg, "Border color" )
      .onChange { shaderUpdateFunction }

    borderFolder
      .addRange( jsCfg, "Border alpha", 0.0f, 1f, 0.05f )
      .onChange { shaderUpdateFunction }

    if( config.`Border size` > 0 ) {
      borderFolder.open()
    }

    colorFolder
      .addColor( jsCfg, "Color" )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Alpha", 0.0f, 1f, 0.05f )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Scale x", 0, 7f, 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Scale y", 0, 7, 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Noise R", -20, 20, 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Noise G", -20, 20, 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Noise B", -20, 20, 1 )
      .onChange { shaderUpdateFunction }

    colorFolder.open()

    colorModeFolder
      .addList( jsCfg, "Color mode", Config.colorModes.toJSArray )
      .onChange { shaderUpdateFunction }

    colorModeFolder
      .addRange( jsCfg, "Color rate", 1, 30, 1 )
      .onChange { shaderUpdateFunction }

    highlightingFolder
      .addList( jsCfg, "Highlighting", Config.highlightings.toJSArray )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addList( jsCfg, "Style", Config.styles.toJSArray )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addRange( jsCfg, "Hscale X", 1, 50f, 1 )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addRange( jsCfg, "Hscale Y", 1, 50f, 1 )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addRange( jsCfg, "Rate", 0.0f, 20f, 0.5f )
      .onChange { shaderUpdateFunction }

    highlightingSubFolder
      .addRange( jsCfg, "Amplitude", -2f, 2f, 0.1f )
      .onChange { shaderUpdateFunction }

    highlightingSubFolder
      .addRange( jsCfg, "Shift", -2f, 2f, 0.1f )
      .onChange { shaderUpdateFunction }

    highlightingFolder.open()
    if( config.`Highlighting` != Config.noHighlighting ) {
      highlightingSubFolder.open()
    } else {
      highlightingSubFolder.close()
    }

    datGUI.open()
  }

  @JSExport
  def loadModel(): Unit = {
    if( devMode ) {
      setupDatGUI( config.asInstanceOf[js.Dynamic] )
    } else {
      simpleSetupDatGUI( config.asInstanceOf[js.Dynamic] )
    }
  }

  @JSExport
  def jsonConfig(): String = config.jsonMe

  @JSExport
  def loadJsonConfig( jsonConfig: String ): Unit = {
    Config.updateConfigWithJson( config, JSON.parse( jsonConfig ) )
    scene.setShader( config.toShader )
    DatGUI.updateDisplay( datGUI )
  }
}
