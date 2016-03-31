package demo.webapp

import demo.JsColors
import rendering.{Colors, DynamicColor, SimpleColor}
import rendering.shaders._
import world2d.LivingHexagon

import scala.annotation.meta.field
import scala.language.implicitConversions
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
  val higlightings = Seq( pulsating, blending, noHighlighting )

  val sinus = "Sinus"
  val simplex2d = "Simplex2d"
  val simplex3d = "Simplex3d"
  val styles = Seq[String]( sinus, simplex2d, simplex3d )

  val defaultBackgroundColor: String = JsColors.colorIntToJsString( Colors.BLACK )
  val defaultShaderName: String = ShadersPack.LimeGradient2.name

  def loadShader(module: ShaderModule[_], bgColor: String, name: String ): Config = {
    val baseConfig = Config().copy(
      `Cubic` = module.cubic,
      `Shader` = name,
      `Background color`= bgColor,
      `Shade center` = module.centerShading
    )

    val richConfig0 = module.border match {
      case Border(color, thickness) =>
        baseConfig.copy(
          `Border size` = thickness,
          `Border color` = JsColors.colorIntToJsString( color.rgbInt ),
          `Border alpha` = color.a
        )
      case NoFX =>
        baseConfig.copy(
          `Border size` = 0f,
          `Border color` = JsColors.colorIntToJsString( Colors.BLACK ),
          `Border alpha` = 0f
        )
    }

    def applyStyle( alphaFunction: AlphaFunction, config: Config ): Config = alphaFunction match {
      case Sinus( rate, amplitude, shift ) =>
        config.copy( `Style` = Config.sinus,
          `Rate` = rate * 10f,
          `Amplitude` = amplitude,
          `Shift` = -shift )
      case SimplexNoise2D( xScale, yScale, rate, amplitude, shift ) =>
        config.copy( `Style` = Config.simplex2d,
          `Hscale X` = ( 1f / xScale ).toInt,
          `Hscale Y` = ( 1f / yScale ).toInt,
          `Rate` = rate * 10f,
          `Amplitude` = amplitude,
          `Shift` = -shift )
      case SimplexNoise3D( xScale, yScale, rate, amplitude, shift ) =>
        config.copy(  `Style` = Config.simplex3d,
          `Hscale X` = ( 1f / xScale ).toInt,
          `Hscale Y` = ( 1f / yScale ).toInt,
          `Rate` = rate * 10f,
          `Amplitude` = amplitude,
          `Shift` = -shift )
    }

    val richConfig1 = module.highlighting match {
      case Pulsating(alphaFunction) =>
        applyStyle( alphaFunction, richConfig0.copy( `Highlighting` = pulsating ) )
      case Blending(alphaFunction) =>
        applyStyle( alphaFunction, richConfig0.copy( `Highlighting` = blending ) )
      case NoFX =>
        richConfig0.copy( `Highlighting` = noHighlighting )
    }

    val richConfig2 = module.colorShading match {
      case NoFX =>
        richConfig1.copy(`Color mode` = Config.noColorMode, `Color rate` = 0)
      case Color3D(rate) =>
        richConfig1.copy(`Color mode` = Config.colorMode3d, `Color rate` = (rate * 20).toInt)
    }

    val richConfig3 = richConfig2.copy(
      `Color` = JsColors.colorIntToJsString( module.color.baseColor.rgbInt ),
      `Alpha` = module.color.baseColor.a,
      `Scale x` = configScaleFromNoiseScale( module.color.noiseScalingX),
      `Scale y` = configScaleFromNoiseScale( module.color.noiseScalingY),
      `Noise R` = configNoiseFactorFromNoiseFactor( module.color.noiseCoeffs._1 ),
      `Noise G` = configNoiseFactorFromNoiseFactor( module.color.noiseCoeffs._2 ),
      `Noise B` = configNoiseFactorFromNoiseFactor( module.color.noiseCoeffs._3 )
    )

    richConfig3
  }

  private def noiseScaleFromConfigScale(coeff: Int): Float = {
    math.pow(2, coeff).toFloat / 100f
  }

  private def configScaleFromNoiseScale(coeff: Float): Int = {
    ( math.log(coeff * 100) / math.log(2) ).toInt
  }

  private def noiseFactorFromConfigNoiseFactor(coeff: Int): Float = math.signum(coeff) match {
    case -1 =>
      - math.pow( 4.5, ( -coeff - 2.5 ).toFloat / 4f ).toFloat / 10f
    case 0 =>
      0
    case 1 =>
      math.pow( 4.5, ( coeff - 2.5 ).toFloat / 4f ).toFloat / 10f
  }

  private def configNoiseFactorFromNoiseFactor(coeff: Float): Int = math.signum(coeff) match {
    case -1 =>
      - (4 * (math.log(10 * -coeff) / math.log(4.5)) + 2.5).toInt
    case 0 =>
      0
    case 1 =>
      (4 * (math.log(10 * coeff) / math.log(4.5)) + 2.5).toInt
  }
}

import Config.{noiseFactorFromConfigNoiseFactor, noiseScaleFromConfigScale}

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
  var `Scale x`: Int = 0,

  @(JSExport @field)
  var `Scale y`: Int = 0,

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
  var `Hscale X`: Int = 0,

  @(JSExport @field)
  var `Hscale Y`: Int = 0,

  @(JSExport @field)
  var `Rate`: Float = 0,

  @(JSExport @field)
  var `Amplitude`: Float = 0,

  @(JSExport @field)
  var `Shift`: Float = 0,

  @(JSExport @field)
  var `Cubic`: Boolean = false,

  @(JSExport @field)
  var `Color mode`: String = Config.noColorMode,

  @(JSExport @field)
  var `Color rate`: Float = 0f,

  @(JSExport @field)
  var `Shade center`: Boolean = true,

  @(JSExport @field)
  var `Shader`: String = Config.defaultShaderName
) {

  private def toDynamicColor = DynamicColor(
      SimpleColor(JsColors.jsStringToRgbaColor( `Color`, `Alpha` )),
      noiseScaleFromConfigScale(`Scale x`),
      noiseScaleFromConfigScale(`Scale y`),
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
      Color3D(`Color rate`.toFloat / 20f)
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
    "custom",
    toDynamicColor,
    border = toBorder,
    cubic = `Cubic`,
    centerShading = `Shade center`,
    colorShading = toColorMode,
    highlighting = toHighlighting
  )

  def apply( otherConfig: Config ): Unit = {
    `Background color` = otherConfig.`Background color`
    `Border size` = otherConfig.`Border size`
    `Border color` = otherConfig.`Border color`
    `Border alpha` = otherConfig.`Border alpha`
    `Color` = otherConfig.`Color`
    `Alpha` = otherConfig.`Alpha`
    `Scale x` = otherConfig.`Scale x`
    `Scale y` = otherConfig.`Scale y`
    `Noise R` = otherConfig.`Noise R`
    `Noise G` = otherConfig.`Noise G`
    `Noise B` = otherConfig.`Noise B`
    `Highlighting` = otherConfig.`Highlighting`
    `Style` = otherConfig.`Style`
    `Hscale X` = otherConfig.`Hscale X`
    `Hscale Y` = otherConfig.`Hscale Y`
    `Rate` = otherConfig.`Rate`
    `Amplitude` = otherConfig.`Amplitude`
    `Shift` = otherConfig.`Shift`
    `Cubic` = otherConfig.`Cubic`
    `Shade center` = otherConfig.`Shade center`
    `Shader` = otherConfig.`Shader`
    `Color mode` = otherConfig.`Color mode`
    `Color rate` = otherConfig.`Color rate`
  }

  def jsonMe: String = s"""{
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
  "Shader": "${`Shader`}",
  "Color mode": "${`Color mode`}",
  "Color rate": ${`Color rate`}
}"""

  def applyJson(json: String): Unit = {
    println(JSON.parse(json))
  }

}