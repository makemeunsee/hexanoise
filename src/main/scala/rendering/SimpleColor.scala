package rendering

/**
 * Created by mgalilee on 08/01/2016.
 */
object SimpleColor {
  def apply(rgba: Int): Color = {
    val r: Float = ( ( rgba & 0xff000000 ) >>> 24 ) / 255f
    val g: Float = ( ( rgba & 0xff0000 ) >>> 16 ) / 255f
    val b: Float = ( ( rgba & 0xff00 ) >>> 8 ) / 255f
    val a: Float = ( rgba & 0xff ) / 255f
    PrivateSimpleColor(bounded(r),bounded(g),bounded(b),bounded(a))
  }
  
  def bounded(f: Float): Float = math.min(1.0, math.max(0.0, f)).toFloat
}

import rendering.SimpleColor.bounded

private case class PrivateSimpleColor(r: Float, g: Float, b: Float, a: Float) extends Color {

  private def fToHex(f: Float) = {
    val value = (255*f).toInt
    String.format("%02X", new Integer( value ) )
  }

  override def toString = s"#${fToHex(r)}${fToHex(g)}${fToHex(b)}${fToHex(a)}"
}