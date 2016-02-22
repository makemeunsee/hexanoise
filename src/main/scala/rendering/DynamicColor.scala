package rendering

case class DynamicColor(
  baseColor: Color,
  noiseScalingX: Float,
  noiseScalingY: Float,
  noiseCoeffs: (Float, Float, Float),
  offsetX: Float = 0,
  offsetY: Float = 0)
