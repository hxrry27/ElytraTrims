package dev.hxrry.elytratrims.listeners;

import dev.hxrry.elytratrims.ElytraTrims;
import dev.hxrry.elytratrims.component.ElytraData;
import dev.hxrry.elytratrims.config.Settings;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(hand);

        if (!ElytraData.isElytra(item)) return;
        if (!player.hasPermission("elytratrims.cauldron")) return;

        if (!wash(item)) return;

        player.getInventory().setItem(hand, item);
        player.incrementStatistic(Statistic.ARMOR_CLEANED);

        // only costs water when something actually came off
        if (block.getBlockData() instanceof Levelled levelled) {
            int level = levelled.getLevel();
            if (level <= 1) {
                block.setType(Material.CAULDRON);
            } else {
                levelled.setLevel(level - 1);
                block.setBlockData(levelled);
            }
        }

        event.setCancelled(true);
        player.sendMessage(plugin.getSettings().getEffectRemovedMessage());
    }

    private boolean wash(ItemStack elytra) {
        boolean cleaned = false;

        if (ElytraData.hasDye(elytra)) {
            ElytraData.removeDye(elytra);
            cleaned = true;
        }
        if (ElytraData.hasBannerPatterns(elytra)) {
            ElytraData.removeBannerPatterns(elytra);
            ElytraData.setBannerFlag(elytra, false);
            cleaned = true;
        }
        if (ElytraData.hasEffect(elytra, Settings.Effect.GLOW)) {
            ElytraData.setEffect(elytra, Settings.Effect.GLOW, false);
            cleaned = true;
        }

        return cleaned;
    }
}
