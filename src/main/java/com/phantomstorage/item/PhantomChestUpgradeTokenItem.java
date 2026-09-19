package com.phantomstorage.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Right-click the player's own active Phantom Chest with this to instantly bump it to this
 * token's tier. Replaces the old design where each tier was its own independent, standalone
 * summoner item — now there is only one summoner (always tier 0), and these tokens upgrade it
 * in place instead of being separate ways to summon a chest.
 */
public class PhantomChestUpgradeTokenItem extends Item {

    private final int tier;

    public PhantomChestUpgradeTokenItem(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("tooltip.phantomstorage.upgrade_token", tier + 1));
    }
}
