package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

public class EditEndFlashPersonalKitLayoutMenu extends BaseMenu {

    private final EndFlashKitManager.Role role;
    private final String kitId;
    private final int backPage;
    private ItemStack[] storageContents;
    private ItemStack[] armorContents;
    private ItemStack offHandItem;

    public EditEndFlashPersonalKitLayoutMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId, int backPage) {
        super(plugin, player, resolveTitle(plugin, kitId), 54);
        this.role = role;
        this.kitId = kitId;
        this.backPage = Math.max(0, backPage);
    }

    private static String resolveTitle(GameFunXiao plugin, String kitId) {
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        return "§0§l" + (kit == null ? "个人Kit" : kit.displayName());
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        if (kit == null) {
            inventory.setItem(22, createItem(Material.BARRIER,
                    "   §8[§cKit不存在§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c这个 Kit 可能已经被删除",
                    "§8· · · · · · · · · · · · · ·"));
            inventory.setItem(45, createBackButton());
            return;
        }

        if (storageContents == null || armorContents == null) {
            EndFlashKitManager.KitLayout saved = plugin.getPlayerDataManager()
                    .getEndFlashPersonalKitLayout(player.getUniqueId(), role, kitId);
            if (saved != null) {
                storageContents = saved.storageContents();
                armorContents = saved.armorContents();
                offHandItem = saved.offHandItem();
            } else {
                storageContents = kit.storageContents();
                armorContents = kit.armorContents();
                offHandItem = kit.offHandItem();
            }
        }

        renderLayout();
        renderEquipment();
        renderButtons(kit);
    }

    @Override
    public boolean isEditableTopInventorySlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot <= 40;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (isEditableTopInventorySlot(slot)) {
            return;
        }
        switch (slot) {
            case 45 -> {
                playPageTurnSound();
                new EndFlashPersonalKitListMenu(plugin, player, role, EndFlashPersonalKitListMenu.EditType.INVENTORY, backPage).open();
            }
            case 49 -> saveLayout();
            case 53 -> resetLayout();
            default -> {
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() != Material.AIR) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setItemOnCursor(new ItemStack(Material.AIR)));
        }
    }

    private void renderLayout() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = slot < storageContents.length ? storageContents[slot] : null;
            inventory.setItem(slot, item == null ? null : item.clone());
        }
    }

    private void renderEquipment() {
        for (int slot = 36; slot <= 44; slot++) {
            inventory.setItem(slot, createItem(Material.GRAY_STAINED_GLASS_PANE,
                    "§8你看我干什么",
                    "§8虽然你点我也没有用ewe"));
        }
        inventory.setItem(36, cloneOrNull(getArmorItem(3)));
        inventory.setItem(37, cloneOrNull(getArmorItem(2)));
        inventory.setItem(38, cloneOrNull(getArmorItem(1)));
        inventory.setItem(39, cloneOrNull(getArmorItem(0)));
        inventory.setItem(40, cloneOrNull(offHandItem));
    }

    private void renderButtons(EndFlashKitManager.Kit kit) {
        inventory.setItem(45, createBackButton());
        inventory.setItem(46, createItem(Material.PURPLE_STAINED_GLASS_PANE,
                "§x§D§D§A§A§F§F拖动上方物品",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(47, createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§x§8§8§D§D§F§F第一行是快捷栏",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(48, createItem(Material.PINK_STAINED_GLASS_PANE,
                "§x§F§F§C§C§F§F第五行前五格是装备",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(49, createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A保存个人摆放§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a保存 §e" + role.displayName() + " §aKit 的个人背包",
                "§f- §dKit: §f" + kit.displayName(),
                "§f- §7只影响你自己",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(50, createItem(Material.BLUE_STAINED_GLASS_PANE,
                "§x§8§8§D§D§F§F关闭不会自动保存",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(51, createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§x§B§B§F§F§E§E不会覆盖公共Kit",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(52, createItem(Material.PURPLE_STAINED_GLASS_PANE,
                "§x§D§D§A§A§F§F右侧可恢复默认",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(53, createItem(Material.BARRIER,
                "   §8[§x§F§F§8§8§8§8恢复默认§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e清除你的个人摆放",
                "§f- §7下次使用公共 Kit 默认摆放",
                "§8· · · · · · · · · · · · · ·"));
    }

    private ItemStack getArmorItem(int index) {
        return armorContents == null || index < 0 || index >= armorContents.length ? null : armorContents[index];
    }

    private void saveLayout() {
        collectLayout();
        EndFlashKitManager.KitLayout layout = new EndFlashKitManager.KitLayout(storageContents, armorContents, offHandItem);
        if (!hasAnyLayout(layout)) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c不能保存空的个人 Kit 摆放。");
            return;
        }
        plugin.getPlayerDataManager().saveEndFlashPersonalKitLayout(player.getUniqueId(), role, kitId, layout);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✔ §a已保存 §e" + role.displayName() + " §a个人终章 Kit 摆放。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.72f, 1.55f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.42f, 1.25f);
    }

    private void resetLayout() {
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        if (kit == null) {
            playErrorSound();
            return;
        }
        plugin.getPlayerDataManager().clearEndFlashPersonalKitLayout(player.getUniqueId(), role, kitId);
        storageContents = kit.storageContents();
        armorContents = kit.armorContents();
        offHandItem = kit.offHandItem();
        setupItems();
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§D§D§8§8⌑ §e已恢复这个终章 Kit 的默认摆放。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.72f, 0.86f);
    }

    private void collectLayout() {
        storageContents = new ItemStack[36];
        for (int slot = 0; slot < 36; slot++) {
            storageContents[slot] = cloneOrNull(inventory.getItem(slot));
        }
        armorContents = new ItemStack[4];
        armorContents[3] = cloneOrNull(inventory.getItem(36));
        armorContents[2] = cloneOrNull(inventory.getItem(37));
        armorContents[1] = cloneOrNull(inventory.getItem(38));
        armorContents[0] = cloneOrNull(inventory.getItem(39));
        offHandItem = cloneOrNull(inventory.getItem(40));
    }

    private boolean hasAnyLayout(EndFlashKitManager.KitLayout layout) {
        if (layout == null) {
            return false;
        }
        for (ItemStack item : layout.storageContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        for (ItemStack item : layout.armorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return layout.offHandItem() != null && layout.offHandItem().getType() != Material.AIR;
    }

    private ItemStack cloneOrNull(ItemStack item) {
        return item == null || item.getType() == Material.AIR ? null : item.clone();
    }
}
