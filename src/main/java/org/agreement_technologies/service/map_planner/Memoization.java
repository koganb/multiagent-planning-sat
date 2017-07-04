package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.common.map_planner.Step;

import java.util.ArrayList;

public class Memoization {
    private static final int MAP_SIZE = 65537;
    private HashEntry<Integer, POPIncrementalPlan> entrySet[];
    private int size;

    @SuppressWarnings("unchecked")
    public Memoization() {
        entrySet = new HashEntry[MAP_SIZE];
        size = 0;
    }

    // Plan comparison
    @SuppressWarnings("unchecked")
    private static boolean equalPlans(POPIncrementalPlan p1, POPIncrementalPlan p2) {
        int numSteps = p1.numSteps();
        if (numSteps != p2.numSteps()) return false;
        ArrayList<Integer> nextSteps1[] = new ArrayList[numSteps];
        ArrayList<Integer> nextSteps2[] = new ArrayList[numSteps];
        for (int i = 0; i < numSteps; i++) {
            nextSteps1[i] = new ArrayList<Integer>();
            nextSteps2[i] = new ArrayList<Integer>();
        }
        String stepNames1[] = new String[numSteps];
        String stepNames2[] = new String[numSteps];
        int n = numSteps;
        while (p2.getFather() != null) {
            stepNames2[--n] = p2.getStep().toString();
            for (CausalLink c : p2.getCausalLinks())
                if (!nextSteps2[c.getIndex1()].contains(c.getIndex2()))
                    nextSteps2[c.getIndex1()].add(c.getIndex2());
            for (Ordering o : p2.getOrderings())
                if (!nextSteps2[o.getIndex1()].contains(o.getIndex2()))
                    nextSteps2[o.getIndex1()].add(o.getIndex2());
            p2 = p2.getFather();
        }
        n = numSteps;
        while (p1.getFather() != null) {
            stepNames1[--n] = p1.getStep().toString();
            for (CausalLink c : p1.getCausalLinks())
                if (!nextSteps1[c.getIndex1()].contains(c.getIndex2()))
                    nextSteps1[c.getIndex1()].add(c.getIndex2());
            for (Ordering o : p1.getOrderings())
                if (!nextSteps1[o.getIndex1()].contains(o.getIndex2()))
                    nextSteps1[o.getIndex1()].add(o.getIndex2());
            p1 = p1.getFather();
        }
        boolean checked[] = new boolean[numSteps];
        return checkStep(0, 0, stepNames1, stepNames2, nextSteps1, nextSteps2, checked);
    }

    private static boolean checkStep(int s1, int s2, String[] stepNames1, String[] stepNames2,
                                     ArrayList<Integer>[] nextSteps1, ArrayList<Integer>[] nextSteps2, boolean checked[]) {
        if (checked[s1]) return true;
        checked[s1] = true;
        for (int next1 : nextSteps1[s1]) {
            int next2 = -1;
            for (int aux : nextSteps2[s2])
                if (stepNames1[next1].equals(stepNames2[aux])) {
                    next2 = aux;
                    break;
                }
            if (next2 == -1 || !checkStep(next1, next2, stepNames1, stepNames2, nextSteps1,
                    nextSteps2, checked))
                return false;
        }
        return true;
    }

    public void add(POPIncrementalPlan p) {
        int code = getPlanCode(p);
        int pos = position(code);
        entrySet[pos] = new HashEntry<Integer, POPIncrementalPlan>(code, p, entrySet[pos]);
        size++;
    }

    public IPlan search(POPIncrementalPlan p) {
        int code = getPlanCode(p);
        int pos = position(code);
        HashEntry<Integer, POPIncrementalPlan> e = entrySet[pos];
        while (e != null) {
            if (e.key == code) {
                if (equalPlans(p, e.value))
                    return e.value;
            }
            e = e.next;
        }
        return null;    // Not found
    }

    private int position(int code) {
        int index = code % entrySet.length;
        if (index < 0) index += entrySet.length;
        return index;
    }

    // Plan comparison
    private int getPlanCode(POPIncrementalPlan p) {
        int code = 0;
        Step s;
        while (p.getFather() != null) {
            s = p.getStep();
            code += s.getActionName().hashCode();
            if (s.getAgent() != null) code += s.getAgent().hashCode();
            p = p.getFather();
        }
        return code;
    }

    private static class HashEntry<K, V> {
        K key;
        V value;
        HashEntry<K, V> next;

        public HashEntry(K key, V value, HashEntry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
}
