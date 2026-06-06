package com.phantomstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record LinkedStorage(BlockPos pos, ResourceKey<Level> dimension, DesignationMode mode) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        tag.putString("Dim", dimension.location().toString());
        tag.putString("Mode", mode.name());
        return tag;
    }

    public static LinkedStorage load(CompoundTag tag) {
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        ResourceKey<Level> dim = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.parse(tag.getString("Dim"))
        );
        DesignationMode mode = DesignationMode.valueOf(tag.getString("Mode"));
        return new LinkedStorage(pos, dim, mode);
    }
}
