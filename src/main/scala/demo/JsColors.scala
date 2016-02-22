package demo

import rendering.Color

import scala.language.implicitConversions

/**
 * Created by markus on 22/06/2015.
 */
object JsColors {
  def colorIntToJsString( color: Int ) : String = String.format( "#%06X", new Integer( color ) )

  def jsStringToColor( color: String ) : Int = Integer.parseInt( color.substring( 1 ), 16 )
  def jsStringToRgbaColor( color: String, alpha: Float ) : Int = 256 * Integer.parseInt( color.substring( 1 ), 16 ) + ( 255 * alpha ).toInt

  def jsStringToFloats = ( Color.intColorToFloatsColor _ ).compose( jsStringToColor )
}
