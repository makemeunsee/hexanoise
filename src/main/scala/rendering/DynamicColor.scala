package rendering

import scala.util.Random

case class DynamicColor(
  baseColor: Color,
  noiseScalingX: Float,
  noiseScalingY: Float,
  noiseCoeffs: (Float, Float, Float),
  offsetX: Float = Random.nextInt(500),
  offsetY: Float = Random.nextInt(500),
  shadingCoeffs: (Float, Float, Float) = (-0.1f, -0.1f, -0.1f))
