package org.agreement_technologies.service.map_planner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class HeuristicAdjustment implements Serializable {
    private static final long serialVersionUID = 8516447848966468906L;
    private HashMap<String, ArrayList<Integer>> newLandmarks;
    private ArrayList<ProposalToSend> proposals;

    public HeuristicAdjustment(int size) {
        newLandmarks = new HashMap<String, ArrayList<Integer>>(size);
        proposals = null;
    }

    public void add(String name, ArrayList<Integer> newLandmarks) {
        this.newLandmarks.put(name, newLandmarks);
    }

    public void addOwnProposals(ArrayList<ProposalToSend> proposals) {
        this.proposals = proposals;
    }

    public ArrayList<ProposalToSend> getProposals() {
        return proposals;
    }

    public void merge(String name, ArrayList<Integer> newLand) {
        if (newLand == null || newLand.isEmpty()) return;
        ArrayList<Integer> storedLand = newLandmarks.get(name);
        if (storedLand == null)
            add(name, newLand);
        else {
            for (Integer l : newLand)
                if (!storedLand.contains(l))
                    storedLand.add(l);
        }
    }

    public int getNumNewLandmarks(String name) {
        ArrayList<Integer> storedLand = newLandmarks.get(name);
        if (storedLand == null)
            return 0;
        else
            return storedLand.size();
    }

    public String newLandmarksList(String name) {
        ArrayList<Integer> storedLand = newLandmarks.get(name);
        return storedLand == null ? "[]" : storedLand.toString();
    }

    public ArrayList<Integer> getNewLandmarks(String name) {
        return newLandmarks.get(name);
    }

    public ArrayList<String> proposalsWithAdjustments() {
        ArrayList<String> plans = new ArrayList<String>();
        for (String name : newLandmarks.keySet()) {
            if (!newLandmarks.get(name).isEmpty())
                plans.add(name);
        }
        return plans;
    }
}
