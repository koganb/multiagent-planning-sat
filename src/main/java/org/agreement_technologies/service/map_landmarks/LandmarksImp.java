package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_landmarks.LandmarkGraph;
import org.agreement_technologies.common.map_landmarks.LandmarkNode;
import org.agreement_technologies.common.map_landmarks.LandmarkOrdering;
import org.agreement_technologies.common.map_landmarks.Landmarks;
import org.agreement_technologies.service.tools.Graph;
import org.agreement_technologies.service.tools.Graph.Adjacent;

import java.util.ArrayList;

/**
 * Landmarks main class
 *
 * @author Alex
 */
public class LandmarksImp implements Landmarks {
    private ArrayList<LandmarkOrdering> orderings;
    private ArrayList<LandmarkNode> nodes;
    private int numGlobalNodes;
    private int numTotalNodes;

    public LandmarksImp(GroundedTask gt, AgentCommunication c) {
        long time = System.currentTimeMillis();
        RPG rpg = new RPG(gt, c);
        LandmarkGraph LG = gt.getAgentNames().length > 1
                ? new MALandmarkGraph(gt, c, rpg, rpg.getGoals())
                : new SALandmarkGraph(gt, rpg, rpg.getGoals());
        time = System.currentTimeMillis() - time;
        //System.out.println("RPG and landmark graph time: " + time + " ms.");
        numGlobalNodes = LG.numGlobalNodes();
        numTotalNodes = LG.numTotalNodes();
        orderings = new ArrayList<LandmarkOrdering>();
        if (LG.getReasonableOrderingList() != null)
            orderings.addAll(LG.getReasonableOrderingList());
        if (LG.getNeccessaryOrderingList() != null)
            orderings.addAll(LG.getNeccessaryOrderingList());
        ArrayList<LandmarkNode> allNodes = LG.getNodes();
        nodes = new ArrayList<LandmarkNode>(allNodes.size());
        for (int i = 0; i < allNodes.size(); i++) {
            assert allNodes.get(i).getIndex() == i;
            nodes.add(allNodes.get(i));
        }
    }

    @Override
    public ArrayList<LandmarkOrdering> getOrderings(int type, boolean onlyGoals) {
        ArrayList<LandmarkOrdering> ords = new ArrayList<LandmarkOrdering>(orderings.size());
        if (onlyGoals) {
            if ((type & REASONABLE_ORDERINGS) > 0) {
                for (LandmarkOrdering o : orderings)
                    if (o.getType() == LandmarkOrdering.REASONABLE && o.getNode1().isGoal()
                            && o.getNode2().isGoal())
                        ords.add(o);
            }
            if ((type & NECESSARY_ORDERINGS) > 0) {
                for (LandmarkOrdering o : orderings)
                    if (o.getType() == LandmarkOrdering.NECESSARY && o.getNode1().isGoal()
                            && o.getNode2().isGoal())
                        ords.add(o);
            }
        } else {
            if (type == ALL_ORDERINGS) ords.addAll(orderings);
            else {
                if ((type & REASONABLE_ORDERINGS) > 0) {
                    for (LandmarkOrdering o : orderings)
                        if (o.getType() == LandmarkOrdering.REASONABLE)
                            ords.add(o);
                }
                if ((type & NECESSARY_ORDERINGS) > 0) {
                    for (LandmarkOrdering o : orderings)
                        if (o.getType() == LandmarkOrdering.NECESSARY)
                            ords.add(o);
                }
            }
        }
        return ords;
    }

    public ArrayList<LandmarkNode> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<LandmarkNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void filterTransitiveOrders() {
        Graph<LandmarkNode, LandmarkOrdering> g = new Graph<LandmarkNode, LandmarkOrdering>();
        for (LandmarkOrdering o : orderings)
            g.addEdge(o.getNode1(), o.getNode2(), o);
        int numOrd = 0;
        while (numOrd < orderings.size()) {
            LandmarkOrdering o = orderings.get(numOrd);
            int n1 = g.getNodeIndex(o.getNode1()), n2 = g.getNodeIndex(o.getNode2());    // n1 -> n2
            boolean remove = false;
            for (Graph.Adjacent<LandmarkOrdering> a : g.getAdjacents(n1))
                if (a.dst != n2) {
                    int dst = g.minDistance(a.dst, n2);
                    if (dst != Graph.INFINITE) {
                        remove = true;        //n1 -> nx -> n2
                        break;
                    }
                }
            if (remove) orderings.remove(numOrd);
            else numOrd++;
        }
    }

    @Override
    public void removeCycles() {
        Graph<LandmarkNode, LandmarkOrdering> g = new Graph<LandmarkNode, LandmarkOrdering>();
        for (LandmarkOrdering o : orderings)
            g.addEdge(o.getNode1(), o.getNode2(), o);
        int[] marks = new int[nodes.size()];
        int nodeOrder[] = g.sortNodesByIndegree();
        for (int i = 0; i < nodeOrder.length; i++) {
            int orig = nodes.get(nodeOrder[i]).getIndex();
            if (marks[orig] == 0) removeCycles(orig, marks, g);
        }
    }

    private boolean removeCycles(int orig, int marks[], Graph<LandmarkNode, LandmarkOrdering> g) {
        marks[orig] = 2;    // Visited in the current branch
        ArrayList<Adjacent<LandmarkOrdering>> adj = g.getAdjacents(orig);
        int i = 0;
        while (i < adj.size()) {
            int dst = adj.get(i).dst;
            if (marks[dst] == 0) {    // Not visited
                removeCycles(dst, marks, g);
                i++;
            } else if (marks[dst] == 2) {    // Return edge
                orderings.remove(adj.get(i).label);
                adj.remove(i);
            } else i++;    // Visited
        }
        marks[orig] = 1;    // Visited
        return true;
    }

    @Override
    public int numGlobalNodes() {
        return numGlobalNodes;
    }

    @Override
    public int numTotalNodes() {
        return numTotalNodes;
    }
}
