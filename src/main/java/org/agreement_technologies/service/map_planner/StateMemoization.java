package org.agreement_technologies.service.map_planner;

import java.util.HashMap;

public class StateMemoization {
    private static final int MAP_SIZE = 65537;
    private HashEntry<Integer, POPIncrementalPlan> entrySet[];
    private int size;
    private int numVars;

    @SuppressWarnings("unchecked")
    public StateMemoization(int numVars) {
        entrySet = new HashEntry[MAP_SIZE];
        size = 0;
        this.numVars = numVars;
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

    // State comparison
    private boolean equalPlans(POPIncrementalPlan p1, POPIncrementalPlan p2) {
        int[] order1 = p1.linearization(), order2 = p2.linearization();
        int[] s1 = p1.computeCodeState(order1, numVars),
                s2 = p2.computeCodeState(order2, numVars);
        for (int v = 0; v < numVars; v++) {
            if (s2[v] != s1[v]) return false;
        }
        return true;
    }

    private boolean equalPlans(int s1[], POPIncrementalPlan p2) {
        int[] order2 = p2.linearization();
        int[] s2 = p2.computeCodeState(order2, numVars);
        for (int v = 0; v < numVars; v++) {
            if (s1[v] != s2[v]) return false;
        }
        return true;
    }

    private int position(int code) {
        int index = code % entrySet.length;
        if (index < 0) index += entrySet.length;
        return index;
    }

    // State comparison
    private int getPlanCode(POPIncrementalPlan p) {
        if (p.isSolution()) return 0;
        int[] order = p.linearization();
        int[] state = p.computeCodeState(order, numVars);
        StringBuilder s = new StringBuilder(numVars << 2);
        for (int v = 0; v < numVars; v++)
            s.append(state[v]);
        return s.toString().hashCode();
    }

    //Check if the addition of an action a to a base plan p leads to a repeated plan
    public IPlan search(POPIncrementalPlan p, POPAction a) {
        int code;
        int[] order = p.linearization();
        int[] s = p.computeCodeState(order, numVars);
        if (p.isSolution()) code = 0;
        else {
            //Update state with the new action a
            for (POPPrecEff eff : a.getEffects())
                s[eff.getVarCode()] = eff.getValueCode();
            //Obtain plan code from the frontier state calculated
            code = getPlanCode(s);
        }
        int pos = position(code);
        HashEntry<Integer, POPIncrementalPlan> e = entrySet[pos];
        while (e != null) {
            if (e.key == code) {
                if (equalPlans(s, e.value))
                    return e.value;
            }
            e = e.next;
        }
        return null;    // Not found
    }

    //Compute the plan code considering the addition of an action act
    private int getPlanCode(int[] state) {
        StringBuilder s = new StringBuilder(numVars << 2);
        for (int v = 0; v < numVars; v++)
            s.append(state[v]);
        return s.toString().hashCode();
    }

    public void histogram() {
        int max = 0;
        HashMap<Integer, Integer> h = new HashMap<Integer, Integer>();
        for (int i = 0; i < entrySet.length; i++) {
            int length = listLength(entrySet[i]);
            int value = h.containsKey(length) ? h.get(length) : 0;
            value++;
            h.put(length, value);
            if (length > max) max = length;
        }
        System.out.println("HISTOGRAM:");
        for (int i = 0; i <= max; i++)
            if (h.containsKey(i))
                System.out.println(i + "\t" + h.get(i));
    }

    private int listLength(HashEntry<Integer, POPIncrementalPlan> hashEntry) {
        int n = 0;
        while (hashEntry != null) {
            n++;
            hashEntry = hashEntry.next;
        }
        return n;
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
