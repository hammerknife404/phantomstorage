package com.phantomstorage.item;

import com.phantomstorage.DesignationMode;
import com.phantomstorage.LinkedStorage;
import com.phantomstorage.entity.PhantomChestEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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

        if (!(level.getBlockEntity(pos) instanceof Container)) {
            return InteractionResult.PASS;
        }

        PhantomChestEntity chest = PhantomChestSummonerItem.findPlayerChest(
            ((ServerLevel) level).getServer(), player
        );
        if (chest == null) {
            player.displayClientMessage(
                Component.translatable("message.phantomstorage.wrench.no_chest"), true
            );
            return InteractionResult.FAIL;
        }

        ResourceKey<Level> dim = level.dimension();
        boolean isSneaking = player.isShiftKeyDown();
        LinkedStorage existing = chest.getLinkedStorage(pos, dim);

        if (existing == null) {
            chest.addLinkedStorage(new LinkedStorage(pos, dim, DesignationMode.OUTPUT));
            player.displayClientMessage(
                Component.translatable("message.phantomstorage.wrench.linked_output"), true
            );
        } else if (isSneaking) {
            chest.removeLinkedStorage(pos, dim);
            if (existing.mode() == DesignationMode.OUTPUT) {
                chest.addLinkedStorage(new LinkedStorage(pos, dim, DesignationMode.INPUT));
                player.displayClientMessage(
                    Component.translatable("message.phantomstorage.wrench.linked_input"), true
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("message.phantomstorage.wrench.unlinked"), true
                );
            }
        } else {
            player.displayClientMessage(
                Component.translatable("message.phantomstorage.wrench.already_linked",
                    existing.mode().name().toLowerCase()), true
            );
        }

        // Immediately push updated highlights to the client so the outline appears without delay
        if (player instanceof ServerPlayer sp) {
            chest.syncHighlightsTo(sp);
        }

        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("tooltip.phantomstorage.wrench"));
    }
}
