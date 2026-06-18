package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.MenuSection;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.Set;

public class ConfirmRejoinMenu extends BaseMenu {

    private final GameRoom room;
    private final MenuSection menuSection;
    private final Set<GameMode> modeFilter;

    public ConfirmRejoinMenu(GameFunXiao plugin, Player player, GameRoom room) {
        this(plugin, player, room, MenuSection.HUNTER, RoomListMenu.defaultHunterFilter());
    }

    public ConfirmRejoinMenu(GameFunXiao plugin, Player player, GameRoom room, MenuSection menuSection, Set<GameMode> modeFilter) {
        super(plugin, player, "§0§l⚔ 返回游戏 ⚔", 27);
        this.room = room;
        this.menuSection = menuSection;
        this.modeFilter = modeFilter;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.IRON_SWORD,
            "§x§F§F§5§5§5§5⚔ §x§F§F§7§7§5§5你§x§F§F§9§9§5§5是§x§F§F§B§B§5§5猎§x§F§F§D§D§5§5人 §x§F§F§5§5§5§5⚔",
            "§8· · · · · · · · · · · · · ·",
            "§f你似乎是猎人，但是退出了游戏",
            "§f房间: §e" + room.getModeName() + " §7| §e" + room.getRoomId(),
            "§f你是否要返回游戏？",
            "§8· · · · · · · · · · · · · ·"));

        // 返回游戏按钮
        inventory.setItem(11, createItem(Material.LIME_CONCRETE,
            "   §8[§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7返§x§9§9§F§F§9§9回§x§B§B§F§F§B§B游§x§D§D§F§F§D§D戏§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §a点击返回游戏",
            "§f- §e以猎人身份继续游戏",
            "§8· · · · · · · · · · · · · ·"));

        // 旁观按钮
        inventory.setItem(13, createItem(Material.SPYGLASS,
            "   §8[§x§8§8§D§D§F§F👁 §x§9§9§D§D§F§F旁§x§A§A§D§D§F§F观§x§B§B§D§D§F§F游§x§C§C§D§D§F§F戏§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §b点击以旁观者身份进入",
            "§f- §7放弃猎人身份",
            "§8· · · · · · · · · · · · · ·"));

        // 取消按钮
        inventory.setItem(15, createItem(Material.RED_CONCRETE,
            "   §8[§x§F§F§5§5§5§5✗ §x§F§F§7§7§7§7取§x§F§F§9§9§9§9消§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §c点击取消",
            "§f- §7返回房间列表",
            "§8· · · · · · · · · · · · · ·"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 11 -> {
                // 返回游戏
                playConfirmSound();
                player.closeInventory();
                if (!plugin.getRoomManager().rejoinGame(player)) {
                    player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("room.rejoin_failed"));
                }
            }
            case 13 -> {
                // 旁观
                playClickSound();
                // 先清除断线记录，再旁观
                plugin.getRoomManager().clearDisconnectedHunter(player.getUniqueId());
                plugin.getRoomManager().spectateRoom(player, room);
            }
            case 15 -> {
                // 取消
                playCancelSound();
                new RoomListMenu(plugin, player, menuSection, modeFilter).open();
            }
        }
    }
}
