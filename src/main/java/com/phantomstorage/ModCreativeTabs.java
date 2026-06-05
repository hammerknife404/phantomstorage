package com.phantomstorage;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PhantomStorageMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PHANTOM_STORAGE_TAB =
            CREATIVE_TABS.register("phantom_storage_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.phantomstorage"))
                    .icon(() -> new ItemStack(ModItems.PHANTOM_CHEST_SUMMONER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.PHANTOM_CHEST_SUMMONER.get());
                        output.accept(ModItems.PHANTOM_CHEST_SUMMONER_UPGRADED.get());
                        output.accept(ModItems.PHANTOM_CHEST_SUMMONER_SUPREME.get());
                    })
                    .build());
}
