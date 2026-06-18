package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class LuckyPillarsShopCategoryMenu extends BaseMenu {

    public LuckyPillarsShopCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 幸运之柱商店 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.EMERALD,
                "§x§5§5§F§F§A§A🍀 §x§7§7§F§F§B§B幸§x§9§9§F§F§C§C运§x§B§B§F§F§D§D之§x§D§D§F§F§E§E柱§x§F§F§F§F§F§F商§x§D§D§F§F§E§E店",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里是幸运之柱自己的商城分类",
                "§f- §a现在可购买胜利特效",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(22, createItem(Material.FIREWORK_ROCKET,
                "   §8[§x§F§F§D§D§5§5🍀 §x§F§F§E§E§7§7购§x§F§F§F§F§9§9买§x§D§D§F§F§B§B胜§x§B§B§F§F§D§D利§x§9§9§F§F§F§F特§x§7§7§E§E§F§F效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b幸运之柱结算时在场地中心播放",
                "§f- §a点击打开购买界面",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 22 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.58f);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55f, 1.86f);
                plugin.getMenuManager().openLuckyPillarsVictoryEffectShopMenu(player);
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMiniGameShopCategoryMenu(player);
            }
        }
    }
}
