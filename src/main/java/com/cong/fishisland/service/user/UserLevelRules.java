package com.cong.fishisland.service.user;

public final class UserLevelRules {
    private static final int[] THRESHOLDS = {0, 125, 300, 600, 1100, 2100, 4100, 6000, 8000,
            10000, 12000, 14000, 16000, 18000};
    public static final int MAX_LEVEL = THRESHOLDS.length;

    private UserLevelRules() {
    }

    public static int levelAtPoints(int points) {
        for (int i = THRESHOLDS.length - 1; i > 0; i--) {
            if (points >= THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }
}
