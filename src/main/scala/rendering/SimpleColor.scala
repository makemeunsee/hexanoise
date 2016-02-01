package rendering

/**
 * Created by mgalilee on 08/01/2016.
 */
object SimpleColor {
  def apply(rgba: Int): SimpleColor = {
    val r: Float = ( ( rgba & 0xff000000 ) >>> 24 ) / 255f
    val g: Float = ( ( rgba & 0xff0000 ) >>> 16 ) / 255f
    val b: Float = ( ( rgba & 0xff00 ) >>> 8 ) / 255f
    val a: Float = ( rgba & 0xff ) / 255f
    new SimpleColor(r,g,b,a)
  }
  
  def bounded(f: Float): Float = math.min(1.0, math.max(0.0, f)).toFloat
}

import rendering.SimpleColor.bounded

class SimpleColor(r0: Float, g0: Float, b0: Float, a0: Float) extends Color {

  val r = bounded(r0)
  val g = bounded(g0)
  val b = bounded(b0)
  val a = bounded(a0)

  private def fToHex(f: Float) = {
    val value = (255*f).toInt
    String.format("%02X", new Integer( value ) )
  }

  override def toString = s"#${fToHex(r)}${fToHex(g)}${fToHex(b)}${fToHex(a)}"
}