package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class BrickGuardNavigationMenu extends BaseMenu {

    private static final int SLOT_RANKING = 0;
    private static final int SLOT_ROOMS = 8;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CREATE = 53;
    private static final int SLOT_ADMIN = 49;
    private static final int[] JOIN_SLOTS = {20, 22, 24};

    public BrickGuardNavigationMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l板砖守卫战", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillBrickGuardFrame();

        inventory.setItem(4, createTitleItem(Material.BRICK,
                "§x§F§F§7§C§0§0板砖守卫战",
                "§8< 我无法介绍 >",
                "§7- 但你必须破坏对面的核心你才能获得胜利"));

        inventory.setItem(SLOT_RANKING, createLeaderboardButton());
        inventory.setItem(SLOT_ROOMS, createRoomListButton());
        inventory.setItem(SLOT_BACK, createBackButton());
        inventory.setItem(SLOT_CREATE, createCreateRoomButton());

        placeCenteredItems(List.of(createQuickMatchButton(), createEnterRoomButton(), createTeamHintButton()), JOIN_SLOTS);

        if (player.hasPermission("gamefunxiao.admin")) {
            inventory.setItem(SLOT_ADMIN, createAdminButton());
        }
    }

    private void fillBrickGuardFrame() {
        Material normal = Material.BLACK_STAINED_GLASS_PANE;
        Material corner = Material.RED_STAINED_GLASS_PANE;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean border = row == 0 || row == 5 || col == 0 || col == 8;
            if (!border) {
                continue;
            }
            Material material = (slot == 0 || slot == 8 || slot == 45 || slot == 53) ? corner : normal;
            inventory.setItem(slot, createItem(material, "§8你看我干什么", "§7虽然你点我也没有用ewe"));
        }
    }

    private void placeCenteredItems(List<org.bukkit.inventory.ItemStack> items, int[] slots) {
        int count = Math.min(items.size(), slots.length);
        int start = (slots.length - count) / 2;
        for (int i = 0; i < count; i++) {
            inventory.setItem(slots[start + i], items.get(i));
        }
    }

    private org.bukkit.inventory.ItemStack createQuickMatchButton() {
        return createItem(Material.NETHER_BRICK,
                "§x§F§F§7§C§0§0[ 快速匹配 ]",
                "§7- 直接去找正在等待的房间",
                "§7- 没有合适房间时会补一个新的");
    }

    private org.bukkit.inventory.ItemStack createEnterRoomButton() {
        return createItem(Material.BRICK,
                "§x§F§F§7§C§0§0[ 进入房间 ]",
                "§7- 看一眼当前能加入的房间",
                "§7- 只显示这个玩法的房间");
    }

    private org.bukkit.inventory.ItemStack createTeamHintButton() {
        return createItem(Material.COMPASS,
                "§x§F§F§7§C§0§0< 队伍选择 >",
                "§7- 进房后可用第一格指南针选边",
                "§7- 会尽量把两边人数压平");
    }

    private org.bukkit.inventory.ItemStack createCreateRoomButton() {
        return createItem(Material.EMERALD,
                "§x§F§F§7§C§0§0[ 创建房间 ]",
                "§7- 开一个新的守卫战房间",
                "§7- 人数和公开状态在下一页设置");
    }

    private org.bukkit.inventory.ItemStack createRoomListButton() {
        return createItem(Material.ENDER_EYE,
                "§x§F§F§7§C§0§0[ 查看房间 ]",
                "§7- 等待中的房间可以直接加入",
                "§7- 进行中的房间按系统处理旁观");
    }

    private org.bukkit.inventory.ItemStack createLeaderboardButton() {
        return createItem(Material.RED_GLAZED_TERRACOTTA,
                "§x§F§F§7§C§0§0[ 战绩排行 ]",
                "§7- 看这个玩法自己的榜单",
                "§7- 只放板砖守卫战相关数据");
    }

    private org.bukkit.inventory.ItemStack createAdminButton() {
        return createItem(Material.PAPER,
                "§x§F§F§7§C§0§0[ 管理调试 ]",
                "§7- 只给管理员看的入口",
                "§7- 用来处理当前玩法房间");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        switch (slot) {
            case SLOT_RANKING -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.52f, 1.08f);
                plugin.getMenuManager().openBrickGuardLeaderboardMenu(player);
            }
            case SLOT_ROOMS, 22 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.66f, 1.08f);
                plugin.getMenuManager().openBrickGuardRoomListMenu(player);
            }
            case 20 -> {
                player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 0.72f, 0.82f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.54f, 1.22f);
                player.closeInventory();
                plugin.getRoomManager().quickMatch(player, GameMode.BRICK_GUARD.getId());
            }
            case SLOT_CREATE -> {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.82f, 1.18f);
                new org.gamefunxiao.menu.hunter.CreateRoomMenu(plugin, player, MenuSection.BRICK_GUARD).open();
            }
            case SLOT_BACK -> {
                playClickSound();
                plugin.getMenuManager().openMainMenu(player);
            }
            case SLOT_ADMIN -> {
                if (player.hasPermission("gamefunxiao.admin")) {
                    player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.55f, 1.1f);
                    player.performCommand("gamefunxiao bg help");
                    player.closeInventory();
                }
            }
            default -> {
            }
        }
    }
}
