package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_landmarks.LandmarkFluent;
import org.agreement_technologies.common.map_landmarks.LandmarkNode;
import org.agreement_technologies.common.map_landmarks.LandmarkSet;

import java.util.ArrayList;

/**
 * @author Alex
 */
public class uSet implements LandmarkSet {
    private String id;
    private ArrayList<LandmarkFluent> set;
    private LandmarkNode node;
    private int value;

    public uSet(LandmarkFluent l) {
        id = l.getVar().getFuctionName();
        set = new ArrayList<LandmarkFluent>();
        set.add(l);
    }

    public String identify() {
        return id;
    }

    public void addElement(LandmarkFluent l) {
        if (!set.contains(l))
            set.add(l);
    }

    public ArrayList<LandmarkFluent> getElements() {
        return set;
    }

    public void setLGNode(LandmarkNode n) {
        node = n;
    }

    public LandmarkNode getLTNode() {
        return node;
    }

    public void calculateValue() {
        value = 0;
        for (LandmarkFluent l : set)
            value += l.getIndex();
    }

    public int compareTo(LandmarkSet o) {
        uSet u = (uSet) o;
        if (this.id.equals(u.id)) {
            if (this.set.size() == u.set.size()) {
                for (int i = 0; i < this.set.size(); i++) {
                    if (this.set.get(i).getVar() != u.set.get(i).getVar() || !this.set.get(i).getValue().equals(u.set.get(i).getValue())) {
                        return 1;
                    }
                }
                return 0;
            }
        }
        return 1;
    }

    public boolean match(LandmarkFluent p) {
        for (LandmarkFluent l : set) {
            if (l.equals(p))
                return true;
        }
        return false;
    }

    public String toString() {
        String res = new String();
        res += "{";

        for (LandmarkFluent d : this.set)
            res += d.toString() + ", ";
        res += "}";

        return res;
    }
}
 
