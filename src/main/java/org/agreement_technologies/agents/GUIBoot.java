package org.agreement_technologies.agents;

import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_heuristic.HeuristicFactory;
import org.agreement_technologies.common.map_negotiation.NegotiationFactory;
import org.agreement_technologies.common.map_parser.AgentList;
import org.agreement_technologies.common.map_planner.PlannerFactory;
import org.agreement_technologies.service.map_parser.ParserImp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.*;
import java.util.Scanner;

/**
 * @author Oscar
 */
public class GUIBoot extends JFrame {

    private static final long serialVersionUID = -5039304283931395812L;
    //private static final String[] searchMethods = {"Speed", "Balanced", "Quality"};
    private static final String[] heuristics = {"Breadth", "FF", "DTG", "Landmarks", "Land.Inc."};
    private static final String[] negotiation = {"Cooperative", "Borda voting", "Runoff voting"};
    private String startDir;    // Start folder for selecting files
    private String qpidHost;
    private int timeout;
    // Variables declaration
    private javax.swing.JButton jButtonAddAgent;
    private javax.swing.JButton jButtonClearAgents;
    private javax.swing.JButton jButtonLoadConfig;
    private javax.swing.JButton jButtonLoadDomain;
    private javax.swing.JButton jButtonLoadProblem;
    private javax.swing.JButton jButtonSaveConfig;
    private javax.swing.JButton jButtonStart;
    //private javax.swing.JButton jButtonBatch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    @SuppressWarnings("rawtypes")
    private javax.swing.JList jListAgents;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextFieldAgent;
    private javax.swing.JTextField jTextFieldDomain;
    private javax.swing.JTextField jTextFieldProblem;
    @SuppressWarnings("rawtypes")
    private javax.swing.JComboBox heuristicType;
    private javax.swing.JComboBox negotiationType;
    //private javax.swing.JComboBox searchType;
    private javax.swing.JCheckBox sameObjects;
    private javax.swing.JCheckBox trace;
    private javax.swing.JCheckBox anytime;
    private JTextField jTextTimeout;
    private JTextField jTextQpid;

    /**
     * Constructs the GUI for launching agents
     */
    public GUIBoot() {
        startDir = null;
        qpidHost = "localhost";
        try {
            Scanner f = new Scanner(new File("configuration/startDir.txt"));
            startDir = f.nextLine();
            if (f.hasNextLine()) {
                qpidHost = f.nextLine();
            }
            f.close();
        } catch (FileNotFoundException e) {
        }
        try {
            if (startDir == null) {
                startDir = new java.io.File(".").getCanonicalPath();
            }
        } catch (Exception e) {
            startDir = "";
        }
        initComponents();
        setSize(590, 380);

        setLocationRelativeTo(null);
    }

    public void saveStartDir() {
        FileWriter outFile;
        try {
            outFile = new FileWriter("configuration/startDir.txt");
            PrintWriter out = new PrintWriter(outFile);
            out.println(startDir);
            out.println(qpidHost);
            out.close();
        } catch (IOException e) {
        }
    }

    /**
     * This method is called from within the constructor to initialize the form
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jTextFieldAgent = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldDomain = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldProblem = new javax.swing.JTextField();
        jButtonLoadDomain = new javax.swing.JButton();
        jButtonLoadProblem = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jButtonClearAgents = new javax.swing.JButton();
        //jButtonBatch = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListAgents = new javax.swing.JList();
        jButtonAddAgent = new javax.swing.JButton();
        jButtonLoadConfig = new javax.swing.JButton();
        jButtonSaveConfig = new javax.swing.JButton();
        jButtonStart = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FMAP - Multi-Agent Planning");
        setResizable(false);
        getContentPane().setLayout(null);

        jLabel1.setText("Agent's name:");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(20, 20, 90, 14);

        getContentPane().add(jTextFieldAgent);
        jTextFieldAgent.setBounds(130, 20, 130, 20);

        jLabel2.setText("Domain file:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 56, 110, 14);
        getContentPane().add(jTextFieldDomain);
        jTextFieldDomain.setBounds(130, 50, 360, 20);

        jLabel3.setText("Problem file:");
        getContentPane().add(jLabel3);
        jLabel3.setBounds(20, 76, 110, 14);
        getContentPane().add(jTextFieldProblem);
        jTextFieldProblem.setBounds(130, 76, 360, 20);

        JLabel jLabel4 = new JLabel("Qpid host:");
        getContentPane().add(jLabel4);
        jLabel4.setBounds(20, 105, 110, 16);
        /*
         searchType = new JComboBox(searchMethods);
         searchType.setSelectedIndex(1);
         getContentPane().add(searchType);
         searchType.setBounds(130, 105, 150, 18);
         searchType.setEnabled(false);
         */
        jTextQpid = new JTextField(qpidHost);
        getContentPane().add(jTextQpid);
        jTextQpid.setBounds(130, 105, 150, 18);
        JLabel jLabel5 = new JLabel("Heuristic function:");
        getContentPane().add(jLabel5);
        jLabel5.setBounds(300, 105, 110, 16);
        heuristicType = new JComboBox(heuristics);
        heuristicType.setSelectedIndex(HeuristicFactory.LAND_DTG_NORM);
        getContentPane().add(heuristicType);
        heuristicType.setBounds(418, 105, 150, 18);

        /**
         * ***************************************************
         */
        JLabel jLabel6 = new JLabel("Negotiation method:");
        getContentPane().add(jLabel6);
        jLabel6.setBounds(290, 20, 120, 14);
        negotiationType = new JComboBox(negotiation);
        negotiationType.setSelectedIndex(NegotiationFactory.COOPERATIVE);
        getContentPane().add(negotiationType);
        negotiationType.setBounds(418, 20, 150, 20);
        /**
         * ***************************************************
         */

        jButtonLoadDomain.setText("Load");
        jButtonLoadDomain.setFocusable(false);
        jButtonLoadDomain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadDomainActionPerformed(evt);
            }
        });
        getContentPane().add(jButtonLoadDomain);
        jButtonLoadDomain.setBounds(500, 50, 70, 20);

        jButtonLoadProblem.setText("Load");
        jButtonLoadProblem.setFocusable(false);
        jButtonLoadProblem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadProblemActionPerformed(evt);
            }
        });
        getContentPane().add(jButtonLoadProblem);
        jButtonLoadProblem.setBounds(500, 76, 70, 20);

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel1.setLayout(null);

        jButtonClearAgents.setText("Clear agents");
        jButtonClearAgents.setFocusable(false);
        jButtonClearAgents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearAgentsActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonClearAgents);
        jButtonClearAgents.setBounds(10, 40, 120, 23);

        jListAgents.setModel(new javax.swing.DefaultListModel());
        jListAgents.setFocusable(false);
        jScrollPane1.setViewportView(jListAgents);

        jPanel1.add(jScrollPane1);
        jScrollPane1.setBounds(150, 10, 400, 130);

        jButtonAddAgent.setText("Add agent");
        jButtonAddAgent.setFocusable(false);
        jButtonAddAgent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddAgentActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonAddAgent);
        jButtonAddAgent.setBounds(10, 10, 120, 30);

        jButtonLoadConfig.setText("Load agents");
        jButtonLoadConfig.setFocusable(false);
        jButtonLoadConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadConfigActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonLoadConfig);
        jButtonLoadConfig.setBounds(10, 110, 120, 30);

        jButtonSaveConfig.setText("Save agents");
        jButtonSaveConfig.setFocusable(false);
        jButtonSaveConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveConfigActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonSaveConfig);
        jButtonSaveConfig.setBounds(10, 80, 120, 30);

        getContentPane().add(jPanel1);
        int posPanel = 140;
        jPanel1.setBounds(10, posPanel, 568, 150);

        jButtonStart.setText("Start agents");
        jButtonStart.setFocusable(false);
        jButtonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartActionPerformed(evt);
            }
        });
        /*
        jButtonBatch.setText("Batch");
        jButtonBatch.setFocusable(false);
        jButtonBatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                batchTest();
            }
        });
        getContentPane().add(jButtonBatch);
        jButtonBatch.setBounds(16, posPanel + 158, 80, 20);
        */
        anytime = new JCheckBox("Anytime");
        getContentPane().add(anytime);
        anytime.setBounds(13, posPanel + 182, 75, 16);
        anytime.setSelected(false);

        final JLabel jLabel7 = new JLabel("Timeout");
        getContentPane().add(jLabel7);
        jLabel7.setBounds(88, posPanel + 182, 80, 16);
        final JLabel jLabel8 = new JLabel("sec.");
        getContentPane().add(jLabel8);
        jLabel8.setBounds(178, posPanel + 182, 80, 16);
        jTextTimeout = new JTextField(timeout);
        getContentPane().add(jTextTimeout);
        jTextTimeout.setHorizontalAlignment(JTextField.RIGHT);
        jTextTimeout.setText("1800");
        jTextTimeout.setBounds(138, posPanel + 182, 40, 16);
        jLabel7.setVisible(false);
        jTextTimeout.setVisible(false);
        jLabel8.setVisible(false);
        anytime.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                jLabel7.setVisible(anytime.isSelected());
                jTextTimeout.setVisible(anytime.isSelected());
                jLabel8.setVisible(anytime.isSelected());
            }
        });

        sameObjects = new JCheckBox("Same objects filtering");
        getContentPane().add(sameObjects);
        sameObjects.setBounds(420, posPanel + 160, 170, 16);
        sameObjects.setSelected(true);

        trace = new JCheckBox("Planning trace");
        getContentPane().add(trace);
        trace.setBounds(420, posPanel + 180, 170, 16);
        trace.setSelected(true);

        getContentPane().add(jButtonStart);
        jButtonStart.setBounds(220, posPanel + 160, 150, 40);

        pack();
    }

    /**
     * Selects the domain file
     *
     * @param evt Event information
     */
    private void jButtonLoadDomainActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser(startDir);
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            startDir = fileChooser.getCurrentDirectory().toString();
            saveStartDir();
            jTextFieldDomain.setText(fileChooser.getSelectedFile().toString());
        }
    }

    /**
     * Selects the problem file
     *
     * @param evt Event information
     */
    private void jButtonLoadProblemActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser(startDir);
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            startDir = fileChooser.getCurrentDirectory().toString();
            saveStartDir();
            jTextFieldProblem.setText(fileChooser.getSelectedFile().toString());
        }
    }

    /**
     * Clears the agents list
     *
     * @param evt Event information
     */
    @SuppressWarnings("rawtypes")
    private void jButtonClearAgentsActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.DefaultListModel model
                = (javax.swing.DefaultListModel) jListAgents.getModel();
        model.clear();
    }

    /**
     * Adds an agent to the list
     *
     * @param evt Event information
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void jButtonAddAgentActionPerformed(java.awt.event.ActionEvent evt) {
        GUIBoot.Agent a = new GUIBoot.Agent(jTextFieldAgent.getText(),
                jTextFieldDomain.getText(), jTextFieldProblem.getText());
        if (a.name.equals("")) {
            javax.swing.JOptionPane.showMessageDialog(this, "The agent must have a name",
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        } else {
            javax.swing.DefaultListModel model
                    = (javax.swing.DefaultListModel) jListAgents.getModel();
            model.addElement(a);
        }
    }

    /**
     * Loads the agents list from a file
     *
     * @param evt Event information
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void jButtonLoadConfigActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.DefaultListModel model
                = (javax.swing.DefaultListModel) jListAgents.getModel();
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser(startDir);
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            startDir = fileChooser.getCurrentDirectory().toString();
            saveStartDir();
            String fileName = fileChooser.getSelectedFile().toString();
            try {
                java.util.Scanner s = new java.util.Scanner(new java.io.File(fileName));
                while (s.hasNext()) {
                    GUIBoot.Agent a = new GUIBoot.Agent(s.nextLine(), s.nextLine(), s.nextLine());
                    model.addElement(a);
                }
                s.close();
            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this, "The file could not be read",
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves the list of agents to a file
     *
     * @param evt Event information
     */
    @SuppressWarnings("rawtypes")
    private void jButtonSaveConfigActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.DefaultListModel model
                = (javax.swing.DefaultListModel) jListAgents.getModel();
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser(startDir);
        if (fileChooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            startDir = fileChooser.getCurrentDirectory().toString();
            saveStartDir();
            String fileName = fileChooser.getSelectedFile().toString();
            try {
                java.io.PrintWriter w = new java.io.PrintWriter(fileName);
                for (int i = 0; i < model.size(); i++) {
                    GUIBoot.Agent a = (GUIBoot.Agent) model.elementAt(i);
                    w.println(a.name);
                    w.println(a.domain);
                    w.println(a.problem);
                }
                w.close();
            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this, "The file could not be saved",
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Launches the agents to start the planning process
     *
     * @param evt Event information
     */
    @SuppressWarnings("rawtypes")
    private void jButtonStartActionPerformed(java.awt.event.ActionEvent evt) {
        qpidHost = jTextQpid.getText();
        saveStartDir();
        javax.swing.DefaultListModel model = (javax.swing.DefaultListModel) jListAgents.getModel();
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        try {
            int x = 0, y = 0;
            int h = heuristicType.getSelectedIndex();
            int n = negotiationType.getSelectedIndex();
            timeout = -1;
            boolean isAnytime = anytime.isSelected();
            try {
                if (isAnytime) {
                    timeout = Integer.parseInt(jTextTimeout.getText());
                }
            } catch (NumberFormatException e) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Timeout is not a number",
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            int searchPerformance = 1; // Balanced: searchType.getSelectedIndex();
            if (h == HeuristicFactory.LAND_DTG_NORM || h == HeuristicFactory.LAND_DTG_INC) {
                searchPerformance = PlannerFactory.SEARCH_LANDMARKS;
            }
            int sameObjects = this.sameObjects.isSelected() ?
                    GroundedTask.SAME_OBJECTS_REP_PARAMS + GroundedTask.SAME_OBJECTS_PREC_EQ_EFF :
                    GroundedTask.SAME_OBJECTS_DISABLED;
            AgentList agList = new ParserImp().createEmptyAgentList();
            for (int i = 0; i < model.size(); i++)
                agList.addAgent(((GUIBoot.Agent) model.getElementAt(i)).name.toLowerCase(), "127.0.0.1");
            for (int i = 0; i < model.size(); i++) {
                GUIBoot.Agent a = (GUIBoot.Agent) model.getElementAt(i);
                PlanningAgent ag = new PlanningAgent(a.name.toLowerCase(), a.domain, a.problem,
                        agList, false, sameObjects, trace.isSelected(), h, searchPerformance, n,
                        isAnytime, timeout, null);
                GUIPlanningAgent gui = new GUIPlanningAgent(ag);
                gui.setLocation(x, y);
                y += gui.getHeight();
                if (y + gui.getHeight() > screenSize.height) {
                    x += gui.getWidth();
                    y = 0;
                }
                MAPboot.planningAgents.add(ag);
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not create the planning agents",
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            for (PlanningAgent ag : MAPboot.planningAgents) {
                ag.start();
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not start the planning agents",
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        jButtonStart.setEnabled(false);
        setState(ICONIFIED);
    }

    // Initial parameters for an agent
    private class Agent {

        String name;
        String domain, problem;

        Agent(String n, String d, String p) {
            name = n;
            domain = d;
            problem = p;
        }

        @Override
        public String toString() {
            return name + " (" + domain + "," + problem + ")";
        }
    }
}
