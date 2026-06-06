package com.phantomstorage.item;

import com.phantomstorage.DesignationMode;
import com.phantomstorage.LinkedStorage;
import com.phantomstorage.entity.PhantomChestEntity;
import com.phantomstorage.network.LinkedStorageSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class PhantomWrenchItem extends Item {

    public PhantomWrenchItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();

        IItemHandler storage = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (storage == null) {
            for (Direction dir : Direction.values()) {
                storage = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
                if (storage != null) break;
            }
        }
        if (storage == null) return InteractionResult.PASS;

        ResourceKey<Level> dim = level.dimension();
        boolean isSneaking = player.isShiftKeyDown();

        // Links live in player persistent data — no chest entity required
        List<LinkedStorage> links = PhantomChestEntity.loadLinksFromPlayer(player);
        LinkedStorage existing = links.stream()
            .filter(s -> s.pos().equals(pos) && s.dimension().equals(dim))
            .findFirst().orElse(null);

        if (existing == null) {
            links.add(new LinkedStorage(pos, dim, DesignationMode.OUTPUT));
            player.displayClientMessage(
                Component.translatable("message.phantomstorage.wrench.linked_output"), true);
        } else if (isSneaking) {
            links.removeIf(s -> s.pos().equals(pos) && s.dimension().equals(dim));
            if (existing.mode() == DesignationMode.OUTPUT) {
                links.add(new LinkedStorage(pos, dim, DesignationMode.INPUT));
                player.displayClientMessage(
                    Component.translatable("message.phantomstorage.wrench.linked_input"), true);
            } else {
                player.displayClientMessage(
                    Component.translatable("message.phantomstorage.wrench.unlinked"), true);
            }
        } else {
            player.displayClientMessage(
                Component.translatable("message.phantomstorage.wrench.already_linked",
                    existing.mode().name().toLowerCase()), true);
        }

        PhantomChestEntity.saveLinksToPlayer(player, links);

        // Keep the chest entity's in-memory list in sync if it's currently active
        PhantomChestEntity chest = PhantomChestSummonerItem.findPlayerChest(
            ((ServerLevel) level).getServer(), player);
        if (chest != null) chest.loadLinksFrom(player);

        // Send highlights directly — works whether or not the chest is summoned
        if (player instanceof ServerPlayer sp) {
            List<LinkedStorageSyncPayload.HighlightEntry> entries = links.stream()
                .filter(s -> s.dimension().equals(dim))
                .map(s -> new LinkedStorageSyncPayload.HighlightEntry(s.pos(), s.mode()))
                .toList();
            PacketDistributor.sendToPlayer(sp, new LinkedStorageSyncPayload(entries));
        }

        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("tooltip.phantomstorage.wrench"));
    }
}
