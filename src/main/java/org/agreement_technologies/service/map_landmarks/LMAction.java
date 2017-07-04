package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_grounding.*;
import org.agreement_technologies.common.map_landmarks.LandmarkAction;
import org.agreement_technologies.common.map_landmarks.LandmarkFluent;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Action for the landmark graph
 *
 * @author Alex
 */
public class LMAction implements LandmarkAction {
    private String name;
    private int level;
    private ArrayList<LandmarkFluent> preconditions;
    private ArrayList<LandmarkFluent> effects;

    public LMAction(Action a, Hashtable<String, GroundedVar> vars, Hashtable<String, LMLiteral> lits, RPG rpg) {
        LMLiteral l;
        String[] agents;

        name = a.getOperatorName();
        for (String s : a.getParams())
            name += " " + s;
        level = a.getMinTime();
        preconditions = new ArrayList<LandmarkFluent>();
        effects = new ArrayList<LandmarkFluent>();

        for (GroundedCond p : a.getPrecs()) {
            if (vars.get(p.getVar().toString()) == null)
                vars.put(p.getVar().toString(), p.getVar());
            if (lits.get(p.getVar().toString() + " " + p.getValue()) == null) {
                agents = rpg.getAgents(p.getVar(), p.getValue());
                l = new LMLiteral(vars.get(p.getVar().toString()), p.getValue(), p.getVar().getMinTime(p.getValue()), agents, false);
                lits.put(l.toString(), l);
            }
            preconditions.add(lits.get(p.getVar().toString() + " " + p.getValue()));
        }
        for (GroundedEff e : a.getEffs()) {
            if (vars.get(e.getVar().toString()) == null)
                vars.put(e.getVar().toString(), e.getVar());
            if (lits.get(e.getVar().toString() + " " + e.getValue()) == null) {
                agents = rpg.getAgents(e.getVar(), e.getValue());
                l = new LMLiteral(vars.get(e.getVar().toString()), e.getValue(), e.getVar().getMinTime(e.getValue()), agents, false);
                lits.put(l.toString(), l);
            }
            effects.add(lits.get(e.getVar().toString() + " " + e.getValue()));
        }
    }

    /**
     * Returns the array of agents with whom a LMLiteral is shareable
     *
     * @param gt  Grounded task
     * @param v   Variable associated to the LMLiteral
     * @param val Value associated to the LMLiteral
     * @return Array of agent names
     */
    private String[] getAgents(GroundedTask gt, GroundedVar v, String val) {
        ArrayList<String> agents = new ArrayList<String>();
        for (int i = 0; i < gt.getAgentNames().length; i++) {
            if (gt.getAgentNames()[i].equals(gt.getAgentName()))
                agents.add(gt.getAgentNames()[i]);
            else if (v.shareable(val, gt.getAgentNames()[i]))
                agents.add(gt.getAgentNames()[i]);
        }
        String[] agentsArray = new String[agents.size()];
        int pos = 0;
        for (String ag : agents) {
            agentsArray[pos] = ag;
            pos++;
        }

        return agentsArray;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int l) {
        level = l;
    }

    public ArrayList<LandmarkFluent> getPreconditions() {
        return preconditions;
    }

    public ArrayList<LandmarkFluent> getEffects() {
        return effects;
    }

    public String toString() {
        return name;
    }
}
