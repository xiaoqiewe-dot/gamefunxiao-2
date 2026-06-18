package org.gamefunxiao.leaderboard;

import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.data.PlayerData;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final GameFunXiao plugin;

    public LeaderboardManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    public List<PlayerData> getLeaderboard(String type, String timeRange) {
        return getLeaderboard(type, timeRange, "all", "all");
    }

    public List<PlayerData> getLeaderboard(String type, String timeRange, String roleType) {
        return getLeaderboard(type, timeRange, roleType, "all");
    }

    public List<PlayerData> getLeaderboard(String type, String timeRange, String roleType, String modeId) {
        return getLeaderboard(type, timeRange, roleType, modeId, null);
    }

    public List<PlayerData> getLeaderboard(String type, String timeRange, String roleType, String modeId, Set<String> modeIds) {
        Collection<PlayerData> allData = plugin.getPlayerDataManager().getAllPlayerData();
        String normalizedModeId = modeId == null || modeId.isBlank() ? "all" : modeId;
        Set<String> normalizedModeIds = normalizeModeIds(modeIds);

        Comparator<PlayerData> comparator = switch (type) {
            case "pass_count" -> {
                if (roleType.equals("prey")) {
                    yield Comparator.comparingInt((PlayerData d) -> getPreyWins(d, timeRange, normalizedModeIds)).reversed();
                } else if (roleType.equals("hunter")) {
                    yield Comparator.comparingInt((PlayerData d) -> getHunterKills(d, timeRange, normalizedModeIds)).reversed();
                } else {
                    yield Comparator.comparingInt((PlayerData d) ->
                        getPreyWins(d, timeRange, normalizedModeIds) + getHunterKills(d, timeRange, normalizedModeIds)).reversed();
                }
            }
            case "fastest_time" -> (d1, d2) -> {
                long t1 = d1.getFastestTime(timeRange, normalizedModeId);
                long t2 = d2.getFastestTime(timeRange, normalizedModeId);
                if (t1 == 0 && t2 == 0) return 0;
                if (t1 == 0) return 1;
                if (t2 == 0) return -1;
                return Long.compare(t1, t2);
            };
            case "play_count" -> Comparator.comparingInt((PlayerData d) ->
                getPlayCount(d, timeRange, normalizedModeIds)).reversed();
            case "hunter_points" -> Comparator.comparingInt((PlayerData d) ->
                getHunterPoints(d, timeRange, normalizedModeIds)).reversed();
            case "prey_points" -> Comparator.comparingInt((PlayerData d) ->
                getPreyPoints(d, timeRange, normalizedModeIds)).reversed();
            case "minigame_points" -> Comparator.comparingInt((PlayerData d) ->
                getMiniGamePoints(d, timeRange, normalizedModeIds)).reversed();
            default -> Comparator.comparingInt((PlayerData d) ->
                getPlayCount(d, timeRange, normalizedModeIds)).reversed();
        };

        return allData.stream()
            .filter(d -> hasRelevantData(d, type, timeRange, roleType, normalizedModeId, normalizedModeIds))
            .sorted(comparator)
            .limit(100)
            .collect(Collectors.toList());
    }

    private boolean hasRelevantData(PlayerData data, String type, String timeRange, String roleType) {
        return hasRelevantData(data, type, timeRange, roleType, "all");
    }

    private boolean hasRelevantData(PlayerData data, String type, String timeRange, String roleType, String modeId) {
        return hasRelevantData(data, type, timeRange, roleType, modeId, null);
    }

    private boolean hasRelevantData(PlayerData data, String type, String timeRange, String roleType, String modeId, Set<String> modeIds) {
        return switch (type) {
            case "pass_count" -> {
                if (roleType.equals("prey")) {
                    yield getPreyWins(data, timeRange, modeIds) > 0;
                } else if (roleType.equals("hunter")) {
                    yield getHunterKills(data, timeRange, modeIds) > 0;
                } else {
                    yield getPreyWins(data, timeRange, modeIds) + getHunterKills(data, timeRange, modeIds) > 0;
                }
            }
            case "fastest_time" -> data.getFastestTime(timeRange, modeId) > 0;
            case "play_count" -> getPlayCount(data, timeRange, modeIds) > 0;
            case "hunter_points" -> getHunterPoints(data, timeRange, modeIds) > 0;
            case "prey_points" -> getPreyPoints(data, timeRange, modeIds) > 0;
            case "minigame_points" -> getMiniGamePoints(data, timeRange, modeIds) > 0;
            default -> true;
        };
    }

    public int getPlayerRank(UUID uuid, String type, String timeRange) {
        List<PlayerData> leaderboard = getLeaderboard(type, timeRange);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    public int getPlayerRank(UUID uuid, String type, String timeRange, String roleType, String modeId) {
        List<PlayerData> leaderboard = getLeaderboard(type, timeRange, roleType, modeId);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    private Set<String> normalizeModeIds(Set<String> modeIds) {
        if (modeIds == null) {
            return null;
        }
        return modeIds.stream()
                .filter(Objects::nonNull)
                .map(id -> id.toLowerCase(Locale.ROOT).trim())
                .filter(id -> !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int getPreyWins(PlayerData data, String timeRange, Set<String> modeIds) {
        if (modeIds == null) {
            return data.getPreyWins(timeRange);
        }
        return modeIds.stream().mapToInt(modeId -> data.getPreyWins(timeRange, modeId)).sum();
    }

    private int getHunterKills(PlayerData data, String timeRange, Set<String> modeIds) {
        if (modeIds == null) {
            return data.getHunterKills(timeRange);
        }
        return modeIds.stream().mapToInt(modeId -> data.getHunterKills(timeRange, modeId)).sum();
    }

    private int getPlayCount(PlayerData data, String timeRange, Set<String> modeIds) {
        if (modeIds == null) {
            return data.getPlayCount(timeRange);
        }
        return modeIds.stream().mapToInt(modeId -> data.getPlayCount(timeRange, modeId)).sum();
    }

    private int getHunterPoints(PlayerData data, String timeRange, Set<String> modeIds) {
        if (modeIds == null) {
            return data.getHunterPoints(timeRange);
        }
        return modeIds.stream().mapToInt(modeId -> data.getHunterPoints(timeRange, modeId)).sum();
    }

    private int getPreyPoints(PlayerData data, String timeRange, Set<String> modeIds) {
        if (modeIds == null) {
            return data.getPreyPoints(timeRange);
        }
        return modeIds.stream().mapToInt(modeId -> data.getPreyPoints(timeRange, modeId)).sum();
    }

    private int getMiniGamePoints(PlayerData data, String timeRange, Set<String> modeIds) {
        if (modeIds == null) {
            return data.getMiniGamePoints(timeRange);
        }
        return modeIds.stream().mapToInt(modeId -> data.getMiniGamePoints(timeRange, modeId)).sum();
    }
}
