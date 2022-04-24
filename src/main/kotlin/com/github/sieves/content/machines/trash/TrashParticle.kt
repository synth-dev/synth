package com.github.sieves.content.machines.trash

import net.minecraft.client.multiplayer.*
import net.minecraft.client.particle.*
import net.minecraft.core.particles.SimpleParticleType

class TrashParticle(
    level: ClientLevel,
    xCoord: Double,
    yCoord: Double,
    zCoord: Double,
    xSpeed: Double,
    ySpeed: Double,
    zSpeed: Double
) : TextureSheetParticle(
    level,
    xCoord,
    yCoord,
    zCoord,
    xSpeed,
    ySpeed,
    zSpeed
) {
    private var large = false
    private var scale = 1.0f

    init {
        friction = 0.4f
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z
        if (age++ >= lifetime || onGround) {
            this.remove()
        } else {
//            yd -= 0.04 * gravity.toDouble()
            move(xd, yd, zd)
            if (speedUpWhenYMotionIsBlocked && y == yo) {
                xd *= 1.1
                zd *= 1.1
            }
            xd *= friction.toDouble()
            yd *= friction.toDouble()
            zd *= friction.toDouble()
            scale(((age / lifetime).toFloat() * 100))
        }
    }

    class Provider(val sprite: SpriteSet) : ParticleProvider<SimpleParticleType> {

        override fun createParticle(
            pType: SimpleParticleType,
            pLevel: ClientLevel,
            pX: Double,
            pY: Double,
            pZ: Double,
            pXSpeed: Double,
            pYSpeed: Double,
            pZSpeed: Double
        ): Particle {
            val particle = TrashParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed)
            particle.setColor(1f, 1f, 1f)
            particle.pickSprite(sprite)
            return particle
        }

    }
}