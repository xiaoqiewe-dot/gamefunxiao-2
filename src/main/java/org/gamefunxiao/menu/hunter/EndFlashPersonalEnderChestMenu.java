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

public class EndFlashPersonalEnderChestMenu extends BaseMenu {

    private final EndFlashKitManager.Role role;
    private final String kitId;
    private final int backPage;
    private ItemStack[] contents;
    private int editSize;

    public EndFlashPersonalEnderChestMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId, int backPage) {
        super(plugin, player, resolveTitle(plugin, kitId), 54);
        this.role = role;
        this.kitId = kitId;
        this.backPage = Math.max(0, backPage);
    }

    private static String resolveTitle(GameFunXiao plugin, String kitId) {
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        return "§0§l" + (kit == null ? "个人末影箱" : kit.displayName());
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

        if (contents == null) {
            editSize = kit.enderChestSize();
            ItemStack[] saved = plugin.getPlayerDataManager()
                    .getEndFlashPersonalEnderChestLayout(player.getUniqueId(), role, kitId, editSize);
            contents = saved == null ? kit.enderChestContents() : saved;
        }

        for (int slot = 0; slot < 45; slot++) {
            if (slot < editSize) {
                ItemStack item = slot < contents.length ? contents[slot] : null;
                inventory.setItem(slot, item == null ? null : item.clone());
            } else {
                inventory.setItem(slot, createItem(Material.GRAY_STAINED_GLASS_PANE,
                        "§8你看我干什么",
                        "§8虽然你点我也没有用ewe"));
            }
        }
        renderButtons(kit);
    }

    @Override
    public boolean isEditableTopInventorySlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < editSize;
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
                new EndFlashPersonalKitListMenu(plugin, player, role, EndFlashPersonalKitListMenu.EditType.ENDER_CHEST, backPage).open();
            }
            case 49 -> saveContents();
            case 53 -> resetContents();
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

    private void renderButtons(EndFlashKitManager.Kit kit) {
        inventory.setItem(45, createBackButton());
        inventory.setItem(46, createItem(Material.PURPLE_STAINED_GLASS_PANE,
                "§x§D§D§A§A§F§F拖动上方末影箱",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(47, createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§x§8§8§D§D§F§F公共大小: §f" + editSize + "格",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(48, createItem(Material.PINK_STAINED_GLASS_PANE,
                "§x§F§F§C§C§F§F关闭不会自动保存",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(49, createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A保存个人末影箱§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a保存 §e" + role.displayName() + " §aKit 的个人末影箱",
                "§f- §dKit: §f" + kit.displayName(),
                "§f- §7只影响你自己",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(50, createItem(Material.BLUE_STAINED_GLASS_PANE,
                "§x§8§8§D§D§F§F不会覆盖公共Kit",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(51, createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§x§B§B§F§F§E§E按公共Kit大小编辑",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(52, createItem(Material.PURPLE_STAINED_GLASS_PANE,
                "§x§D§D§A§A§F§F右侧可恢复默认",
                "§8虽然你点我也没有用ewe"));
        inventory.setItem(53, createItem(Material.BARRIER,
                "   §8[§x§F§F§8§8§8§8恢复默认§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e清除你的个人末影箱摆放",
                "§f- §7下次使用公共 Kit 默认末影箱",
                "§8· · · · · · · · · · · · · ·"));
    }

    private void saveContents() {
        collectContents();
        plugin.getPlayerDataManager().saveEndFlashPersonalEnderChestLayout(player.getUniqueId(), role, kitId, contents, editSize);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✔ §a已保存 §e" + role.displayName() + " §a个人终章末影箱。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.72f, 1.32f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.42f, 1.55f);
    }

    private void resetContents() {
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        if (kit == null) {
            playErrorSound();
            return;
        }
        plugin.getPlayerDataManager().clearEndFlashPersonalEnderChestLayout(player.getUniqueId(), role, kitId);
        editSize = kit.enderChestSize();
        contents = kit.enderChestContents();
        setupItems();
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§F§F§D§D§8§8⌑ §e已恢复这个终章 Kit 的默认末影箱摆放。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.72f, 0.86f);
    }

    private void collectContents() {
        contents = new ItemStack[editSize];
        for (int slot = 0; slot < editSize; slot++) {
            ItemStack item = inventory.getItem(slot);
            contents[slot] = item == null || item.getType() == Material.AIR ? null : item.clone();
        }
    }
}
