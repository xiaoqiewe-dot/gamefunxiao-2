package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.HunterVictoryEffect;
import org.gamefunxiao.menu.base.BaseMenu;

public class GameSettingsMenu extends BaseMenu {

    public GameSettingsMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ GameFun 设置 ✦", 45);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        boolean compact = plugin.getPlayerDataManager().isCompactMessages(player.getUniqueId());

        inventory.setItem(4, createTitleItem(Material.BELL,
                "§x§8§8§D§D§F§F⚙ §x§9§9§E§E§F§F消§x§A§A§F§F§F§F息§x§B§B§F§F§E§E与§x§C§C§F§F§D§D音§x§D§D§F§F§C§C效§x§E§E§F§F§B§B设§x§F§F§F§F§A§A置",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里只管消息频繁程度和提示音效",
                "§f- §a不会影响其他玩家",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(22, createItem(Material.BELL,
                "   §8[§x§8§8§D§D§F§F⚙ §x§A§A§F§F§F§F消§x§B§B§F§F§E§E息§x§C§C§F§F§D§D频§x§D§D§F§F§C§C繁§x§E§E§F§F§B§B程§x§F§F§F§F§A§A度§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b当前模式: " + (compact ? "§b正常" : "§d繁多"),
                "§f- §a左键切换为繁多",
                "§f- §a右键切换为正常",
                compact
                        ? "§f- §7只保留重要提示，并减少很多次要音效"
                        : "§f- §7保留完整提示、倒计时提示与完整音效",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(36, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        switch (slot) {
            case 22 -> {
                boolean compact = event.isRightClick();
                plugin.getPlayerDataManager().setMessageFrequency(player.getUniqueId(), compact ? "normal" : "chatty");
                player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("general.success")
                        + (compact ? "§b消息频繁程度已切换为 §f正常" : "§d消息频繁程度已切换为 §f繁多"));
                player.playSound(player.getLocation(), compact ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.BLOCK_NOTE_BLOCK_CHIME,
                        0.75f, compact ? 1.1f : 1.35f);
                open();
            }
            case 36 -> {
                playClickSound();
                plugin.getMenuManager().openSettingsCategoryMenu(player);
            }
        }
    }
}
