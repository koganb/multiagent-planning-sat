package org.agreement_technologies.service.map_heuristic;

import org.agreement_technologies.common.map_grounding.Action;
import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedEff;

import java.util.ArrayList;
import java.util.HashMap;

public class RPG {
    private HashMap<VarValue, Integer> literalLevels;
    private HashMap<String, Integer> actionLevels;
    private int numLevels;

    public RPG(HashMap<String, ArrayList<String>> state, ArrayList<GroundedCond> goals,
               ArrayList<GroundedCond> pgoals, HashMap<String, ArrayList<Action>> requirers) {
        ArrayList<VarValue> lastLevel = new ArrayList<RPG.VarValue>(2 * state.size()),
                newLevel = new ArrayList<RPG.VarValue>(2 * state.size());
        literalLevels = new HashMap<RPG.VarValue, Integer>();
        actionLevels = new HashMap<String, Integer>();
        ArrayList<VarValue> remainingGoals = new ArrayList<RPG.VarValue>(goals.size());
        for (String var : state.keySet()) {
            VarValue v = new VarValue(var, state.get(var).get(0), 0);
            lastLevel.add(v);
            literalLevels.put(v, 0);
        }
        for (GroundedCond g : goals) {
            VarValue gv = new VarValue(g.getVar().toString(), g.getValue(), 0);
            if (!literalLevels.containsKey(gv))
                remainingGoals.add(gv);
        }
        for (GroundedCond g : pgoals) {
            VarValue gv = new VarValue(g.getVar().toString(), g.getValue(), 0);
            if (!literalLevels.containsKey(gv))
                remainingGoals.add(gv);
        }
        numLevels = 0;
        while (!remainingGoals.isEmpty() && !lastLevel.isEmpty()) {
            newLevel.clear();
            for (VarValue v : lastLevel) {
                ArrayList<Action> aList = requirers.get(v.getId());
                if (aList != null)
                    for (Action a : aList)
                        if (!actionLevels.containsKey(a.toString())) {
                            boolean executable = true;
                            for (GroundedCond prec : a.getPrecs())
                                if (!holds(prec)) {
                                    executable = false;
                                    break;
                                }
                            if (executable) {
                                actionLevels.put(a.toString(), numLevels);
                                //System.out.println(a.toString() + "(" + numLevels + ")");
                                for (GroundedEff eff : a.getEffs()) {
                                    VarValue ev = new VarValue(eff.getVar().toString(),
                                            eff.getValue(), numLevels + 1);
                                    if (!literalLevels.containsKey(ev) && !newLevel.contains(ev)) {
                                        newLevel.add(ev);
                                        //System.out.println(" - " + ev);
                                    }
                                }
                            }
                        }    // End for action
            }    // End for varValues
            numLevels++;
            for (VarValue v : newLevel) {
                literalLevels.put(v, v.level);
                remainingGoals.remove(v);
            }
            ArrayList<VarValue> aux = lastLevel;
            lastLevel = newLevel;
            newLevel = aux;
        }    // End RPG loop
    }

    private boolean holds(GroundedCond prec) {
        return literalLevels.containsKey(new VarValue(prec.getVar().toString(),
                prec.getValue(), 0));
    }

    public int numLevels() {
        return numLevels;
    }

    public VarValue getVarValue(GroundedCond g) {
        VarValue v = new VarValue(g.getVar().toString(), g.getValue(), 0);
        Integer level = literalLevels.get(v);
        if (level == null) return null;
        v.level = level;
        return v;
    }

    public int getLevel(Action a) {
        Integer level = actionLevels.get(a.toString());
        return level != null ? level : -1;
    }

    public int getDifficulty(Action a) {
        int d = 0;
        for (GroundedCond prec : a.getPrecs()) {
            VarValue v = new VarValue(prec.getVar().toString(), prec.getValue(), 0);
            d += literalLevels.get(v);
        }
        return d;
    }


    public static class VarValue implements Comparable<VarValue> {
        String var, value;
        int level;

        public VarValue(String var, String value, int level) {
            this.var = var;
            this.value = value;
            this.level = level;
        }

        public String toString() {
            return var + "=" + value + "(" + level + ")";
        }

        public String getId() {
            return var + "=" + value;
        }

        public boolean equals(Object x) {
            VarValue v = (VarValue) x;
            return var.equals(v.var) && value.equals(v.value);
        }

        public int hashCode() {
            return (var + value).hashCode();
        }

        @Override
        public int compareTo(VarValue v) {
            return v.level - level;
        }
    }
}
