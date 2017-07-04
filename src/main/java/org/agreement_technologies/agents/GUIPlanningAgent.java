package org.agreement_technologies.agents;

import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.PlannerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Oscar
 *         Graphical interface for a planning agent
 */
public class GUIPlanningAgent extends JFrame implements PlanningAgentListener,
        ActionListener {

    private static final long serialVersionUID = -730178402960432486L;
    private static final int FORM_WIDTH = 200;
    private AgentListener ag;            // Planning agent
    private int status;

    // Graphic components
    private JButton btnTrace, btnRPG, btnSerachTree, btnLND;
    private JLabel lblStatus;
    private StatusPanel statusPanel;

    // Sub-forms
    private GUITrace guiTrace;
    private GUIdisRPG guiRPG;
    private GUISearchTree guiSearchTree;
    private GUILandmarks guiLandmarks;

    /**
     * Creates a new graphical interface for an agent
     *
     * @param ag Planning agent
     */
    public GUIPlanningAgent(AgentListener ag) {
        this.ag = ag;
        status = PlanningAlgorithm.STATUS_STARTING;
        ag.setAgentListener(this);
        setTitle(this.ag.getShortName());
        this.setSize(FORM_WIDTH, 200);
        this.setResizable(false);
        initComponents();
        initSubforms();
        setVisible(true);
    }

    /**
     * Initializes the sub-forms
     */
    private void initSubforms() {
        guiTrace = new GUITrace(ag);
        guiRPG = null;
        guiSearchTree = null;
        guiLandmarks = null;
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (guiTrace != null && guiTrace.isVisible()) guiTrace.setVisible(false);
                if (guiRPG != null && guiRPG.isVisible()) guiRPG.setVisible(false);
                if (guiLandmarks != null && guiLandmarks.isVisible()) guiLandmarks.setVisible(false);
                if (guiSearchTree != null && guiSearchTree.isVisible()) guiSearchTree.setVisible(false);
                guiTrace = null;
                guiRPG = null;
                guiSearchTree = null;
                guiLandmarks = null;
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form
     */
    private void initComponents() {
        java.awt.Container panel = getContentPane();
        panel.setLayout(new java.awt.GridLayout(5, 1));
        ((java.awt.GridLayout) panel.getLayout()).setVgap(1);
        btnTrace = new JButton("Trace");
        btnRPG = new JButton("disRPG");
        btnLND = new JButton("Landmarks");
        btnSerachTree = new JButton("Search tree");
        btnRPG.setEnabled(false);
        btnLND.setEnabled(false);
        btnSerachTree.setEnabled(false);
        panel.add(btnTrace);
        panel.add(btnRPG);
        panel.add(btnLND);
        panel.add(btnSerachTree);
        btnTrace.addActionListener(this);
        btnRPG.addActionListener(this);
        btnLND.addActionListener(this);
        btnSerachTree.addActionListener(this);
        statusPanel = new StatusPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Status: ");
        label.setFont(new Font("TimesRoman", Font.BOLD, 12));
        statusPanel.add(label);
        lblStatus = new JLabel("starting");
        lblStatus.setFont(new Font("TimesRoman", Font.ITALIC, 12));
        statusPanel.add(lblStatus);
        panel.add(statusPanel);
    }

    /**
     * Handler for an action performed by the user
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnTrace) guiTrace.setVisible(!guiTrace.isVisible());
        else if (e.getSource() == btnRPG) guiRPG.setVisible(!guiRPG.isVisible());
        else if (e.getSource() == btnSerachTree) guiSearchTree.setVisible(!guiSearchTree.isVisible());
        else if (e.getSource() == btnLND) guiLandmarks.setVisible(!guiLandmarks.isVisible());
    }

    /**
     * Agent status changed notification
     */
    @Override
    public void statusChanged(int status) {
        lblStatus.setText(PlanningAlgorithm.getStatusDesc(status));
        switch (this.status) {
            case PlanningAlgorithm.STATUS_GROUNDING:
                if (status != PlanningAlgorithm.STATUS_ERROR && !btnRPG.isEnabled()) {
                    guiRPG = new GUIdisRPG(ag);
                    btnRPG.setEnabled(true);
                }
                break;
            case PlanningAlgorithm.STATUS_LANDMARKS:
                if (status != PlanningAlgorithm.STATUS_ERROR && !btnLND.isEnabled()) {
                    guiLandmarks = new GUILandmarks(ag);
                    btnLND.setEnabled(true);
                }
                break;
        }
        this.status = status;
    }

    /**
     * Notifies an error message
     */
    @Override
    public void notyfyError(String msg) {
        guiTrace.showError(msg);
    }

    /**
     * Shows a trace message
     */
    @Override
    public void trace(int indentLevel, String msg) {
        guiTrace.showInfo(indentLevel, msg);
    }

    /**
     * Shows a new plan
     */
    @Override
    public void newPlan(Plan plan, PlannerFactory pf) {
        if (guiSearchTree == null) {
            guiSearchTree = new GUISearchTree(ag);
            btnSerachTree.setEnabled(true);
        }
        guiSearchTree.newPlan(plan, pf);
        statusPanel.setH(plan.getH());
    }

    @Override
    public void showPlan(Plan plan, PlannerFactory pf) {
        if (guiSearchTree == null) {
            guiSearchTree = new GUISearchTree(ag);
            btnSerachTree.setEnabled(true);
        }
        guiSearchTree.showPlan(plan, pf);
    }

    @Override
    public void selectPlan(String planName) {
        if (guiSearchTree != null) {
            if (!guiSearchTree.isVisible()) guiSearchTree.setVisible(true);
            guiSearchTree.toFront();
            guiSearchTree.selectPlan(planName);
        }
    }

    private static class StatusPanel extends JPanel {
        private static final long serialVersionUID = 317308883562686365L;
        private int maxH, minH;
        private float blue;

        public StatusPanel(LayoutManager layout) {
            super(layout);
            maxH = minH = -1;
            blue = 255;
        }

        public void setH(int h) {
            if (maxH == -1 && h > 0) {
                maxH = minH = h;
            }
            if (h < minH) {
                minH = h;
                blue = 255;
            } else {
                blue -= 0.25;
                if (blue < 0) blue = 0;
            }
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maxH > 0 && minH > 0) {
                int w = minH * (getWidth() - 10) / maxH, b = (int) blue;
                g.setColor(new Color(255 - b, 0, b));
                g.fillRect(5, 24, w, 6);
            }
        }
    }
}
