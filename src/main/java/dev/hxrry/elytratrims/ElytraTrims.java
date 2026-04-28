package dev.hxrry.elytratrims;

import dev.hxrry.elytratrims.commands.ElytraTrimsCommand;
import dev.hxrry.elytratrims.config.Settings;
import dev.hxrry.elytratrims.listeners.CauldronListener;
import dev.hxrry.elytratrims.listeners.DyeListener;
import dev.hxrry.elytratrims.listeners.SmithingListener;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ElytraTrims extends JavaPlugin {

    private Settings settings;

    @Override
    public void onEnable() {
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        settings = new Settings(this);

        if (settings.isTrimsEnabled() || settings.hasAnyGatedEffect()) {
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

    private final java.util.List<NamespacedKey> registeredRecipes = new java.util.ArrayList<>();

    private void registerRecipes() {
        RecipeChoice elytraChoice = new RecipeChoice.MaterialChoice(Material.ELYTRA);
        ItemStack placeholder = new ItemStack(Material.ELYTRA);

        Settings settings = this.settings;

        if (settings.isTrimsEnabled()) {
            java.util.List<Material> trimTemplates = new java.util.ArrayList<>();
            for (Material mat : Material.values()) {
                if (mat.isItem() && mat.name().endsWith("_ARMOR_TRIM_SMITHING_TEMPLATE")) {
                    trimTemplates.add(mat);
                }
            }

            if (!trimTemplates.isEmpty()) {
                NamespacedKey key = new NamespacedKey(this, "elytra_specific_trim");
                SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                        key, placeholder,
                        new RecipeChoice.MaterialChoice(trimTemplates),
                        elytraChoice,
                        new RecipeChoice.MaterialChoice(
                                Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT,
                                Material.LAPIS_LAZULI, Material.EMERALD, Material.DIAMOND,
                                Material.NETHERITE_INGOT, Material.REDSTONE, Material.AMETHYST_SHARD,
                                Material.QUARTZ, Material.RESIN_BRICK
                        )
                );
                getServer().addRecipe(recipe);
                registeredRecipes.add(key);
            }
        }

        if (settings.isTrimsEnabled()) {
            NamespacedKey key = new NamespacedKey(this, "elytra_random_trim");
            SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                    key, placeholder,
                    new RecipeChoice.MaterialChoice(Material.TRIAL_KEY),
                    elytraChoice,
                    new RecipeChoice.MaterialChoice(
                            Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT,
                            Material.LAPIS_LAZULI, Material.EMERALD, Material.DIAMOND,
                            Material.NETHERITE_INGOT, Material.REDSTONE, Material.AMETHYST_SHARD,
                            Material.QUARTZ, Material.RESIN_BRICK
                    )
            );
            getServer().addRecipe(recipe);
            registeredRecipes.add(key);

            NamespacedKey rerollKey = new NamespacedKey(this, "elytra_random_trim_reroll");
            SmithingTransformRecipe rerollRecipe = new SmithingTransformRecipe(
                    rerollKey, placeholder,
                    new RecipeChoice.MaterialChoice(Material.OMINOUS_TRIAL_KEY),
                    elytraChoice,
                    new RecipeChoice.MaterialChoice(
                            Material.IRON_INGOT, Material.COPPER_INGOT, Material.GOLD_INGOT,
                            Material.LAPIS_LAZULI, Material.EMERALD, Material.DIAMOND,
                            Material.NETHERITE_INGOT, Material.REDSTONE, Material.AMETHYST_SHARD,
                            Material.QUARTZ, Material.RESIN_BRICK
                    )
            );
            getServer().addRecipe(rerollRecipe);
            registeredRecipes.add(rerollKey);
        }

        if (settings.isBannerPatternsEnabled()) {
            RecipeChoice bannerChoice = new RecipeChoice.MaterialChoice(
                    Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER,
                    Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER,
                    Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER,
                    Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
                    Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER,
                    Material.BLACK_BANNER
            );

            NamespacedKey bannerKey = new NamespacedKey(this, "elytra_banner_pattern");
            SmithingTransformRecipe bannerRecipe = new SmithingTransformRecipe(
                    bannerKey, placeholder, bannerChoice, elytraChoice,
                    new RecipeChoice.MaterialChoice(Material.PAPER)
            );
            getServer().addRecipe(bannerRecipe);
            registeredRecipes.add(bannerKey);

            NamespacedKey shieldKey = new NamespacedKey(this, "elytra_shield_pattern");
            SmithingTransformRecipe shieldRecipe = new SmithingTransformRecipe(
                    shieldKey, placeholder, bannerChoice, elytraChoice,
                    new RecipeChoice.MaterialChoice(Material.LEATHER)
            );
            getServer().addRecipe(shieldRecipe);
            registeredRecipes.add(shieldKey);
        }

        for (Settings.Effect effect : Settings.Effect.values()) {
            if (!settings.isEffectEnabled(effect)) continue;
            Settings.EffectConfig config = settings.getEffectConfig(effect);
            if (config == null) continue;

            NamespacedKey key = new NamespacedKey(this, "elytra_effect_" + effect.getConfigKey());
            SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                    key, placeholder,
                    RecipeChoice.empty(),
                    elytraChoice,
                    new RecipeChoice.MaterialChoice(config.ingredient())
            );
            getServer().addRecipe(recipe);
            registeredRecipes.add(key);
        }

        getLogger().info("Registered " + registeredRecipes.size() + " smithing recipes.");
    }

    public Settings getSettings() {
        return settings;
    }

    public void reload() {
        reloadConfig();
        settings = new Settings(this);
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