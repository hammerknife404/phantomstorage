package com.phantomstorage.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SoulParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Identical to vanilla's soul particle in every way except lifetime, which is scaled up so it
 * travels ~15% farther without touching its spawn velocity (distance = velocity x lifetime,
 * and RisingParticle's per-tick friction only decays velocity over time — it never speeds up).
 */
@OnlyIn(Dist.CLIENT)
public class AnchorSoulParticle extends SoulParticle {

    private static final float LIFETIME_MULTIPLIER = 1.15f;

    protected AnchorSoulParticle(ClientLevel level, double x, double y, double z,
                                  double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites);
        setLifetime(Math.round(this.lifetime * LIFETIME_MULTIPLIER));
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double xd, double yd, double zd) {
            AnchorSoulParticle particle = new AnchorSoulParticle(level, x, y, z, xd, yd, zd, sprites);
            particle.setAlpha(1.0F);
            return particle;
        }
    }
}
