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
import org.gamefunxiao.game.EndHunterPosition;
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

public class VoteEndHunterPositionMenu extends BaseMenu {

    private static final int[] POSITION_SLOTS = {11, 13, 15, 29, 31, 33};

    private final GameRoom room;
    private BukkitTask refreshTask;

    public VoteEndHunterPositionMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l📍 末地篇猎人位置投票 📍", 54);
        this.room = room;
    }

    @Override
    public void open() {
        super.open();
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.5f);
        startAutoRefresh();
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.COMPASS,
                "§x§5§5§F§F§D§D📍 §x§7§7§F§F§E§E猎§x§9§9§F§F§F§F人§x§B§B§E§E§F§F位§x§D§D§D§D§F§F置投票",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b投票越多，你自己抽到该位置的概率越高",
                "§f- §d高价值位置自带人数上限，避免扎堆",
                "§8· · · · · · · · · · · · · ·"));

        EndHunterPosition[] positions = EndHunterPosition.values();
        for (int i = 0; i < POSITION_SLOTS.length && i < positions.length; i++) {
            inventory.setItem(POSITION_SLOTS[i], createPositionItem(positions[i]));
        }

        inventory.setItem(48, createSummaryItem());
        inventory.setItem(50, createTipItem());
        inventory.setItem(49, createPlainCloseButton());
    }

    private ItemStack createPositionItem(EndHunterPosition position) {
        ItemStack item = new ItemStack(position.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        EndHunterPosition currentVote = room.getEndHunterPositionVote(player.getUniqueId());
        boolean selected = currentVote == position;
        int votes = countVotes(position);
        double probability = plugin.getGameManager().getEndChapterHunterPositionProbability(room, player.getUniqueId(), position);

        meta.setDisplayName((selected ? "§a✓ " : "§7• ") + "§8[§e" + position.getDisplayName() + "§8]");
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b基础权重: §e" + position.getBaseWeight());
        lore.add("§f- §d当前票数: §e" + votes + " §d票");
        lore.add("§f- §6你的当前概率: §e" + String.format(Locale.US, "%.1f%%", probability));
        lore.add("§f- §c最大同时分配: §e" + position.getMaxAssignments() + "人");
        for (String description : position.getDescription()) {
            lore.add("§f- §7" + description);
        }
        if (position == EndHunterPosition.GATEWAY_CHASER || position == EndHunterPosition.GATEWAY_LOGISTICS) {
            lore.add("§f- §6这里是主世界要塞门房，不是末地维度");
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add(selected ? "§f- §a你当前已投给这个位置" : "§f- §e点击投票给这个位置");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSummaryItem() {
        EndHunterPosition currentVote = room.getEndHunterPositionVote(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b你的当前投票: " + (currentVote == null ? "§7暂未投票" : "§e" + currentVote.getDisplayName()));
        for (EndHunterPosition position : EndHunterPosition.values()) {
            lore.add("§f- §e" + position.getDisplayName() + " §8→ §d" + countVotes(position) + "票");
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        return createItem(Material.BOOK,
                "   §8[§x§5§5§F§F§D§D概§x§7§7§F§F§E§E率§x§9§9§F§F§F§F总§x§B§B§E§E§F§F览§8]",
                lore);
    }

    private ItemStack createTipItem() {
        return createItem(Material.ECHO_SHARD,
                "   §8[§x§F§F§B§B§6§6投§x§F§F§C§C§7§7票§x§F§F§D§D§8§8说§x§F§F§E§E§9§9明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a这里只提高你自己的落点倾向",
                "§f- §b稀有位置会自动限制最大分配人数",
                "§f- §d票数只影响概率，不会强制必出",
                "§8· · · · · · · · · · · · · ·");
    }

    private int countVotes(EndHunterPosition position) {
        int count = 0;
        for (Map.Entry<UUID, EndHunterPosition> entry : room.getAllEndHunterPositionVotes().entrySet()) {
            if (entry.getValue() == position) {
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

        EndHunterPosition[] positions = EndHunterPosition.values();
        for (int i = 0; i < POSITION_SLOTS.length && i < positions.length; i++) {
            if (POSITION_SLOTS[i] != slot) {
                continue;
            }

            EndHunterPosition position = positions[i];
            if (room.getEndHunterPositionVote(player.getUniqueId()) == position) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_hunter_vote_same"));
                return;
            }

            plugin.getGameManager().setEndChapterHunterPositionVote(room, player, position);
            playConfirmSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("target", position.getDisplayName());
            broadcastToHunters(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_hunter_position_voted", placeholders));
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
