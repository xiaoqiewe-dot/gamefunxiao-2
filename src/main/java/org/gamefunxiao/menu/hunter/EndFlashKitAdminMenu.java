package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class EndFlashKitAdminMenu extends BaseMenu {

    public EndFlashKitAdminMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l✦ 终章闪光Kit调试 ✦", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        inventory.setItem(4, createTitleItem(Material.END_CRYSTAL,
                "§x§B§B§8§8§F§F✦ §x§D§D§A§A§F§F终§x§F§F§D§D§A§A章§x§E§E§C§C§F§F闪§x§D§D§B§B§F§F光 §x§B§B§8§8§F§FKit调试",
                "§8· · · · · · · · · · · · · ·",
                "§f- §d在菜单里查看、创建和调整 Kit",
                "§f- §b创建后可以到详情页微调权重",
                "§f- §7权重支持小数，范围仍为 §e0~50",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(20, createRoleButton(EndFlashKitManager.Role.HUNTER));
        inventory.setItem(24, createRoleButton(EndFlashKitManager.Role.PREY));
        inventory.setItem(31, createReloadButton());
        inventory.setItem(45, createBackButton());
    }

    private org.bukkit.inventory.ItemStack createRoleButton(EndFlashKitManager.Role role) {
        List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
        double total = kits.stream().mapToDouble(EndFlashKitManager.Kit::chance).sum();
        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §eKit数量: §b" + kits.size());
        lore.add("§f- §e输入权重总和: §d" + EndFlashKitManager.formatChance(total));
        lore.add(kits.isEmpty() ? "§f- §7当前为空，开局会使用保底 Kit" : "§f- §a点击进入管理列表");
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §b左键打开 §e" + role.displayName() + " §bKit");

        Material material = role == EndFlashKitManager.Role.HUNTER ? Material.NETHERITE_SWORD : Material.ENDER_EYE;
        String color = role == EndFlashKitManager.Role.HUNTER ? "§x§F§F§6§6§6§6" : "§x§5§5§F§F§A§A";
        return createItem(material,
                "   §8[" + color + role.displayName() + "Kit池§8]",
                lore);
    }

    private org.bukkit.inventory.ItemStack createReloadButton() {
        return createItem(Material.AMETHYST_SHARD,
                "   §8[§x§B§B§D§D§F§F↻ §x§D§D§E§E§F§F重载Kit数据§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b重新读取 end_flash_kits.yml",
                "§f- §7用于你手动改文件后刷新",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击重载");
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
                new EndFlashKitRoleMenu(plugin, player, EndFlashKitManager.Role.HUNTER, 0).open();
            }
            case 24 -> {
                playClickSound();
                new EndFlashKitRoleMenu(plugin, player, EndFlashKitManager.Role.PREY, 0).open();
            }
            case 31 -> {
                plugin.getEndFlashKitManager().load();
                player.sendMessage(plugin.getConfigManager().getHunterGamePrefix() + "§x§B§B§D§D§F§F↻ §b已重载终章闪光 Kit 数据。");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.45f);
                setupItems();
            }
            case 45 -> {
                playClickSound();
                plugin.getMenuManager().openHunterGameMenu(player);
            }
            default -> {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().contains("STAINED_GLASS")) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.35f, 1.75f);
                }
            }
        }
    }
}
