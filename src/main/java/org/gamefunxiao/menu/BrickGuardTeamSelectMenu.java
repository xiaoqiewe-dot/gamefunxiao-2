package org.gamefunxiao.menu;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.BrickGuardManager;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class BrickGuardTeamSelectMenu extends BaseMenu {

    private final GameRoom room;

    public BrickGuardTeamSelectMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l▣ 板砖守卫战 - 选择队伍 ▣", 27);
        this.room = room;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();
        inventory.setItem(4, createTitleItem(Material.COMPASS,
                "§x§F§F§7§C§0§0▣ §x§F§F§9§0§2§0队§x§F§F§A§4§4§0伍§x§C§C§5§0§2§0选§x§6§6§1§9§0§0择",
                "§8· · · · · · · · · · · · · ·",
                "§f- §7开局尽量两边人数接近",
                "§f- §a允许相差 1~2 人",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(11, createTeamButton(BrickGuardManager.TeamSide.BRICK));
        inventory.setItem(15, createTeamButton(BrickGuardManager.TeamSide.NETHER));
        inventory.setItem(22, createBackButton());
    }

    private ItemStack createTeamButton(BrickGuardManager.TeamSide side) {
        boolean brick = side == BrickGuardManager.TeamSide.BRICK;
        int amount = Math.max(1, Math.min(99, brick
                ? plugin.getBrickGuardManager().getBrickTeamCount(room)
                : plugin.getBrickGuardManager().getNetherTeamCount(room)));
        ItemStack item = new ItemStack(brick ? Material.BRICK : Material.NETHER_BRICK, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(brick ? "§x§F§F§7§C§0§0板砖队" : "§x§6§6§1§9§0§0下界砖队");
            List<String> lore = new ArrayList<>();
            List<String> names = plugin.getBrickGuardManager().getTeamPreviewNames(room, side, 5);
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §7当前人数: §e" + amount);
            lore.add("§f- §7随机展示 5 名队员:");
            if (names.isEmpty()) {
                lore.add("§8  - 暂无玩家");
            } else {
                for (String name : names) {
                    lore.add("§8  - §f" + name);
                }
            }
            BrickGuardManager.TeamSide selected = plugin.getBrickGuardManager().getSelectedTeam(room, player.getUniqueId());
            lore.add(selected == side ? "§f- §a你当前就在这支队伍" : "§f- §a点击加入这支队伍");
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 99);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 11 -> choose(BrickGuardManager.TeamSide.BRICK);
            case 15 -> choose(BrickGuardManager.TeamSide.NETHER);
            case 22 -> {
                playClickSound();
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    private void choose(BrickGuardManager.TeamSide side) {
        if (!plugin.getBrickGuardManager().canJoinTeam(room, player.getUniqueId(), side)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.team_balance_blocked"));
            return;
        }
        plugin.getBrickGuardManager().selectTeam(room, player.getUniqueId(), side);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.35f);
        player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix(
                side == BrickGuardManager.TeamSide.BRICK ? "brick_guard.join_brick_team" : "brick_guard.join_nether_team"));
        setupItems();
    }
}
