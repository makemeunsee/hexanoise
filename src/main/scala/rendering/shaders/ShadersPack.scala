package rendering.shaders

import world2d.LivingHexagon
import rendering.{SimpleColor, DynamicColor}

case class BackgroundShader(name: String,
                            color: DynamicColor,
                            border: BorderMode = NoFX,
                            highlighting: HighlightMode = NoFX,
                            colorShading: ColorShadingMode = NoFX,
                            cubic: Boolean = false,
                            centerShading: Boolean = true)
extends MonocolorShaderModule[LivingHexagon]

object ShadersPack {

  def apply(shaderName: String): ShaderModule[LivingHexagon] = {
    values.filter(_.name == shaderName).head
  }

  val Grid = new BackgroundShader("Grid",
    DynamicColor(SimpleColor(0xffffffff), 1, 1, (0f,0f,0f), 0, 0),
    Border(SimpleColor(0x0),1f),
    centerShading = false)

  private val colorLavaLamp = DynamicColor(SimpleColor(0xc41a4fff),0.04f,0.04f,(20.0f,20.0f,0.3f))
  val LavaLamp = BackgroundShader("LavaLamp",
      colorLavaLamp,
      cubic = true,
      colorShading = Color3D(0.2f))

  private val colorPinkArt = DynamicColor(SimpleColor(0x620d27ff),0.04f,0.04f,(20.0f,0.7f,10f))
  val PinkArt = new BackgroundShader("PinkArt",
    colorPinkArt,
    cubic = true,
    colorShading = Color3D(0.2f))

  private val colorBlueOnBlue = DynamicColor(SimpleColor(0x141a65ff),0.01f,0.01f,(0.33f,0.5f,1.0f))
  val BlueOnBlue = new BackgroundShader("BlueOnBlue",
    colorBlueOnBlue,
    colorShading = Color3D(0.2f))

  private val colorLavaBasaltGradient = DynamicColor(SimpleColor(0xd83f00ff), 0.16f, 0.16f, (0.33f, 0.5f, 1f))
  val LavaBasaltGradient = new BackgroundShader("LavaBasaltGradient",
    colorLavaBasaltGradient,
    colorShading = Color3D(0.15f) )

  private val colorLavaBasaltCubic = DynamicColor(SimpleColor(0xd83f00ff), 0.16f, 0.16f, (0.33f, 0.5f, 0.5f))
  val LavaBasaltCubic = new BackgroundShader("LavaBasaltCubic",
    colorLavaBasaltCubic,
    colorShading = Color3D(0.2f),
    cubic = true )

  private val colorKurosawa = DynamicColor(SimpleColor(0x3b3979ff),0.16f,0.16f,(60.0f,8.0f,20.0f))
  val Kurosawa = new BackgroundShader("Kurosawa",
    colorKurosawa,
    Border(SimpleColor(0xd513df5b),1.6f),
    colorShading = Color3D(0.2f),
    centerShading = false)

  private val colorMosaic = DynamicColor(SimpleColor(0xc4bf2aff),1.28f,1.28f,(3.0f,9.0f,60.0f))
  val Mosaic = new BackgroundShader("Mosaic",
        colorMosaic,
        Border(SimpleColor(0x2541e137),1.4f),
        colorShading = Color3D(0.2f))

  private val colorPurpleInRain = DynamicColor(SimpleColor(0x51346cff), 0.64f, 0.02f,(60.0f,6.0f,0.7f))
  val PurpleInRain = new BackgroundShader("PurpleInRain",
        colorPurpleInRain,
        Border(SimpleColor(0x49447a56),1.0f),
        colorShading = Color3D(0.2f))

  private val colorSunset = DynamicColor(SimpleColor(0xf87408ff), 0.01f, 0.08f,(0.6f,0.7f,0.8f))
  val Sunset = new BackgroundShader("Sunset",
        colorSunset,
        colorShading = Color3D(0.05f) )

  private val colorImpressionist = DynamicColor(SimpleColor(0xc31770ff), 0.16f, 0.32f,(6.0f,70.0f,0.8f))
  val Impressionist = new BackgroundShader("Impressionist",
        colorImpressionist,
        Border(SimpleColor(0x2abd5605),1.2f),
        colorShading = Color3D(0.2f))

  val StroboAmbers = new BackgroundShader("StroboAmbers",
        DynamicColor(SimpleColor(0xca1302ff), 0.32f, 0.04f,(5.0f,9.0f,0.1f)),
        Border(SimpleColor(0x7a71533a),1.6f),
        highlighting = Pulsating(SimplexNoise2D(xScale = 1f, yScale = 1f, rate = 1f, amplitude = 0.4f, shift = -1.5f)))

  val StroboAmbers2 = new BackgroundShader("StroboAmbers2",
        DynamicColor(SimpleColor(0x650901ff), 0.32f, 1.28f,(100.0f,9.0f,100.0f)),
        StroboAmbers.border,
        highlighting = Blending(SimplexNoise2D(xScale = 1f, yScale = 1f, rate = 1f, amplitude = 0.5f, shift = -0.5f)))

  val StroboAmbersGradient = new BackgroundShader("StroboAmbersGradient",
        StroboAmbers.color,
        colorShading = Color3D(0.15f) )

  private val colorGreenSlither = DynamicColor(SimpleColor(0x75f786ff), 0.02f, 0.08f,(5.0f,0.5f,100f))
  val GreenSlither = new BackgroundShader("GreenSlither",
        colorGreenSlither,
        highlighting = Pulsating(Sinus(0.2f,0.5f,0.6f)),
        colorShading = Color3D(0.2f))

  val HeadacheMachine = new BackgroundShader("HeadacheMachine",
        StroboAmbers.color,
        StroboAmbers.border,
        highlighting = Pulsating(SimplexNoise2D(xScale = 1f/20, yScale = 1f/20, rate = 0.5f, amplitude = 0.8f, shift = -1.2f)))

  private val colorHeadacheMachine2 = DynamicColor(SimpleColor(0x44e408ff), 0.04f, 0.06f,(4f,0.5f,1f))
  val HeadacheMachine2 = new BackgroundShader("HeadacheMachine2",
        colorHeadacheMachine2,
        StroboAmbers.border,
        highlighting = Pulsating(SimplexNoise3D(xScale = 1f/20, yScale = 1f/20, rate = 0.3f, amplitude = 0.8f, shift = -0.6f)))

  val LimeGradient = new BackgroundShader("LimeGradient",
        HeadacheMachine2.color,
        colorShading = Color3D(0.15f) )

  val LimeGradient2 = new BackgroundShader("LimeGradient2",
        LimeGradient.color,
        colorShading = Color3D(0.15f),
        highlighting = Blending(SimplexNoise3D(xScale = 1f/20, yScale = 1f/20, rate = 0.3f, amplitude = 0.5f, shift = -0.5f)) )

  val HeadacheMachine3 = new BackgroundShader("HeadacheMachine3",
        StroboAmbers.color,
        StroboAmbers.border,
        highlighting = Pulsating(SimplexNoise3D(xScale = 1f/30, yScale = 1f/24, rate = 0.2f, amplitude = 0.8f, shift = -1.2f)))
  
  val values = Seq(Grid, LavaLamp, PinkArt, BlueOnBlue,
                   LavaBasaltGradient, LavaBasaltCubic,
                   Kurosawa, Mosaic, PurpleInRain, Sunset, Impressionist,
                   StroboAmbers, StroboAmbers2,
                   StroboAmbersGradient,
                   LimeGradient, LimeGradient2,
                   HeadacheMachine, HeadacheMachine2, HeadacheMachine3,
                   GreenSlither)
}