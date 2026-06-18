package org.gamefunxiao.menu;

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
import java.util.UUID;

public class BrickGuardCoreTransferMenu extends BaseMenu {

    private final GameRoom room;
    private final List<UUID> candidates = new ArrayList<>();

    public BrickGuardCoreTransferMenu(GameFunXiao plugin, Player player, GameRoom room) {
        super(plugin, player, "§0§l▣ 转移下界核心 ▣", 45);
        this.room = room;
        for (UUID uuid : room.getAllPlayerUUIDs()) {
            if (plugin.getBrickGuardManager().isNetherTeam(room, uuid) && !uuid.equals(player.getUniqueId())) {
                candidates.add(uuid);
            }
        }
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();
        inventory.setItem(4, createTitleItem(Material.RECOVERY_COMPASS,
                "§x§6§6§1§9§0§0▣ §x§8§8§3§0§0§0转§x§A§A§4§7§0§0移§x§C§C§5§E§0§0核§x§E§E§7§5§0§0心",
                "§8· · · · · · · · · · · · · ·",
                "§f- §7开局 1 分钟内仅可转移 1 次",
                "§f- §c转移后立即固定为新的核心玩家",
                "§8· · · · · · · · · · · · · ·"));
        int slot = 19;
        for (UUID uuid : candidates) {
            Player target = org.bukkit.Bukkit.getPlayer(uuid);
            if (target == null) {
                continue;
            }
            inventory.setItem(slot++, createCandidateItem(target));
            if (slot == 26) {
                break;
            }
        }
        inventory.setItem(36, createBackButton());
    }

    private ItemStack createCandidateItem(Player target) {
        ItemStack item = new ItemStack(Material.NETHER_BRICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§6§6§1§9§0§0转移给 §f" + target.getName());
            meta.setLore(List.of(
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §7点击后把下界核心身份转移给他",
                    "§f- §c只能操作一次，请谨慎选择",
                    "§8· · · · · · · · · · · · · ·"));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 36) {
            playClickSound();
            player.closeInventory();
            return;
        }
        int index = event.getRawSlot() - 19;
        if (index < 0 || index >= candidates.size()) {
            return;
        }
        UUID targetUuid = candidates.get(index);
        if (plugin.getBrickGuardManager().transferCore(room, player, targetUuid)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getBrickGuardMessageWithPrefix("brick_guard.core_transfer_failed"));
        }
    }
}
