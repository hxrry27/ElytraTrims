package dev.hxrry.elytratrims.listeners;

import dev.hxrry.elytratrims.ElytraTrims;
import dev.hxrry.elytratrims.component.ElytraData;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class DyeListener implements Listener {

    @SuppressWarnings("unused")
    private final ElytraTrims plugin;

    public DyeListener(ElytraTrims plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();

        ItemStack result = inv.getResult();
        if (result == null || !ElytraData.isElytra(result)) return;
        if (!ElytraData.hasDye(result)) return;

        boolean dyed = false;
        for (ItemStack item : inv.getMatrix()) {
            if (item != null && Tag.ITEMS_DYES.isTagged(item.getType())) {
                dyed = true;
                break;
            }
        }
        if (!dyed) return;

        if (event.getView().getPlayer() instanceof Player player
                && !player.hasPermission("elytratrims.craft.dye")) {
            inv.setResult(null);
        }
    }
}
