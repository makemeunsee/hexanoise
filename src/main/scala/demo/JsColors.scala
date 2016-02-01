package demo

import scala.language.implicitConversions
import scala.util.Random

/**
 * Created by markus on 22/06/2015.
 */
object JsColors {
  def colorIntToJsString( color: Int) : String = String.format( "#%06X", new Integer( color ) )

  def jsStringToColor( color: String) : Int = Integer.parseInt( color.substring( 1 ), 16 )

  def intColorToFloatsColors( c: Int ): ( Float, Float, Float ) = {
    val r = ( ( c >> 16 ) & 0xff ).toFloat / 255f
    val g = ( ( c >> 8 ) & 0xff ).toFloat / 255f
    val b = ( c & 0xff ).toFloat / 255f
    ( r, g, b )
  }

  def jsStringToFloats = ( intColorToFloatsColors _ ).compose( jsStringToColor )
}
