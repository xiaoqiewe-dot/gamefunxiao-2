package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class LuckyPillarsEffectCategoryMenu extends BaseMenu {

    public LuckyPillarsEffectCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 幸运之柱特效分类 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.END_CRYSTAL,
                "§x§F§F§D§D§5§5🍀 §x§F§F§E§E§7§7幸§x§F§F§F§F§9§9运§x§D§D§F§F§B§B之§x§B§B§F§F§D§D柱§x§9§9§F§F§F§F特§x§7§7§E§E§F§F效§x§5§5§D§D§F§F分§x§7§7§C§C§F§F类",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里专门放幸运之柱自己的特效设置",
                "§f- §a胜利后会在场地正中间播放",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(22, createItem(Material.FIREWORK_ROCKET,
                "   §8[§x§F§F§D§D§5§5🍀 §x§F§F§E§E§7§7胜§x§F§F§F§F§9§9利§x§D§D§F§F§B§B特§x§B§B§F§F§D§D效§x§9§9§F§F§F§F设§x§7§7§E§E§F§F置§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b切换你已经拥有的幸运之柱胜利特效",
                "§f- §a默认就是烟花",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 22 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.7f);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.55f, 1.35f);
                plugin.getMenuManager().openLuckyPillarsVictoryEffectSettingsMenu(player);
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openSettingsCategoryMenu(player);
            }
        }
    }
}
