package org.gamefunxiao.game;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum EndChapterKit {
    UHC("uhc", "UHC", "GOLDEN_APPLE", Material.GOLDEN_APPLE, 36, 99,
            "终局均衡主战套", "适合绝大多数正面交战"),
    MACE("mace", "MACE", "MACE", Material.BREEZE_ROD, 4, 1,
            "重锤爆发与风弹位移", "高低差收割能力极强"),
    DEBUFF("debuff", "Debuff", "SPLASH_POTION", Material.SPLASH_POTION, 12, 2,
            "负面药水压制流", "擅长追击与打乱节奏"),
    DIAMOND_SMP("diamond_smp", "钻石SMP", "DIAMOND_CHESTPLATE", Material.DIAMOND_CHESTPLATE, 8, 2,
            "偏后期 SMP 近战套", "容错高、站场强"),
    TRAPPER("trapper", "陷阱师", "TRIPWIRE_HOOK", Material.TRIPWIRE_HOOK, 14, 3,
            "陷阱与地形控制流", "适合封路和反打"),
    CRYSTAL("crystal", "水晶", "END_CRYSTAL", Material.END_CRYSTAL, 5, 1,
            "水晶与锚点爆发套", "上限最高的终局配置"),
    POTION("potion", "药水", "POTION", Material.POTION, 18, 4,
            "药水续航与状态流", "适合拉扯与反开"),
    CROSSBOW("crossbow", "弓驽", "CROSSBOW", Material.CROSSBOW, 24, 5,
            "弓弩双远程压制", "中远距离消耗最稳"),
    SPEAR_HAMMER("spear_hammer", "长矛重锤", "SPEAR", Material.TRIDENT, 10, 2,
            "长矛与重锤双切", "突脸补刀和爆发兼顾");

    private final String id;
    private final String displayName;
    private final String iconName;
    private final Material fallbackIcon;
    private final int hunterBaseWeight;
    private final int hunterMaxAssignments;
    private final List<String> description;

    EndChapterKit(String id, String displayName, String iconName, Material fallbackIcon,
                  int hunterBaseWeight, int hunterMaxAssignments, String... description) {
        this.id = id;
        this.displayName = displayName;
        this.iconName = iconName;
        this.fallbackIcon = fallbackIcon;
        this.hunterBaseWeight = hunterBaseWeight;
        this.hunterMaxAssignments = hunterMaxAssignments;
        this.description = Arrays.asList(description);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        Material matched = Material.matchMaterial(iconName);
        return matched == null ? fallbackIcon : matched;
    }

    public int getHunterBaseWeight() {
        return hunterBaseWeight;
    }

    public int getHunterMaxAssignments() {
        return hunterMaxAssignments;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getPreyLoadoutDescription() {
        return switch (this) {
            case UHC -> Arrays.asList(
                    "满钻主甲，主打终局均衡近战与弓战",
                    "锐利钻剑 + 强力火矢弓 + 盾牌，金苹果与珍珠都更足",
                    "附带水桶、岩浆桶与概率图腾，容错最高");
            case MACE -> Arrays.asList(
                    "满钻近战甲，靴子固定高摔落保护",
                    "重锤 + 大量风弹 + 双慢降药水，专打高低差爆发",
                    "珍珠与回复更厚，适合连跳追击");
            case DEBUFF -> Arrays.asList(
                    "满钻控制流，近战同时带多组负面投掷药",
                    "剧毒、缓慢、虚弱、伤害、滞留毒云和蛛网一起上",
                    "适合贴脸压血线后收割");
            case DIAMOND_SMP -> Arrays.asList(
                    "偏大后期 SMP 配装，带下界合金核心护甲",
                    "高等级近战武器 + 盾牌 + 更多金苹果与珍珠",
                    "会带不死图腾，终局站撸能力最强");
            case TRAPPER -> Arrays.asList(
                    "满钻运营甲，胸甲偏爆炸保护",
                    "钻斧/钻剑 + 蛛网 + TNT + TNT矿车 + 轨道 + 绊线 + 重生锚包",
                    "适合门房、桥面、转角做连续陷阱");
            case CRYSTAL -> Arrays.asList(
                    "满钻高爆发配置，核心护甲偏爆炸保护",
                    "效率镐 + 近战武器 + 水晶 + 黑曜石 + 重生锚包",
                    "珍珠和图腾更足，上限最高但操作要求也最高");
            case POTION -> Arrays.asList(
                    "满钻药水流，持续作战非常强",
                    "力量、迅捷、再生、慢降、瞬疗、伤害药和滞留毒云都更全",
                    "适合拉扯补状态后反开团");
            case CROSSBOW -> Arrays.asList(
                    "满钻远程流，护甲偏弹射物防护",
                    "高等级弩 + 强力弓 + 更多箭、减速箭、伤害箭与烟花 + 盾牌",
                    "适合中远距离连续消耗后再近身");
            case SPEAR_HAMMER -> Arrays.asList(
                    "满钻机动爆发流，靴子带高摔落保护",
                    "长矛/三叉戟 + 重锤 + 风弹 + 慢降 + 迅捷 + 珍珠",
                    "适合追击补刀与高低差秒杀");
        };
    }

    public List<String> getHunterLoadoutDescription() {
        return switch (this) {
            case UHC -> Arrays.asList(
                    "三钻起步的标准追击套，强度稳定",
                    "近战主武器 + 弓箭 + 盾牌 + 珍珠与金苹果",
                    "适合绝大多数猎人直接追人");
            case MACE -> Arrays.asList(
                    "三钻机动套，保留重锤爆发但弱于猎物版",
                    "风弹与慢降更少，靴子同样偏摔落保护，偏补刀和跳劈",
                    "仍然是稀有小队型配置");
            case DEBUFF -> Arrays.asList(
                    "控制追击套，药水数量低于猎物版",
                    "带基础负面药、滞留毒云与回复药，适合多人接力压制",
                    "更偏开团辅助，而不是单人终结");
            case DIAMOND_SMP -> Arrays.asList(
                    "三钻近战追击套，耐打度明显高于普通 UHC",
                    "更好的近战武器、盾牌、金苹果与珍珠",
                    "适合当正面主追击位");
            case TRAPPER -> Arrays.asList(
                    "追击运营套，偏抓失误与封路",
                    "网、TNT、TNT矿车、轨道、打火石与绊线更适合多人协同",
                    "强度低于猎物陷阱师，但功能齐全");
            case CRYSTAL -> Arrays.asList(
                    "三钻爆发套，护甲自带基础爆炸保护",
                    "近战武器 + 镐子 + 水晶 + 黑曜石 + 重生锚包",
                    "适合会爆发收头的少数猎人");
            case POTION -> Arrays.asList(
                    "持续追击药水套，机动和回复都更稳",
                    "力量、迅捷、再生、瞬疗与滞留毒云兼顾",
                    "适合团战里反复续航");
            case CROSSBOW -> Arrays.asList(
                    "远程压制套，胸甲偏弹射物防护",
                    "强化弩 + 箭矢 + 减速箭 + 烟花 + 盾牌",
                    "适合中距离连续点射和桥战");
            case SPEAR_HAMMER -> Arrays.asList(
                    "机动突脸套，偏切入与补刀",
                    "长矛/三叉戟 + 重锤 + 风弹 + 慢降 + 迅捷",
                    "更适合从侧翼追击猎物");
        };
    }
}
