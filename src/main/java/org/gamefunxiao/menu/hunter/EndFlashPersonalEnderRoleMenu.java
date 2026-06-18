package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

public class EndFlashPersonalEnderRoleMenu extends BaseMenu {

    public EndFlashPersonalEnderRoleMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 个人末影箱 ✦", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();
        inventory.setItem(4, createTitleItem(Material.ENDER_CHEST,
                "§x§B§B§8§8§F§F✦ §x§D§D§A§A§F§F个§x§F§F§D§D§A§A人§x§D§D§F§F§C§C末§x§B§B§F§F§E§E影箱",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b先选择身份，再选择某个 Kit",
                "§f- §d保存后只有你自己会使用这份末影箱摆放",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(20, createRoleButton(EndFlashKitManager.Role.HUNTER));
        inventory.setItem(24, createRoleButton(EndFlashKitManager.Role.PREY));
        inventory.setItem(45, createBackButton());
    }

    private org.bukkit.inventory.ItemStack createRoleButton(EndFlashKitManager.Role role) {
        int count = plugin.getEndFlashKitManager().getKits(role).size();
        Material material = role == EndFlashKitManager.Role.HUNTER ? Material.CROSSBOW : Material.ENDER_EYE;
        String color = role == EndFlashKitManager.Role.HUNTER ? "§x§F§F§8§8§8§8" : "§x§8§8§D§D§F§F";
        return createItem(material,
                "   §8[" + color + role.displayName() + "末影箱§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e可用Kit: §b" + count + " §7个",
                "§f- §a点击选择要调整末影箱的 " + role.displayName() + " Kit",
                "§8· · · · · · · · · · · · · ·");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        switch (slot) {
            case 20 -> {
                playClickSound();
                new EndFlashPersonalKitListMenu(plugin, player, EndFlashKitManager.Role.HUNTER,
                        EndFlashPersonalKitListMenu.EditType.ENDER_CHEST, 0).open();
            }
            case 24 -> {
                playClickSound();
                new EndFlashPersonalKitListMenu(plugin, player, EndFlashKitManager.Role.PREY,
                        EndFlashPersonalKitListMenu.EditType.ENDER_CHEST, 0).open();
            }
            case 45 -> {
                playClickSound();
                new EndFlashPersonalKitMenu(plugin, player).open();
            }
            default -> {
            }
        }
    }
}
