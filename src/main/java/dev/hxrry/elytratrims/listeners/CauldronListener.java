package dev.hxrry.elytratrims.listeners;

import dev.hxrry.elytratrims.ElytraTrims;
import dev.hxrry.elytratrims.component.ElytraData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CauldronListener implements Listener {

    private final ElytraTrims plugin;

    public CauldronListener(ElytraTrims plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!ElytraData.isElytra(item)) return;
        if (!ElytraData.hasAnyDecoration(item)) return;

        // permission check
        if (!player.hasPermission("elytratrims.cauldron")) return;

        // strip all decorations
        ElytraData.clearAllDecorations(item);
        player.getInventory().setItemInMainHand(item);

        // lower cauldron water level
        if (block.getBlockData() instanceof Levelled levelled) {
            int level = levelled.getLevel();
            if (level <= 1) {
                // empty the cauldron
                block.setType(Material.CAULDRON);
            } else {
                levelled.setLevel(level - 1);
                block.setBlockData(levelled);
            }
        }

        // cancel to prevent vanilla cauldron interaction
        event.setCancelled(true);

        player.sendMessage(plugin.getSettings().getEffectRemovedMessage());
    }
}