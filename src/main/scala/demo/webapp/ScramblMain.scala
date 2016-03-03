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

  // ******************** actual three.js scene ********************

  @JSExport
  val scene = new ThreeScene( config,
    screen.width.toInt,
    screen.height.toInt,
    maxHexagons )

  // ******************** init code ********************

  def setupDatGUI( jsCfg: js.Dynamic ): Unit = {

    import js.JSConverters._

    val commonFolder = datGUI.addFolder("...")
    val borderFolder = datGUI.addFolder("Border")
    val color1Folder = datGUI.addFolder("Color 1")
    val color2Folder = datGUI.addFolder("Color 2")

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

        if( config.`Blending rate` == 0 ) {
          color2Folder.close()
        } else {
          color2Folder.open()
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
      .addRange( jsCfg, "Blending rate", 0.0f, 10f ).step( 0.5f )
      .onChange { updateShaderFct }

    commonFolder
      .addBoolean( jsCfg, "Cubic" )
      .onChange { updateShaderFct }

    commonFolder
      .addBoolean( jsCfg, "Shade center" )
      .onChange { updateShaderFct }

    commonFolder.open()

    borderFolder
      .addRange( jsCfg, "Border size", 0.0f, 8f ).step( 0.2f )
      .onChange { updateShaderFct }

    borderFolder
      .addColor( jsCfg, "Border color" )
      .onChange { updateShaderFct }

    borderFolder
      .addRange( jsCfg, "Border alpha", 0.0f, 1f ).step( 0.05f )
      .onChange { updateShaderFct }

    borderFolder.open()

    color1Folder
      .addColor( jsCfg, "Color 1" )
      .onChange { updateShaderFct }

    color1Folder
      .addRange( jsCfg, "Alpha 1", 0.0f, 1f ).step( 0.05f )
      .onChange { updateShaderFct }

    color1Folder
      .addRange( jsCfg, "Scale x 1", 0, 7f ).step( 1 )
      .onChange { updateShaderFct }

    color1Folder
      .addRange( jsCfg, "Scale y 1", 0, 7 ).step( 1 )
      .onChange { updateShaderFct }

    color1Folder
      .addRange( jsCfg, "Noise R 1", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    color1Folder
      .addRange( jsCfg, "Noise G 1", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    color1Folder
      .addRange( jsCfg, "Noise B 1", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    color1Folder.open()

    color2Folder
      .addColor( jsCfg, "Color 2" )
      .onChange { updateShaderFct }

    color2Folder
      .addRange( jsCfg, "Alpha 2", 0.0f, 1f ).step( 0.05f )
      .onChange { updateShaderFct }

    color2Folder
      .addRange( jsCfg, "Scale x 2", 0, 7 ).step( 1 )
      .onChange { updateShaderFct }

    color2Folder
      .addRange( jsCfg, "Scale y 2", 0, 7 ).step( 1 )
      .onChange { updateShaderFct }

    color2Folder
      .addRange( jsCfg, "Noise R 2", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    color2Folder
      .addRange( jsCfg, "Noise G 2", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    color2Folder
      .addRange( jsCfg, "Noise B 2", -20, 20 ).step( 1 )
      .onChange { updateShaderFct }

    color2Folder.open()

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
