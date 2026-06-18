package org.gamefunxiao.flash;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class FlashModeKeys {

    public static final String ENCHANTMENT_NAMESPACE = "gamefunxiao";
    public static final String UPGRADE_ENCHANTMENT_ID = "flash_upgrade_modification";

    private FlashModeKeys() {
    }

    public static NamespacedKey createUpgradeEnchantmentKey(Plugin plugin) {
        return new NamespacedKey(plugin, UPGRADE_ENCHANTMENT_ID);
    }
}
