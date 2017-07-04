package org.agreement_technologies.service.map_viewer;

import org.agreement_technologies.common.map_planner.*;
import org.agreement_technologies.common.map_viewer.PlanViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Hashtable;

public class PlanViewerImp extends JPanel implements PlanViewer, MouseListener, MouseMotionListener,
        MouseWheelListener, ActionListener {
    private static final long serialVersionUID = -677123886339372031L;
    private static final Color[] AGENT_COLORS = {
            Color.BLACK,               // black
            new Color(160, 30, 35),    // dark red
            new Color(30, 35, 140),    // dark blue
            new Color(40, 100, 25),    // dark green
            new Color(105, 70, 30),    // brown
            new Color(90, 30, 115),    // violet
            new Color(178, 20, 180),   // pink
            new Color(24, 132, 132)}; // cyan
    private static final int VER_SEPARATION = 100;
    private static final int HOR_SEPARATION = 200;
    private static final int NODE_WIDTH = 60;
    private static final int NODE_HEIGHT = 40;
    private static java.awt.Font NODE_FONT = new java.awt.Font(
            "Arial Narrow", java.awt.Font.PLAIN, 11);
    private Plan plan = null;
    private Graph g = null;
    private ArrayList<Node> levels[] = null;
    private int maxLevels, maxNodesPerLevel, width, height, mouseX, mouseY;
    private Hashtable<String, java.awt.Color> agentColor;
    private PopUpMenu popUpMenu;
    private BufferedImage back;
    private double scale;
    private Node selected;
    private boolean showPrecs, showEffs;
    private PlannerFactory pf;

    public PlanViewerImp() {
        super();
        this.pf = null;
        back = null;
        showPrecs = showEffs = false;
        initComponents();
    }

    public PlanViewerImp(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        initComponents();
    }

    public PlanViewerImp(java.awt.LayoutManager layout) {
        super(layout);
        initComponents();
    }

    public PlanViewerImp(java.awt.LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        initComponents();
    }

    private void initComponents() {
        scale = 1;
        selected = null;
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        popUpMenu = new PopUpMenu();
        popUpMenu.itemCopy.addActionListener(this);
        popUpMenu.itemSave.addActionListener(this);
        popUpMenu.showEffs.addActionListener(this);
        popUpMenu.showPrecs.addActionListener(this);
    }

    private int scale(int n) {
        return (int) (scale * n);
    }

    private int unscale(int n) {
        return (int) (n / scale);
    }

    private void showOrderingInfo(Node nOrig, int adjIndex, boolean ordering) {
        String title = ordering ? "Ordering information"
                : "Causal link information";
        String s = nOrig.stepToString();
        if (ordering)
            s += " --> ";
        else {
            s += " -- (" + nOrig.causalLink.get(adjIndex).toString() + ") -->";
        }
        int stepIndex = nOrig.adjacents.get(adjIndex);
        s += g.v[stepIndex].stepToString();
        JOptionPane.showMessageDialog(getParent(), s, title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private double distanceToSegment(Point p, int x, int y, int xx, int yy) {
        double r_numerator = (p.x - x) * (xx - x) + (p.y - y) * (yy - y);
        double r_denomenator = (xx - x) * (xx - x) + (yy - y) * (yy - y);
        double r = r_numerator / r_denomenator;
        double s = ((y - p.y) * (xx - x) - (x - p.x) * (yy - y))
                / r_denomenator;
        double distanceLine = Math.abs(s) * Math.sqrt(r_denomenator);
        double distanceSegment;
        if ((r >= 0) && (r <= 1)) {
            distanceSegment = distanceLine;
        } else {
            double dist1 = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
            double dist2 = (p.x - xx) * (p.x - xx) + (p.y - yy) * (p.y - yy);
            if (dist1 < dist2) {
                distanceSegment = Math.sqrt(dist1);
            } else {
                distanceSegment = Math.sqrt(dist2);
            }
        }
        return distanceSegment;
    }

    private void showNodeInfo(Node n) {
        Step a = n.step;
        String s = a.getIndex() + ": " + n.stepToString();
        s += "\nPrecs: ";
        Condition[] precs = a.getPrecs();
        for (int i = 0; i < precs.length; i++) {
            if (i > 0) {
                s += ", ";
                if (i % 8 == 0)
                    s += "\n\t";
            }
            s += "(" + precs[i].labeled(pf) + ")";
        }
        s += "\nEffs: ";
        Condition[] effs = a.getEffs();
        for (int i = 0; i < effs.length; i++) {
            if (i > 0) {
                s += ", ";
                if (i % 8 == 0)
                    s += "\n\t";
            }
            s += "(" + effs[i].labeled(pf) + ")";
        }
        JOptionPane.showMessageDialog(getParent(), s, "Step information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void showPlan(Plan plan, PlannerFactory pf) {
        this.plan = plan;
        this.pf = pf;
        scale = 1;
        selected = null;
        agentColor = new Hashtable<String, Color>(5);
        createGraph();
        height = 3 * (maxNodesPerLevel * VER_SEPARATION) / 2;
        width = (maxLevels + 2) * (HOR_SEPARATION / 2);
        setPreferredSize(new java.awt.Dimension(width, height));
        back = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        this.repaint();
        revalidate();
    }

    @SuppressWarnings("unchecked")
    private void createGraph() {
        g = new Graph(plan);
        maxLevels = g.getNumLevels();
        levels = new ArrayList[maxLevels];
        maxNodesPerLevel = 0;
        for (int i = 0; i < maxLevels; i++) {
            levels[i] = new ArrayList<Node>();
            g.addNodesAtLevel(maxLevels - i - 1, levels[i]);
            if (levels[i].size() > maxNodesPerLevel) {
                maxNodesPerLevel = levels[i].size();
            }
        }
        int height = maxNodesPerLevel * VER_SEPARATION;
        int x = HOR_SEPARATION / 2;
        for (int level = 0; level < maxLevels; level++) {
            int numNodes = levels[level].size();
            int levelHeight = numNodes * VER_SEPARATION;
            int y = ((height - levelHeight) / 2)
                    + ((VER_SEPARATION - NODE_HEIGHT) / 2);
            for (Node n : levels[level]) {
                n.x = x;
                n.y = y + (int) (Math.random() * 20 - 10);
                y += VER_SEPARATION;
                String agent = n.step.getAgent();
                if (agent != null && !agentColor.containsKey(agent)) {
                    int cindex = agentColor.size() % AGENT_COLORS.length;
                    agentColor.put(agent, AGENT_COLORS[cindex]);
                }
            }
            x += VER_SEPARATION;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (back != null) {
            draw(back.getGraphics());
            g.drawImage(back, 0, 0, null);
        }
    }

    private void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, back.getWidth(), back.getHeight());
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        java.util.Enumeration<String> ags = agentColor.keys();
        int x = 10, y = 10;
        while (ags.hasMoreElements()) {
            String ag = ags.nextElement();
            Color c = agentColor.get(ag);
            g2d.setColor(c);
            g2d.fillRect(x, y, 10, 10);
            g2d.drawString(ag, x + 16, y + 8);
            y += 20;
        }
        g2d.setColor(Color.black);
        g2d.drawString("Makespan: " + (maxLevels - 2), x, y + 8);
        for (int level = 0; level < maxLevels; level++) {
            for (Node n : levels[level]) {
                for (int i = 0; i < n.adjacents.size(); i++) {
                    int dn = n.adjacents.get(i);
                    boolean ordering = n.causalLink.get(i) == null;
                    drawArrow(g2d, n, this.g.v[dn], ordering);
                }
            }
        }
        for (int level = 0; level < maxLevels; level++)
            for (Node n : levels[level])
                drawNode(g2d, n);

    }

    private void drawArrow(Graphics2D g2d, Node n, Node dn, boolean ordering) {
        if (ordering)
            g2d.setColor(java.awt.Color.LIGHT_GRAY);
        else
            g2d.setColor(java.awt.Color.BLACK);
        drawArrow(g2d, scale(n.x + NODE_WIDTH), scale(n.y + (NODE_HEIGHT / 2)),
                scale(dn.x), scale(dn.y + (NODE_HEIGHT / 2)));

    }

    private void drawArrow(Graphics2D g, int x, int y, int xx, int yy) {
        float arrowWidth = 6.0f;
        float theta = 0.423f;
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        float[] vecLine = new float[2];
        float[] vecLeft = new float[2];
        float fLength;
        float th;
        float ta;
        float baseX, baseY;

        xPoints[0] = xx;
        yPoints[0] = yy;

        // build the line vector
        vecLine[0] = (float) xPoints[0] - x;
        vecLine[1] = (float) yPoints[0] - y;

        // build the arrow base vector - normal to the line
        vecLeft[0] = -vecLine[1];
        vecLeft[1] = vecLine[0];

        // setup length parameters
        fLength = (float) Math.sqrt(vecLine[0] * vecLine[0] + vecLine[1]
                * vecLine[1]);
        th = arrowWidth / (2.0f * fLength);
        ta = arrowWidth / (2.0f * ((float) Math.tan(theta) / 2.0f) * fLength);

        // find the base of the arrow
        baseX = ((float) xPoints[0] - ta * vecLine[0]);
        baseY = ((float) yPoints[0] - ta * vecLine[1]);

        // build the points on the sides of the arrow
        xPoints[1] = (int) (baseX + th * vecLeft[0]);
        yPoints[1] = (int) (baseY + th * vecLeft[1]);
        xPoints[2] = (int) (baseX - th * vecLeft[0]);
        yPoints[2] = (int) (baseY - th * vecLeft[1]);

        g.drawLine(x, y, (int) baseX, (int) baseY);
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawNode(Graphics2D g2d, Node n) {
        g2d.setFont(NODE_FONT);
        java.awt.FontMetrics metrics = g2d.getFontMetrics(NODE_FONT);
        if (n.step == this.plan.getInitialStep()) {
            g2d.setColor(java.awt.Color.LIGHT_GRAY);
            g2d.fillOval(scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT));
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawOval(scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT));
            g2d.drawOval(scale(n.x + 3), scale(n.y + 3), scale(NODE_WIDTH - 6), scale(NODE_HEIGHT - 6));
            drawText(g2d, "0.Init", scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT), metrics);
        } else if (n.step == this.plan.getFinalStep()) {
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillOval(scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT));
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawOval(scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT));
            g2d.drawOval(scale(n.x + 3), scale(n.y + 3), scale(NODE_WIDTH - 6), scale(NODE_HEIGHT - 6));
            drawText(g2d, "1.Goals", scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT), metrics);
        } else {
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillOval(scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT));
            Color c = n.step.getAgent() != null ? agentColor.get(n.step
                    .getAgent()) : Color.black;
            if (c == null)
                c = Color.BLACK;
            g2d.setColor(c);
            g2d.drawOval(scale(n.x), scale(n.y), scale(NODE_WIDTH), scale(NODE_HEIGHT));
            String a = n.step != null ? n.step.getActionName() : "";
            drawText(g2d, n.step.getIndex() + "." + a, scale(n.x), scale(n.y), scale(NODE_WIDTH),
                    scale(NODE_HEIGHT), metrics);
        }
        if (showPrecs) {
            int incY = (int) (metrics.getHeight() * 0.9);
            int cx = scale(n.x) + scale(NODE_WIDTH) / 2;
            int y = scale(n.y);
            for (Condition c : n.step.getPrecs()) {
                String p = c.toString();
                int x = cx - metrics.stringWidth(p) / 2;
                g2d.drawString(p, x, y);
                y -= incY;
            }
        }
        if (showEffs) {
            int incY = (int) (metrics.getHeight() * 0.9);
            int cx = scale(n.x) + scale(NODE_WIDTH) / 2;
            int y = scale(n.y) + scale(NODE_HEIGHT) + incY;
            for (Condition ef : n.step.getEffs()) {
                String e = ef.toString();
                int x = cx - metrics.stringWidth(e) / 2;
                g2d.drawString(e, x, y);
                y += incY;
            }
        }
    }

    private void drawText(Graphics2D g2d, String text, int x, int y, int width,
                          int height, java.awt.FontMetrics metrics) {
        int tw = metrics.stringWidth(text);
        int th = (int) (metrics.getHeight() * 0.9);
        if (tw > width) {
            String line1 = text, line2;
            do {
                line1 = line1.substring(0, line1.length() - 1);
            } while (metrics.stringWidth(line1) > width);
            int pos_spc = line1.lastIndexOf(' ');
            if (pos_spc != -1)
                line1 = line1.substring(0, pos_spc);
            line2 = text.substring(line1.length()).trim();
            if (metrics.stringWidth(line2) > width)
                while (metrics.stringWidth(line2 + "...") > width) {
                    line2 = line2.substring(0, line2.length() - 1);
                }
            int posx = x + (width - metrics.stringWidth(line1)) / 2;
            int posy = y + (height / 2);
            g2d.drawString(line1, posx, posy);
            posx = x + (width - metrics.stringWidth(line2)) / 2;
            g2d.drawString(line2, posx, posy + th);
        } else {
            int posx = x + (width - tw) / 2;
            int posy = y + (height / 2) + (th / 4);
            g2d.drawString(text, posx, posy);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == popUpMenu.itemCopy) {
            if (back == null) return;
            ImageSelection imageSelection = new ImageSelection(back);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(imageSelection, null);
        } else if (e.getSource() == popUpMenu.itemSave) {
            try {
                java.io.FileWriter outFile = new java.io.FileWriter("plan.txt");
                java.io.PrintWriter out = new java.io.PrintWriter(outFile);
                out.println(plan.numSteps()); // Number of steps
                for (int i = 0; i < plan.numSteps(); i++) { // For each step
                    Step s = plan.getStepsArray().get(i);
                    out.println(s.getActionName()); // Step name
                    if (s == plan.getInitialStep() || s == plan.getFinalStep()) // Step
                        // agent
                        out.println("");
                    else
                        out.println(s.getAgent());
                    out.println(s.getPrecs().length); // Num. precs.
                    for (Condition prec : s.getPrecs())
                        out.println(prec.toString()); // Preconditions
                    out.println(s.getEffs().length); // Num. effs.
                    for (Condition eff : s.getEffs())
                        out.println(eff.toString()); // Effects
                }
                out.println(plan.getCausalLinksArray().size()); // Num. causal
                // links
                for (CausalLink cl : plan.getCausalLinksArray()) { // For each
                    // causal
                    // link
                    out.println(cl.getIndex1());
                    out.println(cl.getIndex2());
                    out.println(cl.getCondition().toString());
                }
                out.println(plan.getOrderingsArray().size()); // Num. orderings
                for (Ordering o : plan.getOrderingsArray()) { // For each
                    // ordering
                    out.println(o.getIndex1());
                    out.println(o.getIndex2());
                }
                out.close();
            } catch (java.io.IOException e2) {
            }
        } else if (e.getSource() == popUpMenu.showPrecs) {
            showPrecs = popUpMenu.showPrecs.isSelected();
            repaint();
        } else if (e.getSource() == popUpMenu.showEffs) {
            showEffs = popUpMenu.showEffs.isSelected();
            repaint();
        }
    }

    @Override
    public int getMakespan() {
        return maxLevels;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int m = e.getWheelRotation();
        if (m > 0 && scale < 4) {
            scale += 0.25;
        } else if (m < 0 && scale > 0.25) {
            scale -= 0.25;
        }
        int fs;
        if (scale <= 0.25) fs = 8;
        else if (scale <= 0.5) fs = 9;
        else if (scale <= 0.75) fs = 10;
        else if (scale <= 1.25) fs = 11;
        else if (scale <= 2) fs = 12;
        else if (scale <= 3) fs = 14;
        else fs = 16;
        NODE_FONT = new java.awt.Font("Arial Narrow", java.awt.Font.PLAIN, fs);
        setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
        back = new BufferedImage((int) (width * scale), (int) (height * scale), BufferedImage.TYPE_3BYTE_BGR);
        repaint();
        revalidate();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (selected != null) {
            int incX = unscale(e.getX() - mouseX), incY = unscale(e.getY() - mouseY);
            selected.x += incX;
            selected.y += incY;
            mouseX = e.getX();
            mouseY = e.getY();
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        selected = null;
        if (e.getButton() == MouseEvent.BUTTON3) {
            popUpMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private Node selectedNode(Point p) {
        for (int level = 0; level < maxLevels; level++) {
            for (Node n : levels[level]) {
                if (n.contains(p))
                    return n;
            }
        }
        return null;
    }

    private void showSelectedOrderings(Point p) {
        for (int level = 0; level < maxLevels; level++) {
            for (Node n : levels[level]) {
                for (int i = 0; i < n.adjacents.size(); i++) {
                    int dn = n.adjacents.get(i);
                    boolean ordering = n.causalLink.get(i) == null;
                    if (distanceToSegment(p, n.x + NODE_WIDTH, n.y
                            + (NODE_HEIGHT / 2), g.v[dn].x, g.v[dn].y
                            + (NODE_HEIGHT / 2)) < 2)
                        showOrderingInfo(n, i, ordering);
                }
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseX = e.getX();
            mouseY = e.getY();
            Point p = new Point(unscale(mouseX), unscale(mouseY));
            if (e.getClickCount() == 1) {
                if (selected == null) {
                    selected = selectedNode(p);
                    if (selected == null)
                        showSelectedOrderings(p);
                }
            } else {
                Node n = selectedNode(p);
                if (n != null) showNodeInfo(n);
                else showSelectedOrderings(p);
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            popUpMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private static class Node {
        Step step;
        int distanceToLast;
        ArrayList<Integer> adjacents;
        ArrayList<Condition> causalLink;
        int x, y;

        Node(Step ps) {
            step = ps;
            distanceToLast = -1;
            adjacents = new ArrayList<Integer>();
            causalLink = new ArrayList<Condition>();
        }

        public String stepToString() {
            if (step.getIndex() == 0)
                return "Initial step";
            if (step.getIndex() == 1)
                return "Final step";
            return step.getActionName();
        }

        boolean contains(Point p) {
            double xRadius = NODE_WIDTH / 2, yRadius = NODE_HEIGHT / 2;
            double xTar = p.x - this.x - xRadius, yTar = p.y - this.y - yRadius;
            return Math.pow(xTar / xRadius, 2) + Math.pow(yTar / yRadius, 2) <= 1;
        }
    }

    private static class Graph {
        int numNodes, numEdges;
        Node v[];

        public Graph(Plan plan) {
            ArrayList<Step> planSteps = plan.getStepsArray();
            this.numNodes = planSteps.size();
            numEdges = 0;
            v = new Node[numNodes];
            for (int i = 0; i < v.length; i++) {
                v[i] = new Node(planSteps.get(i));
            }
            ArrayList<Ordering> orderings = plan.getOrderingsArray();
            for (Ordering po : orderings) {
                insertEdge(po.getIndex1(), po.getIndex2(), true, null);
            }
            ArrayList<CausalLink> causalLinks = plan.getCausalLinksArray();
            for (CausalLink cl : causalLinks) {
                insertEdge(cl.getIndex1(), cl.getIndex2(), false,
                        cl.getCondition());
            }
            for (int i = 1; i < v.length; i++) {
                boolean hasPred = false;
                for (int j = 0; j < v.length; j++)
                    if (edgeExists(j, i)) {
                        hasPred = true;
                        break;
                    }
                if (!hasPred)
                    insertEdge(0, i, true, null);
            }
            for (int i = 2; i < v.length; i++) {
                boolean hasSuc = false;
                for (int j = 2; j < v.length; j++)
                    if (edgeExists(i, j)) {
                        hasSuc = true;
                        break;
                    }
                if (!hasSuc)
                    insertEdge(i, 1, true, null);
            }
            for (int i = 0; i < v.length; i++) {
                v[i].distanceToLast = maxPath(i, plan.getFinalStep().getIndex());
            }
        }

        private boolean edgeExists(int i, int j) {
            return v[i].adjacents.contains(j);
        }

        public final void insertEdge(int i, int j, boolean ordering,
                                     Condition cond) {
            if (!edgeExists(i, j)) {
                v[i].adjacents.add(j);
                v[i].causalLink.add(cond);
                numEdges++;
            } else if (!ordering) {
                int index = v[i].adjacents.indexOf(j);
                v[i].causalLink.set(index, cond);
            }
        }

        public final int maxPath(int vOrigen, int vDestino) {
            int distanciaMax[] = new int[v.length];
            for (int i = 0; i < v.length; i++) {
                distanciaMax[i] = -1;
            }
            distanciaMax[vOrigen] = 0;
            ArrayDeque<Integer> q = new ArrayDeque<Integer>();
            q.add(vOrigen);
            while (!q.isEmpty()) {
                int vActual = q.poll();
                ArrayList<Integer> aux = v[vActual].adjacents;
                for (int i = 0; i < aux.size(); i++) {
                    int vSiguiente = aux.get(i);
                    if (distanciaMax[vSiguiente] <= distanciaMax[vActual]) {
                        distanciaMax[vSiguiente] = distanciaMax[vActual] + 1;
                        if (distanciaMax[vSiguiente] > v.length) {
                            System.out.println("Error: loop in the plan");
                            return v.length;
                        }
                        q.add(vSiguiente);
                    }
                }
            }
            return distanciaMax[vDestino];
        }

        public void addNodesAtLevel(int level, ArrayList<Node> list) {
            for (int i = 0; i < v.length; i++) {
                if (v[i].distanceToLast == level) {
                    list.add(v[i]);
                }
            }
        }

        public int getNumLevels() {
            int n = -1;
            for (int i = 0; i < v.length; i++) {
                if (v[i].distanceToLast > n) {
                    n = v[i].distanceToLast;
                }
            }
            return n + 1;
        }
    }

    public static class ImageSelection implements Transferable {
        private Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

        // Returns the supported flavors of our implementation
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        // Returns true if flavor is supported
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        // Returns Image object housed by Transferable object
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, java.io.IOException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            // else return the payload
            return image;
        }
    }

    private class PopUpMenu extends JPopupMenu {
        private static final long serialVersionUID = 1L;
        JMenuItem itemCopy, itemSave;
        JCheckBoxMenuItem showPrecs, showEffs;

        public PopUpMenu() {
            itemCopy = new JMenuItem("Copy plan to clipboard");
            add(itemCopy);
            itemSave = new JMenuItem("Save plan to plan.txt");
            add(itemSave);
            addSeparator();
            showPrecs = new JCheckBoxMenuItem("Show preconditions");
            add(showPrecs);
            showEffs = new JCheckBoxMenuItem("Show effects");
            add(showEffs);
        }
    }
}
