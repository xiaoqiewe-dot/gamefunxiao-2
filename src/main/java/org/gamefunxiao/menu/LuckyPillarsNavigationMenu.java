package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LuckyPillarsNavigationMenu extends BaseMenu {

    public LuckyPillarsNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l🍀 幸运之柱 - 经典 🍀", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.GOLD_BLOCK,
                "§x§F§F§D§D§5§5🍀 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8之§x§F§F§9§9§9§9柱 §x§F§F§7§7§A§A· §x§F§F§D§D§5§5经§x§F§F§C§C§6§6典",
                "§8· · · · · · · · · · · · · ·",
                "§f选择房间大小后会直接创建经典房间",
                "§f想细调人数可以点右下角创建房间",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(0, createLeaderboardButton());
        inventory.setItem(8, createRoomListButton());

        inventory.setItem(21, createLuckyRoomButton(GameMode.LUCKY_PILLARS, "小型图", 8, Material.GOLD_NUGGET,
                "§f- §a适合 2-8 人快速开局",
                "§f- §e柱距更近，节奏更快"));
        inventory.setItem(22, createLuckyRoomButton(GameMode.LUCKY_PILLARS, "中型图", 16, Material.GOLD_BLOCK,
                "§f- §a经典默认推荐人数",
                "§f- §e节奏和地图空间更均衡"));
        inventory.setItem(23, createLuckyRoomButton(GameMode.LUCKY_PILLARS, "大型图", 32, Material.BELL,
                "§f- §a适合多人混战房间",
                "§f- §e远程道具和搭桥更热闹"));

        inventory.setItem(44, createCreateRoomButton());
        inventory.setItem(36, createBackButton());
    }

    private ItemStack createLeaderboardButton() {
        return createItem(Material.GOLD_INGOT,
                "   §8[§x§F§F§D§D§5§5🏆 §x§F§F§C§C§6§6幸§x§F§F§B§B§7§7运§x§F§F§A§A§8§8积§x§F§F§9§9§9§9分§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a查看小游戏积分排行榜",
                "§f- §e幸运之柱胜利也会记入这里",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击查看");
    }

    private ItemStack createRoomListButton() {
        return createItem(Material.ENDER_EYE,
                "   §8[§x§5§5§F§F§D§D👁 §x§7§7§F§F§C§C房§x§9§9§F§F§B§B间§x§B§B§F§F§A§A列§x§D§D§F§F§9§9表§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a只看幸运之柱相关房间",
                "§f- §b等待中的可加入，进行中的可旁观",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击查看");
    }

    private ItemStack createCreateRoomButton() {
        return createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A✚ §x§7§7§F§F§B§B创§x§9§9§F§F§C§C建§x§B§B§F§F§D§D房§x§D§D§F§F§E§E间§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a进入幸运之柱自己的创建房间菜单",
                "§f- §b里面只会出现经典模式的选项",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击打开");
    }

    private ItemStack createLuckyRoomButton(GameMode mode, String sizeName, int maxPlayers, Material material, String... extras) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String modeText = "§x§F§F§D§D§5§5经§x§F§F§C§C§6§6典";
            meta.setDisplayName("   §8[" + modeText + " §8/ §b" + sizeName + " §8/ §e" + maxPlayers + "人§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §e模式: §b" + mode.getDisplayName());
            for (String extra : extras) {
                lore.add(extra);
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
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case 0 -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.56f, 1.28f);
                plugin.getMenuManager().openLuckyPillarsLeaderboardMenu(player);
            }
            case 8 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.72f, 1.32f);
                plugin.getMenuManager().openLuckyPillarsRoomListMenu(player);
            }
            case 21 -> createDedicatedRoom(GameMode.LUCKY_PILLARS, 8);
            case 22 -> createDedicatedRoom(GameMode.LUCKY_PILLARS, 16);
            case 23 -> createDedicatedRoom(GameMode.LUCKY_PILLARS, 32);
            case 44 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.42f);
                new org.gamefunxiao.menu.hunter.CreateRoomMenu(plugin, player, MenuSection.LUCKY_PILLARS).open();
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
            default -> {
            }
        }
    }

    private void createDedicatedRoom(GameMode mode, int maxPlayers) {
        if (plugin.getRoomManager().isInRoom(player.getUniqueId())) {
            playErrorSound();
            player.sendMessage(plugin.getMessageManager().getLuckyPillarsMessageWithPrefix("room.already_in_room"));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.42f);
        plugin.getRoomManager().createConfiguredRoom(player, mode, maxPlayers, true, new HashSet<>());
    }
}
