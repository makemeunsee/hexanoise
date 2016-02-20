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

  val higlightings = Seq("Pulsating", "Blending", "None")

  def loadShader(module: ShaderModule[_], bgColor: String, name: String = "custom" ): Config = {
    val baseConfig = Config().copy(
      `Blending rate` = module.blendingRate,
      `Cubic` = module.cubic,
      `Shader` = name,
      `Background color`= bgColor
    )

    val richConfig0 = module.border match {
      case Border(color, thickness) =>
        baseConfig.copy(
          `Border size` = thickness,
          `Border color` = JsColors.colorIntToJsString( color.rgbInt ),
          `Border alpha` = 0f
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
        richConfig1.copy(
          `Bicolor` = false,
          `Color 1` = JsColors.colorIntToJsString( monoModule.color.baseColor.rgbInt ),
          `Alpha 1` = monoModule.color.baseColor.a,
          `Scale x 1` = monoModule.color.noiseScalingX,
          `Scale y 1` = monoModule.color.noiseScalingY,
          `Noise R 1` = monoModule.color.noiseCoeffs._1,
          `Noise G 1` = monoModule.color.noiseCoeffs._2,
          `Noise B 1` = monoModule.color.noiseCoeffs._3,
          `Shading R 1` = monoModule.color.shadingCoeffs._1,
          `Shading G 1` = monoModule.color.shadingCoeffs._2,
          `Shading B 1` = monoModule.color.shadingCoeffs._3
        )
      case biModule: BicolorShaderModule[_] =>
        richConfig1.copy(
          `Bicolor` = true,
          `Color 1` = JsColors.colorIntToJsString( biModule.color0.baseColor.rgbInt ),
          `Alpha 1` = biModule.color0.baseColor.a,
          `Scale x 1` = biModule.color0.noiseScalingX,
          `Scale y 1` = biModule.color0.noiseScalingY,
          `Noise R 1` = biModule.color0.noiseCoeffs._1,
          `Noise G 1` = biModule.color0.noiseCoeffs._2,
          `Noise B 1` = biModule.color0.noiseCoeffs._3,
          `Shading R 1` = biModule.color0.shadingCoeffs._1,
          `Shading G 1` = biModule.color0.shadingCoeffs._2,
          `Shading B 1` = biModule.color0.shadingCoeffs._3,
          `Color 2` = JsColors.colorIntToJsString( biModule.color1.baseColor.rgbInt ),
          `Alpha 2` = biModule.color1.baseColor.a,
          `Scale x 2` = biModule.color1.noiseScalingX,
          `Scale y 2` = biModule.color1.noiseScalingY,
          `Noise R 2` = biModule.color1.noiseCoeffs._1,
          `Noise G 2` = biModule.color1.noiseCoeffs._2,
          `Noise B 2` = biModule.color1.noiseCoeffs._3,
          `Shading R 2` = biModule.color1.shadingCoeffs._1,
          `Shading G 2` = biModule.color1.shadingCoeffs._2,
          `Shading B 2` = biModule.color1.shadingCoeffs._3
        )
    }

    richConfig2
  }
}

@JSExport
case class Config (

  @(JSExport @field)
  var `Background color`: String = JsColors.colorIntToJsString( Colors.BLACK ),

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
  var `Scale x 1`: Float = 0,

  @(JSExport @field)
  var `Scale y 1`: Float = 0,

  @(JSExport @field)
  var `Noise R 1`: Float = 0,

  @(JSExport @field)
  var `Noise G 1`: Float = 0,

  @(JSExport @field)
  var `Noise B 1`: Float = 0,

  @(JSExport @field)
  var `Shading R 1`: Float = 0,

  @(JSExport @field)
  var `Shading G 1`: Float = 0,

  @(JSExport @field)
  var `Shading B 1`: Float = 0,

  @(JSExport @field)
  var `Bicolor`: Boolean = true,

  @(JSExport @field)
  var `Color 2`: String = JsColors.colorIntToJsString( Colors.BLACK ),

  @(JSExport @field)
  var `Alpha 2`: Float = 1.0f,

  @(JSExport @field)
  var `Scale x 2`: Float = 0,

  @(JSExport @field)
  var `Scale y 2`: Float = 0,

  @(JSExport @field)
  var `Noise R 2`: Float = 0,

  @(JSExport @field)
  var `Noise G 2`: Float = 0,

  @(JSExport @field)
  var `Noise B 2`: Float = 0,

  @(JSExport @field)
  var `Shading R 2`: Float = 0,

  @(JSExport @field)
  var `Shading G 2`: Float = 0,

  @(JSExport @field)
  var `Shading B 2`: Float = 0,

  @(JSExport @field)
  var `Highlighting`: String = "None",

  @(JSExport @field)
  var `Cubic`: Boolean = false,

  @(JSExport @field)
  var `Shader`: String = ShadersPack.HeadacheMachine2.name
) {

  def safeBorderSize = math.max( 0.0, math.min( 16.0, `Border size` ) ).toFloat

  def toShader: ShaderModule[LivingHexagon] = {
    if(`Bicolor`) {
      BackgroundShaderBi(
        "customBi",
        DynamicColor(
          SimpleColor(JsColors.jsStringToRgbaColor( `Color 1` )),
          `Scale x 1`,
          `Scale y 1`,
          (`Noise R 1`, `Noise G 1`, `Noise B 1`),
          shadingCoeffs = (`Shading R 1`, `Shading B 1`, `Shading B 1`)
        ),
        DynamicColor(
          SimpleColor(JsColors.jsStringToRgbaColor( `Color 2` )),
          `Scale x 2`,
          `Scale y 2`,
          (`Noise R 2`, `Noise G 2`, `Noise B 2`),
          shadingCoeffs = (`Shading R 2`, `Shading B 2`, `Shading B 2`)
        ),
        border =
          if (`Border size` == 0) NoFX
          else Border(
            SimpleColor( (`Border alpha`*255).toInt + 256 * JsColors.jsStringToColor( `Border color` ) ),
            `Border size`
          ),
        cubic = `Cubic`,
        blendingRate = `Blending rate`
      )
    } else {
      BackgroundShaderMono(
        "customMono",
        DynamicColor(
          SimpleColor(JsColors.jsStringToRgbaColor( `Color 1` )),
          `Scale x 1`,
          `Scale y 1`,
          (`Noise R 1`, `Noise G 1`, `Noise B 1`),
          shadingCoeffs = (`Shading R 1`, `Shading B 1`, `Shading B 1`)
        ),
        border =
          if (`Border size` == 0) NoFX
          else Border(
            SimpleColor( (`Border alpha`*255).toInt + 256 * JsColors.jsStringToColor( `Border color` ) ),
            `Border size`
          ),
        cubic = `Cubic`,
        blendingRate = `Blending rate`
      )
    }
  }

}