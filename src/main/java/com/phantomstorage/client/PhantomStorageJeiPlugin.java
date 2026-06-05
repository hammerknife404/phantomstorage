package com.phantomstorage.client;

import com.phantomstorage.ModItems;
import com.phantomstorage.ModMenuTypes;
import com.phantomstorage.client.screen.PhantomChestScreen;
import com.phantomstorage.inventory.PhantomChestMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@JeiPlugin
public class PhantomStorageJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("phantomstorage", "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        // Click area on the arrow between grid and result — does not interfere with
        // taking items from the output slot
        reg.addRecipeClickArea(
                PhantomChestScreen.class,
                PhantomChestMenu.CRAFT_GRID_X + 54 + 2,
                PhantomChestMenu.CRAFT_GRID_Y + 18,
                20, 18,
                RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(
                new ItemStack(ModItems.PHANTOM_CHEST_SUMMONER.get()),
                RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new PhantomCraftingTransferInfo());
    }

    // ── Transfer info ─────────────────────────────────────────────────────────

    private static class PhantomCraftingTransferInfo
            implements IRecipeTransferInfo<PhantomChestMenu, RecipeHolder<CraftingRecipe>> {

        @Override
        public Class<? extends PhantomChestMenu> getContainerClass() {
            return PhantomChestMenu.class;
        }

        @Override
        public Optional<MenuType<PhantomChestMenu>> getMenuType() {
            return Optional.of(ModMenuTypes.PHANTOM_CHEST_MENU.get());
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public boolean canHandle(PhantomChestMenu container, RecipeHolder<CraftingRecipe> recipe) {
            return container.getActiveTab() == PhantomChestMenu.TAB_CRAFT;
        }

        @Nullable
        @Override
        public IRecipeTransferError getHandlingError(PhantomChestMenu container, RecipeHolder<CraftingRecipe> recipe) {
            return null;
        }

        @Override
        public List<Slot> getRecipeSlots(PhantomChestMenu container, RecipeHolder<CraftingRecipe> recipe) {
            // Crafting grid: slots 55–63
            return new ArrayList<>(container.slots.subList(
                    PhantomChestMenu.CRAFT_GRID_START,
                    PhantomChestMenu.FILTER_START));
        }

        @Override
        public List<Slot> getInventorySlots(PhantomChestMenu container, RecipeHolder<CraftingRecipe> recipe) {
            // Pull from chest inventory (0–53) AND player inventory (73–108)
            List<Slot> slots = new ArrayList<>();
            slots.addAll(container.slots.subList(0, PhantomChestMenu.CHEST_SIZE));
            slots.addAll(container.slots.subList(
                    PhantomChestMenu.PLAYER_INV_START,
                    PhantomChestMenu.TOTAL_SLOTS));
            return slots;
        }
    }
}
