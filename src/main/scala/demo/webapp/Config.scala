package demo.webapp

import demo.JsColors
import rendering.Colors
import rendering.shaders.ShadersPack

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
}

@JSExport
case class Config (

  @(JSExport @field)
  var `Background color`: String = JsColors.colorIntToJsString( Colors.BLACK ),

  // 0 <= downsampling <= 7
  @(JSExport @field)
  var `Downsampling`: Int = 0,

  @(JSExport @field)
  var `Border size`: Float = 1.0f,

  @(JSExport @field)
  var `Cubic`: Boolean = false,

  @(JSExport @field)
  var `Shader`: String = ShadersPack.HeadacheMachine2.name
) {

  def safeDownsamplingFactor = math.pow( 2, math.max( 0, math.min( 7, `Downsampling` ) ) ).toInt
  def safeBorderSize = math.max( 0.0, math.min( 16.0, `Border size` ) ).toFloat

}