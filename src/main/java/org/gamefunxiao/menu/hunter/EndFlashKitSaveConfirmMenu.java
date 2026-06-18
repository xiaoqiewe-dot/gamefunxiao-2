package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

public class EndFlashKitSaveConfirmMenu extends BaseMenu {

    private static final int INFO_SLOT = 4;
    private static final int CONFIRM_SLOT = 22;
    private static final int BACK_SLOT = 40;
    private static final long WAIT_MILLIS = 3000L;

    private final EndFlashKitManager.Role role;
    private final String kitId;
    private final int backPage;
    private final EndFlashKitManager.KitLayout layout;
    private final boolean importedButNotSaved;
    private final long readyAtMillis;

    public EndFlashKitSaveConfirmMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, String kitId,
                                      int backPage, ItemStack[] storageContents, ItemStack[] armorContents,
                                      ItemStack offHandItem, boolean importedButNotSaved) {
        super(plugin, player, "§0§l确认保存Kit", 45);
        this.role = role;
        this.kitId = kitId;
        this.backPage = Math.max(0, backPage);
        this.layout = new EndFlashKitManager.KitLayout(storageContents, armorContents, offHandItem);
        this.importedButNotSaved = importedButNotSaved;
        this.readyAtMillis = System.currentTimeMillis() + WAIT_MILLIS;
    }

    @Override
    public void open() {
        super.open();
        scheduleCountdownRefresh(20L);
        scheduleCountdownRefresh(40L);
        scheduleCountdownRefresh(60L);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillBorder();

        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().getKit(kitId);
        String kitName = kit == null ? kitId : kit.displayName();
        inventory.setItem(INFO_SLOT, createItem(Material.WRITABLE_BOOK,
                "   §8[§x§B§B§8§8§F§F保存确认§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §dKit: §r" + kitName,
                "§f- §b即将覆盖这个 Kit 的快捷栏、背包、盔甲和副手",
                kit == null ? "§f- §d最后编辑: §7未知" : "§f- §d最后编辑: §f" + kit.lastEditorName() + " §8/ §e" + kit.lastEditedAtText(),
                "§f- §7为了防止误点，需要等待 §e3秒 §7后再确认",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(CONFIRM_SLOT, createConfirmButton());
        inventory.setItem(BACK_SLOT, createBackButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == BACK_SLOT) {
            playClickSound();
            new EndFlashKitDetailMenu(plugin, player, role, kitId, backPage,
                    layout.storageContents(), layout.armorContents(), layout.offHandItem(), importedButNotSaved).open();
            return;
        }
        if (slot != CONFIRM_SLOT) {
            playClickSound();
            return;
        }
        if (!isReady()) {
            long seconds = remainingSeconds();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§B§B§6§6⌛ §e还要等待 §f" + seconds + " §e秒，才可以确定保存。");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.68f, 0.76f);
            setupItems();
            return;
        }
        confirmSave();
    }

    private void confirmSave() {
        if (!plugin.getEndFlashKitManager().replaceLayout(kitId, layout, player)) {
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c保存失败，请确认这个 Kit 至少有一个物品。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.72f, 0.82f);
            new EndFlashKitDetailMenu(plugin, player, role, kitId, backPage,
                    layout.storageContents(), layout.armorContents(), layout.offHandItem(), importedButNotSaved).open();
            return;
        }
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✔ §a已保存这个 Kit 的快捷栏、背包、盔甲和副手。");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.72f, 1.55f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.45f, 1.2f);
        new EndFlashKitDetailMenu(plugin, player, role, kitId, backPage).open();
    }

    private ItemStack createConfirmButton() {
        if (!isReady()) {
            return createItem(Material.CLOCK,
                    "   §8[§x§F§F§B§B§6§6等待确认§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §e剩余: §f" + remainingSeconds() + " §7秒",
                    "§f- §7倒计时结束后才可以保存",
                    "§8· · · · · · · · · · · · · ·");
        }
        return createItem(Material.EMERALD_BLOCK,
                "   §8[§x§5§5§F§F§A§A确定保存§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a现在可以确认保存",
                "§f- §b点击后会覆盖这个 Kit 的装备内容",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击确定");
    }

    private void fillBorder() {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE,
                "§8你看我干什么",
                "§8虽然你点我也没有用ewe");
        ItemStack corner = createItem(Material.PURPLE_STAINED_GLASS_PANE,
                "§d你看我干什么",
                "§8虽然你点我也没有用ewe");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                inventory.setItem(slot, border);
            }
        }
        inventory.setItem(0, corner);
        inventory.setItem(8, corner);
        inventory.setItem(36, corner);
        inventory.setItem(44, corner);
    }

    private boolean isReady() {
        return System.currentTimeMillis() >= readyAtMillis;
    }

    private long remainingSeconds() {
        long remaining = Math.max(0L, readyAtMillis - System.currentTimeMillis());
        return Math.max(1L, (remaining + 999L) / 1000L);
    }

    private void scheduleCountdownRefresh(long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() != this) {
                return;
            }
            setupItems();
            player.updateInventory();
            if (isReady()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.48f, 1.65f);
            }
        }, delayTicks);
    }
}
