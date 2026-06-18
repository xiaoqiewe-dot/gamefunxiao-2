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

public class ConfirmSpectateMenu extends BaseMenu {

    private final GameRoom room;
    private final MenuSection menuSection;
    private final Set<GameMode> modeFilter;

    public ConfirmSpectateMenu(GameFunXiao plugin, Player player, GameRoom room) {
        this(plugin, player, room, MenuSection.HUNTER, RoomListMenu.defaultHunterFilter());
    }

    public ConfirmSpectateMenu(GameFunXiao plugin, Player player, GameRoom room, MenuSection menuSection, Set<GameMode> modeFilter) {
        super(plugin, player, "§0§l❓ 确认旁观 ❓", 27);
        this.room = room;
        this.menuSection = menuSection;
        this.modeFilter = modeFilter;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.SPYGLASS,
            "§x§F§F§E§E§5§5❓ §x§F§F§D§D§7§7确§x§F§F§C§C§9§9认§x§F§F§B§B§B§B旁§x§F§F§A§A§D§D观 §x§F§F§E§E§5§5❓",
            "§8· · · · · · · · · · · · · ·",
            "§f你确定要旁观这场游戏吗?",
            "§f房间: §e" + room.getModeName(),
            "§8· · · · · · · · · · · · · ·"));

        // 确认按钮
        inventory.setItem(11, createItem(Material.LIME_CONCRETE,
            "   §8[§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7确§x§9§9§F§F§9§9认§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §a点击确认旁观",
            "§f- §e你将进入游戏世界",
            "§f- §b以旁观者模式观看",
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
                // 确认旁观
                playConfirmSound();
                player.closeInventory();
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
