package rendering

object Color {
  def intColorToFloatsColor( c: Int ): ( Float, Float, Float ) = {
    val r = ( ( c >> 16 ) & 0xff ).toFloat / 255f
    val g = ( ( c >> 8 ) & 0xff ).toFloat / 255f
    val b = ( c & 0xff ).toFloat / 255f
    ( r, g, b )
  }

  def floatsColorToIntColor( r: Float, g: Float, b: Float ): Int = {
    Seq( b, g, r )
      .map { math.min( 1.0, _ ) }
      .map { math.max( 0.0, _ ) }
      .map { 255 * _ }
      .map( _.toInt )
      .zipWithIndex
      .map { case ( component, id ) => component << ( 8 * id ) }
      .sum
  }
}

trait Color {
  def r: Float
  def g: Float
  def b: Float
  def a: Float
  def rgbInt: Int = Color.floatsColorToIntColor( r, g, b )
}