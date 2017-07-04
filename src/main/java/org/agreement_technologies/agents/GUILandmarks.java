package org.agreement_technologies.agents;

import org.agreement_technologies.common.map_landmarks.LandmarkFluent;
import org.agreement_technologies.common.map_landmarks.LandmarkNode;
import org.agreement_technologies.common.map_landmarks.LandmarkOrdering;
import org.agreement_technologies.common.map_landmarks.Landmarks;
import org.agreement_technologies.service.map_viewer.PlanViewerImp.ImageSelection;
import org.agreement_technologies.service.tools.Graph;
import org.agreement_technologies.service.tools.Graph.Adjacent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class GUILandmarks extends JFrame {
    private static final long serialVersionUID = 5812446400385328544L;
    private LandmarksPane landmarksPane;
    private JScrollPane jScrollPane;

    /**
     * Creates a new landmarks form
     */
    public GUILandmarks(AgentListener ag) {
        if (ag != null) setTitle("Landmarks: " + ag.getShortName());
        this.setSize(800, 600);
        this.setLocationRelativeTo(null);
        initComponents(ag);
    }

    /**
     * This method is called from within the constructor to initialize the form
     *
     * @param ag
     */
    private void initComponents(AgentListener ag) {
        getContentPane().setLayout(new java.awt.BorderLayout());
        landmarksPane = new LandmarksPane(ag.getLandmarks());
        jScrollPane = new JScrollPane();
        jScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setPreferredSize(new java.awt.Dimension(200, 150));
        jScrollPane.add(landmarksPane);
        jScrollPane.setViewportView(landmarksPane);
        getContentPane().add(jScrollPane);
    }

    private static class Landmark {
        String varName, varValue;

        public Landmark(String var, String val) {
            varName = var;
            varValue = val;
        }

        public boolean equals(Object x) {
            Landmark l = (Landmark) x;
            return l.varName.equals(varName) && l.varValue.equals(varValue);
        }

        public int hashCode() {
            return (varName + "=" + varValue).hashCode();
        }

        public String toString() {
            return varName + "=" + varValue;
        }
    }

    private static class LandmarkSet {
        private ArrayList<String> landmarks;
        private boolean disjunctive;

        public LandmarkSet(LandmarkNode n) {
            landmarks = new ArrayList<String>();
            disjunctive = !n.isSingleLiteral();
            for (LandmarkFluent f : n.getFluents())
                landmarks.add(f.toString());
        }

        public boolean equals(Object x) {
            LandmarkSet ls = (LandmarkSet) x;
            if (ls.landmarks.size() != landmarks.size() ||
                    ls.disjunctive != disjunctive) return false;
            for (String l : ls.landmarks)
                if (!landmarks.contains(l)) return false;
            return true;
        }

        public int hashCode() {
            int res = 0;
            for (String l : landmarks)
                res += l.hashCode();
            return res;
        }

        public String toString() {
            String res = disjunctive ? "{" : "";
            for (int i = 0; i < landmarks.size(); i++) {
                if (i > 0) res += "\n";
                res += landmarks.get(i);
            }
            return disjunctive ? res + "}" : res;
        }
    }

    private static class DrawNode {
        static final int BOX_WIDTH = 150;
        static java.awt.Font NODE_FONT = new java.awt.Font("Arial Narrow",
                java.awt.Font.PLAIN, 16);
        int index, x, y, level, lineHeight;
        double scale;
        LandmarkSet lset;
        ArrayList<DrawNode> nextNodes;
        ArrayList<Integer> arrowTypes;

        public DrawNode(int i, LandmarkSet ls, double scale) {
            index = i;
            lset = ls;
            level = -1;
            this.scale = scale;
            lineHeight = 14;
            nextNodes = new ArrayList<GUILandmarks.DrawNode>();
            arrowTypes = new ArrayList<Integer>();
        }

        private int scale(int n) {
            return (int) (scale * n);
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
            fLength = (float) Math.sqrt(vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1]);
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

        public int height() {
            return lset.landmarks.size() * lineHeight + 8;
        }

        public void paintNodes(Graphics2D g2d) {
            g2d.setFont(NODE_FONT);
            java.awt.FontMetrics metrics = g2d.getFontMetrics(NODE_FONT);
            lineHeight = metrics.getHeight();
            int h = height(), w = scale(BOX_WIDTH);
            if (lset.disjunctive) g2d.setColor(Color.lightGray);
            else g2d.setColor(Color.white);
            g2d.fillRect(scale(x), scale(y), w, h);
            g2d.setColor(Color.black);
            g2d.drawRect(scale(x), scale(y), w, h);
            int ty = scale(y);
            for (int i = 0; i < lset.landmarks.size(); i++) {
                String l = lset.landmarks.get(i);
                if (lset.disjunctive) {
                    if (i == 0) l = "{" + l;
                    if (i == lset.landmarks.size() - 1) l = l + "}";
                }
                int ws = metrics.stringWidth(l.toString());
                g2d.drawString(l.toString(), scale(x) + (w - ws) / 2, ty + lineHeight);
                ty += lineHeight;
            }
        }

        public void paintArrows(Graphics2D g2d) {
            int y = scale(this.y) + height() / 2;
            for (int i = 0; i < nextNodes.size(); i++) {
                DrawNode n = nextNodes.get(i);
                switch (arrowTypes.get(i)) {
                    case LandmarkOrdering.REASONABLE:
                        g2d.setColor(Color.blue);
                        break;
                    default:
                        g2d.setColor(Color.darkGray);
                }
                drawArrow(g2d, scale(x + BOX_WIDTH), y, scale(n.x),
                        scale(n.y) + n.height() / 2);
            }
        }

        public void addNext(DrawNode next, int type) {
            for (DrawNode n : nextNodes)
                if (n.index == next.index) return;
            nextNodes.add(next);
            arrowTypes.add(type);
        }

        public void setScale(double s) {
            scale = s;
            int fs;
            if (s <= 0.25) fs = 8;
            else if (s <= 0.5) fs = 9;
            else if (s <= 0.75) fs = 12;
            else if (s <= 1.5) fs = 16;
            else if (s <= 2.5) fs = 18;
            else fs = 16;
            NODE_FONT = new java.awt.Font("Arial Narrow", java.awt.Font.PLAIN, fs);
        }

        public boolean isSelected(int x, int y) {
            int rx = (int) (x / scale), ry = (int) (y / scale);
            return rx >= this.x && ry >= this.y && rx <= this.x + BOX_WIDTH && ry <= this.y + height();
        }

        public void move(int incX, int incY) {
            x += (int) incX / scale;
            y += (int) incY / scale;
        }
    }

    private static class LandmarksPane extends JPanel implements MouseListener, MouseMotionListener,
            MouseWheelListener, ActionListener {
        private static final long serialVersionUID = -1659156643473507799L;
        private static final int HORIZ_DST = 300, VERT_GAP = 60;
        private static final int HORIZ_MARGIN = 20, VERT_MARGIN = 20;
        private BufferedImage back;
        private Graph<LandmarkSet, Integer> graph;
        private ArrayList<DrawNode> nodes;
        private int maxLevel, width, height, mouseX, mouseY;
        private double scale;
        private DrawNode selected;
        private PopUpMenu popUpMenu;
        private Landmarks landmarks;

        public LandmarksPane(Landmarks landmarks) {
            setBackground(Color.WHITE);
            scale = 1;
            selected = null;
            this.landmarks = landmarks;
            generateNodes();
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            popUpMenu = new PopUpMenu();
            popUpMenu.itemTrans.addActionListener(this);
            popUpMenu.itemCycles.addActionListener(this);
            popUpMenu.itemCopy.addActionListener(this);
        }

        private void generateNodes() {
            graph = new Graph<LandmarkSet, Integer>();
            nodes = new ArrayList<GUILandmarks.DrawNode>();
            if (landmarks != null) {
                preprocessOrderings(landmarks);
                deleteEmptyLevels();
                locateNodes();
            }
            //if (graph.isAcyclic()) System.out.println("Aciclico");
            //else System.out.println("Tiene ciclos");
            graph = null;
            width = 100;
            height = 100;
            for (DrawNode n : nodes) {
                if (n.x + DrawNode.BOX_WIDTH > width) width = n.x + DrawNode.BOX_WIDTH + 20;
                if (n.y + n.height() > height) height = n.y + n.height() + 20;
            }
            width += 20;
            height += 20;
            back = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            setPreferredSize(new java.awt.Dimension(width, height));
        }

        private void deleteEmptyLevels() {
            maxLevel = 0;
            for (DrawNode n : nodes)
                if (n.level > maxLevel) maxLevel = n.level;
            int level = 0;
            while (level <= maxLevel) {
                boolean empty = true;
                for (DrawNode n : nodes)
                    if (n.level == level) {
                        empty = false;
                        break;
                    }
                if (empty) {
                    for (DrawNode n : nodes)
                        if (n.level > level) n.level--;
                    maxLevel--;
                } else level++;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
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
            for (DrawNode n : nodes)
                n.paintArrows(g2d);
            for (DrawNode n : nodes)
                n.paintNodes(g2d);
        }

        private void locateNodes() {
            Random rnd = new Random();
            DrawNode levels[] = new DrawNode[maxLevel + 1];
            for (DrawNode n : nodes) {
                n.x = HORIZ_MARGIN + n.level * HORIZ_DST;
                DrawNode prev = levels[n.level];
                n.y = prev == null ? VERT_MARGIN : prev.y + prev.height() + VERT_GAP +
                        rnd.nextInt(VERT_GAP / 4) - (VERT_GAP / 8);
                levels[n.level] = n;
            }
        }

        private void preprocessOrderings(Landmarks landmarks) {
            ArrayList<DrawNode> rootNodes = new ArrayList<DrawNode>();
            ArrayList<LandmarkOrdering> ords = landmarks.getOrderings(Landmarks.ALL_ORDERINGS, false);
            for (LandmarkOrdering o : ords) {
                LandmarkSet ls1 = new LandmarkSet(o.getNode1()),
                        ls2 = new LandmarkSet(o.getNode2());
                graph.addEdge(ls1, ls2, new Integer(o.getType()));
            }
            for (int i = 0; i < graph.numNodes(); i++)
                nodes.add(new DrawNode(i, graph.getNode(i), scale));
            for (DrawNode n : nodes) {
                ArrayList<Adjacent<Integer>> adj = graph.getAdjacents(n.index);
                for (Adjacent<Integer> a : adj)
                    n.addNext(nodes.get(a.dst), (Integer) a.label);
                if (graph.isRoot(n.index))
                    rootNodes.add(n);
            }
            if (rootNodes.isEmpty() && graph.numNodes() > 0) {
                int[] nList = graph.sortNodesByIndegree();
                int minDegree = graph.inDegree(nList[0]);
                for (int i = 0; i < nList.length; i++) {
                    if (graph.inDegree(nList[i]) == minDegree) {
                        rootNodes.add(nodes.get(nList[i]));
                    }
                }
            }
            boolean placedNodes[] = new boolean[nodes.size()];
            for (DrawNode n : rootNodes) {
                n.level = 0;
                placedNodes[n.index] = true;
            }
            for (int i = 0; i < placedNodes.length; i++)
                if (!placedNodes[i])
                    placeNode(nodes.get(i), rootNodes);
        }

        private void placeNode(DrawNode n, ArrayList<DrawNode> rootNodes) {
            n.level = 0;
            for (DrawNode orig : rootNodes) {
                int dist = graph.maxDistanceWithCycles(orig.index, n.index);
                if (dist != Graph.INFINITE && dist > n.level)
                    n.level = dist;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && selected == null) {
                mouseX = e.getX();
                mouseY = e.getY();
                for (DrawNode n : nodes)
                    if (n.isSelected(mouseX, mouseY)) {
                        selected = n;
                        break;
                    }
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                popUpMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            selected = null;
            if (e.getButton() == MouseEvent.BUTTON3) {
                popUpMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selected != null) {
                selected.move(e.getX() - mouseX, e.getY() - mouseY);
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int m = e.getWheelRotation();
            if (m > 0 && scale < 4) {
                scale += 0.25;
            } else if (m < 0 && scale > 0.25) {
                scale -= 0.25;
            }
            setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
            back = new BufferedImage((int) (width * scale), (int) (height * scale), BufferedImage.TYPE_3BYTE_BGR);
            for (DrawNode n : nodes) n.setScale(scale);
            repaint();
            revalidate();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == popUpMenu.itemTrans) {
                landmarks.filterTransitiveOrders();
                generateNodes();
                popUpMenu.itemTrans.setEnabled(false);
                repaint();
                revalidate();
            } else if (e.getSource() == popUpMenu.itemCycles) {
                landmarks.removeCycles();
                generateNodes();
                popUpMenu.itemCycles.setEnabled(false);
                repaint();
                revalidate();
            } else if (e.getSource() == popUpMenu.itemCopy) {
                if (back == null) return;
                ImageSelection imageSelection = new ImageSelection(back);
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(imageSelection, null);
            }
        }

        private class PopUpMenu extends JPopupMenu {
            private static final long serialVersionUID = 1L;
            JMenuItem itemTrans, itemCycles, itemCopy;

            public PopUpMenu() {
                itemTrans = new JMenuItem("Filter transitive orderings");
                add(itemTrans);
                itemCycles = new JMenuItem("Remove cycles");
                add(itemCycles);
                itemCopy = new JMenuItem("Copy to clipboard");
                add(itemCopy);
            }
        }
    }
}
