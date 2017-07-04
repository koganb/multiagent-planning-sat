package org.agreement_technologies.agents;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Oscar
 *         Graphical interface to show trace data of a planning agent
 */
public class GUITrace extends JFrame implements CaretListener, MouseListener {

    private static final long serialVersionUID = 3465968268556479994L;
    private static final String[] VIGNETTE = {"", "*", "+", "-"};
    private AgentListener ag;    // Planning agent
    private JTextArea taTrace;    // Text area to show the trace
    private int lineNum;

    /**
     * Creates a new trace form
     */
    public GUITrace(AgentListener ag) {
        this.ag = ag;
        setTitle("Trace: " + this.ag.getShortName());
        this.setSize(500, 600);
        this.setLocationRelativeTo(null);
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form
     */
    private void initComponents() {
        lineNum = -1;
        taTrace = new javax.swing.JTextArea();
        taTrace.setEditable(false);
        taTrace.addCaretListener(this);
        taTrace.addMouseListener(this);
        JScrollPane jspScroll = new JScrollPane();
        jspScroll.setViewportView(taTrace);
        getContentPane().add(jspScroll);
    }

    /**
     * Shows an error message
     *
     * @param msg Error message
     */
    public void showError(String msg) {
        showInfo(0, "ERROR: " + msg);
    }

    /**
     * Shows trace information
     *
     * @param level Indentation level
     * @param msg   Trace message
     */
    public void showInfo(int level, String msg) {
        String pre = level < VIGNETTE.length ? VIGNETTE[level] : ">";
        for (int i = 0; i < level; i++) pre = "  " + pre;
        if (level != 0) pre += " ";
        taTrace.append(pre + msg + "\n");
        taTrace.setCaretPosition(taTrace.getDocument().getLength());
    }

    @Override
    public void caretUpdate(CaretEvent arg0) {
        int caretPos = taTrace.getCaretPosition();
        try {
            lineNum = taTrace.getLineOfOffset(caretPos);
        } catch (BadLocationException e) {
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && lineNum >= 0) {
            try {
                int start = taTrace.getLineStartOffset(lineNum);
                int end = taTrace.getLineEndOffset(lineNum);
                String line = taTrace.getText().substring(start, end).trim();
                int pos = line.indexOf("\u03A0");
                if (pos >= 0) {
                    line = line.substring(pos);
                    pos = line.indexOf(" ");
                    if (pos >= 0) line = line.substring(0, pos);
                    ag.selectPlan(line.trim());
                }
            } catch (BadLocationException e1) {
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

}
