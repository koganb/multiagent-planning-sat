package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_grounding.GroundedVar;
import org.agreement_technologies.common.map_landmarks.LandmarkAction;
import org.agreement_technologies.common.map_landmarks.LandmarkFluent;

import java.util.ArrayList;

/**
 * @author Alex
 */
public class LMLiteral implements LandmarkFluent {
    private GroundedVar var;
    private String value;
    private int level;
    private int index;
    private ArrayList<LandmarkAction> producers;
    //All the producers of the literal, even if they come after the literal in the RPG
    private ArrayList<LandmarkAction> totalProducers;
    private boolean isGoal;
    private ArrayList<String> agents;

    public LMLiteral(GroundedVar v, String val, int t, String[] ag, boolean goal) {
        var = v;
        value = val;
        level = t;
        isGoal = goal;

        //agents variable stores the agents that share the LMLiteral
        agents = new ArrayList<String>(ag.length);
        if (ag.length == 1)
            agents.add(ag[0]);
        else {
            //Add all the agents that share the literal
            for (String a : ag)
                agents.add(a);
        }
    }

    public LMLiteral(String val, ArrayList<String> ag) {
        value = val;
        agents = ag;
        var = null;
        level = -1;
        index = -1;
        isGoal = false;
    }

    public ArrayList<LandmarkAction> getProducers() {
        return this.producers;
    }

    public void setProducers(ArrayList<LandmarkAction> p) {
        this.producers = p;
    }

    public ArrayList<LandmarkAction> getTotalProducers() {
        return this.totalProducers;
    }

    public void setTotalProducers(ArrayList<LandmarkAction> p) {
        this.totalProducers = p;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public GroundedVar getVar() {
        return var;
    }

    public String getValue() {
        return value;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int l) {
        level = l;
    }

    public boolean isGoal() {
        return isGoal;
    }

    public String toString() {
        if (var != null)
            return var.toString() + " " + value;
        return value;
    }

    @Override
    public String getVarName() {
        return var.toString();
    }

    @Override
    public ArrayList<String> getAgents() {
        return agents;
    }
}
