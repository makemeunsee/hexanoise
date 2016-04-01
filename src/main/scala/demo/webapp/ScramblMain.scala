package demo.webapp

/**
 * Created by markus on 16/02/2015.
 */

import org.scalajs.dom
import org.scalajs.dom.screen
import datgui.{DatController, DatGUI}
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

    val defaultConfig = Config.loadShader( ShadersPack( Config.defaultShaderName ), Config.defaultBackgroundColor, Config.defaultShaderName )
    val config = args
      .get( "config" )
      .map( jsonCfg => Try( JSON.parse(jsonCfg).asInstanceOf[Config] ) )
      .flatMap( _.toOption )
      .getOrElse( defaultConfig )

    val maxHexagons = args
      .get("maxhexas")
      .flatMap( str => Try{ Integer.parseInt( str ) }.toOption )
      .getOrElse(DEFAULT_MAX_HEXAGONS)

    new ScramblMain(config, maxHexagons)
  }
}

class ScramblMain(config: Config, maxHexagons:Int ) {

  private val datGUI = new DatGUI( js.Dynamic.literal( "load" -> JSON.parse( Config.presets ), "preset" -> "Default" ) )

  private val commonFolder = datGUI.addFolder("...")
  private val borderFolder = datGUI.addFolder("Border")
  private val colorFolder = datGUI.addFolder("Color")
  private val colorModeFolder = datGUI.addFolder("Color mode")
  private val highlightingFolder = datGUI.addFolder("Effect")
  private val highlightingSubFolder = highlightingFolder.addFolder( "Effect params" )


  private var folders: Map[DatGUI, Seq[DatController[_]]] = Map.empty.withDefaultValue( Seq.empty )

  // ******************** actual three.js scene ********************

  @JSExport
  val scene = new ThreeScene( config,
    screen.width.toInt,
    screen.height.toInt,
    maxHexagons )

  // ******************** init code ********************

  def setupDatGUI( jsCfg: js.Dynamic ): Unit = {

    import js.JSConverters._

    commonFolder
      .addList( jsCfg, "Shader", ShadersPack.values.map( _.name).toJSArray )
      .onFinishChange { string: String =>
        val shader = ShadersPack( string )
        scene.setShader( shader )

        val upToDateConfig = Config.loadShader( shader, config.`Background color`, string )
        config.apply( upToDateConfig )
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
      .addRange( jsCfg, "Border size", 0.0f, 3.5f ).step( 0.1f )
      .onChange { shaderUpdateFunction }

    borderFolder
      .addColor( jsCfg, "Border color" )
      .onChange { shaderUpdateFunction }

    borderFolder
      .addRange( jsCfg, "Border alpha", 0.0f, 1f ).step( 0.05f )
      .onChange { shaderUpdateFunction }

    if( config.`Border size` > 0 ) {
      borderFolder.open()
    }

    colorFolder
      .addColor( jsCfg, "Color" )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Alpha", 0.0f, 1f ).step( 0.05f )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Scale x", 0, 7f ).step( 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Scale y", 0, 7 ).step( 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Noise R", -20, 20 ).step( 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Noise G", -20, 20 ).step( 1 )
      .onChange { shaderUpdateFunction }

    colorFolder
      .addRange( jsCfg, "Noise B", -20, 20 ).step( 1 )
      .onChange { shaderUpdateFunction }

    colorFolder.open()

    colorModeFolder
      .addList( jsCfg, "Color mode", Config.colorModes.toJSArray )
      .onChange { shaderUpdateFunction }

    colorModeFolder
      .addRange( jsCfg, "Color rate", 1, 30 ).step( 1 )
      .onChange { shaderUpdateFunction }

    highlightingFolder
      .addList( jsCfg, "Highlighting", Config.higlightings.toJSArray )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addList( jsCfg, "Style", Config.styles.toJSArray )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addRange( jsCfg, "Hscale X", 1, 50f ).step( 1 )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addRange( jsCfg, "Hscale Y", 1, 50f ).step( 1 )
      .onChange( shaderUpdateFunction )

    highlightingSubFolder
      .addRange( jsCfg, "Rate", 0.0f, 20f ).step( 0.5f )
      .onChange { shaderUpdateFunction }

    highlightingSubFolder
      .addRange( jsCfg, "Amplitude", -2f, 2f ).step( 0.1f )
      .onChange { shaderUpdateFunction }

    highlightingSubFolder
      .addRange( jsCfg, "Shift", -2f, 2f ).step( 0.1f )
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
    setupDatGUI( config.asInstanceOf[js.Dynamic] )
  }

  @JSExport
  def jsonConfig(): String = s"<br><br><br>${config.jsonMe.replaceAll("\\n", "<br>")}"

  @JSExport
  def loadJsonConfig(jsonConfig: String): Unit = {
    config.applyJson(jsonConfig)
    scene.setShader( config.toShader )
    DatGUI.updateDisplay( datGUI )
  }
}
