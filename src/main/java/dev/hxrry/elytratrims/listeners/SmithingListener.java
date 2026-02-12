package dev.hxrry.elytratrims.listeners;

import dev.hxrry.elytratrims.ElytraTrims;
import dev.hxrry.elytratrims.component.ElytraData;
import dev.hxrry.elytratrims.config.Settings;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SmithingListener implements Listener {

    private final ElytraTrims plugin;
    private final Random random = new Random();

    private List<TrimPattern> trimPatterns;

    public SmithingListener(ElytraTrims plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    private List<TrimPattern> getTrimPatterns() {
        if (trimPatterns == null) {
            trimPatterns = new ArrayList<>();
            Registry.TRIM_PATTERN.forEach(trimPatterns::add);
        }
        return trimPatterns;
    }

    // ── Recipe Detection ──

    private enum RecipeType {
        SPECIFIC_TRIM,
        RANDOM_TRIM,
        RANDOM_TRIM_REROLL,
        BANNER_PATTERN,
        SHIELD_PATTERN,
        GATED_EFFECT,
        NONE
    }

    private record RecipeMatch(RecipeType type, Settings.Effect effect) {
        static RecipeMatch none() { return new RecipeMatch(RecipeType.NONE, null); }
        static RecipeMatch of(RecipeType type) { return new RecipeMatch(type, null); }
        static RecipeMatch gated(Settings.Effect effect) { return new RecipeMatch(RecipeType.GATED_EFFECT, effect); }
    }

    private RecipeMatch detectRecipe(ItemStack template, ItemStack base, ItemStack addition) {
        if (!ElytraData.isElytra(base)) return RecipeMatch.none();
        if (addition == null || addition.getType().isAir()) return RecipeMatch.none();

        Settings settings = plugin.getSettings();

        if (template == null || template.getType().isAir()) {
            Settings.Effect effect = settings.getEffectByIngredient(addition.getType());
            if (effect != null && settings.isEffectEnabled(effect)) {
                return RecipeMatch.gated(effect);
            }
        }

        if (template != null && template.getType() == Material.TRIAL_KEY && isTrimMaterial(addition)) {
            if (settings.isTrimsEnabled()) return RecipeMatch.of(RecipeType.RANDOM_TRIM);
        }

        if (template != null && template.getType() == Material.OMINOUS_TRIAL_KEY && isTrimMaterial(addition)) {
            if (settings.isTrimsEnabled()) return RecipeMatch.of(RecipeType.RANDOM_TRIM_REROLL);
        }

        if (template != null && isTrimTemplate(template.getType()) && isTrimMaterial(addition)) {
            if (settings.isTrimsEnabled()) return RecipeMatch.of(RecipeType.SPECIFIC_TRIM);
        }

        if (template != null && isBanner(template.getType()) && addition.getType() == Material.PAPER) {
            if (settings.isBannerPatternsEnabled()) return RecipeMatch.of(RecipeType.BANNER_PATTERN);
        }

        if (template != null && isBanner(template.getType()) && addition.getType() == Material.LEATHER) {
            if (settings.isBannerPatternsEnabled()) return RecipeMatch.of(RecipeType.SHIELD_PATTERN);
        }

        return RecipeMatch.none();
    }

    // ── PrepareSmithingEvent: Build result preview ──

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingInventory inv = event.getInventory();

        ItemStack template = inv.getInputTemplate();
        ItemStack base = inv.getInputEquipment();
        ItemStack addition = inv.getInputMineral();

        RecipeMatch match = detectRecipe(template, base, addition);
        if (match.type() == RecipeType.NONE) return;

        Player player = getViewingPlayer(event.getViewers());
        if (player == null) return;

        if (!hasPermission(player, match)) {
            event.setResult(null);
            return;
        }

        ItemStack result = buildResult(match, template, base, addition);
        if (result != null) {
            event.setResult(result);
        }
    }

    // ── InventoryClickEvent: Handle ingredient consumption ──

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof SmithingInventory inv)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = inv.getResult();
        if (result == null || result.getType().isAir()) return;

        ItemStack template = inv.getInputTemplate();
        ItemStack base = inv.getInputEquipment();
        ItemStack addition = inv.getInputMineral();

        RecipeMatch match = detectRecipe(template, base, addition);
        if (match.type() == RecipeType.NONE) return;

        if (!hasPermission(player, match)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getSettings().getNoPermissionMessage());
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            inv.setItem(1, null);

            consumeOne(inv, 2);

            if (match.type() == RecipeType.SPECIFIC_TRIM
                    || match.type() == RecipeType.RANDOM_TRIM
                    || match.type() == RecipeType.RANDOM_TRIM_REROLL
                    || match.type() == RecipeType.BANNER_PATTERN
                    || match.type() == RecipeType.SHIELD_PATTERN) {
                consumeOne(inv, 0);
            }

            inv.setResult(null);
        });
    }

    // ── Result Building ──

    private ItemStack buildResult(RecipeMatch match, ItemStack template, ItemStack base, ItemStack addition) {
        ItemStack result = base.clone();
        result.setAmount(1);

        switch (match.type()) {
            case SPECIFIC_TRIM -> applySpecificTrim(result, template, addition);
            case RANDOM_TRIM -> applyRandomTrim(result, addition, true);
            case RANDOM_TRIM_REROLL -> applyRandomTrim(result, addition, false);
            case BANNER_PATTERN -> applyBannerPatterns(result, template);
            case SHIELD_PATTERN -> applyBannerPatterns(result, template);
            case GATED_EFFECT -> ElytraData.setEffect(result, match.effect(), true);
            default -> { return null; }
        }

        return result;
    }

    private void applySpecificTrim(ItemStack elytra, ItemStack template, ItemStack addition) {
        TrimMaterial material = findTrimMaterial(addition);
        if (material == null) return;

        TrimPattern pattern = findTrimPatternForTemplate(template.getType());
        if (pattern == null) return;

        ArmorTrim trim = new ArmorTrim(material, pattern);
        ElytraData.setTrim(elytra, ItemArmorTrim.itemArmorTrim(trim).build());
    }

    private void applyRandomTrim(ItemStack elytra, ItemStack addition, boolean consistent) {
        List<TrimPattern> patterns = getTrimPatterns();
        if (patterns.isEmpty()) return;

        TrimMaterial material = findTrimMaterial(addition);
        if (material == null) return;

        TrimPattern currentPattern = null;
        if (ElytraData.hasTrim(elytra)) {
            ItemArmorTrim existing = ElytraData.getTrim(elytra);
            if (existing != null) {
                currentPattern = existing.armorTrim().getPattern();
            }
        }

        TrimPattern chosen;
        if (patterns.size() == 1) {
            chosen = patterns.getFirst();
        } else {
            int attempts = 0;
            do {
                chosen = patterns.get(random.nextInt(patterns.size()));
                attempts++;
            } while (chosen.equals(currentPattern) && attempts < 20);
        }

        ArmorTrim trim = new ArmorTrim(material, chosen);
        ElytraData.setTrim(elytra, ItemArmorTrim.itemArmorTrim(trim).build());
    }

    private void applyBannerPatterns(ItemStack elytra, ItemStack banner) {
        if (banner == null) return;

        BannerPatternLayers patterns = banner.getData(DataComponentTypes.BANNER_PATTERNS);
        if (patterns != null) {
            ElytraData.setBannerPatterns(elytra, patterns);
        }
    }

    // ── Permission Checking ──

    private boolean hasPermission(Player player, RecipeMatch match) {
        Settings settings = plugin.getSettings();

        return switch (match.type()) {
            case SPECIFIC_TRIM, RANDOM_TRIM, RANDOM_TRIM_REROLL -> player.hasPermission("elytratrims.craft.trim");
            case BANNER_PATTERN, SHIELD_PATTERN -> player.hasPermission("elytratrims.craft.pattern");
            case GATED_EFFECT -> {
                Settings.EffectConfig config = settings.getEffectConfig(match.effect());
                yield config != null && player.hasPermission(config.permission());
            }
            default -> true;
        };
    }

    // ── Utility Methods ──

    private Player getViewingPlayer(List<HumanEntity> viewers) {
        for (HumanEntity viewer : viewers) {
            if (viewer instanceof Player p) return p;
        }
        return null;
    }

    private boolean isTrimMaterial(ItemStack item) {
        return findTrimMaterial(item) != null;
    }

    @SuppressWarnings("deprecation")
    private TrimMaterial findTrimMaterial(ItemStack item) {
        if (item == null) return null;
        for (TrimMaterial material : Registry.TRIM_MATERIAL) {
            if (matchesTrimMaterial(item.getType(), material)) {
                return material;
            }
        }
        return null;
    }

    private boolean matchesTrimMaterial(Material material, TrimMaterial trimMaterial) {
        @SuppressWarnings("removal")
        String key = trimMaterial.getKey().getKey();
        return switch (key) {
            case "iron" -> material == Material.IRON_INGOT;
            case "copper" -> material == Material.COPPER_INGOT;
            case "gold" -> material == Material.GOLD_INGOT;
            case "lapis" -> material == Material.LAPIS_LAZULI;
            case "emerald" -> material == Material.EMERALD;
            case "diamond" -> material == Material.DIAMOND;
            case "netherite" -> material == Material.NETHERITE_INGOT;
            case "redstone" -> material == Material.REDSTONE;
            case "amethyst" -> material == Material.AMETHYST_SHARD;
            case "quartz" -> material == Material.QUARTZ;
            case "resin" -> material == Material.RESIN_BRICK;
            default -> false;
        };
    }

    private boolean isBanner(Material material) {
        return Tag.BANNERS.isTagged(material);
    }

    private boolean isTrimTemplate(Material material) {
        return material.name().endsWith("_ARMOR_TRIM_SMITHING_TEMPLATE");
    }

    @SuppressWarnings({ "deprecation", "removal" })
    private TrimPattern findTrimPatternForTemplate(Material template) {
        String name = template.name(); // e.g. WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE
        String patternKey = name.replace("_ARMOR_TRIM_SMITHING_TEMPLATE", "").toLowerCase();
        for (TrimPattern pattern : Registry.TRIM_PATTERN) {
            if (pattern.getKey().getKey().equals(patternKey)) {
                return pattern;
            }
        }
        return null;
    }

    private void consumeOne(SmithingInventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType().isAir()) return;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            inv.setItem(slot, item);
        } else {
            inv.setItem(slot, null);
        }
    }
}