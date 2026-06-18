package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class HunterGameShopCategoryMenu extends BaseMenu {

    public HunterGameShopCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 猎人游戏商店 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.EMERALD,
                "§x§5§5§F§F§A§A✦ §x§7§7§F§F§B§B猎§x§9§9§F§F§C§C人§x§B§B§F§F§D§D游§x§D§D§F§F§E§E戏§x§F§F§F§F§F§F商§x§D§D§F§F§E§E店",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里是猎人游戏自己的商城分类",
                "§f- §a不同玩法的商品以后也能继续分开",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(21, createItem(Material.FIREWORK_ROCKET,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§A§A§4§4购§x§F§F§8§8§8§8买§x§F§F§6§6§C§C胜§x§F§F§4§4§F§F利§x§D§D§6§6§F§F特§x§B§B§8§8§F§F效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b进入猎人游戏胜利特效商城",
                "§f- §a点击打开购买界面",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(23, createItem(Material.NETHERITE_SWORD,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§A§A§4§4购§x§F§F§8§8§8§8买§x§F§F§6§6§C§C击§x§F§F§4§4§F§F杀§x§D§D§6§6§F§F特§x§B§B§8§8§F§F效§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b击杀猎物或猎人时在死亡点播放特效",
                "§f- §a点击打开购买界面",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 21 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.55f);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55f, 1.82f);
                plugin.getMenuManager().openHunterVictoryEffectShopMenu(player);
            }
            case 23 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.38f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.55f, 0.9f);
                plugin.getMenuManager().openHunterKillEffectShopMenu(player);
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMiniGameShopCategoryMenu(player);
            }
        }
    }
}
