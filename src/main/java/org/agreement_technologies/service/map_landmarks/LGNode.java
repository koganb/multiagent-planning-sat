package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_landmarks.LandmarkAction;
import org.agreement_technologies.common.map_landmarks.LandmarkFluent;
import org.agreement_technologies.common.map_landmarks.LandmarkNode;
import org.agreement_technologies.common.map_landmarks.LandmarkSet;

import java.util.ArrayList;

/**
 * Landmark tree node
 *
 * @author Alex
 */
public class LGNode implements LandmarkNode {
    private LandmarkFluent literal;
    private LandmarkSet disjunction;
    private boolean type;
    private int index;
    private ArrayList<String> agents;
    private ArrayList<Integer> antecessors;
    private int globalId;

    public LGNode(LandmarkSet u) {
        literal = null;
        disjunction = u;
        type = DISJUNCTION;
        antecessors = new ArrayList<Integer>();
        globalId = -1;
    }

    public LGNode(LandmarkFluent lit) {
        literal = lit;
        disjunction = null;
        type = SINGLE_LITERAL;
        antecessors = new ArrayList<Integer>();
        globalId = -1;
    }

    @Override
    public int setGlobalId(int globalIndex) {
        globalId = globalIndex;
        return globalIndex + 1;
    }

    @Override
    public int getGlobalId() {
        return globalId;
    }

    public ArrayList<Integer> getAntecessors() {
        return this.antecessors;
    }

    public void setAntecessors(ArrayList<Integer> ant) {
        this.antecessors = ant;
    }

    public ArrayList<LandmarkAction> getProducers() {
        if (type == SINGLE_LITERAL)
            return literal.getProducers();
        ArrayList<LandmarkAction> a = new ArrayList<LandmarkAction>();
        for (LandmarkFluent l : disjunction.getElements())
            for (LandmarkAction p : l.getProducers())
                if (!a.contains(p))
                    a.add(p);

        return a;
    }

    public ArrayList<LandmarkAction> getTotalProducers() {
        if (type == SINGLE_LITERAL)
            return literal.getTotalProducers();
        ArrayList<LandmarkAction> a = new ArrayList<LandmarkAction>();
        for (LandmarkFluent l : disjunction.getElements())
            for (LandmarkAction p : l.getTotalProducers())
                if (!a.contains(p))
                    a.add(p);

        return a;
    }


    public ArrayList<String> getAgents() {
        if (type == SINGLE_LITERAL)
            return literal.getAgents();
        return agents;
    }

    public void setAgents(ArrayList<String> ag) {
        this.agents = ag;
    }

    public boolean isSingleLiteral() {
        return type;
    }

    public LandmarkFluent getLiteral() {
        return literal;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String identify() {
        if (type == SINGLE_LITERAL)
            return literal.toString();
        return this.disjunction.identify();
    }

    public String toString() {
        if (type == SINGLE_LITERAL)
            if (literal != null)
                if (this.isGoal())
                    return "Global[" + this.getGlobalId() + "]Local[" + this.index + "] " + literal.toString() + " [Goal]";
                else if (this.literal.getLevel() == 0)
                    return "Global[" + this.getGlobalId() + "]Local[" + this.index + "] " + literal.toString() + " [Initial state]";
                else
                    return "Global[" + this.getGlobalId() + "]Local[" + this.index + "] " + literal.toString();
            else
                return "[" + this.index + "] " + literal.toString();
        else {
            String res = "[" + this.index + "] " + "{";

            for (LandmarkFluent l : disjunction.getElements())
                res += l.toString() + ", ";

            return res + "}";
        }
    }

    public LandmarkSet getDisjunction() {
        if (type == LGNode.SINGLE_LITERAL)
            return null;
        else
            return disjunction;
    }

    @Override
    public LandmarkFluent[] getFluents() {
        LandmarkFluent[] f;
        if (type == LGNode.SINGLE_LITERAL) {
            f = new LandmarkFluent[1];
            f[0] = this.literal;
        } else {
            f = new LandmarkFluent[disjunction.getElements().size()];
            for (int i = 0; i < disjunction.getElements().size(); i++) {
                f[i] = disjunction.getElements().get(i);
            }
        }

        return f;
    }

    @Override
    public boolean isGoal() {
        return type == SINGLE_LITERAL && literal.isGoal();
    }

    public boolean equals(Object x) {
        return index == ((LandmarkNode) x).getIndex();
    }

    public int hashCode() {
        return index;
    }
}
