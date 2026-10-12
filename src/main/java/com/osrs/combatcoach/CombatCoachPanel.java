package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/** Narrow RuneLite sidebar designed as cards, not a single wall of text. */
final class CombatCoachPanel extends PluginPanel {
    private static final Color BG = new Color(29, 32, 36);
    private static final Color CARD = new Color(39, 44, 50);
    private static final Color WHITE = new Color(240, 244, 249);
    private static final Color MUTED = new Color(176, 187, 200);
    private static final Color CYAN = new Color(93, 213, 240);
    private static final Color GREEN = new Color(112, 220, 156);
    private static final Color YELLOW = new Color(255, 205, 107);

    private final JLabel skill = label("Log in for recommendations", 14, WHITE, true);
    private final JTextArea target = emphasis("Waiting for game...", 16, GREEN);
    private final JTextArea rate = emphasis("Waiting for combat XP", 14, WHITE);
    private final JLabel statsA = label("-", 13, WHITE, false);
    private final JLabel statsB = label("-", 13, WHITE, false);
    private final JLabel statsC = label("-", 13, WHITE, false);
    private final JLabel weapon = label("-", 14, CYAN, true);
    private final JTextArea armor = wrap();
    private final JTextArea reason = wrap();
    private final JTextArea access = wrap();

    CombatCoachPanel(ActionListener refresh, ActionListener navigate) {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel contents = new JPanel();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        contents.setBackground(BG);
        contents.setBorder(new EmptyBorder(12, 10, 18, 10));

        JLabel title = label("COMBAT COACH", 19, WHITE, true);
        contents.add(title);
        contents.add(gap(14));

        JPanel suggestion = card();
        suggestion.add(section("CURRENT TRAINING", CYAN));
        suggestion.add(gap(6));
        suggestion.add(skill);
        suggestion.add(gap(9));
        suggestion.add(section("RECOMMENDED MONSTER", GREEN));
        suggestion.add(gap(5));
        suggestion.add(target);
        contents.add(suggestion);
        contents.add(gap(10));

        JPanel xp = card();
        xp.add(section("YOUR XP / HOUR", YELLOW));
        xp.add(gap(6));
        xp.add(rate);
        xp.add(gap(5));
        xp.add(label("Measured during this session; not a prediction.", 11, MUTED, false));
        contents.add(xp);
        contents.add(gap(10));

        JPanel build = card();
        build.add(section("COMBAT LEVELS", CYAN));
        build.add(gap(7));
        build.add(statsA);
        build.add(gap(4));
        build.add(statsB);
        build.add(gap(4));
        build.add(statsC);
        build.add(gap(11));
        build.add(section("EQUIPPED WEAPON", CYAN));
        build.add(gap(5));
        build.add(weapon);
        build.add(gap(11));
        build.add(section("OTHER EQUIPMENT", CYAN));
        build.add(gap(5));
        build.add(armor);
        contents.add(build);
        contents.add(gap(10));

        JPanel info = card();
        info.add(section("WHY THIS SPOT?", YELLOW));
        info.add(gap(6));
        info.add(reason);
        info.add(gap(11));
        info.add(section("REQUIREMENTS", CYAN));
        info.add(gap(5));
        info.add(access);
        contents.add(info);
        contents.add(gap(12));

        JButton nav = action("NAVIGATE TO MONSTER", navigate, true);
        JButton refreshButton = action("Refresh recommendation", refresh, false);
        contents.add(nav);
        contents.add(gap(8));
        contents.add(refreshButton);
        contents.add(gap(12));
        JTextArea disclaimer = wrap();
        disclaimer.setForeground(MUTED);
        disclaimer.setFont(new Font(Font.DIALOG, Font.ITALIC, 11));
        disclaimer.setText("Training spots are equipment-aware suggestions, not yet verified highest XP/hour.");
        contents.add(disclaimer);

        JScrollPane scroller = new JScrollPane(contents);
        scroller.setBorder(null);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        add(scroller, BorderLayout.CENTER);
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setBorder(new EmptyBorder(11, 10, 11, 10));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.DIALOG, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel section(String text, Color color) {
        return label(text, 11, color, true);
    }

    private static JTextArea emphasis(String value, int size, Color color) {
        JTextArea result = wrap();
        result.setText(value);
        result.setFont(new Font(Font.DIALOG, Font.BOLD, size));
        result.setForeground(color);
        return result;
    }

    private static JTextArea wrap() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        a.setForeground(WHITE);
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        a.setBorder(null);
        return a;
    }

    private static Component gap(int pixels) {
        return Box.createVerticalStrut(pixels);
    }

    private static JButton action(String title, ActionListener callback, boolean primary) {
        JButton button = new JButton(title);
        button.setFont(new Font(Font.DIALOG, Font.BOLD, primary ? 12 : 13));
        button.setFocusPainted(false);
        button.setBackground(primary ? new Color(36, 115, 130) : CARD);
        button.setForeground(WHITE);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(callback);
        return button;
    }

    void showRecommendation(String trainingSkill, int skillLevel, TrainingSpot spot,
                            CombatLoadout gear, String xpRate) {
        skill.setText(trainingSkill + "  /  Level " + skillLevel);
        target.setText(spot.name);
        rate.setText(xpRate.replace("Measured XP/hr: ", ""));
        statsA.setText("ATK " + gear.attack + "    STR " + gear.strength + "    DEF " + gear.defence);
        statsB.setText("HP " + gear.hitpoints + "    RNG " + gear.ranged + "    MAG " + gear.magic);
        statsC.setText("PRAYER " + gear.prayer);
        weapon.setText(gear.weapon);
        armor.setText(gear.armor);
        // The legacy planner puts a duplicated gear/stat summary in the reason.
        // Display the actionable explanation without repeating all seven levels.
        String explanation = spot.reason;
        int delimiter = explanation.indexOf(". ");
        if (explanation.startsWith("Gear: ") && delimiter >= 0) {
            explanation = explanation.substring(delimiter + 2);
        }
        reason.setText(explanation);
        access.setText(spot.requirements);
        revalidate();
        repaint();
    }

    void clear() {
        skill.setText("Log in for recommendations");
        target.setText("Waiting for game...");
        rate.setText("Waiting for combat XP");
        statsA.setText("-");
        statsB.setText("-");
        statsC.setText("-");
        weapon.setText("-");
        armor.setText("");
        reason.setText("");
        access.setText("");
    }
}
