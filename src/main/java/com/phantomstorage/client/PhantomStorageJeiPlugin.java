package com.phantomstorage.client;

import com.phantomstorage.ModItems;
import com.phantomstorage.client.screen.PhantomChestScreen;
import com.phantomstorage.inventory.PhantomChestMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@JeiPlugin
public class PhantomStorageJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("phantomstorage", "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        // Clicking on the crafting output slot opens the JEI recipe list
        reg.addRecipeClickArea(
                PhantomChestScreen.class,
                PhantomChestMenu.CRAFT_RESULT_X,
                PhantomChestMenu.CRAFT_RESULT_Y,
                18, 18,
                RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(
                new ItemStack(ModItems.PHANTOM_CHEST_SUMMONER.get()),
                RecipeTypes.CRAFTING);
    }
}
