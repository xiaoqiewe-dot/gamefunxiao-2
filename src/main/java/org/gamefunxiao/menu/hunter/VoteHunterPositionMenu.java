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
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.NetherHunterScenario;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class VoteHunterPositionMenu extends BaseMenu {

    private static final int[] SCENARIO_SLOTS = {11, 13, 15, 29, 31, 33};

    private final GameRoom room;
    private BukkitTask refreshTask;

    public VoteHunterPositionMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l🧭 下界篇落点投票 🧭", 54);
        this.room = room;
    }

    @Override
    public void open() {
        super.open();
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.45f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.9f);
        startAutoRefresh();
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.RECOVERY_COMPASS,
                "§x§8§8§D§D§F§F🧭 §x§A§A§E§E§F§F下§x§C§C§F§F§F§F界§x§D§D§F§F§C§C篇§x§E§E§D§D§A§A落§x§F§F§C§C§8§8点§x§F§F§B§B§6§6投§x§F§F§A§A§5§5票",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b票数越多，该位置权重越高",
                "§f- §d菜单会实时刷新当前概率",
                "§8· · · · · · · · · · · · · ·"));

        Map<NetherHunterScenario, Double> probabilities = plugin.getGameManager().getNetherHunterScenarioProbabilities(room);
        for (int i = 0; i < SCENARIO_SLOTS.length && i < NetherHunterScenario.values().length; i++) {
            NetherHunterScenario scenario = NetherHunterScenario.values()[i];
            inventory.setItem(SCENARIO_SLOTS[i], createScenarioItem(scenario, probabilities.getOrDefault(scenario, 0.0D)));
        }

        inventory.setItem(48, createSummaryItem(probabilities));
        inventory.setItem(50, createTipItem());
        inventory.setItem(49, createPlainCloseButton());
    }

    private ItemStack createScenarioItem(NetherHunterScenario scenario, double probability) {
        ItemStack item = new ItemStack(scenario.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        NetherHunterScenario currentVote = room.getPlayerNetherHunterScenarioVote(player.getUniqueId());
        boolean selected = scenario == currentVote;
        int votes = room.getNetherHunterScenarioVoteCount(scenario);
        int effectiveWeight = plugin.getGameManager().getNetherHunterScenarioEffectiveWeight(room, scenario);

        meta.setDisplayName((selected ? "§a✓ " : "§7• ") + "§8[§e" + scenario.getDisplayName() + "§8]");

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b基础权重: §e" + scenario.getBaseWeight());
        lore.add("§f- §d当前票数: §e" + votes + " §d票");
        lore.add("§f- §6当前概率: §e" + String.format(Locale.US, "%.1f%%", probability));
        lore.add("§f- §a实时权重: §e" + effectiveWeight);
        for (String description : scenario.getDescription()) {
            lore.add("§f- §7" + description);
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add(selected ? "§f- §a你当前已投给这个位置" : "§f- §e点击投票给这个位置");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSummaryItem(Map<NetherHunterScenario, Double> probabilities) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        NetherHunterScenario currentVote = room.getPlayerNetherHunterScenarioVote(player.getUniqueId());
        meta.setDisplayName("   §8[§x§8§8§D§D§F§F概§x§A§A§E§E§F§F率§x§C§C§F§F§F§F总§x§E§E§D§D§A§A览§8]");

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b你的当前投票: " + (currentVote == null ? "§7暂未投票" : "§e" + currentVote.getDisplayName()));
        for (NetherHunterScenario scenario : NetherHunterScenario.values()) {
            double probability = probabilities.getOrDefault(scenario, 0.0D);
            int votes = room.getNetherHunterScenarioVoteCount(scenario);
            lore.add("§f- §e" + scenario.getDisplayName() + " §8→ §d" + votes + "票 §8/ §6" +
                    String.format(Locale.US, "%.1f%%", probability));
        }
        lore.add("§8· · · · · · · · · · · · · ·");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTipItem() {
        return createItem(Material.ECHO_SHARD,
                "   §8[§x§F§F§B§B§6§6投§x§F§F§C§C§7§7票§x§F§F§D§D§8§8说§x§F§F§E§E§9§9明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b每1票都会追加落点权重",
                "§f- §a票数越高，被抽中的概率越高",
                "§f- §d若没人投票，则沿用默认概率",
                "§8· · · · · · · · · · · · · ·");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        for (int i = 0; i < SCENARIO_SLOTS.length && i < NetherHunterScenario.values().length; i++) {
            if (SCENARIO_SLOTS[i] != slot) {
                continue;
            }

            NetherHunterScenario scenario = NetherHunterScenario.values()[i];
            NetherHunterScenario currentVote = room.getPlayerNetherHunterScenarioVote(player.getUniqueId());
            if (currentVote == scenario) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.nether_position_vote_same"));
                return;
            }

            room.voteNetherHunterScenario(player.getUniqueId(), scenario);
            playConfirmSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("target", scenario.getDisplayName());
            broadcastToHunters(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.nether_position_voted", placeholders));

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

            if (room.getGameMode() != GameMode.NETHER_CHAPTER || room.getState() != RoomState.SELECTING || !room.isHunter(player.getUniqueId())) {
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
