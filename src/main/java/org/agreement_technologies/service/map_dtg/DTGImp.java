package org.agreement_technologies.service.map_dtg;

import org.agreement_technologies.common.map_dtg.DTG;
import org.agreement_technologies.common.map_dtg.DTGRequest;
import org.agreement_technologies.common.map_dtg.DTGSet;
import org.agreement_technologies.common.map_dtg.DTGTransition;
import org.agreement_technologies.common.map_grounding.*;

import java.util.*;

public class DTGImp implements DTG {
    private static final int MAX_SEARCH_NODES = 9999;
    private DTGSet dtgSet;
    private GroundedTask task;
    private GroundedVar var;
    private String values[];
    private Hashtable<String, Integer> valueIndex;
    private ArrayList<ArrayList<Transition>> transitions;
    private Vector<Hashtable<TransitionMemo, Path>> shortestPaths;
    private Vector<Hashtable<TransitionMemo, Integer>> distances;
    private Hashtable<String, Dijkstra> shortestPathsMulti;
    private int searchNodes;

    public DTGImp(DTGSet dtgSet, GroundedVar v, GroundedTask task) {
        this.dtgSet = dtgSet;
        this.task = task;
        var = v;
        values = v.getReachableValues();
        if (v.isBoolean() && values.length < 2) {
            values = new String[2];
            values[0] = "true";
            values[1] = "false";
        }
        valueIndex = new Hashtable<String, Integer>(values.length);
        transitions = new ArrayList<ArrayList<Transition>>(values.length);
        for (int i = 0; i < values.length; i++) {
            valueIndex.put(values[i], i);
            transitions.add(new ArrayList<DTGImp.Transition>());
        }
        for (Action a : task.getActions()) {
            GroundedEff eff = changes(a, var);
            if (eff != null)
                addTransition(a, eff, task.getAgentName().toLowerCase());
        }
        shortestPaths = new Vector<Hashtable<TransitionMemo, DTGImp.Path>>();
        distances = new Vector<Hashtable<TransitionMemo, Integer>>();
        shortestPathsMulti = new Hashtable<String, DTGImp.Dijkstra>();
    }

    @Override
    public int pathCostMulti(String initValue, String endValue) {
        Dijkstra sp = shortestPathsMulti.get(initValue);
        if (sp == null) {
            Integer vIndex = valueIndex.get(initValue);
            sp = new Dijkstra(vIndex);
        }
        return sp.getPathCost(endValue);
    }

    @Override
    public String[] getPathMulti(String initValue, String endValue) {
        Dijkstra sp = shortestPathsMulti.get(initValue);
        if (sp == null) {
            Integer vIndex = valueIndex.get(initValue);
            sp = new Dijkstra(vIndex);
        }
        return sp.getPath(endValue);
    }

    private void addTransition(Action a, GroundedEff eff, String fromAgent) {
        GroundedCond prec = requires(a, var);
        int toValue = valueIndex.get(eff.getValue());
        if (prec == null || prec.getCondition() == GroundedCond.DISTINCT) {
            for (int fromValue = 0; fromValue < values.length; fromValue++)
                if (toValue != fromValue) {
                    Transition t = getTransition(fromValue, toValue);
                    if (t == null) {
                        t = new Transition(fromValue, toValue, a, fromAgent);
                        transitions.get(fromValue).add(t);
                    } else t.addAction(a, fromAgent);
                }
        } else {
            int fromValue = valueIndex.get(prec.getValue());
            Transition t = getTransition(fromValue, toValue);
            if (t == null) {
                t = new Transition(fromValue, toValue, a, fromAgent);
                transitions.get(fromValue).add(t);
            } else t.addAction(a, fromAgent);
        }
    }

    private Transition getTransition(int fromValue, int toValue) {
        for (Transition t : transitions.get(fromValue))
            if (t.toValue == toValue) return t;
        return null;
    }


    @Override
    public DTGTransition[] getTransitionsFrom(String fromValue) {
        Integer index = valueIndex.get(fromValue);
        if (index == null) return null;
        ArrayList<Transition> trans = transitions.get(index);
        DTGTransition[] res = new DTGTransition[trans.size()];
        for (int i = 0; i < res.length; i++)
            res[i] = trans.get(i);
        return res;
    }

    @Override
    public DTGTransition[] getTransitionsTo(String toValue) {
        Integer index = valueIndex.get(toValue);
        if (index == null) return null;
        ArrayList<DTGTransition> res = new ArrayList<DTGTransition>();
        for (ArrayList<Transition> trans : transitions)
            for (Transition t : trans)
                if (t.toValue == index)
                    res.add(t);
        return res.toArray(new DTGTransition[res.size()]);
    }

    private GroundedEff changes(Action a, GroundedVar v) {
        for (GroundedEff eff : a.getEffs())
            if (eff.getVar().equals(v)) return eff;
        return null;
    }

    private GroundedCond requires(Action a, GroundedVar v) {
        for (GroundedCond pre : a.getPrecs())
            if (pre.getVar().equals(v)) return pre;
        return null;
    }

    public DTGTransition[] getNewTransitions() {
        ArrayList<DTGTransition> tList = new ArrayList<DTGTransition>();
        for (int i = 0; i < values.length; i++) {
            ArrayList<Transition> tv = transitions.get(i);
            for (Transition t : tv)
                if (t.newTransition) {
                    t.newTransition = false;
                    tList.add(t);
                }
        }
        return tList.toArray(new DTGTransition[tList.size()]);
    }

    public void addTransition(String startValue, String finalValue,
                              GroundedCond[] commonPrecs, GroundedEff[] commonEffs, String fromAgent) {
        boolean newFinalValue = valueIndex.containsKey(finalValue);
        int fvIndex = newFinalValue ? valueIndex.get(finalValue) : addNewValue(finalValue);
        if (!valueIndex.containsKey(startValue)) {    // New value for this variable
            int svIndex = addNewValue(startValue);
            transitions.get(svIndex).add(new Transition(svIndex, fvIndex, commonPrecs,
                    commonEffs, fromAgent));
        } else {                                    // Existing value
            int svIndex = valueIndex.get(startValue);
            ArrayList<Transition> tList = transitions.get(svIndex);
            Transition t = null;
            for (Transition aux : tList)
                if (aux.toValue == fvIndex) {
                    t = aux;
                    break;
                }
            if (t == null) {    // New transition
                tList.add(new Transition(svIndex, fvIndex, commonPrecs, commonEffs,
                        fromAgent));
            } else {            // Existing transition
                t.boundCommonPrecs(commonPrecs, fromAgent);
            }
        }
        if (newFinalValue) {    // Generate new possible transitions
            for (Action a : task.getActions()) {
                GroundedCond prec = requires(a, var);
                if (prec != null && prec.getValue().equals(finalValue)) {
                    GroundedEff eff = changes(a, var);
                    if (eff != null)
                        addTransition(a, eff, task.getAgentName().toLowerCase());
                }
            }
        }
    }

    private int addNewValue(String newValue) {
        String auxValues[] = new String[values.length + 1];
        for (int i = 0; i < values.length; i++) auxValues[i] = values[i];
        int reachedValueIndex = values.length;
        auxValues[reachedValueIndex] = newValue;
        values = auxValues;
        valueIndex.put(newValue, reachedValueIndex);
        transitions.add(new ArrayList<Transition>());
        return reachedValueIndex;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DTG (" + var.toString() + "):\n");
        for (ArrayList<Transition> tList : transitions)
            for (Transition t : tList)
                sb.append(t.toString() + "\n");
        return sb.toString();
    }

    private Path computePath(String initialValue, String endValue, HashMap<String, String> state,
                             HashMap<String, ArrayList<String>> newValues, int threadIndex) {
        Hashtable<TransitionMemo, Path> spTable = getShortestPathTable(threadIndex);
        TransitionMemo tm = new TransitionMemo(initialValue, endValue);
        Path p = spTable.get(tm);
        if (p == null) {
            p = new Path(initialValue, endValue, state, newValues, threadIndex);
            spTable.put(tm, p);
        }
        return p;
    }

    private Hashtable<TransitionMemo, Path> getShortestPathTable(int threadIndex) {
        if (threadIndex < shortestPaths.size()) return shortestPaths.get(threadIndex);
        Hashtable<TransitionMemo, Path> table = new Hashtable<DTGImp.TransitionMemo, Path>();
        shortestPaths.add(table);
        return table;
    }

    @Override
    public String[] getPath(String initValue, String endValue, HashMap<String, String> state,
                            HashMap<String, ArrayList<String>> newValues, int threadIndex) {
        Path p = computePath(initValue, endValue, state, newValues, threadIndex);
        return p.getPath();
    }

    @Override
    public boolean unknownValue(String value) {
        return value.equals("?") || !valueIndex.containsKey(value);
    }

    @Override
    public int pathCost(String initValue, String endValue, HashMap<String, String> state,
                        HashMap<String, ArrayList<String>> newValues, int threadIndex) {
        Path p = computePath(initValue, endValue, state, newValues, threadIndex);
        return p.getCost();
    }

    @Override
    public DTGTransition getTransition(String initValue, String endValue) {
        Integer v1 = valueIndex.get(initValue),
                v2 = valueIndex.get(endValue);
        if (v1 == null) v1 = valueIndex.get("?");
        if (v2 == null) v2 = valueIndex.get("?");
        return getTransition(v1, v2);
    }

    @Override
    public String getVarName() {
        return var.toString();
    }

    @Override
    public int getDistance(String initValue, String endValue, int threadIndex) {
        Hashtable<TransitionMemo, Integer> distTable = getDistanceTable(threadIndex);
        TransitionMemo tm = new TransitionMemo(initValue, endValue);
        Integer dst = distTable.get(tm);
        if (dst == null) {
            dst = computeDijkstraDistance(initValue, endValue);
            distTable.put(tm, dst);
        }
        return dst;
    }

    private Hashtable<TransitionMemo, Integer> getDistanceTable(int threadIndex) {
        if (threadIndex < distances.size()) return distances.get(threadIndex);
        Hashtable<TransitionMemo, Integer> dt = new Hashtable<TransitionMemo, Integer>();
        distances.add(dt);
        return dt;
    }

    public String[] computeDijkstraPath(String initValue, String endValue) {
        Integer init = valueIndex.get(initValue), dst = valueIndex.get(endValue);
        if (init == null || dst == null) return null;
        int minCost[] = new int[values.length], minPath[] = new int[values.length];
        boolean visited[] = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            minCost[i] = INFINITE;
            minPath[i] = -1;
        }
        minCost[init] = 0;
        PriorityQueue<DistanceToV> qPrior = new PriorityQueue<DistanceToV>();
        qPrior.add(new DistanceToV(init, 0));
        while (!qPrior.isEmpty()) {
            int v = qPrior.poll().index;
            if (!visited[v]) {
                visited[v] = true;
                for (Transition t : transitions.get(v)) {
                    if (minCost[t.toValue] > minCost[v] + t.getCost()) {
                        minCost[t.toValue] = minCost[v] + t.getCost();
                        minPath[t.toValue] = v;
                        qPrior.add(new DistanceToV(t.toValue, minCost[t.toValue]));
                    }
                }
            }
        }
        if (minCost[dst] == INFINITE) return null;
        ArrayList<String> res = new ArrayList<String>();
        res.add(values[dst]);
        for (int vAux = minPath[dst]; vAux != -1; vAux = minPath[vAux])
            res.add(0, values[vAux]);
        return res.toArray(new String[res.size()]);
    }

    private int computeDijkstraDistance(String initValue, String endValue) {
        Integer init = valueIndex.get(initValue), dst = valueIndex.get(endValue);
        if (init == null || dst == null) return INFINITE;
        int minCost[] = new int[values.length];
        boolean visited[] = new boolean[values.length];
        for (int i = 0; i < values.length; i++)
            minCost[i] = INFINITE;
        minCost[init] = 0;
        PriorityQueue<DistanceToV> qPrior = new PriorityQueue<DistanceToV>();
        qPrior.add(new DistanceToV(init, 0));
        while (!qPrior.isEmpty()) {
            int v = qPrior.poll().index;
            if (!visited[v]) {
                visited[v] = true;
                for (Transition t : transitions.get(v)) {
                    if (minCost[t.toValue] > minCost[v] + t.getCost()) {
                        minCost[t.toValue] = minCost[v] + t.getCost();
                        qPrior.add(new DistanceToV(t.toValue, minCost[t.toValue]));
                    }
                }
            }
        }
        return minCost[dst];
    }

    /*************************************************/
    /*            D i s t a n c e T o V              */
    @Override
    public void clearCache(int threadIndex) {
        getShortestPathTable(threadIndex).clear();
        getDistanceTable(threadIndex).clear();
    }

    /*************************************************/
	/*             T R A N S I T I O N               */

    /*************************************************/

    private static class DistanceToV implements Comparable<DistanceToV> {
        int index;
        int cost;

        public DistanceToV(int index, int cost) {
            this.index = index;
            this.cost = cost;
        }

        public int compareTo(DistanceToV dv) {
            if (cost < dv.cost) return -1;
            else if (cost > dv.cost) return 1;
            else return 0;
        }
    }

    /*************************************************/
	/*                   P A T H                     */

    private static class TransitionMemo {
        String fromValue;
        String toValue;

        public TransitionMemo(String initialValue, String endValue) {
            this.fromValue = initialValue;
            this.toValue = endValue;
        }

        public boolean equals(Object x) {
            TransitionMemo tm = (TransitionMemo) x;
            return tm.fromValue.equals(fromValue) && tm.toValue.equals(toValue);
        }

        public String toString() {
            return fromValue + " " + toValue;
        }

        public int hashCode() {
            return (fromValue + " " + toValue).hashCode();
        }
    }

    /*************************************************/

    public class Transition implements DTGTransition {
        int fromValue, toValue, cost;
        ArrayList<Action> actions;
        ArrayList<GroundedCond> commonPrecs;
        ArrayList<GroundedEff> commonEffs;
        boolean newTransition;
        ArrayList<String> agents;

        public Transition(int fromValue, int toValue, Action a, String agent) {
            agents = new ArrayList<String>();
            agents.add(agent);
            this.fromValue = fromValue;
            this.toValue = toValue;
            newTransition = true;
            actions = new ArrayList<Action>();
            actions.add(a);
            commonPrecs = new ArrayList<GroundedCond>();
            for (GroundedCond prec : a.getPrecs())
                commonPrecs.add(prec);
            commonEffs = new ArrayList<GroundedEff>();
            for (GroundedEff eff : a.getEffs())
                commonEffs.add(eff);
            cost = -1;
        }

        public Transition(int svIndex, int fvIndex, GroundedCond[] precs,
                          GroundedEff[] effs, String agent) {
            agents = new ArrayList<String>();
            agents.add(agent);
            this.fromValue = svIndex;
            this.toValue = fvIndex;
            newTransition = !values[svIndex].equals("?") && !values[fvIndex].equals("?");
            actions = new ArrayList<Action>();
            commonPrecs = new ArrayList<GroundedCond>();
            for (GroundedCond prec : precs)
                commonPrecs.add(prec);
            commonEffs = new ArrayList<GroundedEff>();
            for (GroundedEff eff : effs)
                commonEffs.add(eff);
        }

        public int getCost() {
            return 1;
        }

        public void boundCommonPrecs(GroundedCond[] precs, String fromAgent) {
            int i = 0;
            while (i < commonPrecs.size()) {
                boolean exists = false;
                GroundedCond cPrec = commonPrecs.get(i);
                for (GroundedCond p : precs)
                    if (p.getCondition() == cPrec.getCondition() &&
                            p.getVar().equals(cPrec.getVar()) &&
                            p.getValue().equals(cPrec.getValue())) {
                        exists = true;
                        break;
                    }
                if (exists) i++;
                else commonPrecs.remove(i);
            }
            if (!agents.contains(fromAgent))
                agents.add(fromAgent);
        }

        public void addAction(Action a, String agent) {
            boolean newAction = true;
            if (!agents.contains(agent))
                agents.add(agent);
            for (Action aux : actions)
                if (aux == a) {
                    newAction = false;
                    break;
                }
            if (newAction) {
                actions.add(a);
                int i = 0;
                while (i < commonPrecs.size()) {
                    if (requires(a, commonPrecs.get(i))) i++;
                    else commonPrecs.remove(i);
                }
                i = 0;
                while (i < commonEffs.size()) {
                    if (changes(a, commonEffs.get(i))) i++;
                    else commonEffs.remove(i);
                }
            }
        }

        private boolean changes(Action a, GroundedEff eff) {
            for (GroundedEff e : a.getEffs())
                if (e.getVar().equals(eff.getVar()) &&
                        e.getValue().equals(eff.getValue()))
                    return true;
            return false;
        }

        private boolean requires(Action a, GroundedCond prec) {
            for (GroundedCond p : a.getPrecs())
                if (p.getCondition() == prec.getCondition() &&
                        p.getVar().equals(prec.getVar()) &&
                        p.getValue().equals(prec.getValue()))
                    return true;
            return false;
        }

        public String toString() {
            String s1 = "";
            for (GroundedCond c : commonPrecs)
                if (s1.equals("")) s1 = c.toString();
                else s1 = s1 + "," + c.toString();
            String s2 = "";
            for (Action a : actions)
                if (s2.equals("")) s2 = a.toString();
                else s2 = s2 + "," + a.toString();
            String s3 = "";
            for (GroundedEff c : commonEffs)
                if (s3.equals("")) s3 = c.toString();
                else s3 = s3 + "," + c.toString();
            return "[" + agents + "] " + values[fromValue] + "->" + values[toValue] +
                    " [" + s1 + "]" + " {" + s2 + "}" + " [" + s3 + "]";
        }

        @Override
        public GroundedVar getVar() {
            return var;
        }

        @Override
        public String getStartValue() {
            return values[fromValue];
        }

        @Override
        public String getFinalValue() {
            return values[toValue];
        }

        @Override
        public ArrayList<GroundedCond> getCommonPreconditions() {
            return commonPrecs;
        }

        @Override
        public ArrayList<GroundedEff> getCommonEffects() {
            return commonEffs;
        }

        @Override
        public ArrayList<String> getAgents() {
            return agents;
        }
    }

    /*************************************************/

    public class Path {
        private String path[];
        private int cost;

        public Path(String initialValue, String endValue, HashMap<String, String> state,
                    HashMap<String, ArrayList<String>> newValues, int threadIndex) {
            path = null;
            cost = INFINITE;
            Integer index = valueIndex.get(initialValue);
            if (index == null) return;
            int init = index;
            index = valueIndex.get(endValue);
            if (index == null) return;
            int end = index;
            if (init == end) {
                path = new String[1];
                path[0] = initialValue;
                cost = 0;
            } else {
                path = computeDijkstraPath(initialValue, endValue);
                if (path == null) return;
                cost = evaluateCost(path, state, newValues, threadIndex);
                computeShortestPath(init, end, state, newValues, threadIndex);
            }
        }

        private int evaluateCost(String[] p, HashMap<String, String> state,
                                 HashMap<String, ArrayList<String>> newValues, int threadIndex) {
            Hashtable<String, String> s = new Hashtable<String, String>();
            if (state != null) s.putAll(state);
            int c = 0;
            for (int i = 1; i < p.length; i++) {
                DTGTransition t = getTransition(p[i - 1], p[i]);
                if (t == null) return INFINITE;
                c += computeCost(t, s, newValues, threadIndex) + 1;
                updateState(t, s, null);
            }
            return c;
        }

        private void updateState(DTGTransition t, Hashtable<String, String> state,
                                 ArrayList<String> valuesBackup) {
            for (GroundedEff eff : t.getCommonEffects()) {
                String value = state.put(eff.getVar().toString(), eff.getValue());
                if (valuesBackup != null) valuesBackup.add(value);
            }
        }

        private int computeCost(DTGTransition t, Hashtable<String, String> state,
                                HashMap<String, ArrayList<String>> newValues, int threadIndex) {
            int res = 0;
            for (GroundedCond c : t.getCommonPreconditions()) {
                int bestCost;
                String varName = c.getVar().toString();
                String stateValue = state.get(varName);
                DTG dtg = dtgSet.getDTG(varName);
                if (stateValue != null && !stateValue.equals(c.getValue()))
                    bestCost = dtg.getDistance(stateValue, c.getValue(), threadIndex);
                else bestCost = 0;
                if (newValues != null) {
                    ArrayList<String> valueList = newValues.get(varName);
                    if (valueList != null)
                        for (String value : valueList) {
                            int cost;
                            if (value.equals(c.getValue())) cost = 0;
                            else cost = dtg.getDistance(value, c.getValue(), threadIndex);
                            if (cost < bestCost) bestCost = cost;
                        }
                }
                res += bestCost;
            }
            return res;
        }

        private void computeShortestPath(int initValue, int endValue, HashMap<String, String> state,
                                         HashMap<String, ArrayList<String>> newValues, int threadIndex) {
            searchNodes = 0;
            Hashtable<String, String> s = new Hashtable<String, String>();
            boolean visited[] = new boolean[values.length];
            ArrayList<String> p = new ArrayList<String>();
            visited[initValue] = true;
            for (Transition t : transitions.get(initValue)) {
                s.clear();
                if (state != null) s.putAll(state);
                p.clear();
                p.add(values[initValue]);
                computeShortestPath(t, endValue, s, newValues, p, visited, 0, threadIndex);
            }
        }

        private void computeShortestPath(Transition t, int endValue, Hashtable<String, String> state,
                                         HashMap<String, ArrayList<String>> newValues, ArrayList<String> p, boolean[] visited,
                                         int currentCost, int threadIndex) {
            currentCost++;
            if (currentCost >= cost) return;
            currentCost += computeCost(t, state, newValues, threadIndex);    // Compute the cost of the transition preconditions
            if (currentCost >= cost) return;
            p.add(values[t.toValue]);
            if (t.toValue == endValue) {    // End value reached: check if the cost is better
                if (currentCost < cost) {
                    cost = currentCost;
                    path = p.toArray(new String[p.size()]);
                    p.remove(p.size() - 1);
                    //System.out.println("OK");
                }
                return;
            }
            ArrayList<String> valuesBackup = new ArrayList<String>();    // Apply transition effects
            updateState(t, state, valuesBackup);
            visited[t.toValue] = true;                                    // Continue the path building
            if (++searchNodes < MAX_SEARCH_NODES)
                for (Transition next : transitions.get(t.toValue)) {
                    if (!visited[next.toValue])
                        computeShortestPath(next, endValue, state, newValues, p, visited,
                                currentCost, threadIndex);
                }
            visited[t.toValue] = false;                                    // Restore previous values
            p.remove(p.size() - 1);
            int i = 0;
            for (GroundedEff eff : t.commonEffs) {
                String value = valuesBackup.get(i++);
                if (value != null) state.put(eff.getVar().toString(), value);
                else state.remove(eff.getVar().toString());
            }
        }

        public int getCost() {
            return cost;
        }

        public String[] getPath() {
            return path;
        }
    }

    /*************************************************/
	/*               D I J S K T R A                 */

    /*************************************************/

    private class Dijkstra {
        private static final int INFINITE = (Integer.MAX_VALUE) / 3;
        int minCost[];
        int minPath[];
        String agent[];
        private int initialValue;

        public Dijkstra(Integer vIndex) {
            minCost = new int[values.length];
            minPath = new int[values.length];
            agent = new String[values.length];
            boolean visited[] = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                minCost[i] = INFINITE;
                minPath[i] = -1;
            }
            if (vIndex == null)
                return;
            initialValue = vIndex.intValue();
            minCost[initialValue] = 0;
            PriorityQueue<DistanceToV> qPrior = new PriorityQueue<DistanceToV>();
            qPrior.add(new DistanceToV(initialValue, 0));
            while (!qPrior.isEmpty()) {
                int v = qPrior.poll().index;
                if (!visited[v]) {
                    visited[v] = true;
                    for (Transition t : transitions.get(v)) {
                        if (minCost[t.toValue] > minCost[v] + t.getCost()) {
                            minCost[t.toValue] = minCost[v] + t.getCost();
                            minPath[t.toValue] = v;
                            qPrior.add(new DistanceToV(t.toValue, minCost[t.toValue]));
                        }
                    }
                }
            }
        }

        public String[] getPath(String value) {
            Integer vIndex = valueIndex.get(value);
            if (vIndex == null) vIndex = valueIndex.get("?");
            int length = 0, index = vIndex;
            while (minPath[index] != -1) {
                length++;
                index = minPath[index];
            }
            String path[] = new String[length + 1];
            path[length--] = value;
            while (minPath[vIndex] != -1) {
                path[length--] = values[minPath[vIndex]];
                vIndex = minPath[vIndex];
            }
            return path;
        }

        public String previousValue(String value) {
            Integer vIndex = valueIndex.get(value);
            if (vIndex == null) return null;
            int prev = minPath[vIndex];
            if (prev < 0) return null;
            return values[prev];
        }

        private void addNewValue() {
            int newLength = minCost.length + 1;
            int[] auxMinCost = new int[newLength],
                    auxMinPath = new int[newLength];
            String[] auxAgent = new String[newLength];
            for (int i = 0; i < minCost.length; i++) {
                auxMinCost[i] = minCost[i];
                auxMinPath[i] = minPath[i];
                auxAgent[i] = agent[i];
            }
            minCost = auxMinCost;
            minPath = auxMinPath;
            agent = auxAgent;
            minCost[newLength - 1] = INFINITE;
            minPath[newLength - 1] = -1;
        }

        public void update(DTGRequest request) {
            int reachedValueIndex = valueIndex.get(request.reachedValue());
            if (minCost[reachedValueIndex] > request.reachedValueCost()) {    // Update
                minCost[reachedValueIndex] = request.reachedValueCost();
                minPath[reachedValueIndex] = -2;
                agent[reachedValueIndex] = request.fromAgent();
                boolean visited[] = new boolean[values.length];
                int unkIndex = valueIndex.containsKey("?") ? valueIndex.get("?") : -1;
                if (unkIndex >= 0) visited[unkIndex] = true;
                PriorityQueue<DistanceToV> qPrior = new PriorityQueue<DistanceToV>();
                qPrior.add(new DistanceToV(reachedValueIndex, request.reachedValueCost()));
                while (!qPrior.isEmpty()) {
                    int v = qPrior.poll().index;
                    if (!visited[v]) {
                        visited[v] = true;
                        for (Transition t : transitions.get(v)) {
                            if (minCost[t.toValue] > minCost[v] + t.getCost() || t.toValue == unkIndex) {
                                if (minCost[t.toValue] > minCost[v] + t.getCost()) {
                                    minCost[t.toValue] = minCost[v] + t.getCost();
                                    minPath[t.toValue] = v;
                                    agent[t.toValue] = null;
                                    qPrior.add(new DistanceToV(t.toValue, minCost[t.toValue]));
                                }
                            }
                        }
                    }
                }
            }
        }

        public int getPathCost(String v2) {
            Integer index = valueIndex.get(v2);
            if (index == null)
                return -1;
            return minCost[index.intValue()];
        }
    }
}
