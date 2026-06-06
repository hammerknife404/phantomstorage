package com.phantomstorage;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, PhantomStorageMod.MODID);

    // alwaysShow=false — we use addAlwaysVisibleParticle at the call site for
    // all-settings visibility while keeping the count conservative
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHANTOM_LINK_FLOW =
            PARTICLE_TYPES.register("phantom_link_flow", () -> new SimpleParticleType(false));
}
