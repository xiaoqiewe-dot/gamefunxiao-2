package org.gamefunxiao.cosmetics;

import org.bukkit.Material;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum HunterKillEffect {
    NONE("none", "没有", Material.BARRIER, 0,
            "§f- §7不播放任何击杀粒子与音效",
            "§f- §8适合喜欢干净战斗反馈的人"),
    FIREWORK_BURST("firework_burst", "烟花爆点", Material.FIREWORK_ROCKET, 600,
            "§f- §a死亡点炸开一圈烟花色粒子",
            "§f- §b并给附近玩家一个清脆庆祝音效"),
    SOUL_BREAK("soul_break", "灵魂碎裂", Material.SOUL_LANTERN, 720,
            "§f- §3灵魂火花从死亡点散开",
            "§f- §b适合近战收割的反馈"),
    THUNDER_MARK("thunder_mark", "雷鸣标记", Material.LIGHTNING_ROD, 880,
            "§f- §e落雷感粒子和低音雷鸣",
            "§f- §6击杀瞬间存在感很强"),
    BLOOD_BLOOM("blood_bloom", "猩红绽放", Material.REDSTONE, 760,
            "§f- §c红色粒子在死亡点绽开",
            "§f- §6适合强节奏追杀玩法"),
    VOID_CRACK("void_crack", "虚空裂痕", Material.OBSIDIAN, 980,
            "§f- §8黑紫裂痕在死亡点张开",
            "§f- §5像被虚空瞬间撕开"),
    FROST_BITE("frost_bite", "霜寒咬痕", Material.PACKED_ICE, 820,
            "§f- §b冰晶和雪雾在死亡点炸散",
            "§f- §f带一点寒冷收割感"),
    SOLAR_FLARE("solar_flare", "炽阳耀斑", Material.BLAZE_POWDER, 930,
            "§f- §6金橙火花快速爆闪",
            "§f- §e适合高调终结"),
    WITCH_CURSE("witch_curse", "女巫诅咒", Material.SPIDER_EYE, 840,
            "§f- §5诅咒药雾和魔法粒子扩散",
            "§f- §d很像被阴了一手"),
    CHORUS_SHATTER("chorus_shatter", "紫颂破碎", Material.CHORUS_FRUIT, 910,
            "§f- §d紫颂传送残影和碎光同时出现",
            "§f- §5末地风格很浓"),
    TOTEM_COLLAPSE("totem_collapse", "图腾塌陷", Material.TOTEM_OF_UNDYING, 1080,
            "§f- §e图腾色粒子先亮后碎",
            "§f- §6像把最后的生机直接打碎"),
    ECHO_PULSE("echo_pulse", "回响脉冲", Material.ECHO_SHARD, 960,
            "§f- §3幽深脉冲从死亡点震开",
            "§f- §b适合神秘风格击杀"),
    ROSE_FUNERAL("rose_funeral", "凋零玫痕", Material.WITHER_ROSE, 870,
            "§f- §8黑雾夹着暗红花粉绽开",
            "§f- §c带一点危险优雅感");

    private final String id;
    private final String defaultName;
    private final Material material;
    private final int defaultPrice;
    private final List<String> defaultLore;

    HunterKillEffect(String id, String defaultName, Material material, int defaultPrice, String... defaultLore) {
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
        return plugin.getConfigManager().getConfig().getString("shop.hunter_kill_effects." + id + ".name", defaultName);
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrice(GameFunXiao plugin) {
        return Math.max(0, plugin.getConfigManager().getConfig().getInt("shop.hunter_kill_effects." + id + ".price", defaultPrice));
    }

    public List<String> getDescription(GameFunXiao plugin) {
        List<String> configured = plugin.getConfigManager().getConfig().getStringList("shop.hunter_kill_effects." + id + ".lore");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return new ArrayList<>(defaultLore);
    }

    public static HunterKillEffect byId(String id) {
        if (id == null || id.isBlank()) {
            return NONE;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (HunterKillEffect effect : values()) {
            if (effect.id.equals(normalized)) {
                return effect;
            }
        }
        return NONE;
    }
}
