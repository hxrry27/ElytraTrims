package dev.hxrry.elytratrims;

import dev.hxrry.elytratrims.commands.ElytraTrimsCommand;
import dev.hxrry.elytratrims.config.Settings;
import dev.hxrry.elytratrims.listeners.CauldronListener;
import dev.hxrry.elytratrims.listeners.DyeListener;
import dev.hxrry.elytratrims.listeners.SmithingListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ElytraTrims extends JavaPlugin {

    private Settings settings;
    private boolean datapackActive;

    @Override
    public void onEnable() {
        //NBT API 2.15.7 *does* support 26.2 functionally, have tested
        saveDefaultConfig();
        settings = new Settings(this);

        // the datapack decides whether trims and dyeing work at all
        checkDatapack();

        // SmithingListener owns every recipe below, so it has to cover all of them
        if (settings.isTrimsEnabled() || settings.isBannerPatternsEnabled() || settings.hasAnyGatedEffect()) {
            getServer().getPluginManager().registerEvents(new SmithingListener(this), this);
        }
        if (settings.isDyeingEnabled()) {
            getServer().getPluginManager().registerEvents(new DyeListener(this), this);
        }
        if (settings.isCauldronWashingEnabled()) {
            getServer().getPluginManager().registerEvents(new CauldronListener(this), this);
        }

        registerRecipes();

        registerCommand();

        getLogger().info("ElytraTrims enabled.");
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : registeredRecipes) {
            getServer().removeRecipe(key);
        }
        getLogger().info("ElytraTrims disabled.");
    }

    // datapack shenanigans
    private void checkDatapack() {
        datapackActive = Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(Material.ELYTRA);
        if (!datapackActive) {
            getLogger().warning("The ElytraTrims datapack is not loaded, so elytra trims and dyeing "
                    + "will not work. If it was turned off, re-enable it with: "
                    + "/datapack enable \"file/elytratrims\"");
        }
    }

    public boolean isDatapackActive() {
        return datapackActive;
    }

    // recipe shenanigans

    private final java.util.List<NamespacedKey> registeredRecipes = new java.util.ArrayList<>();

    private void registerRecipes() {
        RecipeChoice elytraChoice = new RecipeChoice.MaterialChoice(Material.ELYTRA);
        ItemStack placeholder = new ItemStack(Material.ELYTRA);

        Settings settings = this.settings;

        if (settings.isTrimsEnabled()) {
            RecipeChoice trimMaterials = new RecipeChoice.MaterialChoice(Tag.ITEMS_TRIM_MATERIALS);

            NamespacedKey key = new NamespacedKey(this, "elytra_random_trim");
            getServer().addRecipe(new SmithingTransformRecipe(
                    key, placeholder,
                    new RecipeChoice.MaterialChoice(Material.TRIAL_KEY),
                    elytraChoice,
                    trimMaterials
            ));
            registeredRecipes.add(key);

            NamespacedKey rerollKey = new NamespacedKey(this, "elytra_random_trim_reroll");
            getServer().addRecipe(new SmithingTransformRecipe(
                    rerollKey, placeholder,
                    new RecipeChoice.MaterialChoice(Material.OMINOUS_TRIAL_KEY),
                    elytraChoice,
                    trimMaterials
            ));
            registeredRecipes.add(rerollKey);
        }

        if (settings.isBannerPatternsEnabled()) {
            RecipeChoice bannerChoice = new RecipeChoice.MaterialChoice(Tag.ITEMS_BANNERS);

            NamespacedKey bannerKey = new NamespacedKey(this, "elytra_banner_pattern");
            getServer().addRecipe(new SmithingTransformRecipe(
                    bannerKey, placeholder, bannerChoice, elytraChoice,
                    new RecipeChoice.MaterialChoice(Material.PAPER)
            ));
            registeredRecipes.add(bannerKey);

            NamespacedKey shieldKey = new NamespacedKey(this, "elytra_shield_pattern");
            getServer().addRecipe(new SmithingTransformRecipe(
                    shieldKey, placeholder, bannerChoice, elytraChoice,
                    new RecipeChoice.MaterialChoice(Material.LEATHER)
            ));
            registeredRecipes.add(shieldKey);
        }

        for (Settings.Effect effect : Settings.Effect.values()) {
            if (!settings.isEffectEnabled(effect)) continue;
            Settings.EffectConfig config = settings.getEffectConfig(effect);
            if (config == null) continue;

            NamespacedKey key = new NamespacedKey(this, "elytra_effect_" + effect.getConfigKey());
            getServer().addRecipe(new SmithingTransformRecipe(
                    key, placeholder,
                    RecipeChoice.empty(),
                    elytraChoice,
                    new RecipeChoice.MaterialChoice(config.ingredient())
            ));
            registeredRecipes.add(key);
        }

        getLogger().info("Registered " + registeredRecipes.size() + " smithing recipes.");
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean reload() {
        Settings previous = settings;
        reloadConfig();
        settings = new Settings(this);

        return previous.isTrimsEnabled() != settings.isTrimsEnabled()
                || previous.isDyeingEnabled() != settings.isDyeingEnabled()
                || previous.isBannerPatternsEnabled() != settings.isBannerPatternsEnabled()
                || previous.isCauldronWashingEnabled() != settings.isCauldronWashingEnabled()
                || effectsChanged(previous, settings);
    }

    private boolean effectsChanged(Settings previous, Settings current) {
        for (Settings.Effect effect : Settings.Effect.values()) {
            Settings.EffectConfig before = previous.getEffectConfig(effect);
            Settings.EffectConfig after = current.getEffectConfig(effect);
            if (before == null || after == null) {
                if (before != after) return true;
                continue;
            }
            if (before.enabled() != after.enabled() || before.ingredient() != after.ingredient()) return true;
        }
        return false;
    }

    private void registerCommand() {
        ElytraTrimsCommand handler = new ElytraTrimsCommand(this);
        Command command = new Command("elytratrims", "ElytraTrims admin commands", "/elytratrims", List.of("et")) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return handler.onCommand(sender, this, label, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                List<String> result = handler.onTabComplete(sender, this, alias, args);
                return result == null ? List.of() : result;
            }
        };
        command.setPermission("elytratrims.admin");
        getServer().getCommandMap().register("elytratrims", command);
    }
}
