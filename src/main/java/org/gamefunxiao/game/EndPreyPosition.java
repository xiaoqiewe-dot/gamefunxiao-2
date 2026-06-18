package org.gamefunxiao.game;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum EndPreyPosition {
    OBSIDIAN_PLATFORM("黑曜石平台", Material.OBSIDIAN,
            "靠近末地平台", "容错高但容易被找方向"),
    FOUNTAIN_FRONTLINE("传送门前线", Material.END_PORTAL_FRAME,
            "主岛中央传送门附近", "适合主动打架与控中"),
    END_PORTAL("要塞门房", Material.ENDER_EYE,
            "主世界要塞的末地传送门房附近", "不是直接传进末地维度"),
    CHORUS_FOREST("紫颂林", Material.CHORUS_PLANT,
            "外岛紫颂林区域", "资源多、视野复杂"),
    END_CITY_APPROACH("末地城前哨", Material.PURPUR_BLOCK,
            "靠近末地城但不直接送进城", "有更高上限资源点"),
    OUTER_GATEWAY_RING("折跃外环", Material.END_STONE_BRICKS,
            "外岛环线跳点附近", "更偏机动与转点");

    private final String displayName;
    private final Material icon;
    private final List<String> description;

    EndPreyPosition(String displayName, Material icon, String... description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = Arrays.asList(description);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }
}
