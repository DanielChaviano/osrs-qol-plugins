package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

final class CombatCoachPanel extends PluginPanel {
    private final JLabel skill = new JLabel("Log in to get suggestions");
    private final JLabel target = new JLabel(" ");
    private final JTextArea description = new JTextArea();
    private final JLabel level = new JLabel(" ");
    CombatCoachPanel(ActionListener refresh, ActionListener navigate) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("COMBAT COACH");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 17f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(heading); add(Box.createVerticalStrut(14));
        for (JLabel l : new JLabel[]{skill,level,target}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(l); add(Box.createVerticalStrut(8));
        }
        description.setEditable(false); description.setOpaque(false);
        description.setWrapStyleWord(true); description.setLineWrap(true);
        description.setFocusable(false);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(description); add(Box.createVerticalStrut(12));
        JButton button = new JButton("Refresh suggestion");
        button.addActionListener(refresh);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(button);
        add(Box.createVerticalStrut(8));
        JButton navigateButton = new JButton("Navigate with Shortest Path");
        navigateButton.addActionListener(navigate);
        navigateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(navigateButton);
    }
    void showRecommendation(String trainingSkill, int skillLevel, TrainingSpot spot) {
        skill.setText("Training: " + trainingSkill);
        level.setText("Skill level: " + skillLevel);
        target.setText("Suggested: " + spot.name);
        description.setText("WHY: " + spot.reason + "\n\nREQUIRES: " + spot.requirements +
            "\n\nClick Navigate with Shortest Path to request a walkable route, or open the world map (M) for the blue marker. " +
            "Suggestions are a curated starting route, not guaranteed maximum XP/hr.");
    }
    void clear() { skill.setText("Log in to get suggestions"); level.setText(" "); target.setText(" "); description.setText(""); }
}
