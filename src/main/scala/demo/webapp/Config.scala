package demo.webapp

import demo.JsColors
import rendering.{SimpleColor, DynamicColor, Colors}
import rendering.shaders._
import world2d.LivingHexagon

import scala.annotation.meta.field
import scala.language.implicitConversions

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

  val higlightings = Seq("Pulsating", "Blending", "None")

  val defaultBackgroundColor: String = JsColors.colorIntToJsString( Colors.BLACK )
  val defaultShaderName: String = ShadersPack.HeadacheMachine2.name

  def loadShader(module: ShaderModule[_], bgColor: String, name: String = "custom" ): Config = {
    val baseConfig = Config().copy(
      `Blending rate` = module.blendingRate,
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

    val richConfig1 = module.highlighting match {
      case Pulsating(alphaFunction) =>
        richConfig0.copy( `Highlighting` = "Pulsating" )
      case Blending(alphaFunction) =>
        richConfig0.copy( `Highlighting` = "Blending" )
      case NoFX =>
        richConfig0.copy( `Highlighting` = "None" )
    }

    val richConfig2 = module match {
      case monoModule: MonocolorShaderModule[_] =>
        val richConfig3 = monoModule.colorShading match {
          case NoFX =>
            richConfig1.copy(`Color mode` = Config.noColorMode, `Rate` = 0)
          case Color3D(rate) =>
            richConfig1.copy(`Color mode` = Config.colorMode3d, `Rate` = (rate * 20).toInt)
        }
        richConfig3.copy(
          `Color 1` = JsColors.colorIntToJsString( monoModule.color.baseColor.rgbInt ),
          `Alpha 1` = monoModule.color.baseColor.a,
          `Scale x 1` = configScaleFromNoiseScale( monoModule.color.noiseScalingX),
          `Scale y 1` = configScaleFromNoiseScale( monoModule.color.noiseScalingY),
          `Noise R 1` = configNoiseFactorFromNoiseFactor( monoModule.color.noiseCoeffs._1 ),
          `Noise G 1` = configNoiseFactorFromNoiseFactor( monoModule.color.noiseCoeffs._2 ),
          `Noise B 1` = configNoiseFactorFromNoiseFactor( monoModule.color.noiseCoeffs._3 )
        )
      case biModule: BicolorShaderModule[_] =>
        richConfig1.copy(
          `Color 1` = JsColors.colorIntToJsString( biModule.color0.baseColor.rgbInt ),
          `Alpha 1` = biModule.color0.baseColor.a,
          `Scale x 1` = configScaleFromNoiseScale( biModule.color0.noiseScalingX),
          `Scale y 1` = configScaleFromNoiseScale( biModule.color0.noiseScalingY),
          `Noise R 1` = configNoiseFactorFromNoiseFactor( biModule.color0.noiseCoeffs._1 ),
          `Noise G 1` = configNoiseFactorFromNoiseFactor( biModule.color0.noiseCoeffs._2 ),
          `Noise B 1` = configNoiseFactorFromNoiseFactor( biModule.color0.noiseCoeffs._3 ),
          `Color 2` = JsColors.colorIntToJsString( biModule.color1.baseColor.rgbInt ),
          `Alpha 2` = biModule.color1.baseColor.a,
          `Scale x 2` = configScaleFromNoiseScale( biModule.color1.noiseScalingX),
          `Scale y 2` = configScaleFromNoiseScale( biModule.color1.noiseScalingY),
          `Noise R 2` = configNoiseFactorFromNoiseFactor( biModule.color1.noiseCoeffs._1 ),
          `Noise G 2` = configNoiseFactorFromNoiseFactor( biModule.color1.noiseCoeffs._2 ),
          `Noise B 2` = configNoiseFactorFromNoiseFactor( biModule.color1.noiseCoeffs._3 ),
          `Color mode` = Config.noColorMode,
          `Rate` = 0
        )
    }

    richConfig2
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

  // 0 < blending rate
  @(JSExport @field)
  var `Blending rate`: Float = 1f,

  @(JSExport @field)
  var `Border size`: Float = 1.0f,

  @(JSExport @field)
  var `Border color`: String = JsColors.colorIntToJsString( Colors.GRAY ),

  @(JSExport @field)
  var `Border alpha`: Float = 1.0f,

  @(JSExport @field)
  var `Color 1`: String = JsColors.colorIntToJsString( Colors.WHITE ),

  @(JSExport @field)
  var `Alpha 1`: Float = 1.0f,

  @(JSExport @field)
  var `Scale x 1`: Int = 0,

  @(JSExport @field)
  var `Scale y 1`: Int = 0,

  @(JSExport @field)
  var `Noise R 1`: Int = 0,

  @(JSExport @field)
  var `Noise G 1`: Int = 0,

  @(JSExport @field)
  var `Noise B 1`: Int = 0,

  @(JSExport @field)
  var `Color 2`: String = JsColors.colorIntToJsString( Colors.BLACK ),

  @(JSExport @field)
  var `Alpha 2`: Float = 1.0f,

  @(JSExport @field)
  var `Scale x 2`: Int = 0,

  @(JSExport @field)
  var `Scale y 2`: Int = 0,

  @(JSExport @field)
  var `Noise R 2`: Int = 0,

  @(JSExport @field)
  var `Noise G 2`: Int = 0,

  @(JSExport @field)
  var `Noise B 2`: Int = 0,

  @(JSExport @field)
  var `Highlighting`: String = "None",

  @(JSExport @field)
  var `Cubic`: Boolean = false,

  @(JSExport @field)
  var `Color mode`: String = Config.noColorMode,

  @(JSExport @field)
  var `Rate`: Int = 10,

  @(JSExport @field)
  var `Shade center`: Boolean = true,

  @(JSExport @field)
  var `Shader`: String = Config.defaultShaderName
) {

  private def toDynamicColor0 = DynamicColor(
      SimpleColor(JsColors.jsStringToRgbaColor( `Color 1`, `Alpha 1` )),
      noiseScaleFromConfigScale(`Scale x 1`),
      noiseScaleFromConfigScale(`Scale y 1`),
      ( noiseFactorFromConfigNoiseFactor( `Noise R 1` ), noiseFactorFromConfigNoiseFactor( `Noise G 1` ), noiseFactorFromConfigNoiseFactor( `Noise B 1` ) )
    )

  private def toDynamicColor1 = DynamicColor(
      SimpleColor(JsColors.jsStringToRgbaColor( `Color 2`, `Alpha 2` )),
      noiseScaleFromConfigScale(`Scale x 2`),
      noiseScaleFromConfigScale(`Scale y 2`),
      ( noiseFactorFromConfigNoiseFactor( `Noise R 2` ), noiseFactorFromConfigNoiseFactor( `Noise G 2` ), noiseFactorFromConfigNoiseFactor( `Noise B 2` ) )
    )

  private def toBorder =
    if (`Border size` == 0) NoFX
    else Border(
      SimpleColor( (`Border alpha`*255).toInt + 256 * JsColors.jsStringToColor( `Border color` ) ),
      `Border size`
    )

  private def toColorMode = `Color mode` match {
    case Config.colorMode3d =>
      Color3D(`Rate`.toFloat / 20f)
    case _ =>
      NoFX
  }

  def toShader: ShaderModule[LivingHexagon] = {
    if(`Blending rate` != 0) {
      BackgroundShaderBi(
        "customBi",
        toDynamicColor0,
        toDynamicColor1,
        border = toBorder,
        cubic = `Cubic`,
        blendingRate = `Blending rate`,
        centerShading = `Shade center`
      )
    } else {
      BackgroundShaderMono(
        "customMono",
        toDynamicColor0,
        border = toBorder,
        cubic = `Cubic`,
        centerShading = `Shade center`,
        colorShading = toColorMode
      )
    }
  }

  def apply( otherConfig: Config ): Unit = {
    `Background color` = otherConfig.`Background color`
    `Blending rate` = otherConfig.`Blending rate`
    `Border size` = otherConfig.`Border size`
    `Border color` = otherConfig.`Border color`
    `Border alpha` = otherConfig.`Border alpha`
    `Color 1` = otherConfig.`Color 1`
    `Alpha 1` = otherConfig.`Alpha 1`
    `Scale x 1` = otherConfig.`Scale x 1`
    `Scale y 1` = otherConfig.`Scale y 1`
    `Noise R 1` = otherConfig.`Noise R 1`
    `Noise G 1` = otherConfig.`Noise G 1`
    `Noise B 1` = otherConfig.`Noise B 1`
    `Color 2` = otherConfig.`Color 2`
    `Alpha 2` = otherConfig.`Alpha 2`
    `Scale x 2` = otherConfig.`Scale x 2`
    `Scale y 2` = otherConfig.`Scale y 2`
    `Noise R 2` = otherConfig.`Noise R 2`
    `Noise G 2` = otherConfig.`Noise G 2`
    `Noise B 2` = otherConfig.`Noise B 2`
    `Highlighting` = otherConfig.`Highlighting`
    `Cubic` = otherConfig.`Cubic`
    `Shade center` = otherConfig.`Shade center`
    `Shader` = otherConfig.`Shader`
    `Color mode` = otherConfig.`Color mode`
    `Rate` = otherConfig.`Rate`
  }

}