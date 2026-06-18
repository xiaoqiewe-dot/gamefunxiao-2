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
import org.gamefunxiao.game.EndPreyPosition;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndPreyPositionMenu extends BaseMenu {

    private static final int[] POSITION_SLOTS = {11, 13, 15, 29, 31, 33};

    private final GameRoom room;
    private BukkitTask refreshTask;

    public EndPreyPositionMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l📍 末地篇猎物位置 📍", 54);
        this.room = room;
    }

    @Override
    public void open() {
        super.open();
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.75f, 1.55f);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.55f, 1.2f);
        startAutoRefresh();
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.COMPASS,
                "§x§F§F§6§6§0§0📍 §x§F§F§8§8§2§2猎§x§F§F§A§A§4§4物§x§F§F§C§C§6§6位§x§F§F§E§E§8§8置选择",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里决定你在末地篇的出生点类型",
                "§f- §d双猎物时若选末地门，末影之眼会自动减半",
                "§8· · · · · · · · · · · · · ·"));

        EndPreyPosition[] positions = EndPreyPosition.values();
        for (int i = 0; i < POSITION_SLOTS.length && i < positions.length; i++) {
            inventory.setItem(POSITION_SLOTS[i], createPositionItem(positions[i]));
        }

        inventory.setItem(48, createSummaryItem());
        inventory.setItem(50, createTipItem());
        inventory.setItem(49, createPlainCloseButton());
    }

    private ItemStack createPositionItem(EndPreyPosition position) {
        ItemStack item = new ItemStack(position.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        EndPreyPosition selected = room.getEndPreyPositionSelection(player.getUniqueId());
        boolean current = selected == position;
        meta.setDisplayName((current ? "§a✓ " : "§7• ") + "§8[§e" + position.getDisplayName() + "§8]");

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        for (String line : position.getDescription()) {
            lore.add("§f- §7" + line);
        }
        if (position == EndPreyPosition.END_PORTAL) {
            lore.add("§f- §6会送到主世界要塞门房，不会直接进末地");
            lore.add("§f- §d额外补给: §e12~18 个末影之眼");
            lore.add("§f- §7双猎物房间时会自动按一半发放");
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add(current ? "§f- §a你当前已选择这个位置" : "§f- §b点击切换为这个位置");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSummaryItem() {
        EndPreyPosition selected = room.getEndPreyPositionSelection(player.getUniqueId());
        return createItem(Material.BOOK,
                "   §8[§x§F§F§6§6§0§0当§x§F§F§8§8§2§2前§x§F§F§A§A§4§4位§x§F§F§C§C§6§6置§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前出生点: §b" + (selected == null ? "未选择" : selected.getDisplayName()),
                "§f- §7没选时会默认给你传送门前线",
                "§8· · · · · · · · · · · · · ·");
    }

    private ItemStack createTipItem() {
        return createItem(Material.ECHO_SHARD,
                "   §8[§x§F§F§C§C§7§7位§x§F§F§D§D§8§8置§x§F§F§E§E§9§9说§x§F§F§F§F§A§A明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a这里只影响你的出生区域与部分补给",
                "§f- §b不影响猎人的投票菜单",
                "§f- §d到倒计时结束前都可以改",
                "§8· · · · · · · · · · · · · ·");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        EndPreyPosition[] positions = EndPreyPosition.values();
        for (int i = 0; i < POSITION_SLOTS.length && i < positions.length; i++) {
            if (POSITION_SLOTS[i] != slot) {
                continue;
            }

            EndPreyPosition position = positions[i];
            if (room.getEndPreyPositionSelection(player.getUniqueId()) == position) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_prey_selection_same"));
                return;
            }

            plugin.getGameManager().setEndChapterPreyPosition(room, player, position);
            playConfirmSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("target", position.getDisplayName());
            room.broadcast(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_prey_position_selected", placeholders));
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
