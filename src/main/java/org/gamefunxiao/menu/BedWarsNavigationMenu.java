package org.gamefunxiao.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class BedWarsNavigationMenu extends BaseMenu {

    public BedWarsNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l起床战争", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        inventory.setItem(4, createTitleItem(Material.RED_BED,
                "§x§F§F§8§8§2§2[ 起床战争 ]",
                "§f- §a选择房间或直接进入等待队列",
                "§f- §7守住核心，压住对手节奏"));

        inventory.setItem(0, createLeaderboardButton());
        inventory.setItem(8, createRoomListButton());
        inventory.setItem(22, createQuickJoinButton());
        inventory.setItem(45, createBackButton());
        inventory.setItem(53, createCreateRoomButton());
    }

    private ItemStack createLeaderboardButton() {
        return createItem(Material.GOLD_INGOT,
                "§x§7§D§F§F§C§8[ 排行榜 ]",
                "§f- §a查看本局和当前房间积分",
                "§f- §7击杀、胜利会优先显示");
    }

    private ItemStack createRoomListButton() {
        return createItem(Material.BOOK,
                "§x§7§D§F§F§C§8[ 房间列表 ]",
                "§f- §a查看等待中的房间",
                "§f- §7可以选择指定房间加入");
    }

    private ItemStack createQuickJoinButton() {
        return createItem(Material.NETHER_STAR,
                "§x§7§D§F§F§C§8[ 快速加入 ]",
                "§f- §a自动进入可用房间",
                "§f- §7没有房间时会自动创建");
    }

    private ItemStack createCreateRoomButton() {
        return createItem(Material.CRAFTING_TABLE,
                "§x§F§F§8§8§2§2[ 创建房间 ]",
                "§f- §a创建一个新的等待房间",
                "§f- §7适合和朋友一起开局");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case 0 -> runBrickGuardCommand("rank", Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.56f, 1.25f);
            case 8 -> runBrickGuardCommand("rooms", Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.72f, 1.32f);
            case 22 -> runBrickGuardCommand("quick", Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.42f);
            case 45 -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
            case 53 -> runBrickGuardCommand("create", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.72f, 1.55f);
            default -> {
            }
        }
    }

    private void runBrickGuardCommand(String subCommand, Sound sound, float volume, float pitch) {
        if (Bukkit.getPluginManager().getPlugin("BrickGuard") == null && Bukkit.getPluginManager().getPlugin("YuYunBrickGuard") == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.85f);
            player.sendMessage("§x§F§F§8§8§5§5板砖守卫战还没有加载。");
            return;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.performCommand("brickguard " + subCommand);
            }
        });
    }
}
