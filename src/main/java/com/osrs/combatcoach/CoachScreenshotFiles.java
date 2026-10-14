package com.osrs.combatcoach;

import java.util.Locale;
import net.runelite.client.RuneLite;
import net.runelite.client.util.Filepath;

/**
 * The sole filesystem capability boundary for the screenshot gallery.
 *
 * ImageCapture still owns PNG creation. Its legacy output is outside a plugin's
 * data directory, so ONE trusted-root Filepath.Unchecked call is required. This
 * restricted API must be manually reviewed; it is not an automatic-review claim.
 * No profile value can select a root. Only PNGs in a direct player/Milestones
 * subdirectory of RuneLite.SCREENSHOT_DIR can be read. No recursive scanning.
 */
public final class CoachScreenshotFiles
{
    private CoachScreenshotFiles() {}

    private static Filepath root()
    {
        return Filepath.Unchecked.getRooted(RuneLite.SCREENSHOT_DIR.toPath());
    }

    public static Filepath resolve(String storedPath)
    {
        Filepath root = root();
        String relative = relativeMilestonePath(root.toString(), storedPath);
        Filepath result = root;
        for (String segment : relative.split("/"))
        {
            result = result.joinSegment(segment);
        }
        return result;
    }

    public static String folder(String storedPath)
    {
        return storedPath == null || storedPath.isEmpty()
            ? root().toString() : resolve(storedPath).getParent().toString();
    }

    /** Pure validation, also exercised with Windows and Unix path fixtures. */
    static String relativeMilestonePath(String trustedRoot, String storedPath)
    {
        if (trustedRoot == null || storedPath == null || storedPath.length() > 4096)
        {
            throw new IllegalArgumentException("Invalid capture path");
        }
        String base = trustedRoot.replace('\\', '/');
        String path = storedPath.replace('\\', '/');
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!path.startsWith(base + "/"))
        {
            throw new IllegalArgumentException("Capture is outside this RuneLite screenshot folder");
        }
        String relative = path.substring(base.length() + 1);
        String[] parts = relative.split("/", -1);
        if (parts.length != 3 || !"Milestones".equals(parts[1]))
        {
            throw new IllegalArgumentException("Only this plugin's Milestones captures can be opened");
        }
        for (String part : parts)
        {
            if (part.isEmpty() || part.equals(".") || part.equals("..") || part.indexOf(':') >= 0)
            {
                throw new IllegalArgumentException("Invalid capture path segment");
            }
        }
        String name = parts[2];
        if (!name.toLowerCase(Locale.ROOT).endsWith(".png") || !name.contains("-level-"))
        {
            throw new IllegalArgumentException("Only milestone PNG files can be previewed");
        }
        return relative;
    }
}
