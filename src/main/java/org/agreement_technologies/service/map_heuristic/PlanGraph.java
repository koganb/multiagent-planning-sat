package org.agreement_technologies.service.map_heuristic;

public class PlanGraph {
    /*
	private ArrayList<Step> steps;
	private ArrayList<Adjacent> adjacents[];
	
	@SuppressWarnings("unchecked")
	public PlanGraph(Plan p) {
		steps = p.getStepsArray();
		adjacents = new ArrayList[steps.size()];
		for (int i = 0; i < steps.size(); i++)
			adjacents[i] = new ArrayList<Adjacent>();
		for (CausalLink cl: p.getCausalLinksArray())
			addAdjacent(cl.getIndex1(), cl.getIndex2(), cl.getGroundedCondition());
		for (Ordering o: p.getOrderingsArray())
			addAdjacent(o.getStep1(), o.getStep2(), null);
	}
	
	private void addAdjacent(int step1, int step2, GroundedCond cond) {
		ArrayList<Adjacent> adj = adjacents[step1]; 
		for (Adjacent a: adj) {
			if (a.step.getIndex() == step2) {
				if (cond != null) a.addCondition(cond);
				return;
			}
		}
		Adjacent a = new Adjacent(steps.get(step2));
		if (cond != null) a.addCondition(cond);
		adj.add(a);
	}

	private boolean effectOfStepDeleted(GoalCondition oc, Step step) {
		for (GroundedEff e: step.getGroundedAction().getEffs())
			if (e.getVar().toString().equals(oc.varName)) return true;
		for (Adjacent a: adjacents[step.getIndex()])
			if (effectOfStepDeleted(oc, a.step)) return true;
		return false;
	}

	private class Adjacent {
		private Step step;
		private ArrayList<GroundedCond> condition;
		
		private Adjacent(Step step) {
			this.step = step;
			condition = new ArrayList<GroundedCond>();
		}

		public void addCondition(GroundedCond cond) {
			condition.add(cond);
		}
	}
	
	public boolean effectOfStepDeleted(GoalCondition oc, int step) {
		for (Adjacent a: adjacents[step])
			if (effectOfStepDeleted(oc, a.step)) return true;
		return false;
	}

	public String effectValue(int step, String varName) {
		for (GroundedEff e: steps.get(step).getGroundedAction().getEffs())
			if (e.getVar().toString().equals(varName)) return e.getValue();
		return null;
	}*/
}
