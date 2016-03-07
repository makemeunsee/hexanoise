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

    val shaderName = args
      .get("shader")
      .filter( ShadersPack.values.map(_.name).toSet.contains )
      .getOrElse(Config.defaultShaderName)

    val bg = args
      .get("bg")
      .map("#"+_)
      .getOrElse(Config.defaultBackgroundColor)

    val config = Config.loadShader( ShadersPack( shaderName ), bg )
    config.`Shader` = shaderName

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
      .addList( jsCfg, "Shader", ( ShadersPack.values.map(_.name) :+ "customBi" :+ "customMono" ).toJSArray )
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

    def updateShaderFct[T]: T => Unit = _ => {
      println(config)
      scene.setShader( config.toShader )
    }

    commonFolder
      .addBoolean( jsCfg, "Cubic" )
      .onChange { updateShaderFct }

    commonFolder
      .addBoolean( jsCfg, "Shade center" )
      .onChange { updateShaderFct }

    commonFolder.open()

    borderFolder
      .addRange( jsCfg, "Border size", 0.0f, 3.5f ).step( 0.1f )
      .onChange { updateShaderFct }

    borderFolder
      .addColor( jsCfg, "Border color" )
      .onChange { updateShaderFct }

    borderFolder
      .addRange( jsCfg, "Border alpha", 0.0f, 1f ).step( 0.05f )
      .onChange { updateShaderFct }

    if( config.`Border size` > 0 ) {
      borderFolder.open()
    }

    colorFolder
      .addColor( jsCfg, "Color" )
      .onChange { updateShaderFct }

    colorFolder
      .addRange( jsCfg, "Alpha", 0.0f, 1f ).step( 0.05f )
      .onChange { updateShaderFct }

    colorFolder
      .addRange( jsCfg, "Scale x", 0, 7f ).step( 1 )
      .onChange { updateShaderFct }

    colorFolder
      .addRange( jsCfg, "Scale y", 0, 7 ).step( 1 )
      .onChange { updateShaderFct }

    colorFolder
      .addRange( jsCfg, "Noise R", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    colorFolder
      .addRange( jsCfg, "Noise G", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    colorFolder
      .addRange( jsCfg, "Noise B", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    colorFolder.open()

    colorModeFolder
      .addList( jsCfg, "Color mode", Config.colorModes.toJSArray )
      .onChange { updateShaderFct }

    colorModeFolder
      .addRange( jsCfg, "Color rate", 1, 30 ).step( 1 )
      .onChange { updateShaderFct }

    highlightingFolder
      .addList( jsCfg, "Highlighting", Config.higlightings.toJSArray )
      .onChange( updateShaderFct )

    highlightingSubFolder
      .addList( jsCfg, "Style", Config.styles.toJSArray )
      .onChange( updateShaderFct )

    highlightingSubFolder
      .addRange( jsCfg, "Hscale X", 1, 50f ).step( 1 )
      .onChange( updateShaderFct )

    highlightingSubFolder
      .addRange( jsCfg, "Hscale Y", 1, 50f ).step( 1 )
      .onChange( updateShaderFct )

    highlightingSubFolder
      .addRange( jsCfg, "Rate", 0.0f, 20f ).step( 0.5f )
      .onChange { updateShaderFct }

    highlightingSubFolder
      .addRange( jsCfg, "Amplitude", -10f, 10f ).step( 0.5f )
      .onChange { updateShaderFct }

    highlightingSubFolder
      .addRange( jsCfg, "Shift", 0.0f, 10f ).step( 0.5f )
      .onChange { updateShaderFct }

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
  def shareLink(): String = {
    var path = dom.location.pathname + "?"
    path += "bg=" + config.`Background color`.substring(1)
    path += "&shader=" + config.`Shader`
    path
  }
}
