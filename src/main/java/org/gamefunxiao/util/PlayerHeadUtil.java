package org.gamefunxiao.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public final class PlayerHeadUtil {

    private PlayerHeadUtil() {
    }

    public static void applyPlayerSkin(SkullMeta meta, Player target) {
        if (target == null) {
            return;
        }
        applyPlayerSkin(meta, target.getUniqueId(), target.getName());
    }

    public static void applyPlayerSkin(SkullMeta meta, UUID uuid, String fallbackName) {
        if (meta == null || uuid == null) {
            return;
        }

        String playerName = getBestPlayerName(uuid, fallbackName);
        try {
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                PlayerProfile profile = onlinePlayer.getPlayerProfile();
                if (profile != null) {
                    if (!profile.hasTextures()) {
                        profile.completeFromCache(true, false);
                    }
                    meta.setPlayerProfile(profile);
                    return;
                }
            }

            PlayerProfile profile = playerName == null || playerName.isBlank()
                    ? Bukkit.createProfile(uuid)
                    : Bukkit.createProfile(uuid, playerName);
            if (profile != null) {
                profile.completeFromCache(true, false);
                meta.setPlayerProfile(profile);
                return;
            }
        } catch (Throwable ignored) {
            // 旧缓存或离线模式没有皮肤缓存时，继续走 Bukkit 的兜底拥有者逻辑。
        }

        try {
            OfflinePlayer offlinePlayer = playerName == null || playerName.isBlank()
                    ? Bukkit.getOfflinePlayer(uuid)
                    : Bukkit.getOfflinePlayer(playerName);
            meta.setOwningPlayer(offlinePlayer);
        } catch (Throwable ignored) {
            // 头颅皮肤不是核心逻辑，不能因为缓存异常影响菜单打开。
        }
    }

    public static String getBestPlayerName(UUID uuid, String fallbackName) {
        if (uuid == null) {
            return fallbackName == null ? "未知玩家" : fallbackName;
        }
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return onlinePlayer.getName();
        }
        if (fallbackName != null && !fallbackName.isBlank() && !"Unknown".equalsIgnoreCase(fallbackName)) {
            return fallbackName;
        }
        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
        return offlineName == null || offlineName.isBlank() ? "未知玩家" : offlineName;
    }
}
