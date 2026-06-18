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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class VoteEndHunterKitMenu extends BaseMenu {

    private static final int[] KIT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 21, 23};

    private final GameRoom room;
    private BukkitTask refreshTask;

    public VoteEndHunterKitMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l🧭 末地篇猎人 Kit 投票 🧭", 54);
        this.room = room;
    }

    @Override
    public void open() {
        super.open();
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.45f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.85f);
        startAutoRefresh();
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.RECOVERY_COMPASS,
                "§x§8§8§D§D§F§F🧭 §x§A§A§E§E§F§F猎§x§C§C§F§F§F§F人§x§D§D§F§F§C§CK§x§E§E§D§D§A§Ai§x§F§F§C§C§8§8t投票",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b投票只会提高你自己抽到该 Kit 的概率",
                "§f- §d稀有 Kit 有人数上限，不会一窝蜂重复",
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

        EndChapterKit currentVote = room.getEndHunterKitVote(player.getUniqueId());
        boolean selected = currentVote == kit;
        boolean customized = plugin.getPlayerDataManager().getEndChapterKitLayout(player.getUniqueId(), EndChapterKitRole.HUNTER, kit) != null;
        int votes = countVotes(kit);
        double probability = plugin.getGameManager().getEndChapterHunterKitProbability(room, player.getUniqueId(), kit);

        meta.setDisplayName((selected ? "§a✓ " : "§7• ") + "§8[§d" + kit.getDisplayName() + "§8]");
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b基础权重: §e" + kit.getHunterBaseWeight());
        lore.add("§f- §d当前票数: §e" + votes + " §d票");
        lore.add("§f- §6你的当前概率: §e" + String.format(Locale.US, "%.1f%%", probability));
        lore.add("§f- §c最大同时分配: §e" + kit.getHunterMaxAssignments() + "人");
        for (String description : kit.getDescription()) {
            lore.add("§f- §7" + description);
        }
        lore.add("§f- §d猎人详细配装:");
        for (String description : kit.getHunterLoadoutDescription()) {
            lore.add("§f- §b" + description);
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add(selected ? "§f- §a左键：你当前已投给这个 Kit" : "§f- §e左键：投票给这个 Kit");
        lore.add("§f- §d右键：预览并编辑这个 Kit 的摆放");
        lore.add("§f- §6布局状态: " + (customized ? "§a已保存自定义" : "§7使用默认摆放"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSummaryItem() {
        EndChapterKit currentVote = room.getEndHunterKitVote(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b你的当前投票: " + (currentVote == null ? "§7暂未投票" : "§d" + currentVote.getDisplayName()));
        for (EndChapterKit kit : EndChapterKit.values()) {
            lore.add("§f- §d" + kit.getDisplayName() + " §8→ §e" + countVotes(kit) + "票");
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        return createItem(Material.BOOK,
                "   §8[§x§8§8§D§D§F§F概§x§A§A§E§E§F§F率§x§C§C§F§F§F§F总§x§E§E§D§D§A§A览§8]",
                lore);
    }

    private ItemStack createTipItem() {
        return createItem(Material.ECHO_SHARD,
                "   §8[§x§F§F§B§B§6§6投§x§F§F§C§C§7§7票§x§F§F§D§D§8§8说§x§F§F§E§E§9§9明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a这里只提高你自己的抽取倾向",
                "§f- §b并不会把整个房间都变成同一种 Kit",
                "§f- §d极稀有 Kit 自带分配上限保护",
                "§8· · · · · · · · · · · · · ·");
    }

    private int countVotes(EndChapterKit kit) {
        int count = 0;
        for (Map.Entry<UUID, EndChapterKit> entry : room.getAllEndHunterKitVotes().entrySet()) {
            if (entry.getValue() == kit) {
                count++;
            }
        }
        return count;
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
                new EditEndKitLayoutMenu(plugin, player, room, EndChapterKitRole.HUNTER, kit).open();
                return;
            }

            if (room.getEndHunterKitVote(player.getUniqueId()) == kit) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_hunter_vote_same"));
                return;
            }

            plugin.getGameManager().setEndChapterHunterKitVote(room, player, kit);
            playConfirmSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("target", kit.getDisplayName());
            broadcastToHunters(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_hunter_kit_voted", placeholders));
            setupItems();
            player.updateInventory();
            return;
        }

        if (slot == 49) {
            cancelAutoRefresh();
            handlePlainCloseAction();
        }
    }

    private void broadcastToHunters(String message) {
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (!room.isHunter(uuid)) {
                continue;
            }
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                online.sendMessage(message);
            }
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
                    || !room.isHunter(player.getUniqueId())) {
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
