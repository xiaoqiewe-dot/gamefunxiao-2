package org.gamefunxiao.cosmetics;

import org.bukkit.Material;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum LuckyPillarsVictoryEffect {
    FIREWORKS("fireworks", "默认烟花", Material.FIREWORK_ROCKET, 0,
            "§f- §a幸运之柱默认胜利特效",
            "§f- §b会在场地中心炸开庆祝烟花"),
    GOLDEN_PILLAR("golden_pillar", "金柱升辉", Material.GOLD_BLOCK, 780,
            "§f- §6中心升起金色光柱",
            "§f- §e像幸运值直接拉满"),
    CLOVER_RING("clover_ring", "四叶环光", Material.LIME_DYE, 820,
            "§f- §a绿色环光围着中心旋转",
            "§f- §b适合幸运之柱主题"),
    SKY_GIFT("sky_gift", "天降礼盒", Material.CHEST, 900,
            "§f- §e像奖励从天上砸下来",
            "§f- §6带一点开彩蛋的感觉"),
    VOID_LOTUS("void_lotus", "虚空莲爆", Material.OBSIDIAN, 980,
            "§f- §5黑紫粒子向外开花",
            "§f- §8有一点危险的幸运感"),
    HONEY_SPLASH("honey_splash", "蜜糖飞溅", Material.HONEYCOMB, 760,
            "§f- §6金黄粒子啪一下散开",
            "§f- §e轻快又亮眼"),
    PRISM_COLUMN("prism_column", "虹晶柱潮", Material.PRISMARINE_CRYSTALS, 940,
            "§f- §b彩晶粒子沿着中心向上冲",
            "§f- §d非常适合场地中央展示"),
    TOTEM_GARDEN("totem_garden", "图腾花园", Material.TOTEM_OF_UNDYING, 1060,
            "§f- §e图腾与花火一起绽放",
            "§f- §a胜利感会更强");

    private final String id;
    private final String defaultName;
    private final Material material;
    private final int defaultPrice;
    private final List<String> defaultLore;

    LuckyPillarsVictoryEffect(String id, String defaultName, Material material, int defaultPrice, String... defaultLore) {
        this.id = id;
        this.defaultName = defaultName;
        this.material = material;
        this.defaultPrice = defaultPrice;
        this.defaultLore = Arrays.asList(defaultLore);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName(GameFunXiao plugin) {
        return plugin.getConfigManager().getConfig().getString("shop.lucky_pillars_victory_effects." + id + ".name", defaultName);
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrice(GameFunXiao plugin) {
        return Math.max(0, plugin.getConfigManager().getConfig().getInt("shop.lucky_pillars_victory_effects." + id + ".price", defaultPrice));
    }

    public List<String> getDescription(GameFunXiao plugin) {
        List<String> configured = plugin.getConfigManager().getConfig().getStringList("shop.lucky_pillars_victory_effects." + id + ".lore");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return new ArrayList<>(defaultLore);
    }

    public static LuckyPillarsVictoryEffect byId(String id) {
        if (id == null || id.isBlank()) {
            return FIREWORKS;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (LuckyPillarsVictoryEffect effect : values()) {
            if (effect.id.equals(normalized)) {
                return effect;
            }
        }
        return FIREWORKS;
    }
}
