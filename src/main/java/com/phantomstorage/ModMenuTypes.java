package com.phantomstorage;

import com.phantomstorage.inventory.PhantomChestMenu;
import com.phantomstorage.inventory.PhantomLinkMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PhantomStorageMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PhantomChestMenu>> PHANTOM_CHEST_MENU =
            MENU_TYPES.register("phantom_chest_menu",
                    () -> IMenuTypeExtension.create(PhantomChestMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhantomLinkMenu>> PHANTOM_LINK_MENU =
            MENU_TYPES.register("phantom_link_menu",
                    () -> IMenuTypeExtension.create(PhantomLinkMenu::new));
}
