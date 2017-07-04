package org.agreement_technologies.agents;

import org.agreement_technologies.common.map_grounding.Action;
import org.agreement_technologies.common.map_grounding.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * @author Oscar
 *         Graphical interface to show the disRPG data
 */
public class GUIdisRPG extends JFrame implements MouseListener {
    private static final long serialVersionUID = 3080890313036553788L;
    private AgentListener ag;    // Planning agent
    private JTable jTableRPG;

    /**
     * Creates a new RPG form
     */
    public GUIdisRPG(AgentListener ag) {
        this.ag = ag;
        setTitle("disRPG: " + this.ag.getShortName());
        this.setSize(700, 600);
        this.setLocationRelativeTo(null);
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form
     */
    private void initComponents() {
        JScrollPane jScrollPaneRPG = new JScrollPane();
        jTableRPG = new JTable();
        jTableRPG.setModel(new RPGTableModel());
        jTableRPG.setFillsViewportHeight(true);
        jTableRPG.setShowHorizontalLines(false);
        jTableRPG.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jTableRPG.addMouseListener(this);
        for (int i = 0; i < jTableRPG.getModel().getColumnCount(); i++)
            jTableRPG.getColumnModel().getColumn(i).setPreferredWidth(150);
        jScrollPaneRPG.setViewportView(jTableRPG);
        getContentPane().add(jScrollPaneRPG);
    }

    // Double click on a cell
    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2) {
            java.awt.Point p = event.getPoint();
            int row = jTableRPG.rowAtPoint(p);
            int column = jTableRPG.columnAtPoint(p);
            Object obj = jTableRPG.getModel().getValueAt(row, column);
            String desc = null, title = "";
            if (obj instanceof Action) {
                Action a = (Action) obj;
                title = a.toString();
                desc = a.getOperatorName() + " (";
                for (String param : a.getParams()) desc += " " + param;
                desc += ")\n* Precs:\n";
                for (GroundedCond prec : a.getPrecs())
                    desc += "    (" + prec + ")\n";
                desc += "* Effs:";
                for (GroundedEff eff : a.getEffs())
                    desc += "\n    (" + eff + ")";
            } else if (obj instanceof RPGTableModel.RPGFact) {
                RPGTableModel.RPGFact f = (RPGTableModel.RPGFact) obj;
                title = f.v.toString();
                desc = "(" + f.v + ")";
                for (String a : ag.getCommunication().getAgentList()) {
                    int time = f.v.getMinTime(f.obj, a);
                    if (time != -1)
                        desc += "\n* " + a + ": level " + time;
                }
            }
            if (desc != null)
                JOptionPane.showMessageDialog(this, desc, title, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }

    @Override
    public void mousePressed(MouseEvent event) {
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    /**
     * Table model for the disRPG
     */
    private class RPGTableModel extends javax.swing.table.AbstractTableModel {
        private static final long serialVersionUID = -6431770911028834234L;
        private ArrayList<ArrayList<Object>> levels;
        private int largestLevel;

        // Initializes the RPG levels
        public RPGTableModel() {
            largestLevel = 0;
            levels = new ArrayList<ArrayList<Object>>();
            GroundedTask g = ag.getGroundedTask();
            for (GroundedVar v : g.getVars()) {
                for (String obj : v.getReachableValues()) {
                    int time = v.getMinTime(obj);
                    if (time != -1) addFact(v, obj, time);
                }
            }
            for (Action a : g.getActions()) {
                addAction(a, a.getMinTime());
            }
        }

        // Adds an action to the RPG
        private void addAction(Action a, int time) {
            time = 2 * time + 1;
            while (levels.size() <= time) levels.add(new ArrayList<Object>());
            levels.get(time).add(a);
            if (levels.get(time).size() > largestLevel)
                largestLevel = levels.get(time).size();
        }

        // Adds a fact to the RPG level
        private void addFact(GroundedVar v, String obj, int time) {
            time *= 2;
            while (levels.size() <= time) levels.add(new ArrayList<Object>());
            levels.get(time).add(new RPGFact(v, obj));
            if (levels.get(time).size() > largestLevel)
                largestLevel = levels.get(time).size();
        }

        // Returns the column name
        @Override
        public String getColumnName(int col) {
            if (col % 2 == 0) return "Fact level " + (col / 2);
            return "Action level " + (col / 2);
        }

        // Number of columns -> number of levels in the RPG
        @Override
        public int getColumnCount() {
            return levels.size();
        }

        // Number of rows in the RPG
        @Override
        public int getRowCount() {
            return largestLevel;
        }

        // Value in a table position
        @Override
        public Object getValueAt(int row, int col) {
            if (col >= 0 && col < levels.size()) {
                ArrayList<Object> level = levels.get(col);
                if (row >= 0 && row < level.size())
                    return level.get(row);
            }
            return "";
        }

        // Class to store facts
        private class RPGFact {
            GroundedVar v;
            String obj;

            public RPGFact(GroundedVar v, String obj) {
                this.v = v;
                this.obj = obj;
            }

            public String toString() {
                String desc = "[";
                boolean first = true;
                for (String a : ag.getCommunication().getAgentList())
                    if (v.getMinTime(obj, a) != -1) {
                        desc += first ? a : "," + a;
                        first = false;
                    }
                return desc + "](= (" + v + ") " + obj + ")";
            }
        }
    }
}
