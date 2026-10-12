package com.osrs.combatcoach;

import java.awt.*;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private int textWidth = 140;
    private final java.util.List<JTextArea> fittedAreas = new java.util.ArrayList<>();

    private final JLabel skill = label("Log in for recommendations", 14, WHITE, true);
    private final JTextArea target = emphasis("Waiting for game...", 16, GREEN);
    private final JTextArea rate = emphasis("Waiting for combat XP", 14, WHITE);
    private final JLabel statsA = label("-", 13, WHITE, false);
    private final JPanel statTiles = new JPanel(new GridLayout(0,3,6,6));
    private final JSpinner goal = new JSpinner(new SpinnerNumberModel(20,1,99,1));
    private final JTextArea eta = emphasis("Set a goal to see an ETA",14,WHITE);
    private final JTextArea duration = emphasis("",12,MUTED);
    private final JLabel remaining = label("Waiting for XP...",12,MUTED,false);
    private final JTextArea measuredBest = emphasis("No measured winner yet",13,GREEN);
    private final JTextArea measuredCompare = wrap();
    private final JTextArea modelCompare = wrap();
    private int currentLevel=1;
    private int currentXp=0;
    private long currentRate=-1;
    private final ActionListener goalCallback;
    private final JLabel statsB = label("-", 13, WHITE, false);
    private final JLabel statsC = label("-", 13, WHITE, false);
    private final JTextArea reason = wrap();
    private final JTextArea access = wrap();

    CombatCoachPanel(ActionListener refresh, ActionListener navigate, ActionListener goalChanged) {
        goalCallback=goalChanged;
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
        JTextArea xpCaption = wrap();
        xpCaption.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        xpCaption.setForeground(MUTED);
        fit(xpCaption, "Live rate from your own combat XP.");
        xp.add(xpCaption);
        contents.add(xp);
        contents.add(gap(10));

        JPanel goalCard = card();
        goalCard.add(section("LEVEL GOAL + LOCAL ETA", YELLOW));
        goalCard.add(gap(7));
        JPanel goalRow = new JPanel(new BorderLayout(8,0));
        goalRow.setBackground(CARD);
        goalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        goalRow.add(label("Target level",13,WHITE,true),BorderLayout.WEST);
        goal.setFont(new Font(Font.DIALOG,Font.BOLD,14));
        goal.addChangeListener(e -> { updateGoal(); goalCallback.actionPerformed(null); });
        goalRow.add(goal,BorderLayout.EAST);
        goalCard.add(goalRow);
        goalCard.add(gap(9));
        goalCard.add(remaining);
        goalCard.add(gap(6));
        goalCard.add(eta);
        goalCard.add(gap(5));
        goalCard.add(duration);
        contents.add(goalCard);
        contents.add(gap(10));

        JPanel comparison = card();
        comparison.add(section("MONSTER XP COMPARISON", YELLOW));
        comparison.add(gap(7));
        comparison.add(measuredBest);
        comparison.add(gap(7));
        measuredCompare.setForeground(MUTED);
        comparison.add(measuredCompare);
        comparison.add(gap(7));
        JTextArea comparisonCaption = wrap();
        comparisonCaption.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        comparisonCaption.setForeground(MUTED);
        fit(comparisonCaption, "Only monsters you have tested.");
        comparison.add(comparisonCaption);
        contents.add(comparison);
        contents.add(gap(10));
        JPanel estimates = card();
        estimates.add(section("PREDICTED MELEE XP / HOUR", CYAN));
        estimates.add(gap(6));
        modelCompare.setForeground(WHITE);
        estimates.add(modelCompare);
        contents.add(estimates);
        contents.add(gap(10));

        JPanel build = card();
        build.add(section("COMBAT LEVELS", CYAN));
        build.add(gap(7));
        statTiles.setOpaque(false);
        statTiles.setAlignmentX(Component.LEFT_ALIGNMENT);
        build.add(statTiles);
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
        fit(disclaimer, "Predictions are estimates, not verified best XP/hour.");
        contents.add(disclaimer);

        JScrollPane scroller = new JScrollPane(contents);
        scroller.setBorder(null);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.getViewport().addChangeListener(e -> {
            int width = scroller.getViewport().getExtentSize().width;
            if (width <= 0) return;
            int available = Math.max(92, width - 42);
            if (available != textWidth) {
                textWidth = available;
                for (JTextArea area : fittedAreas) resizeArea(area);
                contents.revalidate();
            }
        });
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
        a.setColumns(18);
        return a;
    }

    private void fit(JTextArea area, String content) {
        String value=content == null ? "" : content;
        if (!value.equals(area.getText())) area.setText(value);
        if (!fittedAreas.contains(area)) fittedAreas.add(area);
        resizeArea(area);
    }

    private void resizeArea(JTextArea area) {
        int width=textWidth;
        // Measure using the exact available width; zero-height text areas are hidden.
        area.setPreferredSize(null);
        area.setSize(width, Short.MAX_VALUE);
        int height=area.getText().isEmpty() ? 0 :
            Math.max(area.getFontMetrics(area.getFont()).getHeight()+3,area.getPreferredSize().height+3);
        Dimension d=new Dimension(width,height);
        area.setPreferredSize(d);
        area.setMinimumSize(d);
        area.setMaximumSize(d);
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
                            CombatLoadout gear, String xpRate, String predicted) {
        skill.setText(trainingSkill + "  /  Level " + skillLevel);
        String shortPrediction=predicted == null ? "" : predicted;
        int assumptions=shortPrediction.indexOf("Estimates assume");
        if (assumptions>=0) shortPrediction=shortPrediction.substring(0,assumptions).trim();
        shortPrediction=shortPrediction.replace("Highest modelled: ","Model best: ");
        fit(modelCompare,shortPrediction);
        fit(target,spot.name);
        fit(rate,xpRate.replace("Measured XP/hr: ", ""));
        statTiles.removeAll();
        addTile("ATK",gear.attack,trainingSkill.equalsIgnoreCase("Attack"));
        addTile("STR",gear.strength,trainingSkill.equalsIgnoreCase("Strength"));
        addTile("DEF",gear.defence,trainingSkill.equalsIgnoreCase("Defence"));
        addTile("HP",gear.hitpoints,false);
        addTile("RNG",gear.ranged,trainingSkill.equalsIgnoreCase("Ranged"));
        addTile("MAG",gear.magic,trainingSkill.equalsIgnoreCase("Magic"));
        addTile("PRAY",gear.prayer,false);
        currentLevel=skillLevel;
        if (((Number)goal.getValue()).intValue()<=skillLevel && skillLevel<99) goal.setValue(Math.min(99,skillLevel+1));
        updateGoal();
        statsB.setText("HP " + gear.hitpoints + "    RNG " + gear.ranged + "    MAG " + gear.magic);
        statsC.setText("PRAYER " + gear.prayer);
        // The legacy planner puts a duplicated gear/stat summary in the reason.
        // Display the actionable explanation without repeating all seven levels.
        String explanation = spot.reason;
        int delimiter = explanation.indexOf(". ");
        if (explanation.startsWith("Gear: ") && delimiter >= 0) {
            explanation = explanation.substring(delimiter + 2);
        }
        fit(reason,explanation);
        fit(access,spot.requirements);
        statTiles.revalidate();
        revalidate();
        repaint();
    }

    private void addTile(String name,int value,boolean highlighted) {
        JPanel tile=new JPanel(new BorderLayout());
        tile.setBackground(highlighted?new Color(31,96,85):new Color(51,59,68));
        tile.setBorder(new EmptyBorder(7,5,7,5));
        JLabel nameLabel=label(name,11,highlighted?GREEN:MUTED,true);
        JLabel valueLabel=label(Integer.toString(value),19,WHITE,true);
        tile.add(nameLabel,BorderLayout.NORTH);
        tile.add(valueLabel,BorderLayout.SOUTH);
        statTiles.add(tile);
    }

    void updateProgress(int xp,long ratePerHour,String bestMonster,String comparisonText) {
        fit(measuredBest,bestMonster);
        fit(measuredCompare,comparisonText == null ? "" : comparisonText.replace("; ","\n"));
        currentXp=xp;
        currentRate=ratePerHour;
        fit(rate,ratePerHour<0?"Waiting for XP gains":String.format(java.util.Locale.US,"%,d XP/hr",ratePerHour));
        updateGoal();
    }

    private void updateGoal() {
        int targetLevel=((Number)goal.getValue()).intValue();
        if (targetLevel<=currentLevel) {
            remaining.setText("Goal reached!");
            fit(eta,"Choose a higher target level");
            fit(duration,"");
            return;
        }
        long needed=Math.max(0L,xpForLevel(targetLevel)-currentXp);
        remaining.setText(String.format(java.util.Locale.US,"%,d XP remaining",needed));
        if (currentXp<=0 || currentRate<=0) {
            fit(eta,"ETA: waiting for XP rate");
            fit(duration,"");
            return;
        }
        // Math is intentionally unchanged; only the display is extended.
        long seconds=(long)Math.ceil(needed*3600.0/currentRate);
        java.time.ZonedDateTime finish=java.time.ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(seconds);
        String when=DateTimeFormatter.ofPattern("EEE, MMM d, uuuu 'at' h:mm a",
            java.util.Locale.US).format(finish);
        fit(eta,"Estimated completion:\n" + when);
        long days=seconds/86400L;
        long hours=(seconds%86400L)/3600L;
        long minutes=(seconds%3600L)/60L;
        String timeLeft=(days>0 ? days+" days, " : "")+
            (hours>0 ? hours+" hours, " : "")+minutes+" min";
        fit(duration,"Remaining: " + timeLeft);
    }

        private static int xpForLevel(int level) {
        int sum=0;
        for(int i=1;i<level;i++) sum+=(int)Math.floor(i+300*Math.pow(2,i/7.0));
        return sum/4;
    }

    void clear() {
        skill.setText("Log in for recommendations");
        fit(target,"Waiting for game...");
        fit(rate,"Waiting for combat XP");
        fit(measuredBest,"No measured winner yet");
        fit(measuredCompare,"");
        fit(modelCompare,"");
        statsA.setText("-");
        statsB.setText("-");
        statsC.setText("-");
        fit(reason,"");
        fit(access,"");
    }
}
