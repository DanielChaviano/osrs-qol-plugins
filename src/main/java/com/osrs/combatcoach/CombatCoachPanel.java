package com.osrs.combatcoach;

import java.awt.*;
import java.util.function.BiConsumer;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

/** Uses PluginPanel(false): RuneLite must not add a second scroll pane around this view. */
final class CombatCoachPanel extends PluginPanel {
    private final CoachDashboard dashboard;
    private final BiConsumer<String,String> actions;
    private CoachUiState state=CoachUiState.waiting();
    private JDialog expanded;
    private CoachDashboard expandedView;
    private final java.util.Map<String,java.awt.image.BufferedImage> thumbnails=new java.util.HashMap<>();
    CombatCoachPanel(BiConsumer<String,String> actions) {
        super(false);
        this.actions=actions;
        setLayout(new BorderLayout());setBorder(null);setBackground(CoachArt.BG);
        dashboard=new CoachDashboard(this::dispatch);
        add(scroll(dashboard),BorderLayout.CENTER);
    }
    private JScrollPane scroll(CoachDashboard content) {
        JScrollPane pane=new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(null);pane.getViewport().setBackground(CoachArt.BG);
        pane.getVerticalScrollBar().setUnitIncrement(24);
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(12,1));
        return pane;
    }
    private void dispatch(String action,String value) {
        if("expand".equals(action)){openExpanded();return;}
        actions.accept(action,value);
    }
    private void openExpanded() {
        if(expanded!=null&&expanded.isDisplayable()){return;}
        Window owner=SwingUtilities.getWindowAncestor(this);
        expanded=new JDialog(owner,"Combat Coach - live dashboard",Dialog.ModalityType.MODELESS);
        expandedView=new CoachDashboard(this::dispatch);expandedView.setState(state);thumbnails.forEach(expandedView::setThumbnail);
        expanded.setContentPane(scroll(expandedView));
        expanded.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        expanded.setMinimumSize(new Dimension(260,420));expanded.setSize(400,900);
        expanded.setLocationRelativeTo(owner);
        expanded.addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowClosed(java.awt.event.WindowEvent e){if(expandedView!=null){expandedView.dispose();expandedView=null;}expanded=null;}});
        expanded.setVisible(true);
    }
    void thumbnail(String id,java.awt.image.BufferedImage image){
        thumbnails.put(id,image);if(thumbnails.size()>10)thumbnails.remove(thumbnails.keySet().iterator().next());
        dashboard.setThumbnail(id,image);if(expandedView!=null)expandedView.setThumbnail(id,image);
    }
    void update(CoachUiState value) {
        state=value;dashboard.setState(value);
        if(expandedView!=null)expandedView.setState(value);
    }
    void disposePanel() {
        dashboard.dispose();if(expandedView!=null)expandedView.dispose();
        if(expanded!=null)expanded.dispose();
    }
}
