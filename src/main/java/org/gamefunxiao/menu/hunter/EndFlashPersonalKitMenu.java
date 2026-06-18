package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

public class EndFlashPersonalKitMenu extends BaseMenu {

    public EndFlashPersonalKitMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 个人终章Kit ✦", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.END_CRYSTAL,
                "§x§B§B§8§8§F§F✦ §x§D§D§A§A§F§F个§x§F§F§D§D§A§A人§x§E§E§C§C§F§F终§x§D§D§B§B§F§F章 §x§B§B§8§8§F§FKit",
                "§8· · · · · · · · · · · · · ·",
                "§f- §d这里普通玩家也可以打开",
                "§f- §b选择某个终章 Kit 后，可以调整自己的摆放",
                "§f- §a不会改掉管理员设置的公共 Kit",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(20, createRoleButton(EndFlashKitManager.Role.HUNTER, false));
        inventory.setItem(22, createRoleButton(EndFlashKitManager.Role.PREY, false));
        inventory.setItem(24, createEnderChestButton());
        inventory.setItem(45, createBackButton());
    }

    private org.bukkit.inventory.ItemStack createRoleButton(EndFlashKitManager.Role role, boolean enderChest) {
        int count = plugin.getEndFlashKitManager().getKits(role).size();
        Material material = role == EndFlashKitManager.Role.HUNTER ? Material.NETHERITE_SWORD : Material.ENDER_EYE;
        String color = role == EndFlashKitManager.Role.HUNTER ? "§x§F§F§8§8§8§8" : "§x§8§8§D§D§F§F";
        return createItem(material,
                "   §8[" + color + role.displayName() + "背包§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e可用Kit: §b" + count + " §7个",
                "§f- §a点击选择要调整摆放的 " + role.displayName() + " Kit",
                "§f- §d开局会按管理员设置概率随机抽取",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击进入");
    }

    private org.bukkit.inventory.ItemStack createEnderChestButton() {
        return createItem(Material.ENDER_CHEST,
                "   §8[§x§B§B§8§8§F§F个§x§D§D§A§A§F§F人§x§F§F§D§D§A§A末§x§D§D§F§F§C§C影§x§B§B§F§F§E§E箱§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b选择某个终章 Kit 的末影箱",
                "§f- §d只调整你自己的末影箱摆放",
                "§f- §7公共 Kit 不会被修改",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击进入");
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
                        EndFlashPersonalKitListMenu.EditType.INVENTORY, 0).open();
            }
            case 22 -> {
                playClickSound();
                new EndFlashPersonalKitListMenu(plugin, player, EndFlashKitManager.Role.PREY,
                        EndFlashPersonalKitListMenu.EditType.INVENTORY, 0).open();
            }
            case 24 -> {
                playClickSound();
                new EndFlashPersonalEnderRoleMenu(plugin, player).open();
            }
            case 45 -> {
                playClickSound();
                plugin.getMenuManager().openHunterGameMenu(player);
            }
            default -> {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().contains("STAINED_GLASS")) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.35f, 1.65f);
                }
            }
        }
    }
}
