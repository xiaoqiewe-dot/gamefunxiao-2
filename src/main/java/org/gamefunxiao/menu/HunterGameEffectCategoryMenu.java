package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

public class HunterGameEffectCategoryMenu extends BaseMenu {

    public HunterGameEffectCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 猎人游戏特效分类 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.FIREWORK_ROCKET,
                "§x§F§F§D§7§0§0✦ §x§F§F§A§A§4§4猎§x§F§F§8§8§8§8人§x§F§F§6§6§C§C游§x§F§F§4§4§F§F戏§x§D§D§6§6§F§F特§x§B§B§8§8§F§F效§x§9§9§A§A§F§F分§x§7§7§C§C§F§F类",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里专门放猎人游戏自己的特效设置",
                "§f- §a不同玩法的特效以后也能继续分开扩展",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(22, createItem(Material.TOTEM_OF_UNDYING,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§A§A§4§4胜§x§F§F§8§8§8§8利§x§F§F§6§6§C§C特§x§F§F§4§4§F§F效§x§D§D§6§6§F§F设§x§B§B§8§8§F§F置§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b切换你已经拥有的猎人游戏胜利特效",
                "§f- §a点击进入设置",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(24, createItem(Material.NETHERITE_SWORD,
                "   §8[§x§F§F§D§7§0§0✦ §x§F§F§A§A§4§4击§x§F§F§8§8§8§8杀§x§F§F§6§6§C§C特§x§F§F§4§4§F§F效§x§D§D§6§6§F§F设§x§B§B§8§8§F§F置§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b切换你已经拥有的猎人游戏击杀特效",
                "§f- §a点击进入设置",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 22 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.65f);
                plugin.getMenuManager().openHunterVictoryEffectSettingsMenu(player);
            }
            case 24 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.38f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.55f, 0.92f);
                plugin.getMenuManager().openHunterKillEffectSettingsMenu(player);
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openSettingsCategoryMenu(player);
            }
        }
    }
}
