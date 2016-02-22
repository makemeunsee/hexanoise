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
        richConfig1.copy(
          `Color 1` = JsColors.colorIntToJsString( monoModule.color.baseColor.rgbInt ),
          `Alpha 1` = monoModule.color.baseColor.a,
          `Scale x 1` = monoModule.color.noiseScalingX * 50,
          `Scale y 1` = monoModule.color.noiseScalingY * 50,
          `Noise R 1` = monoModule.color.noiseCoeffs._1,
          `Noise G 1` = monoModule.color.noiseCoeffs._2,
          `Noise B 1` = monoModule.color.noiseCoeffs._3
        )
      case biModule: BicolorShaderModule[_] =>
        richConfig1.copy(
          `Color 1` = JsColors.colorIntToJsString( biModule.color0.baseColor.rgbInt ),
          `Alpha 1` = biModule.color0.baseColor.a,
          `Scale x 1` = biModule.color0.noiseScalingX * 50,
          `Scale y 1` = biModule.color0.noiseScalingY * 50,
          `Noise R 1` = biModule.color0.noiseCoeffs._1,
          `Noise G 1` = biModule.color0.noiseCoeffs._2,
          `Noise B 1` = biModule.color0.noiseCoeffs._3,
          `Color 2` = JsColors.colorIntToJsString( biModule.color1.baseColor.rgbInt ),
          `Alpha 2` = biModule.color1.baseColor.a,
          `Scale x 2` = biModule.color1.noiseScalingX * 50,
          `Scale y 2` = biModule.color1.noiseScalingY * 50,
          `Noise R 2` = biModule.color1.noiseCoeffs._1,
          `Noise G 2` = biModule.color1.noiseCoeffs._2,
          `Noise B 2` = biModule.color1.noiseCoeffs._3
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
  var `Highlighting`: String = "None",

  @(JSExport @field)
  var `Cubic`: Boolean = false,

  @(JSExport @field)
  var `Shade center`: Boolean = true,

  @(JSExport @field)
  var `Shader`: String = ShadersPack.HeadacheMachine2.name
) {

  private def toDynamicColor0 = DynamicColor(
      SimpleColor(JsColors.jsStringToRgbaColor( `Color 1`, `Alpha 1` )),
      `Scale x 1` / 50f,
      `Scale y 1` / 50f,
      (`Noise R 1`, `Noise G 1`, `Noise B 1`)
    )

  private def toDynamicColor1 = DynamicColor(
      SimpleColor(JsColors.jsStringToRgbaColor( `Color 2`, `Alpha 2` )),
      `Scale x 2` / 50f,
      `Scale y 2` / 50f,
      (`Noise R 2`, `Noise G 2`, `Noise B 2`)
    )

  private def toBorder =
    if (`Border size` == 0) NoFX
    else Border(
      SimpleColor( (`Border alpha`*255).toInt + 256 * JsColors.jsStringToColor( `Border color` ) ),
      `Border size`
    )

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
        centerShading = `Shade center`
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
  }

}