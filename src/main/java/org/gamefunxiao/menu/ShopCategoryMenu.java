package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class ShopCategoryMenu extends BaseMenu {

    public ShopCategoryMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ GameFun 小游戏商城 ✦", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.EMERALD,
                "§x§5§5§F§F§A§A✦ §x§7§7§F§F§B§B小§x§9§9§F§F§C§C游§x§B§B§F§F§D§D戏§x§D§D§F§F§E§E商§x§F§F§F§F§F§F城 §x§5§5§F§F§A§A✦",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b货币: §e" + plugin.getConfigManager().getMiniGameCurrencyName(),
                "§f- §a余额: §e" + plugin.getPlayerDataManager().getCoins(player.getUniqueId()),
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(20, createCategory(Material.FIREWORK_ROCKET,
                "§x§F§F§D§7§0§0✦ §x§F§F§B§B§3§3胜§x§F§F§9§9§6§6利§x§F§F§7§7§9§9特§x§F§F§5§5§C§C效",
                "§f- §a购买游戏结束时播放的特效",
                "§f- §e已接入猎人游戏胜利结算",
                "§f- §d点击进入猎人胜利特效商城"));

        inventory.setItem(45, createBackButton());
    }

    private ItemStack createCategory(Material material, String name, String... lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§r" + name + "§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            for (String line : lines) {
                lore.add(line);
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 20 -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
                plugin.getMenuManager().openHunterVictoryEffectShopMenu(player);
            }
            case 45 -> {
                playClickSound();
                plugin.getMenuManager().openMiniGameShopCategoryMenu(player);
            }
        }
    }
}
