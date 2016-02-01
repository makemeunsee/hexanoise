package rendering.shaders

import org.denigma.threejs._
import rendering.shaders.MeshMaker._
import rendering.shaders.Shader._
import threejs._
import world2d.{LivingHexagon, Hexagon}
import rendering.Color
import rendering.DynamicColor

import scala.scalajs.js
import scala.scalajs.js.typedarray.{Uint32Array, Float32Array}

sealed trait BorderMode
case class Border(color: Color, thickness: Float = 0.8f) extends BorderMode

sealed trait SproutMode
case object Shrinking extends SproutMode
case object Fading extends SproutMode
case class  Wither(shift: Float = 1f) extends SproutMode

sealed trait HighlightMode
case class Pulsating(rate: Float = 1, amplitude: Float = 0.25f, shift: Float = 0f) extends HighlightMode
case class Blending(rate: Float = 1, amplitude: Float = 0.25f, shift: Float = 0f) extends HighlightMode
case class Shaking(rate: Float = 10, amplitude: (Float, Float) = (2f, 1f)) extends HighlightMode
case class PulsatingShaking(rateS: Float = 10, amplitudeS: (Float, Float) = (2f, 1f),
                            rateP: Float = 1, amplitudeP: Float = 0.25f, shiftP: Float = 0f) extends HighlightMode
case class Squeezing(restDuration: Float = 4.5f, squeezeDuration: Float = 0.1f, amplitude: Float = 0.2f) extends HighlightMode
case class ColorCycling(period: Float) extends HighlightMode
case class SimplexNoise(xScale: Float = 1, yScale: Float = 1, rate: Float = 1, amplitude: Float = 0.25f, shift: Float = 0f) extends HighlightMode
case object Dead extends HighlightMode

sealed trait ColorShadingMode
case object Bicolor extends ColorShadingMode
case class Color3D(rate: Float = 1) extends ColorShadingMode

case object NoFX extends HighlightMode with BorderMode with SproutMode with ColorShadingMode

trait ShaderModule[H <: Hexagon] extends Shader with MeshMaker[H] {
  
  def color0: DynamicColor
  def color1: DynamicColor

  def birthFunction: ( H, Int ) => Float
  def deathFunction: ( H, Int ) => Float
  
  // TODO: split shader and mesh creation?
  def makeMesh( hexas: Seq[_ <: H],
    birthOffset: Float = 0,
    limit: Option[Int] = None ): Mesh = {

    val aliveLimit = limit.getOrElse( hexas.length )

    val customUniforms = js.Dynamic.literal(
      "u_time" -> js.Dynamic.literal(
        "type" -> "1f",
        "value" -> new js.Array[Float]
      ),
      "u_color0" -> js.Dynamic.literal(
        "type" -> "v4",
        "value" -> new js.Array[Float]
      ),
      "u_noise0" -> js.Dynamic.literal(
        "type" -> "v3",
        "value" -> new js.Array[Float]
      ),
      "u_shading0" -> js.Dynamic.literal(
        "type" -> "v3",
        "value" -> new js.Array[Float]
      ),
      "u_offsets0" -> js.Dynamic.literal(
        "type" -> "v2",
        "value" -> new js.Array[Float]
      ),
      "u_scaling0" -> js.Dynamic.literal(
        "type" -> "v2",
        "value" -> new js.Array[Float]
      ),
      "u_color1" -> js.Dynamic.literal(
        "type" -> "v4",
        "value" -> new js.Array[Float]
      ),
      "u_noise1" -> js.Dynamic.literal(
        "type" -> "v3",
        "value" -> new js.Array[Float]
      ),
      "u_shading1" -> js.Dynamic.literal(
        "type" -> "v3",
        "value" -> new js.Array[Float]
      ),
      "u_offsets1" -> js.Dynamic.literal(
        "type" -> "v2",
        "value" -> new js.Array[Float]
      ),
      "u_scaling1" -> js.Dynamic.literal(
        "type" -> "v2",
        "value" -> new js.Array[Float]
      )
    )

    val geom = new MyBufferGeometry()

    val attrs = js.Dynamic.literal(
      "a_position" -> js.Dynamic.literal(
        "type" -> "v2",
        "value" -> new js.Array[Float]
      ),
      "a_center" -> js.Dynamic.literal(
        "type" -> "v2",
        "value" -> new js.Array[Float]
      ),
      "a_centerFlag" -> js.Dynamic.literal(
        "type" -> "f",
        "value" -> new js.Array[Float]
      ),
      "a_tier" -> js.Dynamic.literal(
        "type" -> "f",
        "value" -> new js.Array[Float]
      ),
      "a_birthtime" -> js.Dynamic.literal(
        "type" -> "f",
        "value" -> new js.Array[Float]
      ),
      "a_deathtime" -> js.Dynamic.literal(
        "type" -> "f",
        "value" -> new js.Array[Float]
      )
    )

    val shaderMaterial = new ShaderMaterial
    shaderMaterial.attributes = attrs
    shaderMaterial.uniforms = customUniforms
    shaderMaterial.vertexShader = vertexShader
    shaderMaterial.fragmentShader = fragmentShader
    shaderMaterial.transparent = true

    // 0 = cullback
    // 1 = cullfront
    // 2 = cullnone
    shaderMaterial.asInstanceOf[js.Dynamic].updateDynamic( "side" )( 0 )

    val count = hexas.size * verticePerHexa

    val vertices = new Float32Array( count * 2 )
    val centers = new Float32Array( count * 2 )
    val centerFlags = new Float32Array( count )
    val tiers = new Float32Array( count )
    val birthtimes = new Float32Array( count )
    val deathtimes = new Float32Array( count )
    val indices = new Uint32Array( count * indicePerHexa )
    var offset = 0
    var indicesOffset = 0

    for ( ( hexa, id ) <- hexas.zipWithIndex ) {

      // times
      val ( birthTime, deathTime ) =
        if ( id > aliveLimit )
          ( -1000f, -1000f )
        else
          ( birthFunction(hexa, id) + birthOffset, deathFunction(hexa, id) )

      val doubleOffset = offset*2

      // hexa center

      // position
      vertices.set( doubleOffset, hexa.center.x )
      vertices.set( doubleOffset + 1, hexa.center.y )
      // center position
      centers.set( doubleOffset, hexa.center.x )
      centers.set( doubleOffset + 1, hexa.center.y )
      // tier
      tiers.set( offset, 0 )
      // is center flag
      centerFlags.set( offset, 1 )
      // birth time
      birthtimes.set( offset, birthTime )
      // death time
      deathtimes.set( offset, deathTime )

      val centerOffset = offset
      offset = offset + 1

      // hexa points

      for( i <- 0 until 6 ; point = hexa.points(i) ) {
        val doubleOffset = offset*2

        // position
        vertices.set( doubleOffset, point.x )
        vertices.set( doubleOffset + 1, point.y )
        // center position
        centers.set( doubleOffset, hexa.center.x )
        centers.set( doubleOffset + 1, hexa.center.y )
        // tier
        tiers.set( offset, if (i==0) -1 else if (i==2) 1 else 0 )
        // is center flag
        centerFlags.set( offset, 0 )
        // birth time
        birthtimes.set( offset, birthTime )
        // death time
        deathtimes.set( offset, deathTime )

        indices.set( indicesOffset, centerOffset )
        indices.set( indicesOffset + 1, if ( offset == centerOffset + 6 ) centerOffset + 1 else offset +1 )
        indices.set( indicesOffset + 2, offset )

        offset = offset + 1

        indicesOffset = indicesOffset + 3
      }
    }

    geom.setIndex( new BufferAttribute( indices, 1 ) )
    geom.addAttribute( "a_position", new BufferAttribute( vertices, 2 ) )
    geom.addAttribute( "a_center", new BufferAttribute( centers, 2 ) )
    geom.addAttribute( "a_tier", new BufferAttribute( tiers, 1 ) )
    geom.addAttribute( "a_centerFlag", new BufferAttribute( centerFlags, 1 ) )
    geom.addAttribute( "a_birthtime", new BufferAttribute( birthtimes, 1 ) )
    geom.addAttribute( "a_deathtime", new BufferAttribute( deathtimes, 1 ) )

    assembleMesh( geom, shaderMaterial, "baseMesh" )
  }
  
  def loadUniforms(loadUniform: (String, js.Any) => Unit ): Unit = {

    loadUniform( "u_color0", new Vector4(
      color0.baseColor.r,
      color0.baseColor.g,
      color0.baseColor.b,
      color0.baseColor.a
    ) )
    val noise0 = color0.noiseCoeffs
    loadUniform( "u_noise0", new Vector3(
      noise0._1,
      noise0._2,
      noise0._3
    ) )
    val shading0 = color0.shadingCoeffs
    loadUniform( "u_shading0", new Vector3(
      shading0._1,
      shading0._2,
      shading0._3
    ) )
    loadUniform( "u_offsets0", new Vector2(
      color0.offsetX,
      color0.offsetY
    ) )
    loadUniform( "u_scaling0", new Vector2(
      color0.noiseScalingX,
      color0.noiseScalingY
    ) )

    loadUniform( "u_color1", new Vector4(
      color1.baseColor.r,
      color1.baseColor.g,
      color1.baseColor.b,
      color1.baseColor.a
    ) )
    val noise1 = color1.noiseCoeffs
    loadUniform( "u_noise1", new Vector3(
      noise1._1,
      noise1._2,
      noise1._3
    ) )
    val shading1 = color1.shadingCoeffs
    loadUniform( "u_shading1", new Vector3(
      shading1._1,
      shading1._2,
      shading1._3
    ) )
    loadUniform( "u_offsets1", new Vector2(
      color1.offsetX,
      color1.offsetY
    ) )
    loadUniform( "u_scaling1", new Vector2(
      color1.noiseScalingX,
      color1.noiseScalingY
    ) )
  }

  // shaders related code

  def hexagonWidth: Float
  
  def blendingRate: Float
  def border: BorderMode
  def sprouting: SproutMode
  def highlighting: HighlightMode
  def colorShading: ColorShadingMode
  def cubic: Boolean

  private def alphaCompute =
    s"""|  float timeLive = u_time - a_birthtime;
        |  float timeRemaining = a_deathtime - u_time;
        |  float alpha = 0.0;
        |  if ( u_time > a_birthtime && u_time < a_deathtime ) {
        |    if ( timeRemaining < $transitionTime && timeLive < $transitionTime ) {
        |      alpha = min(timeRemaining / $transitionTime, timeLive / $transitionTime);
        |    } else if ( timeRemaining < $transitionTime ) {
        |      alpha = timeRemaining / $transitionTime;
        |    } else if ( timeLive < $transitionTime ) {
        |      alpha = timeLive / $transitionTime;
        |    } else {
        |      alpha = 1.0;
        |    }
        |  }""".stripMargin

  private val twoPi = 2*math.Pi
  private val twoPiBy1000 = twoPi / 1000f

  private def highlight = highlighting match {
    case Dead =>
      s"""|  v_color = vec4(1.0, 1.0, 1.0, v_color.a + v_color.a) - v_color;
          |  position = position - 0.5 * ( position - a_center );""".stripMargin
    case Pulsating(rate, amplitude, shift) =>
      s"""|  float hAlpha = sin((u_time - a_birthtime) * $rate * $twoPiBy1000);
          |  position = position + (hAlpha * $amplitude + $shift) * (position - a_center);""".stripMargin
    case Shaking(rate, (amplitudeX, amplitudeY)) =>
      s"""|  float alphaShake = sin((u_time - a_birthtime) * $rate * $twoPiBy1000);
          |  vec2 amplitude = vec2($amplitudeX, $amplitudeY);
          |  position = position + alphaShake * amplitude;""".stripMargin
    case PulsatingShaking(rateS, (amplitudeX, amplitudeY), rateP, amplitudeP, shiftP) =>
      s"""|  float alphaShake = sin((u_time - a_birthtime) * $rateS * $twoPiBy1000);
          |  float alphaPulse = sin((u_time - a_birthtime) * $rateP * $twoPiBy1000);
          |  vec2 amplitude = vec2($amplitudeX, $amplitudeY);
          |  position = position + alphaShake * amplitude + (alphaPulse * $amplitudeP + $shiftP) * (position - a_center);""".stripMargin
    case Blending(rate, amplitude, shift) =>
      s"""|  float hAlpha = sin((u_time - a_birthtime) * $rate * $twoPiBy1000);
          |  v_color.a = (0.5 + $shift + hAlpha * $amplitude) * v_color.a;""".stripMargin
    case Squeezing(restD, squeezeD, amplitude) =>
      val period = restD + squeezeD
      s"""|  float life = (u_time - a_birthtime) / 1000.0;
          |  float state = life - $period * floor(life / $period);
          |  if (state > $restD) {
          |    position = position - $amplitude * (position - a_center);
          |  }""".stripMargin
    case ColorCycling(period) =>
      s"""float life = (u_time - a_birthtime) / 1000.0;
         |float state = (life - $period * floor(life / $period)) / $period;
         |if (state < 0.0833) {
         |  v_color.x = 0.0; v_color.y = 0.0; // blue
         |} else if (state < 0.1667) {
         |  v_color.x = 0.43 * v_color.x; v_color.y = 0.0; v_color.z = 0.9 * v_color.z; // blue/violet
         |} else if (state < 0.25) {
         |  v_color.x = 0.7 * v_color.x; v_color.y = 0.0; v_color.z = 0.7 * v_color.z; // violet
         |} else if (state < 0.3333) {
         |  v_color.x = 0.9 * v_color.x; v_color.y = 0.0; v_color.z = 0.43 * v_color.z; // violet/red
         |} else if (state < 0.4167) {
         |  v_color.y = 0.0; v_color.z = 0.0; // red
         |} else if (state < 0.5) {
         |  v_color.x = 0.9 * v_color.x; v_color.y = 0.43 * v_color.y; v_color.z = 0.0; // orange
         |} else if (state < 0.5833) {
         |  v_color.x = 0.7 * v_color.x; v_color.y = 0.7 * v_color.y; v_color.z = 0.0; // yellow
         |} else if (state < 0.6667) {
         |  v_color.x = 0.43 * v_color.x; v_color.y = 0.9 * v_color.y; v_color.z = 0.0; // yellow/green
         |} else if (state < 0.75) {
         |  v_color.x = 0.0; v_color.z = 0.0; // green
         |} else if (state < 0.8333) {
         |  v_color.x = 0.0; v_color.y = 0.9 * v_color.y; v_color.z = 0.43 * v_color.z; // green/cyan
         |} else if (state < 0.9167) {
         |  v_color.x = 0.0; v_color.y = 0.7 * v_color.y; v_color.z = 0.7 * v_color.z; // cyan
         |} else {
         |  v_color.x = 0.0; v_color.y = 0.43 * v_color.y; v_color.z = 0.9 * v_color.z; // cyan/blue
         |}""".stripMargin
    case SimplexNoise(xScale, yScale, rate, amplitude, shift) =>
      s"""|  float life = u_time / 1000.0 * $rate;
          |  float noise = snoise3D(vec3( a_center / ${LivingHexagon.scaling * 2} * vec2( $xScale, $yScale ), life ));
          |  float hAlpha = sin(noise * $twoPi);
          |  position = position + (hAlpha * $amplitude + $shift) * (position - a_center);""".stripMargin
    case NoFX => ""
  }

  private def sprout = sprouting match {
    case Shrinking =>
      s"""|$alphaCompute
          |  position = a_center + alpha * (position - a_center);""".stripMargin

    case Fading =>
      s"""|$alphaCompute
          |  v_color.a = alpha * v_color.a;""".stripMargin
          
    case Wither(shiftRatio) =>
      val shift = hexagonWidth * shiftRatio * 0.5f
      val piBy3 = 1.04719755f
      s"""|$alphaCompute
          |  float xShift = sin(mod(a_birthtime, 6.0) * $piBy3) * $shift;
          |  float yShift = cos(mod(a_birthtime, 6.0) * $piBy3) * $shift;
          |  vec2 shift = vec2( xShift, yShift );
          |  v_color = (1.0 - alpha) * vec4(1.0 - v_color.rgb, v_color.a) + alpha * v_color;
          |  position = position - (1.0 - alpha) * (0.3 * (position - a_center) + shift);""".stripMargin

    case NoFX =>
      ""
  }

  private def applyColorShading = colorShading match {
    case Bicolor =>
      """  vec4 color0;
        |  vec4 color1;
        |  if ( a_centerFlag == 1.0 ) {
        |    color0 = shaded(u_color0, u_noise0, u_shading0, u_offsets0, u_scaling0);
        |    color1 = shaded(u_color1, u_noise1, u_shading1, u_offsets1, u_scaling1);
        |  } else {
        |    color0 = noised(u_color0, u_noise0, u_offsets0, u_scaling0);
        |    color1 = noised(u_color1, u_noise1, u_offsets1, u_scaling1);
        |  }
        |  v_color = blendAlpha * color0 + (1.0 - blendAlpha) * color1;""".stripMargin
    case Color3D(rate) =>
      s"""|  float colorLife = u_time / 1000.0 * $rate;
          |  if ( a_centerFlag == 1.0 ) {
          |    v_color = shaded3D(u_color0, u_noise0, u_shading0, u_offsets0, u_scaling0, colorLife);
          |  } else {
          |    v_color = noised3D(u_color0, u_noise0, u_offsets0, u_scaling0, colorLife);
          |  }""".stripMargin
    case NoFX =>
      """  v_color = u_color0;"""
  }

  private def vertexShaderRaw = s"""#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
#endif

attribute vec2 a_position;
attribute vec2 a_center;
attribute float a_centerFlag;
varying float v_centerFlag;

attribute float a_tier;
varying float v_tier;

uniform vec4 u_color0;
uniform vec3 u_noise0;
uniform vec3 u_shading0;
uniform vec2 u_offsets0;
uniform vec2 u_scaling0;

uniform vec4 u_color1;
uniform vec3 u_noise1;
uniform vec3 u_shading1;
uniform vec2 u_offsets1;
uniform vec2 u_scaling1;

varying vec4 v_color;

uniform float u_time;
attribute float a_birthtime;
attribute float a_deathtime;

${SimplexNoiseGLSL.mod289_3D}

${SimplexNoiseGLSL.noise2D}

${SimplexNoiseGLSL.noise3D}

float noise2DOf(vec2 position, vec2 offsets, vec2 scaling) {
  return snoise2D( position * scaling + offsets );
}

vec4 noised(vec4 color, vec3 noise, vec2 offsets, vec2 scaling) {
  float n = noise2DOf( a_center / ${LivingHexagon.scaling * 2}, offsets, scaling );
  return clamp( vec4( color.rgb * ( 1.0 + n * noise ), color.a ),
                0.0,
                1.0 );
}

vec4 shaded(vec4 color, vec3 noise, vec3 shading, vec2 offsets, vec2 scaling) {
  vec4 noisedColor = noised( color, noise, offsets, scaling );
  return clamp( vec4( noisedColor.rgb + shading, noisedColor.a ),
                0.0,
                1.0 );
}

float noise3DOf(vec2 position, vec2 offsets, vec2 scaling, float z) {
  return snoise3D( vec3( position * scaling + offsets, z ) );
}

vec4 noised3D(vec4 color, vec3 noise, vec2 offsets, vec2 scaling, float z) {
  float n = noise3DOf( a_center / ${LivingHexagon.scaling * 2}, offsets, scaling, z );
  return clamp( vec4( color.rgb * ( 1.0 + n * noise ), color.a ),
                0.0,
                1.0 );
}

vec4 shaded3D(vec4 color, vec3 noise, vec3 shading, vec2 offsets, vec2 scaling, float z) {
  vec4 noisedColor = noised3D( color, noise, offsets, scaling, z );
  return clamp( vec4( noisedColor.rgb + shading, noisedColor.a ),
                0.0,
                1.0 );
}

void main()
{
  v_centerFlag = a_centerFlag;
  v_tier = a_tier;
  float blendAlpha = sin($blendingRate * u_time / 2000.0) / 2.0 + 0.5;

$applyColorShading
  
  vec2 position = a_position;
$sprout
$highlight
  gl_Position = modelViewMatrix * projectionMatrix * vec4(position, 0.0, 1.0);;
}"""

  protected def withEdge(color: Color, thickness: Float) = s"""float f = edgeFactor($thickness, v_centerFlag);
  gl_FragColor = mix(vec4(${color.r}, ${color.g}, ${color.b}, ${color.a} * tierColor.a), tierColor, f);"""

  protected def edgeLess ="""gl_FragColor = tierColor;"""

  private def applyEdge = border match {
    case Border(color, thickness) => withEdge(color, thickness)
    case _ => edgeLess
  }

  private def tierColor =
    if (cubic) """if (v_tier < 0.0) {
    tierColor = vec4(v_color.rgb - 0.07, v_color.a);
  } else if (v_tier > 0.0) {
    tierColor = vec4(v_color.rgb + 0.07, v_color.a);
  } else {
    tierColor = v_color;
  }"""
    else """tierColor = v_color;"""

  private def fragmentShaderRaw = s"""#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
#endif

varying float v_centerFlag;
varying float v_tier;
varying vec4 v_color;

float edgeFactor(const float thickness, const float centerFlag)
{
  return smoothstep(0.0, fwidth(centerFlag)*thickness, centerFlag);
}

void main()
{
  vec4 tierColor;
  $tierColor
  $applyEdge
}"""

  private val intRegex = "([\\( ,][0-9]+)([\\) ,;])".r
  private def intToFloatInString(str: String): String = intRegex.replaceAllIn(str, m =>
    m.group(1) + ".0" + m.group(2)
  )

  val fragmentShader = intToFloatInString(fragmentShaderRaw)
  val vertexShader = intToFloatInString(vertexShaderRaw)
}
