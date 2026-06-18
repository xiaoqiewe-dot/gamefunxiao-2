package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndChapterKit;
import org.gamefunxiao.game.EndChapterKitRole;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EndPreyKitMenu extends BaseMenu {

    private static final int[] KIT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 21, 23};

    private final GameRoom room;
    private BukkitTask refreshTask;

    public EndPreyKitMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l🌌 末地篇猎物 Kit 🌌", 54);
        this.room = room;
    }

    @Override
    public void open() {
        super.open();
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.35f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.6f, 1.75f);
        startAutoRefresh();
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.RECOVERY_COMPASS,
                "§x§B§B§8§8§F§F🌌 §x§C§C§9§9§F§F猎§x§D§D§A§A§F§F物§x§E§E§B§B§F§FK§x§F§F§C§C§F§Fi§x§F§F§D§D§F§Ft选择",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b每位猎物单独决定自己的开局配置",
                "§f- §d选择后会实时刷新手中指南针显示",
                "§8· · · · · · · · · · · · · ·"));

        EndChapterKit[] kits = EndChapterKit.values();
        for (int i = 0; i < KIT_SLOTS.length && i < kits.length; i++) {
            inventory.setItem(KIT_SLOTS[i], createKitItem(kits[i]));
        }

        inventory.setItem(48, createSummaryItem());
        inventory.setItem(50, createTipItem());
        inventory.setItem(49, createPlainCloseButton());
    }

    private ItemStack createKitItem(EndChapterKit kit) {
        ItemStack item = new ItemStack(kit.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        EndChapterKit selected = room.getEndPreyKitSelection(player.getUniqueId());
        boolean current = selected == kit;
        boolean customized = plugin.getPlayerDataManager().getEndChapterKitLayout(player.getUniqueId(), EndChapterKitRole.PREY, kit) != null;
        meta.setDisplayName((current ? "§a✓ " : "§7• ") + "§8[§d" + kit.getDisplayName() + "§8]");

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        for (String line : kit.getDescription()) {
            lore.add("§f- §7" + line);
        }
        lore.add("§f- §d猎物详细配装:");
        for (String line : kit.getPreyLoadoutDescription()) {
            lore.add("§f- §b" + line);
        }
        lore.add("§f- §e猎人基础权重参考: §6" + kit.getHunterBaseWeight());
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add(current ? "§f- §a左键：你当前已选择这个 Kit" : "§f- §b左键：切换为这个 Kit");
        lore.add("§f- §d右键：预览并编辑这个 Kit 的摆放");
        lore.add("§f- §6布局状态: " + (customized ? "§a已保存自定义" : "§7使用默认摆放"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSummaryItem() {
        EndChapterKit selected = room.getEndPreyKitSelection(player.getUniqueId());
        return createItem(Material.BOOK,
                "   §8[§x§B§B§8§8§F§F当§x§C§C§9§9§F§F前§x§D§D§A§A§F§F选§x§E§E§B§B§F§F择§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前 Kit: §d" + (selected == null ? "未选择" : selected.getDisplayName()),
                "§f- §7没选时会默认给你 UHC",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createTipItem() {
        return createItem(Material.ECHO_SHARD,
                "   §8[§x§F§F§C§C§7§7选§x§F§F§D§D§8§8择§x§F§F§E§E§9§9说§x§F§F§F§F§A§A明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a这里决定的是你自己的开局套装",
                "§f- §b不会直接影响猎人投票结果",
                "§f- §d你可以反复切换到倒计时结束前一刻",
                "§8· · · · · · · · · · · · · ·");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        EndChapterKit[] kits = EndChapterKit.values();
        for (int i = 0; i < KIT_SLOTS.length && i < kits.length; i++) {
            if (KIT_SLOTS[i] != slot) {
                continue;
            }

            EndChapterKit kit = kits[i];
            if (event.isRightClick()) {
                cancelAutoRefresh();
                playClickSound();
                new EditEndKitLayoutMenu(plugin, player, room, EndChapterKitRole.PREY, kit).open();
                return;
            }

            if (room.getEndPreyKitSelection(player.getUniqueId()) == kit) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_prey_selection_same"));
                return;
            }

            plugin.getGameManager().setEndChapterPreyKit(room, player, kit);
            playConfirmSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("target", kit.getDisplayName());
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_prey_kit_selected", placeholders));
            setupItems();
            player.updateInventory();
            return;
        }

        if (slot == 49) {
            cancelAutoRefresh();
            handlePlainCloseAction();
        }
    }

    private void startAutoRefresh() {
        cancelAutoRefresh();
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelAutoRefresh();
                return;
            }
            if (room.getGameMode() != GameMode.END_CHAPTER
                    || room.getState() != RoomState.SELECTING
                    || !room.isEndChapterDivisionActive()
                    || !room.isPrey(player.getUniqueId())) {
                cancelAutoRefresh();
                if (player.getOpenInventory().getTopInventory().getHolder() == this) {
                    player.closeInventory();
                }
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() != this) {
                cancelAutoRefresh();
                return;
            }
            setupItems();
        }, 10L, 10L);
    }

    private void cancelAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }
}
