package org.gamefunxiao.game;

import org.bukkit.Material;

import java.util.List;

public enum NetherHunterScenario {
    CAVE(
            "cave",
            "矿洞组",
            Material.DEEPSLATE_IRON_ORE,
            50,
            List.of("更容易刷到地下矿洞落点", "偏向矿物、铁铜装备与挖矿物资")
    ),
    CHASE(
            "chase",
            "追击组",
            Material.IRON_SWORD,
            20,
            List.of("更靠近猎物刷新", "偏向近战追击、弓箭与机动资源")
    ),
    WOOD(
            "wood",
            "伐木组",
            Material.IRON_AXE,
            30,
            List.of("更容易出现在地表树林附近", "偏向木头、斧头与基础建材")
    ),
    TRIAL(
            "trial",
            "试练大厅组",
            Material.CHISELED_TUFF_BRICKS,
            60,
            List.of("会尽量传进试练大厅附近", "偏向铁套、不祥药水与更强武器")
    ),
    NETHER(
            "nether",
            "下界安全点",
            Material.NETHERRACK,
            10,
            List.of("会直接刷新在下界安全位置", "偏向抗火、黑石与更强近战配置")
    ),
    ANCIENT_CITY(
            "ancient_city",
            "古城组",
            Material.SCULK_CATALYST,
            3,
            List.of("极低概率刷在古城附近", "额外带附魔台、书架、甘蔗、皮革与钻锄")
    );

    private final String id;
    private final String displayName;
    private final Material icon;
    private final int baseWeight;
    private final List<String> description;

    NetherHunterScenario(String id, String displayName, Material icon, int baseWeight, List<String> description) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.baseWeight = baseWeight;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public List<String> getDescription() {
        return description;
    }
}
