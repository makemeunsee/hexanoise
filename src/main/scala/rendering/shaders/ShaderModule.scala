package rendering.shaders

import org.denigma.threejs._
import threejs._
import world2d.{LivingHexagon, Hexagon}
import rendering.Color
import rendering.DynamicColor

import scala.scalajs.js
import scala.scalajs.js.typedarray.{Uint32Array, Float32Array}

sealed trait BorderMode
case class Border(color: Color, thickness: Float = 0.8f) extends BorderMode

sealed trait AlphaFunction
case class Sinus(rate: Float = 1, amplitude: Float = 0.25f, shift: Float = 0f) extends AlphaFunction
case class SimplexNoise2D(xScale: Float = 1, yScale: Float = 1, rate: Float = 1, amplitude: Float = 0.25f, shift: Float = 0f) extends AlphaFunction
case class SimplexNoise3D(xScale: Float = 1, yScale: Float = 1, rate: Float = 1, amplitude: Float = 0.25f, shift: Float = 0f) extends AlphaFunction

sealed trait HighlightMode
case class Pulsating(alphaFunction: AlphaFunction) extends HighlightMode
case class Blending(alphaFunction: AlphaFunction) extends HighlightMode

sealed trait ColorShadingMode
case class Color3D(rate: Float = 1) extends ColorShadingMode

case object NoFX extends HighlightMode with BorderMode with ColorShadingMode

object ShaderModule {
  val shortToType: Map[String, String] = Map(
    "v4" -> "vec4",
    "v3" -> "vec3",
    "v2" -> "vec2",
    "m4" -> "mat4",
    "m3" -> "mat3",
    "m2" -> "mat2",
    "f" -> "float")

  def uniformLoader( mesh: Mesh ): ( String, js.Any ) => Unit = {
    val meshUniforms = mesh.material.asInstanceOf[ShaderMaterial].uniforms.asInstanceOf[scala.scalajs.js.Dynamic]
    ( field, value ) => {
      meshUniforms
        .selectDynamic( field )
        .updateDynamic( "value" )( value )
    }
  }

  val verticePerHexa = 7 // 7 points to define the triangles in a hexagon
  val indicePerHexa = 18 // 6 explicit triangles

  def makeMesh[H <: Hexagon]( hexas: Iterable[_ <: H] ): Mesh = {

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
      )
    )

    val shaderMaterial = new ShaderMaterial
    shaderMaterial.attributes = attrs
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
    val indices = new Uint32Array( count * indicePerHexa )
    var offset = 0
    var indicesOffset = 0

    for ( ( hexa, id ) <- hexas.zipWithIndex ) {

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

    assembleMesh( geom, shaderMaterial, "baseMesh" )
  }
}

import ShaderModule._

trait ShaderModule[H <: Hexagon] extends Shader {

  def colors: Seq[DynamicColor]

  def name: String

  def commonVertexUniforms = Map(
    "u_time" -> "f",
    "u_blendingRate" -> "f",
    "u_xDisplacement" -> "f",
    "u_yDisplacement" -> "f"
  )

  def vertexUniforms: Map[String, String] = Map.empty

  def commonFragmentUniforms = border match {
    case Border( color, thickness ) =>
      Map(
        "u_borderRed" -> "f",
        "u_borderGreen" -> "f",
        "u_borderBlue" -> "f",
        "u_borderAlpha" -> "f",
        "u_borderThickness" -> "f"
      )
    case _ =>
      Map.empty
  }

  def fragmentUniforms: Map[String, String] = Map.empty

  private def allUniforms = commonVertexUniforms ++ vertexUniforms ++ commonFragmentUniforms ++ fragmentUniforms

  private def defineUniforms( mesh: Mesh ): Unit = {
    val customUniforms = allUniforms.foldLeft( js.Dynamic.literal() ) {
      case (dynamic, (key, value)) =>
        dynamic.updateDynamic( key )( js.Dynamic.literal(
            "type" -> value,
            "value" -> new js.Array[Float]
          ) )
        dynamic
    }
    mesh.material.asInstanceOf[ShaderMaterial].uniforms = customUniforms
  }
  
  protected def loadUniforms( mesh: Mesh ): Unit

  private def loadCommonUniforms( mesh: Mesh ): Unit = {
    uniformLoader( mesh )( "u_blendingRate", blendingRate )
    val loader = uniformLoader( mesh )
    border match {
      case Border( color, thickness ) =>
        loader( "u_borderRed", color.r )
        loader( "u_borderGreen", color.g )
        loader( "u_borderBlue", color.b )
        loader( "u_borderAlpha", color.a )
        loader( "u_borderThickness", thickness )
      case _ =>
        ()
    }
  }

  private def loadShaders( mesh: Mesh ): Boolean = {
    val shaderMaterial = mesh.material.asInstanceOf[ShaderMaterial]
    
    val oldVertexShader = shaderMaterial.vertexShader
    val oldFragmentShader = shaderMaterial.fragmentShader
    
    shaderMaterial.vertexShader = vertexShader
    shaderMaterial.fragmentShader = fragmentShader

    oldVertexShader != vertexShader || oldFragmentShader != fragmentShader
  }

  def update( mesh: Mesh ): Unit = {
    if ( loadShaders( mesh ) ) {
      println("recompiled shaders")
      defineUniforms( mesh )
      mesh.material.asInstanceOf[ShaderMaterial].needsUpdate = true
    }
    loadCommonUniforms( mesh )
    loadUniforms( mesh )
  }

  // shaders related code

  def blendingRate: Float
  def border: BorderMode
  def highlighting: HighlightMode
  def cubic: Boolean
  def centerShading: Boolean

  private val twoPi = 2*math.Pi
  private val twoPiBy1000 = twoPi / 1000f

  private def alpha(alphaFunction: AlphaFunction) = alphaFunction match {
    case Sinus(rate, amplitude, shift) =>
      s"   float hAlpha = sin(u_time * $rate * $twoPiBy1000) * $amplitude + $shift;"
    case SimplexNoise2D(xScale, yScale, rate, amplitude, shift) =>
      s"""|  float noise = snoise3D(vec3(center2d / ${LivingHexagon.scaling * 2} * vec2( $xScale, $yScale ), 4.44));
          |  float hAlpha = sin(u_time * $rate * $twoPiBy1000 + noise * $twoPi) * $amplitude + $shift;""".stripMargin
    case SimplexNoise3D(xScale, yScale, rate, amplitude, shift) =>
      s"""|  float life = u_time / 1000.0 * $rate;
          |  float noise = snoise3D(vec3( center2d / ${LivingHexagon.scaling * 2} * vec2( $xScale, $yScale ), life ));
          |  float hAlpha = sin(u_time * $rate * $twoPiBy1000 + noise * $twoPi) * $amplitude + $shift;""".stripMargin
  }

  private def highlight = highlighting match {
    case Pulsating(alphaFunction) =>
      s"""|${alpha(alphaFunction)}
          |  position2d = position2d + hAlpha * (position2d - center2d);""".stripMargin
    case Blending(alphaFunction) =>
      s"""|${alpha(alphaFunction)}
          |  v_color.a = (1.0 + hAlpha) * v_color.a;""".stripMargin
    case NoFX => ""
  }

  def applyColorShading: String

  private def glslUniformsDeclaration( uniforms: Map[String, String] ): String = uniforms
    .map { p => ( p._1, shortToType(p._2) ) }
    .map { case (key, value) => s"uniform $value $key;" }
    .mkString("\n")

  private def glslVertexUniformsDeclaration = glslUniformsDeclaration( commonVertexUniforms ++ vertexUniforms )

  private def glslFragmentUniformsDeclaration = glslUniformsDeclaration( commonFragmentUniforms ++ fragmentUniforms )

  private def vertexShaderRaw = s"""#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
#endif

attribute vec2 a_position;
attribute vec2 a_center;
attribute float a_centerFlag;
varying float v_centerFlag;

attribute float a_tier;
varying float v_tier;

$glslVertexUniformsDeclaration

varying vec4 v_color;

${SimplexNoiseGLSL.mod289_3D}

${SimplexNoiseGLSL.noise3D}

float noise3DOf(vec2 position2d, vec2 scaling, float z) {
  return snoise3D( vec3( position2d * scaling, z ) );
}

vec4 noised3D(vec2 position2d, vec4 color, vec3 noise, vec2 scaling, float z) {
  float n = noise3DOf( position2d / ${LivingHexagon.scaling * 2}, scaling, z );
  return clamp( vec4( color.rgb * ( 1.0 + n * noise ), color.a ),
                0.0,
                1.0 );
}

vec4 shaded3D(vec2 position2d, vec4 color, vec3 noise, vec2 scaling, float z) {
  vec4 noisedColor = noised3D( position2d, color, noise, scaling, z );
  return clamp( vec4( noisedColor.rgb ${if (centerShading) "- 0.1" else ""}, noisedColor.a ),
                0.0,
                1.0 );
}

void main()
{
  v_centerFlag = a_centerFlag;
  v_tier = a_tier;
  float blendAlpha = cos(u_blendingRate * u_time / 2000.0) / 2.0 + 0.5;

  vec2 center2d = a_center + vec2(u_xDisplacement, u_yDisplacement);

$applyColorShading

  vec2 position2d = a_position + vec2(u_xDisplacement, u_yDisplacement);
$highlight
  gl_Position = modelViewMatrix * projectionMatrix * vec4(position2d, 0.0, 1.0);
}"""

  protected def withEdge = s"""float f = edgeFactor(u_borderThickness, v_centerFlag);
  gl_FragColor = mix(vec4(u_borderRed, u_borderGreen, u_borderBlue, u_borderAlpha * tierColor.a), tierColor, f);"""

  protected def edgeLess ="""gl_FragColor = tierColor;"""

  private def applyEdge = border match {
    case Border(_, _) => withEdge
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

$glslFragmentUniformsDeclaration

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

trait MonocolorShaderModule[H <: Hexagon] extends ShaderModule[H] {

  val blendingRate = 0f
  
  def color: DynamicColor

  val colors = Seq(color)

  override def vertexUniforms: Map[String, String] = Map(
    "u_color0" -> "v4",
    "u_noise0" -> "v3",
    "u_scaling0" -> "v2"
    )

  protected def loadUniforms( mesh: Mesh ): Unit = {
    val loader = uniformLoader( mesh )

    loader( "u_color0", new Vector4(
      color.baseColor.r,
      color.baseColor.g,
      color.baseColor.b,
      color.baseColor.a
    ) )
    val noise0 = color.noiseCoeffs
    loader( "u_noise0", new Vector3(
      noise0._1,
      noise0._2,
      noise0._3
    ) )
    loader( "u_scaling0", new Vector2(
      color.noiseScalingX,
      color.noiseScalingY
    ) )
  }

  def colorShading: ColorShadingMode

  def applyColorShading = colorShading match {
    case Color3D(rate) =>
      s"""|  float colorLife = u_time / 1000.0 * $rate;
          |  if ( a_centerFlag == 1.0 ) {
          |    v_color = shaded3D(center2d, u_color0, u_noise0, u_scaling0, colorLife);
          |  } else {
          |    v_color = noised3D(center2d, u_color0, u_noise0, u_scaling0, colorLife);
          |  }""".stripMargin
    case NoFX =>
      s"""|  if ( a_centerFlag == 1.0 ) {
          |    v_color = shaded3D(center2d, u_color0, u_noise0, u_scaling0, 4.44);
          |  } else {
          |    v_color = noised3D(center2d, u_color0, u_noise0, u_scaling0, 4.44);
          |  }""".stripMargin
  }
}

trait BicolorShaderModule[H <: Hexagon] extends ShaderModule[H] {

  def color0: DynamicColor

  def color1: DynamicColor

  val colors = Seq(color0, color1)

  override def vertexUniforms: Map[String, String] = Map(
    "u_color0" -> "v4",
    "u_noise0" -> "v3",
    "u_scaling0" -> "v2",
    "u_color1" -> "v4",
    "u_noise1" -> "v3",
    "u_scaling1" -> "v2"
    )

  protected def loadUniforms( mesh: Mesh ): Unit = {
    val loader = uniformLoader( mesh )
  
    loader( "u_color0", new Vector4(
      color0.baseColor.r,
      color0.baseColor.g,
      color0.baseColor.b,
      color0.baseColor.a
    ) )
    val noise0 = color0.noiseCoeffs
    loader( "u_noise0", new Vector3(
      noise0._1,
      noise0._2,
      noise0._3
    ) )
    loader( "u_scaling0", new Vector2(
      color0.noiseScalingX,
      color0.noiseScalingY
    ) )

    loader( "u_color1", new Vector4(
      color1.baseColor.r,
      color1.baseColor.g,
      color1.baseColor.b,
      color1.baseColor.a
    ) )
    val noise1 = color1.noiseCoeffs
    loader( "u_noise1", new Vector3(
      noise1._1,
      noise1._2,
      noise1._3
    ) )
    loader( "u_scaling1", new Vector2(
      color1.noiseScalingX,
      color1.noiseScalingY
    ) )
  }

  def applyColorShading =
      """  vec4 color0;
        |  vec4 color1;
        |  if ( a_centerFlag == 1.0 ) {
        |    color0 = shaded3D(center2d, u_color0, u_noise0, u_scaling0, 4.44);
        |    color1 = shaded3D(center2d, u_color1, u_noise1, u_scaling1, 4.44);
        |  } else {
        |    color0 = noised3D(center2d, u_color0, u_noise0, u_scaling0, 4.44);
        |    color1 = noised3D(center2d, u_color1, u_noise1, u_scaling1, 4.44);
        |  }
        |  v_color = blendAlpha * color0 + (1.0 - blendAlpha) * color1;""".stripMargin
}