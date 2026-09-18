package com.phantomstorage.events;

import com.phantomstorage.ModEntities;
import com.phantomstorage.PhantomStorageMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@EventBusSubscriber(modid = PhantomStorageMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    /**
     * Exposes the chest's 54-slot main inventory as a standard IItemHandler, so inventory
     * sorting/organizing mods (Inventory Tweaks, Inventory Profiles Next, and similar) that key
     * off the NeoForge capability system — rather than only doing GUI-heuristic shift-clicks —
     * recognize the Phantom Chest as a valid storage to sort. Wraps the entity's own
     * VoidFilterContainer directly, so external insertion still respects the void filter.
     * Deliberately scoped to the main inventory only — the crafting grid, void filter, and
     * refill slots are special-purpose and not meant for external sorting.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                Capabilities.ItemHandler.ENTITY,
                ModEntities.PHANTOM_CHEST.get(),
                (chest, context) -> new InvWrapper(chest.getInventory()));
    }
}
