package org.gamefunxiao.cosmetics;

import org.bukkit.Material;
import org.gamefunxiao.GameFunXiao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum HunterVictoryEffect {
    FIREWORKS("fireworks", "默认烟花", Material.FIREWORK_ROCKET, 0,
            "§f- §a经典烟花庆祝，默认拥有",
            "§f- §b适合所有猎人收尾场面"),
    BLACK_HOLE("black_hole", "黑洞", Material.BLACK_CONCRETE, 1200,
            "§f- §8生成巨大的黑色核心",
            "§f- §d周围粒子会被吸入中心"),
    STAR_RAIN("star_rain", "星雨", Material.AMETHYST_SHARD, 900,
            "§f- §b天空降下星光雨",
            "§f- §d末地感很强的胜利收束"),
    DRAGON_BREATH_BLOOM("dragon_breath_bloom", "龙息绽放", Material.DRAGON_BREATH, 1000,
            "§f- §d龙息环形爆开",
            "§f- §5像击败巨龙后的能量花"),
    THUNDER_CROWN("thunder_crown", "雷霆王冠", Material.LIGHTNING_ROD, 1100,
            "§f- §e雷光围成王冠",
            "§f- §6适合强势终结猎物"),
    SOUL_VORTEX("soul_vortex", "灵魂漩涡", Material.SOUL_LANTERN, 950,
            "§f- §3灵魂火焰旋转升起",
            "§f- §b幽蓝粒子包围战场"),
    AURORA_SPIRAL("aurora_spiral", "极光螺旋", Material.PRISMARINE_CRYSTALS, 880,
            "§f- §b青紫极光向上盘旋",
            "§f- §d柔和但很显眼的结算特效"),
    CRYSTAL_BLOOM("crystal_bloom", "晶簇绽放", Material.AMETHYST_CLUSTER, 760,
            "§f- §d紫晶光点向四周绽放",
            "§f- §f干净、亮眼、不遮视野");

    private final String id;
    private final String defaultName;
    private final Material material;
    private final int defaultPrice;
    private final List<String> defaultLore;

    HunterVictoryEffect(String id, String defaultName, Material material, int defaultPrice, String... defaultLore) {
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
        return plugin.getConfigManager().getConfig().getString("shop.hunter_victory_effects." + id + ".name", defaultName);
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrice(GameFunXiao plugin) {
        return Math.max(0, plugin.getConfigManager().getConfig().getInt("shop.hunter_victory_effects." + id + ".price", defaultPrice));
    }

    public List<String> getDescription(GameFunXiao plugin) {
        List<String> configured = plugin.getConfigManager().getConfig().getStringList("shop.hunter_victory_effects." + id + ".lore");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return new ArrayList<>(defaultLore);
    }

    public static HunterVictoryEffect byId(String id) {
        if (id == null || id.isBlank()) {
            return FIREWORKS;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (HunterVictoryEffect effect : values()) {
            if (effect.id.equals(normalized)) {
                return effect;
            }
        }
        return FIREWORKS;
    }
}
