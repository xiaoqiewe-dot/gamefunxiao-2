package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EndFlashPersonalKitListMenu extends BaseMenu {

    public enum EditType {
        INVENTORY,
        ENDER_CHEST
    }

    private static final int ITEMS_PER_PAGE = 45;

    private final EndFlashKitManager.Role role;
    private final EditType editType;
    private int page;

    public EndFlashPersonalKitListMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, EditType editType, int page) {
        super(plugin, player, "§0§l" + role.displayName() + (editType == EditType.ENDER_CHEST ? "末影箱" : "背包"), 54);
        this.role = role;
        this.editType = editType == null ? EditType.INVENTORY : editType;
        this.page = Math.max(0, page);
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
        int maxPage = Math.max(0, (kits.size() - 1) / ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
        }

        int start = page * ITEMS_PER_PAGE;
        for (int slot = 0; slot < ITEMS_PER_PAGE && start + slot < kits.size(); slot++) {
            EndFlashKitManager.Kit kit = kits.get(start + slot);
            inventory.setItem(slot, createKitButton(kit, kits));
        }
        if (kits.isEmpty()) {
            inventory.setItem(22, createItem(Material.STRUCTURE_VOID,
                    "   §8[§7暂无终章Kit§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c管理员还没有给 " + role.displayName() + " 添加 Kit",
                    "§f- §7没有可编辑的个人摆放",
                    "§8· · · · · · · · · · · · · ·"));
        }

        inventory.setItem(45, createBackButton());
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }
        inventory.setItem(49, createInfoButton());
        if ((page + 1) * ITEMS_PER_PAGE < kits.size()) {
            inventory.setItem(50, createNextPageButton());
        }
    }

    private ItemStack createKitButton(EndFlashKitManager.Kit kit, List<EndFlashKitManager.Kit> sameRoleKits) {
        Material material = Material.CHEST;
        if (editType == EditType.ENDER_CHEST) {
            material = Material.ENDER_CHEST;
        } else if (!kit.items().isEmpty()) {
            material = kit.items().get(0).getType();
        }

        double total = sameRoleKits.stream().mapToDouble(EndFlashKitManager.Kit::chance).sum();
        double balanced = total <= 0 ? 100.0D / Math.max(1, sameRoleKits.size()) : kit.chance() * 100.0D / total;

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §eID: §7" + kit.id());
        lore.add("§f- §e输入权重: §d" + EndFlashKitManager.formatChance(kit.chance()) + " §7/ §d50");
        lore.add("§f- §e实际权重: §b" + String.format(Locale.ROOT, "%.1f", balanced) + "%");
        lore.add("§f- §e背包物品: §a" + kit.items().size() + " §7件");
        lore.add("§f- §e末影箱格数: §5" + kit.enderChestSize() + " §7格");
        lore.add("§f- §e末影箱物品: §d" + kit.enderChestItems().size() + " §7件");
        lore.add("§f- §6开局抽取: §a按概率随机");
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add(editType == EditType.ENDER_CHEST ? "§f- §a左键编辑你的末影箱摆放" : "§f- §a左键编辑你的背包摆放");
        lore.add("§f- §b右键同样进入编辑，开局不会固定Kit");

        return createItem(material, "   §8[§x§D§D§A§A§F§F" + kit.displayName() + "§8]", lore);
    }

    private ItemStack createInfoButton() {
        return createItem(editType == EditType.ENDER_CHEST ? Material.ENDER_EYE : Material.BOOK,
                "   §8[§x§8§8§D§D§F§F个人编辑说明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b左键某个 Kit 进入个人编辑",
                "§f- §d右键某个 Kit 也会进入编辑",
                "§f- §a终章开局会按管理员概率随机抽取",
                "§f- §7保存后只影响你自己，不会覆盖公共 Kit",
                "§8· · · · · · · · · · · · · ·");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (slot < ITEMS_PER_PAGE) {
            List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
            int index = page * ITEMS_PER_PAGE + slot;
            if (index < kits.size()) {
                EndFlashKitManager.Kit kit = kits.get(index);
                if (editType == EditType.ENDER_CHEST) {
                    playClickSound();
                    new EndFlashPersonalEnderChestMenu(plugin, player, role, kit.id(), page).open();
                } else {
                    playClickSound();
                    new EditEndFlashPersonalKitLayoutMenu(plugin, player, role, kit.id(), page).open();
                }
            }
            return;
        }

        switch (slot) {
            case 45 -> {
                playClickSound();
                if (editType == EditType.ENDER_CHEST) {
                    new EndFlashPersonalEnderRoleMenu(plugin, player).open();
                } else {
                    new EndFlashPersonalKitMenu(plugin, player).open();
                }
            }
            case 48 -> {
                if (page > 0) {
                    playPageTurnSound();
                    page--;
                    setupItems();
                }
            }
            case 50 -> {
                List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
                if ((page + 1) * ITEMS_PER_PAGE < kits.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            default -> {
            }
        }
    }
}

