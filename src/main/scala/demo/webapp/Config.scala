package demo.webapp

import demo.JsColors
import rendering.Colors
import rendering.shaders.ShadersPack
import world2d.FlakeGenerator

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
                    |        "Seed": "0",
                    |        "Background color": "#000000",
                    |        "Downsampling": 0,
                    |        "Draw flake": true,
                    |        "Borders width": 1
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
  var `Seed`: String = "0",

  @(JSExport @field)
  var `Background color`: String = JsColors.colorIntToJsString( Colors.BLACK ),

  // 0 <= downsampling <= 7
  @(JSExport @field) 
  var `Downsampling`: Int = 0,

  @(JSExport @field)
  var `Draw flake`: Boolean = true,

  @(JSExport @field)
  var `Flake size`: Int = FlakeGenerator.sizeMax,

  @(JSExport @field)
  var `Flake spacing`: Int = 2,

@(JSExport @field)
  var `Shader`: String = ShadersPack.LavaBasaltGradient.name
) {

  def safeDownsamplingFactor = math.pow( 2, math.max( 0, math.min( 7, `Downsampling` ) ) ).toInt

  def safeFlakeSpacing = math.max( 1, `Flake spacing` )

  def safeFlakeSize = math.max( 0, math.min( FlakeGenerator.sizeMax, `Flake size` ) )

  def updateSeed(seed: String): Unit = {
    if (`Seed` != seed) {
      `Seed` = seed
    }
  }

}