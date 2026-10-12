package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

final class CombatCoachPanel extends PluginPanel {
    private final JLabel heading = new JLabel("COMBAT COACH");
    private final JLabel skill = new JLabel("Log in to get a suggestion");
    private final JLabel target = new JLabel(" ");
    private final JLabel rates = new JLabel(" ");
    private final JTextArea details = new JTextArea();
    private final JButton navigateButton = new JButton("Navigate with Shortest Path");

    CombatCoachPanel(ActionListener refresh, ActionListener navigate) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        for (JLabel label : new JLabel[]{heading, skill, target, rates}) {
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(label);
            add(Box.createVerticalStrut(9));
        }
        details.setEditable(false);
        details.setOpaque(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFocusable(false);
        details.setFont(details.getFont().deriveFont(12f));
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 4));
        add(details);
        JButton refreshButton = new JButton("Refresh recommendation");
        refreshButton.addActionListener(refresh);
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(refreshButton);
        add(Box.createVerticalStrut(7));
        navigateButton.addActionListener(navigate);
        navigateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(navigateButton);
    }

    void showRecommendation(String trainingSkill, int skillLevel, TrainingSpot spot,
                            CombatLoadout gear, String xpRate) {
        skill.setText("Training " + trainingSkill + "  |  Level " + skillLevel);
        target.setText("Suggested: " + spot.name);
        rates.setText(xpRate);
        details.setText(
            "STATS\n" +
            "Attack " + gear.attack + "  /  Strength " + gear.strength + "  /  Defence " + gear.defence + "\n" +
            "HP " + gear.hitpoints + "  /  Ranged " + gear.ranged + "  /  Magic " + gear.magic + "\n\n" +
            "WEAPON\n" + gear.weapon + "\n\n" +
            "ARMOUR\n" + gear.armor + "\n\n" +
            "WHY THIS TARGET\n" + spot.reason + "\n\n" +
            "ACCESS\n" + spot.requirements + "\n\n" +
            "Recommendation is a heuristic, not verified optimal XP/hour. " +
            "Measured XP/hour is specific to your session and does not account for travel.");
        details.setCaretPosition(0);
    }

    void clear() {
        skill.setText("Log in to get a suggestion");
        target.setText(" ");
        rates.setText(" ");
        details.setText("");
    }
}
