package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.OpenCondition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

//Implementación del gestor de precondiciones mediante una cola de prioridad
public class POPOpenConditionManagerQueue implements POPOpenConditionManager {
    //Parámetros: cola de prioridad
    PriorityQueue<OpenCondition> queue;
    ArrayList<OpenCondition> initialOpenConditions;

    POPOpenConditionManagerQueue(Comparator<OpenCondition> comp) {
        this.initialOpenConditions = new ArrayList<OpenCondition>();
        this.queue = new PriorityQueue<OpenCondition>(30, comp);
    }

    public POPOpenCondition getNextOpenCondition() {
        return (POPOpenCondition) queue.poll();
    }

    public POPOpenCondition checkNextOpenCondition() {
        return (POPOpenCondition) queue.peek();
    }

    public void addOpenConditions(ArrayList<OpenCondition> precs) {
        for (OpenCondition p : precs)
            queue.add(p);
    }

    /**
     * Clears the queue and restores the initial open conditions
     */
    public void restoreOpenConditions() {
        queue.clear();
        for (OpenCondition o : this.initialOpenConditions)
            queue.add(o);
    }

    public void clearOpenConditions() {
        queue.clear();
    }

    public Iterator<OpenCondition> getIterator() {
        return queue.iterator();
    }

    public int size() {
        return queue.size();
    }

    public ArrayList<OpenCondition> getList() {
        ArrayList<OpenCondition> precs = new ArrayList<OpenCondition>(queue.size());
        Iterator<OpenCondition> it = this.queue.iterator();
        while (it.hasNext()) precs.add((POPOpenCondition) it.next());
        return precs;
    }

    public void addInitialOpenConditions(ArrayList<OpenCondition> precs) {
        for (OpenCondition p : precs)
            queue.add(p);
        this.initialOpenConditions = precs;
    }

    public void update() {
        ArrayList<POPOpenCondition> precs = new ArrayList<POPOpenCondition>();
        while (!this.queue.isEmpty()) precs.add((POPOpenCondition) this.queue.poll());
        this.queue.clear();
        for (POPOpenCondition p : precs) this.queue.add(p);
    }

    public OpenCondition[] getOpenConditions() {
        return (OpenCondition[]) this.queue.toArray();
    }

}
