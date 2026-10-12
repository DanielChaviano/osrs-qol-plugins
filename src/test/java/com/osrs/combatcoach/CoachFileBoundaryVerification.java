package com.osrs.combatcoach;

/** Pure checks for the restricted screenshot read boundary; no user files touched. */
public final class CoachFileBoundaryVerification
{
    private static int checks;
    private static void rejected(String base, String path)
    {
        try { CoachScreenshotFiles.relativeMilestonePath(base, path); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Unsafe capture path accepted: " + path);
    }
    public static void main(String[] args)
    {
        String root = "C:\\Users\\example\\.runelite\\screenshots";
        String suffix = "player/Milestones/strength-level-20-test.png";
        if (!suffix.equals(CoachScreenshotFiles.relativeMilestonePath(root, root + "\\" + suffix.replace('/', '\\')))) throw new AssertionError("Windows path rejected");
        checks++;
        if (!suffix.equals(CoachScreenshotFiles.relativeMilestonePath("/home/example/.runelite/screenshots", "/home/example/.runelite/screenshots/" + suffix))) throw new AssertionError("Unix path rejected");
        checks++;
        rejected(root, root + "-other/" + suffix);
        rejected(root, root + "/../profiles2/default.properties");
        rejected(root, root + "/player/Milestones/../../credentials.properties");
        rejected(root, root + "/player/Other/strength-level-20.png");
        rejected(root, root + "/player/Milestones/strength-level-20.exe");
        rejected(root, root + "/player/Milestones/strength-level-20.png:secret");
        rejected(root, root + "/player/Milestones/normal.png");
        rejected(root, root + "/../Milestones/strength-level-20.png");
        rejected(root, root + "//Milestones/strength-level-20.png");
        rejected(root, "/tmp/player/Milestones/strength-level-20.png");
        rejected(root, null);
        System.out.println("FILE BOUNDARY CHECKS PASSED: " + checks);
    }
}
