package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.List;

public class VoteDoublePreyMenu extends BaseMenu {

    private final GameRoom room;

    public VoteDoublePreyMenu(GameFunXiao plugin, org.bukkit.entity.Player player, GameRoom room) {
        super(plugin, player, "§0§l📜 双猎物投票 📜", 45);
        this.room = room;
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        int required = plugin.getGameManager().getDoublePreyRequiredYesVotes(room);
        int current = room.getDoublePreyVoteCount();

        inventory.setItem(4, createTitleItem(Material.TOTEM_OF_UNDYING,
                "§x§F§F§8§8§5§5📜 §x§F§F§B§B§6§6双§x§F§F§D§D§8§8猎§x§D§D§F§F§A§A物§x§B§B§F§F§C§C投§x§9§9§F§F§E§E票",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前同意: §6" + current + " §7/ §e需要: §6" + required,
                "§f- §a达到猎人一半同意后立即开启",
                "§f- §d开启后会锁定第一猎物并重开第二轮投票",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(22, createAgreeItem(current, required));
        inventory.setItem(40, createCloseButton());
    }

    private ItemStack createAgreeItem(int current, int required) {
        ItemStack item = new ItemStack(Material.PAPER);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§5§5§F§F§5§5✔ §x§7§7§F§F§7§7同§x§9§9§F§F§9§9意§x§B§B§F§F§B§B开§x§D§D§F§F§D§D启§f双猎物");
            meta.setCustomModelData(10012);
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("paper"));
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §a点击后立刻投出同意票");
            lore.add("§f- §b当前进度: §e" + current + " §7/ §e" + required);
            if (room.hasVotedDoublePrey(player.getUniqueId())) {
                lore.add("§f- §6你已经投过同意票了");
            } else {
                lore.add("§f- §d本菜单只有同意票，没有反对票");
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (slot == 22) {
            if (room.hasVotedDoublePrey(player.getUniqueId())) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.double_prey_vote_same"));
                return;
            }

            playConfirmSound();
            plugin.getGameManager().handleDoublePreyVote(player, room);
            player.closeInventory();
            return;
        }

        if (slot == 40) {
            handleCloseButtonAction();
        }
    }
}
