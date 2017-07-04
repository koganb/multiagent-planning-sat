/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_landmarks.LandmarkFluent;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Alex
 */
public class LMLiteralInfo implements Serializable {
    private static final long serialVersionUID = 5010121211010241301L;
    private String literal;
    private String var;
    private String value;
    private String function;
    private String agent;
    private ArrayList<String> agents;
    private int level;
    private boolean isGoal;

    public LMLiteralInfo(LandmarkFluent l, String ag, ArrayList<String> ags) {
        literal = l.toString();
        var = l.getVarName();
        value = l.getValue();
        function = l.getVar().getFuctionName();
        agent = ag;
        agents = ags;
        level = l.getLevel();
        isGoal = l.isGoal();
    }

    public LMLiteralInfo(String f, ArrayList<String> ag) {
        literal = null;
        var = null;
        value = null;
        function = f;
        agent = null;
        agents = ag;
    }

    public String getLiteral() {
        return literal;
    }

    public int getLevel() {
        return level;
    }

    public boolean isGoal() {
        return isGoal;
    }

    public String getVariable() {
        return var;
    }

    public String getValue() {
        return value;
    }

    public String getFunction() {
        return function;
    }

    public String getAgent() {
        return agent;
    }

    public ArrayList<String> getAgents() {
        return agents;
    }

    public String toString() {
        return literal;
    }
}
