package com.osrs.combatcoach;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.runelite.client.util.Filepath;

/** User-clicked local previews. Never launches an external program or forces window focus. */
public final class CoachImageActions implements AutoCloseable
{
    private final Executor executor;
    private final Consumer<String> notice;
    // Accessed only on Swing's event dispatch thread.
    private final Set<JDialog> windows = new HashSet<>();
    private volatile boolean closed;

    public CoachImageActions(Executor executor, Consumer<String> notice)
    {
        this.executor = executor;
        this.notice = notice;
    }

    public void preview(String storedPath)
    {
        if (closed) return;
        executor.execute(() -> {
            try
            {
                Filepath file = CoachScreenshotFiles.resolve(storedPath);
                BufferedImage image = CoachThumbnails.load(file, 1600, 1000);
                String path = file.toString();
                SwingUtilities.invokeLater(() -> {
                    if (closed) return;
                    // A manual viewer, not a change to the game client's window.
                    JDialog dialog = new JDialog((java.awt.Window) null, "Combat Coach capture", Dialog.ModalityType.MODELESS);
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    JPanel body = new JPanel(new BorderLayout());
                    body.add(new JScrollPane(new JLabel(new ImageIcon(image))), BorderLayout.CENTER);
                    JTextArea caption = new JTextArea(path);
                    caption.setEditable(false);
                    caption.setLineWrap(true);
                    caption.setWrapStyleWord(true);
                    caption.setRows(3);
                    body.add(caption, BorderLayout.SOUTH);
                    dialog.setContentPane(body);
                    dialog.setSize(new Dimension(Math.min(900, Math.max(340, image.getWidth() + 40)), Math.min(720, Math.max(320, image.getHeight() + 100))));
                    dialog.addWindowListener(new WindowAdapter() {
                        @Override public void windowClosed(WindowEvent event) { windows.remove(dialog); }
                    });
                    windows.add(dialog);
                    dialog.setVisible(true);
                });
            }
            catch (Exception error)
            {
                if (!closed) notice.accept("Capture preview unavailable: " + error.getMessage());
            }
        });
    }

    public void copyFolder(String storedPath)
    {
        if (closed) return;
        try
        {
            String path = CoachScreenshotFiles.folder(storedPath);
            SwingUtilities.invokeLater(() -> {
                if (closed) return;
                try
                {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path), null);
                    notice.accept("Capture folder path copied. Paste it into your file manager.");
                }
                catch (RuntimeException error)
                {
                    notice.accept("Capture folder: " + path);
                }
            });
        }
        catch (IllegalArgumentException error)
        {
            notice.accept(error.getMessage());
        }
    }

    @Override public void close()
    {
        closed = true;
        SwingUtilities.invokeLater(() -> {
            for (JDialog window : new ArrayList<>(windows)) window.dispose();
            windows.clear();
        });
    }
}
