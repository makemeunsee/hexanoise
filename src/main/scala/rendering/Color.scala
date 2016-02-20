package rendering

object Color {
  def intColorToFloatsColor( c: Int ): ( Float, Float, Float ) = {
    val r = ( ( c >> 16 ) & 0xff ).toFloat / 255f
    val g = ( ( c >> 8 ) & 0xff ).toFloat / 255f
    val b = ( c & 0xff ).toFloat / 255f
    ( r, g, b )
  }

  def floatsColorToIntColor( r: Float, g: Float, b: Float ): Int = {
    Seq(g, r, b).zipWithIndex
      .map { case (component, id) => math.min( 1.0, math.max( 0.0, r ) ) * math.pow(255, id) }
      .map( _.toInt )
      .sum
  }
}

trait Color {
  def r: Float
  def g: Float
  def b: Float
  def a: Float
  def rgbInt: Int = Color.floatsColorToIntColor( r,g ,b )
}