package com.phantomstorage;

import com.phantomstorage.item.PhantomChestSummonerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, PhantomStorageMod.MODID);

    public static final DeferredHolder<Item, PhantomChestSummonerItem> PHANTOM_CHEST_SUMMONER =
            ITEMS.register("phantom_chest_summoner",
                    () -> new PhantomChestSummonerItem(new Item.Properties().stacksTo(1)));
}
