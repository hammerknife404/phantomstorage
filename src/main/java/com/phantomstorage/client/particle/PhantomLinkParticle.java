package com.phantomstorage.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhantomLinkParticle extends TextureSheetParticle {

    private final float baseAlpha;

    PhantomLinkParticle(ClientLevel level, double x, double y, double z,
                        double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.pickSprite(sprites);

        this.lifetime  = 15 + random.nextInt(10);            // 15–25 ticks
        this.quadSize  = 0.05f + random.nextFloat() * 0.05f; // 0.05–0.10 blocks
        this.baseAlpha = 0.3f + random.nextFloat() * 0.7f;   // 30–100 %
        this.alpha     = this.baseAlpha;
        this.hasPhysics = false;
        this.gravity    = 0f;
    }

    @Override
    public void tick() {
        super.tick();
        // Linear fade from baseAlpha → 0 over the full lifetime
        this.alpha = this.baseAlpha * (1.0f - (float) this.age / this.lifetime);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
