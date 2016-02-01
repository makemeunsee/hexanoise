package rendering.shaders

import scala.util.Random
import world2d.LivingHexagon
import rendering.{SimpleColor, DynamicColor}

trait LavaBasalt {
  val colderLavaBaseColor = SimpleColor(0xd83f00ff)
  val hotterLavaBaseColor = SimpleColor(0xf24c00ff)
  val basaltColor         = SimpleColor(0x201008FF)
  val noiseScaling = 0.16f
}

case class BackgroundShader(color0: DynamicColor,
                            color1: DynamicColor,
                            override val border: BorderMode = NoFX,
                            override val highlighting: HighlightMode = NoFX,
                            override val colorShading: ColorShadingMode = Bicolor,
                            override val cubic: Boolean = false)
extends ShaderModule[LivingHexagon] {
  override def blendingRate = 1f
  override def sprouting: SproutMode = NoFX
  val birthFunction: ( LivingHexagon, Int ) => Float = (_,_) => Random.nextInt(1000)
  val deathFunction: ( LivingHexagon, Int ) => Float = (_,_) => Float.MaxValue
  def hexagonWidth = LivingHexagon.scaling
}

case class BlockShader(color: DynamicColor,
                       override val border: BorderMode = NoFX,
                       override val sprouting: SproutMode = NoFX,
                       override val highlighting: HighlightMode = NoFX,
                       override val colorShading: ColorShadingMode = Bicolor,
                       override val cubic: Boolean = false)
extends ShaderModule[LivingHexagon] {
  val color0 = color
  val color1 = color
  override def blendingRate = 1f
  val birthFunction: ( LivingHexagon, Int ) => Float = (_,_) => Random.nextInt(1000)
  val deathFunction: ( LivingHexagon, Int ) => Float = (_,_) => Float.MaxValue
  def hexagonWidth = LivingHexagon.scaling
}

object ShadersPack {
  
  sealed abstract class ShaderPair {
    def backgroundShader: BackgroundShader
    def blockShader: BlockShader
    def name: String
  }

  def apply(shaderName: String): ShaderPair = {
    values.filter(_.name == shaderName).head
  }

  val Artsy = new ShaderPair {

    def name = "Artsy"

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xc41a4fff),0.04f,0.04f,(20.0f,20.0f,0.3f)),
        DynamicColor(SimpleColor(0x620d27ff),0.04f,0.04f,(20.0f,0.7f,10f)),
        cubic = true)

    val blockCol = DynamicColor(SimpleColor(0x1f1f1fff),0.14f,0.14f,(0.4f,0.4f,0.4f))
    val blockShader = new BlockShader(blockCol, Border(SimpleColor(0xffffff22), 2f),Shrinking, highlighting = NoFX)
  }

  val Electric = new ShaderPair {

    def name = "Electric"

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0x2236afff),0.14f,0.14f,(0.33f,0.5f,40f)),
        DynamicColor(SimpleColor(0x251f44ff),0.14f,0.14f,(0.33f,0.5f,40f)),
        cubic = true)

    val blockCol = DynamicColor(SimpleColor(0x663333ff),0.14f,0.14f,(0f,0f,0.5f))
    val blockShader = new BlockShader(blockCol, Border(SimpleColor(0x88884488), 1.2f),Shrinking, highlighting = NoFX,cubic = true)
  }

  val BlueOnBlue = new ShaderPair {

    def name = "BlueOnBlue"

    val blockCol = DynamicColor(SimpleColor(0x395ce1ff),0.32f,0.32f,(0.5f,0.5f,0.5f))

    val blockShader = new BlockShader(blockCol, sprouting = Fading)

    val backgroundShader = new BackgroundShader(DynamicColor(SimpleColor(0x2935caff),0.01f,0.01f,(0.33f,0.5f,1.0f)),
        DynamicColor(SimpleColor(0x141a65ff),0.01f,0.01f,(0.33f,0.5f,1.0f)))
  }

  val MetallicViolet = new ShaderPair {

    def name = "MetallicViolet"

    val blockCol = DynamicColor(SimpleColor(0x330954ff),0.16f,0.16f,(0.5f,0.5f,0.5f))

    val blockShader = new BlockShader(blockCol, sprouting = Fading, highlighting = NoFX)

    val backgroundShader = new BackgroundShader(DynamicColor(SimpleColor(0xdd92c7ff),0.02f,0.02f,(0.33f,0.5f,1.0f)),
        DynamicColor(SimpleColor(0x6e4963ff),0.02f,0.02f,(0.33f,0.5f,1.0f)),
        border = Border(SimpleColor(0x6b9a222e),1.6f))
  }

  val LaitFraise = new ShaderPair {

    def name = "LaitFraise"

    val blockCol = DynamicColor(SimpleColor(0xfb735dff),0.02f,0.02f,(0.5f,0.7f,0.6f),425,185)

    val blockShader = new BlockShader(blockCol,sprouting = Shrinking,cubic = true)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xf0adadff),0.01f,0.01f,(0.8f,0.9f,0.5f),434,346),
        DynamicColor(SimpleColor(0x785656ff),0.01f,0.01f,(7.0f,4.0f,3.0f),28,69),
        Border(SimpleColor(0xe5b75ba7),3.2f),cubic = true)
  }

  val GoldenCore = new ShaderPair {

    def name = "GoldenCore"

    val blockCol = DynamicColor(SimpleColor(0xec4906ff),0.01f,0.01f,(0.6f,4.0f,10.0f),160,153)

    val blockShader = new BlockShader(blockCol,sprouting = Fading,highlighting = Pulsating(rate = 0.3f),cubic = true)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xa722c4ff),0.32f,0.32f,(0.6f,2.0f,2.0f),215,199),
        DynamicColor(SimpleColor(0x531162ff),0.32f,0.32f,(0.3f,7.0f,7.0f),401,70),
        Border(SimpleColor(0x2c0ccfe7),2.4f),cubic = true)
  }

  val LavaBasaltGradient = new ShaderPair with LavaBasalt {

    def name = "LavaBasaltGradient"

    val gradCol1 = DynamicColor(colderLavaBaseColor,
      noiseScaling, noiseScaling,
      (0.33f, 0.5f, 1f))
    val gradCol2 = DynamicColor(hotterLavaBaseColor,
      noiseScaling, noiseScaling,
      (0.33f, 0.5f, 1f))

    val backgroundShader = new BackgroundShader(gradCol1, gradCol2)

    val gradCol = DynamicColor(basaltColor,
      1f, 1f,
      (0.7f, 0.7f, 0.7f),
      shadingCoeffs = (0.1f, 0.1f, 0.1f))

    val blockShader = new BlockShader(gradCol, sprouting = Shrinking, highlighting = NoFX, border = Border(SimpleColor(0)))
  }

  val LavaBasaltGradient2 = new ShaderPair with LavaBasalt {

    def name = "LavaBasaltGradient2"

    val backgroundShader = new BackgroundShader(
        LavaBasaltGradient.backgroundShader.color0,
        LavaBasaltGradient.backgroundShader.color0,
        colorShading = Color3D(0.2f) )

    val gradCol = DynamicColor(basaltColor,
      1f, 1f,
      (0.7f, 0.7f, 0.7f),
      shadingCoeffs = (0.1f, 0.1f, 0.1f))

    val blockShader = LavaBasaltGradient.blockShader
  }

  val LavaBasaltCubic = new ShaderPair with LavaBasalt {

    def name = "LavaBasaltCubic"

    val cubeCol1 = DynamicColor(colderLavaBaseColor,
      noiseScaling, noiseScaling,
      (0.33f, 0.5f, 0.5f),
      128, 0,
      shadingCoeffs = (-0.1f, -0.1f, -0.1f))
    val cubeCol2 = DynamicColor(hotterLavaBaseColor,
      noiseScaling, noiseScaling,
      (0.33f, 0.5f, 0.5f),
      504, 0,
      shadingCoeffs = (-0.1f, -0.1f, -0.1f))

    val backgroundShader = new BackgroundShader(cubeCol1, cubeCol2, cubic = true)

    val cubeCol = DynamicColor(basaltColor,
      1, 1,
      (0.5f, 0.5f, 0.5f),
       shadingCoeffs = (0.1f, 0.1f, 0.1f))

    val blockShader = new BlockShader(cubeCol, sprouting = Fading, cubic = true)
  }

  val Kurosawa = new ShaderPair {

    def name = "Kurosawa"

    val blockCol = DynamicColor(SimpleColor(0xba842fff),0.01f,0.01f,(0.3f,4.0f,8.0f), shadingCoeffs = (0.0f,0.0f,0.0f))

    val blockShader = new BlockShader(
      blockCol, Border(SimpleColor(0xaf07c69a),1.6f),sprouting = Fading)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0x7673f3ff),0.16f,0.16f,(50.0f,0.2f,10.0f), shadingCoeffs = (0.0f,0.0f,0.0f)),
        DynamicColor(SimpleColor(0x3b3979ff),0.16f,0.16f,(60.0f,8.0f,20.0f), shadingCoeffs = (0.0f,0.0f,0.0f)),
        Border(SimpleColor(0xd513df5b),1.6f))
  }

  val Mosaic = new ShaderPair {

    def name = "Mosaic"

    val blockCol = DynamicColor(SimpleColor(0x63b3fbff),0.01f,0.01f,(2.0f,0.5f,4.0f))

    val blockShader = new BlockShader(blockCol, sprouting = Fading, highlighting = NoFX,cubic = true)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xc4bf2aff),1.28f,1.28f,(3.0f,9.0f,60.0f)),
        DynamicColor(SimpleColor(0x625f15ff),1.28f,1.28f,(7.0f,0.1f,5.0f)),
        Border(SimpleColor(0x2541e137),2.4f))
  }
  
  val PurpleInRain = new ShaderPair {

    def name = "PurpleInRain"

    val blockCol = DynamicColor(SimpleColor(0xfd31f3ff),0.01f,0.01f,(0.7f,3.0f,0.2f))
      
    val blockShader = new BlockShader(blockCol, highlighting = Squeezing(1.9f, 0.1f, 0.2f), sprouting = Shrinking) {
      override val birthFunction = (_: LivingHexagon, id: Int) => ( (math.ceil(id / 6) * 50) % 2000 ).toFloat
    }
    
    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xa269d8ff), 0.16f, 0.32f,(0.9f,10.0f,50.0f)),
        DynamicColor(SimpleColor(0x51346cff), 0.64f, 0.02f,(60.0f,6.0f,0.7f)),
        Border(SimpleColor(0x49447a56),3.2f))
  }

  val Sunset = new ShaderPair {

    def name = "Sunset"

    val blockCol = DynamicColor(SimpleColor(0x322005ff), 0.02f, 0.02f,(20.0f,4.0f,0.1f), 0, 0)
    val blockShader = new BlockShader(blockCol, highlighting = NoFX,sprouting = Fading)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xf87408ff), 0.01f, 0.08f,(0.6f,0.7f,0.8f)),
        DynamicColor(SimpleColor(0xf87408ff), 0.01f, 0.08f,(0.6f,0.7f,0.8f)))
  }

  val Sunset2 = new ShaderPair {

    def name = "Sunset2"

    val blockShader = new BlockShader(
        Sunset.blockShader.color0,
        sprouting = Fading,
        highlighting = Pulsating(),
        colorShading = Color3D(0.1f) ) {
      override val birthFunction = (_: LivingHexagon, id: Int) => ( (math.ceil(id / 6) * 50) % 1000 ).toFloat
    }

    val backgroundShader = Sunset.backgroundShader.copy( colorShading = Color3D(0.05f) )
  }

  val Sunset3 = new ShaderPair {

    def name = "Sunset3"

    private val color = Sunset.blockShader.color0
    val blockShader = new BlockShader(
        color,
        sprouting = Fading,
        highlighting = SimplexNoise(color.noiseScalingX, color.noiseScalingY, rate = 0.1f, amplitude = 0.1f, shift = -0.1f),
        colorShading = Color3D(0.1f) )

    val backgroundShader = Sunset.backgroundShader.copy( colorShading = Color3D(0.05f) )
    new BlockShader(
        DynamicColor(SimpleColor(0xf87408ff), 0.01f, 0.08f,(0.6f,0.7f,0.8f)),
        sprouting = Fading,
        colorShading = Color3D(0.05f))
  }

  val Impressionist = new ShaderPair {

    def name = "Impressionist"

    val blockCol = DynamicColor(SimpleColor(0xfd31f3ff),0.01f,0.01f,(0.7f,3.0f,0.2f))
    val blockShader = new BlockShader(blockCol,sprouting = Shrinking)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xc317a0ff), 0.16f, 0.32f,(6.0f,70.0f,0.8f)),
        DynamicColor(SimpleColor(0x610b50ff), 0.01f, 0.16f,(2.0f,60.0f,90.0f)),
        Border(SimpleColor(0x2abd5605),1.6f),highlighting = Pulsating()) {
      override val birthFunction = (_: LivingHexagon, id: Int) => ( (math.ceil(id / 6) * 50) % 2000 ).toFloat
    }
  }
  
  val StroboAmbers = new ShaderPair {

    def name = "StroboAmbers"

    val blockShader = new BlockShader(
        DynamicColor(SimpleColor(0xa4483fff),0.08f,0.08f,(0.5f,0.3f,0.8f), shadingCoeffs = (-0.1f,0.1f,-0.1f)),
        Border(SimpleColor(0x00000005),0.8f),
        sprouting = Fading, highlighting = NoFX, cubic = false)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xca1302ff), 0.32f, 0.04f,(5.0f,9.0f,0.1f)),
        DynamicColor(SimpleColor(0x650901ff), 0.32f, 1.28f,(100.0f,9.0f,100.0f)),
        Border(SimpleColor(0x7a71533a),1.6f), highlighting = Pulsating(1.0f,0.4f,-1.5f))
  }

  val GreenSlither = new ShaderPair {

    def name = "GreenSlither"

    val blockShader = new BlockShader(
        DynamicColor(SimpleColor(0xe1699bff), 1.28f, 0.08f,(0.7f,4.0f,1.0f)),
        sprouting = Fading, highlighting = NoFX, cubic = true)

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0x75f786ff), 0.02f, 0.08f,(5.0f,0.5f,100f)),
        DynamicColor(SimpleColor(0x3a7b43ff), 0.02f, 0.01f,(4.0f,4.0f,8.0f)),
        Border(SimpleColor(0xc55c9938),0.8f), highlighting = Pulsating(0.2f,0.5f,0.6f))
  }

  val HeadacheMachine = new ShaderPair {

    def name = "HeadacheMachine"

    val blockShader = StroboAmbers.blockShader

    val backgroundShader = new BackgroundShader(
        StroboAmbers.backgroundShader.color0,
        StroboAmbers.backgroundShader.color1,
        StroboAmbers.backgroundShader.border,
        highlighting = Pulsating(0.5f,0.8f,-1.2f)) {
      private val offset = new scala.util.Random().nextFloat() * 500f - 250f
      override val birthFunction = (hexa: LivingHexagon, _: Int) =>
        noise.SimplexNoise.noise(offset + hexa.x.toFloat / 30f, offset + hexa.y.toFloat / 30f).toFloat*1000f
    }
  }

  val HeadacheMachine2 = new ShaderPair {

    def name = "HeadacheMachine2"

    val blockShader = Artsy.blockShader

    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0x44e408ff), 0.04f, 0.06f,(4f,0.5f,1f)),
        DynamicColor(SimpleColor(0x44e408ff), 0.04f, 0.06f,(4f,0.5f,1f)),
        HeadacheMachine.backgroundShader.border,
        highlighting = SimplexNoise(xScale = 1f/20, yScale = 1f/20, rate = 0.3f, amplitude = 0.8f, shift = -0.6f)) {
      override val birthFunction = (_: LivingHexagon, _: Int) => 0f
    }
  }

  val HeadacheMachine3 = new ShaderPair {

    def name = "HeadacheMachine3"

    val blockShader = HeadacheMachine.blockShader

    val backgroundShader = new BackgroundShader(
        HeadacheMachine.backgroundShader.color0,
        HeadacheMachine.backgroundShader.color1,
        HeadacheMachine.backgroundShader.border,
        highlighting = SimplexNoise(xScale = 1f/30, yScale = 1f/24, rate = 0.2f, amplitude = 0.8f, shift = -1.2f)) {
      override val birthFunction = (_: LivingHexagon, _: Int) => 0f
    }
  }

  val Rainbow8bit = new ShaderPair {

    def name = "Rainbow8bit"

    val blockShader = new BlockShader(
        DynamicColor(SimpleColor(0xcfbeadff),
          0.0f,
          0.0f,
          (0.3f,0.3f,0.3f),
          shadingCoeffs = (-0.1f, -0.1f, -0.1f)),
        sprouting = Shrinking,
        highlighting = ColorCycling(6.0f),
        cubic = false) {
      override val birthFunction = (_: LivingHexagon, id: Int) =>
        ( (math.ceil(id / 6) * 60) % 6000 ).toFloat
    }
    
    val backgroundShader = new BackgroundShader(
        DynamicColor(SimpleColor(0xe5b5b5ff),
          0.01f,
          0.01f,
          (0.5f,0.5f,0.5f),
          new scala.util.Random().nextFloat() * 500 - 250,
          new scala.util.Random().nextFloat() * 500 - 250,
          (-0.1f, -0.1f, -0.1f)),
        DynamicColor(SimpleColor(0xe5b5b5ff),
          0.01f,
          0.01f,
          (0.5f,0.5f,0.5f),
          new scala.util.Random().nextFloat() * 500 - 250,
          new scala.util.Random().nextFloat() * 500 - 250,
          (-0.1f, -0.1f, -0.1f)))
  }

  val RainbowClock = new ShaderPair {

    def name = "RainbowClock"

    val blockShader = new BlockShader(
        DynamicColor(SimpleColor(0xcfbeadff),
          0.0f,
          0.0f,
          (0.3f,0.3f,0.3f),
          shadingCoeffs = (-0.1f, -0.1f, -0.1f)),
        sprouting = Shrinking,
        highlighting = ColorCycling(3.0f),
        cubic = false) {
      override val birthFunction = (_: LivingHexagon, id: Int) =>
        ( (id - 1) % 6 * world2d.FlakeGenerator.sizeMax ).toFloat
    }
    
    val backgroundShader = Rainbow8bit.backgroundShader
  }
  
  val values = Seq(Artsy, Electric, BlueOnBlue, MetallicViolet, LaitFraise, GoldenCore, LavaBasaltGradient,
                   LavaBasaltGradient2, LavaBasaltCubic, Kurosawa, Mosaic, PurpleInRain, Sunset, Sunset2, Sunset3,
                   Impressionist, StroboAmbers, HeadacheMachine, HeadacheMachine2, HeadacheMachine3, GreenSlither,
                   Rainbow8bit, RainbowClock)
}