package com.phantomstorage;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, PhantomStorageMod.MODID);

    /**
     * Same look as vanilla's soul particle (reuses its sprite frames — see
     * assets/phantomstorage/particles/anchor_soul.json), but with a longer lifetime so it
     * travels ~15% farther without any change to its spawn velocity. Used by the Phantom
     * Anchor block while a chest is docked, mirroring the chest's own ambient soul particles.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ANCHOR_SOUL =
            PARTICLE_TYPES.register("anchor_soul", () -> new SimpleParticleType(false));
}
