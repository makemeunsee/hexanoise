package world2d

object Point {
  def apply[T](x: T, y: T)(implicit num: Numeric[T]): Point = {
    import num._
    Point(x.toFloat(), y.toFloat())
  }
}

case class Point(x: Float, y: Float) {
  def dot(other: Point): Float = x * other.x + y * other.y
  def *(scalar: Float): Point = copy(x * scalar, y * scalar)
  def /(scalar: Float): Point = copy(x / scalar, y / scalar)
  def +(other: Point): Point = copy(x + other.x, y + other.y)
  def -(other: Point): Point = copy(x - other.x, y - other.y)
  def unary_- = copy(-x, -y)
}