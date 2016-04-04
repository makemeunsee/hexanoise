package demo.webapp

import demo.JsColors
import rendering.{Colors, DynamicColor, SimpleColor}
import rendering.shaders._
import world2d.LivingHexagon

import scala.annotation.meta.field
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExport

/**
 * Created by markus on 10/07/15.
 */
object Config {

  val minSupportedTextureSize: Int = 2048

  val presets  = """{
                    |  "preset": "Default",
                    |  "closed": false,
                    |  "remembered": {
                    |    "Default": {
                    |      "0": {
                    |        "Background color": "#000000",
                    |        "Downsampling": 0
                    |      }
                    |    }
                    |  },
                    |  "folders": {
                    |    "General": {
                    |      "preset": "Default",
                    |      "closed": false,
                    |      "folders": {}
                    |    }
                    |  }
                    |}""".stripMargin

  val noColorMode: String = "NoFX"
  val colorMode3d: String = "Color3D"
  val colorModes: Seq[String] = Seq(noColorMode, colorMode3d)

  val noHighlighting = "None"
  val blending = "Blending"
  val pulsating = "Pulsating"
  val highlightings = Seq( pulsating, blending, noHighlighting )

  val sinus = "Sinus"
  val simplex2d = "Simplex2d"
  val simplex3d = "Simplex3d"
  val styles = Seq[String]( sinus, simplex2d, simplex3d )

  val defaultBackgroundColor: String = JsColors.colorIntToJsString( Colors.BLACK )
  val defaultName: String = "Funky trip"

  private def noiseFactorFromConfigNoiseFactor(coeff: Int): Float = math.signum(coeff) match {
    case -1 =>
      - math.pow( 4.5, ( -coeff - 2.5 ).toFloat / 4f ).toFloat / 10f
    case 0 =>
      0
    case 1 =>
      math.pow( 4.5, ( coeff - 2.5 ).toFloat / 4f ).toFloat / 10f
  }

  def updateConfigWithJson( config: Config, json: js.Dynamic ): Unit = {
    config.`Background color` = json.selectDynamic("Background color").asInstanceOf[String]
    config.`Border size` = json.selectDynamic("Border size").asInstanceOf[Float]
    config.`Border color` = json.selectDynamic("Border color").asInstanceOf[String]
    config.`Border alpha` = json.selectDynamic("Border alpha").asInstanceOf[Float]
    config.`Color` = json.selectDynamic("Color").asInstanceOf[String]
    config.`Alpha` = json.selectDynamic("Alpha").asInstanceOf[Float]
    config.`Scale x` = json.selectDynamic("Scale x").asInstanceOf[Int]
    config.`Scale y` = json.selectDynamic("Scale y").asInstanceOf[Int]
    config.`Noise R` = json.selectDynamic("Noise R").asInstanceOf[Int]
    config.`Noise G` = json.selectDynamic("Noise G").asInstanceOf[Int]
    config.`Noise B` = json.selectDynamic("Noise B").asInstanceOf[Int]
    config.`Highlighting` = json.selectDynamic("Highlighting").asInstanceOf[String]
    config.`Style` = json.selectDynamic("Style").asInstanceOf[String]
    config.`Hscale X` = json.selectDynamic("Hscale X").asInstanceOf[Int]
    config.`Hscale Y` = json.selectDynamic("Hscale Y").asInstanceOf[Int]
    config.`Rate` = json.selectDynamic("Rate").asInstanceOf[Float]
    config.`Amplitude` = json.selectDynamic("Amplitude").asInstanceOf[Float]
    config.`Shift` = json.selectDynamic("Shift").asInstanceOf[Float]
    config.`Cubic` = json.selectDynamic("Cubic").asInstanceOf[Boolean]
    config.`Shade center` = json.selectDynamic("Shade center").asInstanceOf[Boolean]
    config.`Name` = json.selectDynamic("Name").asInstanceOf[String]
    config.`Color mode` = json.selectDynamic("Color mode").asInstanceOf[String]
    config.`Color rate` = json.selectDynamic("Color rate").asInstanceOf[Float]
  }

  def fromJson( json: js.Dynamic ): Config = {
    val config = Config()
    updateConfigWithJson( config, json )
    config
  }
}

import Config.noiseFactorFromConfigNoiseFactor

@JSExport
case class Config (

  @(JSExport @field)
  var `Background color`: String = Config.defaultBackgroundColor,

  @(JSExport @field)
  var `Border size`: Float = 1.0f,

  @(JSExport @field)
  var `Border color`: String = JsColors.colorIntToJsString( Colors.GRAY ),

  @(JSExport @field)
  var `Border alpha`: Float = 1.0f,

  @(JSExport @field)
  var `Color`: String = JsColors.colorIntToJsString( Colors.WHITE ),

  @(JSExport @field)
  var `Alpha`: Float = 1.0f,

  @(JSExport @field)
  var `Scale x`: Int = 1,

  @(JSExport @field)
  var `Scale y`: Int = 1,

  @(JSExport @field)
  var `Noise R`: Int = 0,

  @(JSExport @field)
  var `Noise G`: Int = 0,

  @(JSExport @field)
  var `Noise B`: Int = 0,

  @(JSExport @field)
  var `Highlighting`: String = Config.noHighlighting,

  @(JSExport @field)
  var `Style`: String = Config.sinus,

  @(JSExport @field)
  var `Hscale X`: Int = 1,

  @(JSExport @field)
  var `Hscale Y`: Int = 1,

  @(JSExport @field)
  var `Rate`: Float = 1,

  @(JSExport @field)
  var `Amplitude`: Float = 0.5f,

  @(JSExport @field)
  var `Shift`: Float = 0.5f,

  @(JSExport @field)
  var `Cubic`: Boolean = false,

  @(JSExport @field)
  var `Color mode`: String = Config.noColorMode,

  @(JSExport @field)
  var `Color rate`: Float = 1f,

  @(JSExport @field)
  var `Shade center`: Boolean = true,

  @(JSExport @field)
  var `Name`: String = Config.defaultName
) {

  private def toDynamicColor = DynamicColor(
      SimpleColor(JsColors.jsStringToRgbaColor( `Color`, `Alpha` )),
      1f / `Scale x`,
      1f / `Scale y`,
      ( noiseFactorFromConfigNoiseFactor( `Noise R` ), noiseFactorFromConfigNoiseFactor( `Noise G` ), noiseFactorFromConfigNoiseFactor( `Noise B` ) )
    )

  private def toBorder =
    if (`Border size` == 0) NoFX
    else Border(
      SimpleColor( (`Border alpha`*255).toInt + 256 * JsColors.jsStringToColor( `Border color` ) ),
      `Border size`
    )

  private def toColorMode = `Color mode` match {
    case Config.colorMode3d =>
      Color3D(`Color rate`.toFloat / 10f)
    case _ =>
      NoFX
  }

  private def toAlphaFunction = `Style` match {
    case Config.sinus =>
      Sinus( `Rate` / 10f,
        `Amplitude`,
        -`Shift` )
    case Config.simplex2d =>
      SimplexNoise2D( 1f / `Hscale X`.toFloat,
        1f / `Hscale Y`.toFloat,
        `Rate` / 10f,
        `Amplitude`,
        -`Shift` )
    case Config.simplex3d =>
      SimplexNoise3D( 1f / `Hscale X`.toFloat,
        1f / `Hscale Y`.toFloat,
        `Rate` / 10f,
        `Amplitude`,
        -`Shift` )
  }

  private def toHighlighting = `Highlighting` match {
    case Config.blending =>
      Blending( toAlphaFunction )
    case Config.pulsating =>
      Pulsating( toAlphaFunction )
    case Config.noHighlighting =>
      NoFX
  }

  def toShader: ShaderModule[LivingHexagon] = BackgroundShader(
    `Name`,
    toDynamicColor,
    border = toBorder,
    cubic = `Cubic`,
    centerShading = `Shade center`,
    colorShading = toColorMode,
    highlighting = toHighlighting
  )

  def jsonMe: String = s"""{
  "Name": "${`Name`}",
  "Background color": "${`Background color`}",
  "Border size": ${`Border size`},
  "Border color": "${`Border color`}",
  "Border alpha": ${`Border alpha`},
  "Color": "${`Color`}",
  "Alpha": ${`Alpha`},
  "Scale x": ${`Scale x`},
  "Scale y": ${`Scale y`},
  "Noise R": ${`Noise R`},
  "Noise G": ${`Noise G`},
  "Noise B": ${`Noise B`},
  "Highlighting": "${`Highlighting`}",
  "Style": "${`Style`}",
  "Hscale X": ${`Hscale X`},
  "Hscale Y": ${`Hscale Y`},
  "Rate": ${`Rate`},
  "Amplitude": ${`Amplitude`},
  "Shift": ${`Shift`},
  "Cubic": ${`Cubic`},
  "Shade center": ${`Shade center`},
  "Color mode": "${`Color mode`}",
  "Color rate": ${`Color rate`}
}"""

}