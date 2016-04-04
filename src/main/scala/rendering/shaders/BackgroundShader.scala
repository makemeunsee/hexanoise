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