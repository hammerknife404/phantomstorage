package com.phantomstorage.network;

import com.phantomstorage.DesignationMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record LinkedStorageSyncPayload(List<HighlightEntry> entries) implements CustomPacketPayload {

    public record HighlightEntry(BlockPos pos, DesignationMode mode) {}

    public static final Type<LinkedStorageSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("phantomstorage", "linked_storage_sync"));

    private static final StreamCodec<ByteBuf, HighlightEntry> ENTRY_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        HighlightEntry::pos,
        ByteBufCodecs.STRING_UTF8.map(DesignationMode::valueOf, DesignationMode::name),
        HighlightEntry::mode,
        HighlightEntry::new
    );

    public static final StreamCodec<ByteBuf, LinkedStorageSyncPayload> CODEC =
        ENTRY_CODEC.apply(ByteBufCodecs.list())
                   .map(LinkedStorageSyncPayload::new, LinkedStorageSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
