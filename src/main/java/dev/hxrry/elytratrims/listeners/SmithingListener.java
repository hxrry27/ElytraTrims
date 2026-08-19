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

    private List<TrimPattern> trimPatterns;

    private int seed = new Random().nextInt();

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
            return RecipeMatch.none();
        }

        if (settings.isTrimsEnabled() && findTrimMaterial(addition) != null) {
            if (template.getType() == Material.TRIAL_KEY) return RecipeMatch.of(RecipeType.RANDOM_TRIM);
            if (template.getType() == Material.OMINOUS_TRIAL_KEY) return RecipeMatch.of(RecipeType.RANDOM_TRIM_REROLL);
        }

        if (settings.isBannerPatternsEnabled() && Tag.BANNERS.isTagged(template.getType())) {
            if (addition.getType() == Material.PAPER) return RecipeMatch.of(RecipeType.BANNER_PATTERN);
            if (addition.getType() == Material.LEATHER) return RecipeMatch.of(RecipeType.SHIELD_PATTERN);
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
        if (match.type() == RecipeType.NONE) {
            gateVanillaTrim(event, base);
            return;
        }

        Player player = getViewingPlayer(event.getViewers());
        if (player != null && !hasPermission(player, match)) {
            event.setResult(null);
            return;
        }

        // our recipes result in a bare elytra, so anything short of a real result has to be
        // cleared — otherwise the player trades an enchanted elytra for a fresh one
        event.setResult(buildResult(match, template, base, addition));
    }

    private void gateVanillaTrim(PrepareSmithingEvent event, ItemStack base) {
        if (!ElytraData.isElytra(base)) return;

        ItemStack result = event.getResult();
        if (result == null || !result.hasData(DataComponentTypes.TRIM)) return;

        if (!plugin.getSettings().isTrimsEnabled()) {
            event.setResult(null);
            return;
        }

        Player player = getViewingPlayer(event.getViewers());
        if (player != null && !player.hasPermission("elytratrims.craft.trim")) {
            event.setResult(null);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof SmithingInventory inv)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack result = inv.getResult();
        if (result == null || result.getType().isAir()) return;

        seed = new Random(seed).nextInt();
    }

    // ── Result Building ──

    private ItemStack buildResult(RecipeMatch match, ItemStack template, ItemStack base, ItemStack addition) {
        ItemStack result = base.clone();
        result.setAmount(1);

        boolean applied = switch (match.type()) {
            case RANDOM_TRIM -> applyRandomTrim(result, addition, true);
            case RANDOM_TRIM_REROLL -> applyRandomTrim(result, addition, false);
            case BANNER_PATTERN -> applyBannerPatterns(result, template, true);
            case SHIELD_PATTERN -> applyBannerPatterns(result, template, false);
            case GATED_EFFECT -> {
                ElytraData.setEffect(result, match.effect(), true);
                yield true;
            }
            default -> false;
        };

        return applied ? result : null;
    }

    private boolean applyRandomTrim(ItemStack elytra, ItemStack addition, boolean consistent) {
        List<TrimPattern> patterns = getTrimPatterns();
        if (patterns.isEmpty()) return false;

        TrimMaterial material = findTrimMaterial(addition);
        if (material == null) return false;

        ArmorTrim current = null;
        if (ElytraData.hasTrim(elytra)) {
            ItemArmorTrim existing = ElytraData.getTrim(elytra);
            if (existing != null) current = existing.armorTrim();
        }

        // seeded for the trial key so every player gets the same pattern until the seed advances,
        // fresh for the ominous key so it actually rerolls
        Random rng = consistent ? new Random(seed) : new Random();

        TrimPattern chosen = patterns.get(rng.nextInt(patterns.size()));
        for (int attempt = 0; attempt < 9 && current != null; attempt++) {
            if (!chosen.equals(current.getPattern()) || !material.equals(current.getMaterial())) break;
            chosen = patterns.get(rng.nextInt(patterns.size()));
        }

        ElytraData.setTrim(elytra, ItemArmorTrim.itemArmorTrim(new ArmorTrim(material, chosen)).build());
        return true;
    }

    private boolean applyBannerPatterns(ItemStack elytra, ItemStack banner, boolean bannerStyle) {
        if (banner == null) return false;

        BannerPatternLayers patterns = banner.getData(DataComponentTypes.BANNER_PATTERNS);
        if (patterns == null || patterns.patterns().isEmpty()) return false;

        ElytraData.setBannerPatterns(elytra, patterns);
        // banner (paper) -> entity/wings/banner textures; shield (leather) -> entity/wings/shield
        ElytraData.setBannerFlag(elytra, bannerStyle);
        return true;
    }

    // ── Permission Checking ──

    private boolean hasPermission(Player player, RecipeMatch match) {
        Settings settings = plugin.getSettings();

        return switch (match.type()) {
            case RANDOM_TRIM, RANDOM_TRIM_REROLL -> player.hasPermission("elytratrims.craft.trim");
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

    private TrimMaterial findTrimMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return item.getData(DataComponentTypes.PROVIDES_TRIM_MATERIAL);
    }
}
