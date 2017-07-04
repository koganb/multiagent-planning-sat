package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_planner.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Serializable class to send/receive plan proposals to/from other agents
 *
 * @author Oscar Sapena
 * @since June 2011
 */
public class ProposalToSend implements Serializable {
    private static final long serialVersionUID = 6509357846584167L;
    private ArrayList<PCausalLink> causalLinks;
    private ArrayList<POrdering> orderings;
    private PStep step;
    private boolean isSolution;
    private boolean repeatedState;
    private int h, hLand;
    private BitSet achievedLandmarks;

    /**
     * Builds a plan proposal to sent to the given agent
     *
     * @param plan     Plan to send
     * @param basePlan Base plan of the given plan
     * @param ag       Destination agent
     */
    public ProposalToSend(InternalProposal prop, String ag, boolean repeated) {
        repeatedState = repeated;
        causalLinks = new ArrayList<PCausalLink>();
        orderings = new ArrayList<POrdering>();
        POPIncrementalPlan plan = (POPIncrementalPlan) prop.plan;
        for (CausalLink c : plan.getCausalLinks()) {
            causalLinks.add(new PCausalLink(c));
            //causalLinks.add(new PCausalLink(c, !gCond.getVar().shareable(gCond.getValue(), ag)));
        }
        for (Ordering o : plan.getOrderings())
            orderings.add(new POrdering(o));
        step = new PStep(plan.getStep());
        isSolution = plan.isSolution();
        h = plan.getH();
        hLand = plan.getHLan();
        achievedLandmarks = prop.achievedLandmarks;
    }

    public BitSet getAchievedLandmarks() {
        return achievedLandmarks;
    }

    public boolean isRepeated() {
        return repeatedState;
    }

    public Step getStep(int stepIndex, PlannerFactoryImp configuration) {
        return step.toStep(configuration);
    }

    public ArrayList<Ordering> getOrderings(PlannerFactoryImp configuration) {
        ArrayList<Ordering> o = new ArrayList<Ordering>(orderings.size());
        for (POrdering po : orderings)
            o.add(po.toOrdering(configuration));
        return o;
    }

    public CausalLink[] getCausalLinks(PlannerFactoryImp configuration,
                                       POPIncrementalPlan basePlan, Step newStep, ArrayList<Ordering> orderings) {
        ArrayList<CausalLink> acl = new ArrayList<CausalLink>();
        CausalLink cl;
        for (PCausalLink pcl : causalLinks)
            if (pcl != null) {
                cl = pcl.toCausalLink(configuration, basePlan, newStep);
                if (cl != null) acl.add(cl);
                else {    // Add as ordering
                    orderings.add(configuration.createOrdering(pcl.step1, pcl.step2));
                }
            }
        return acl.toArray(new CausalLink[acl.size()]);
    }

    public boolean isSolution() {
        return isSolution;
    }

    public int getH() {
        return h;
    }

    public int getHLand() {
        return hLand;
    }

    public static class PGroundedCond implements Serializable {
        private static final long serialVersionUID = -425234291857601218L;
        private int var;
        private int value;
        private int condition;

        public PGroundedCond(Condition cond) {
            var = cond.getVarCode();
            value = cond.getValueCode();
            condition = cond.getType();
        }

        public Condition toCondition() {
            return new POPCondition(condition, var, value);
        }

        public String toString() {
            String res = condition == GroundedCond.EQUAL ? "=" : "<>";
            return var + res + value;
        }

		/*
        public boolean correspondsTo(GroundedCond cond) {
			boolean res = condition == cond.getCondition() &&
				function.equals(cond.getVar().getFuctionName()) &&
				value.equals(cond.getValue());
			if (res) {
				String condParams[] = cond.getVar().getParams();
				res = fncParams.length == condParams.length;
				for (int i = 0; i < condParams.length && res; i++)
					res = fncParams[i].equals(condParams[i]);
			}
			return res;
		}

		public GroundedEff toGroundedEff(GroundedTask gTask) {
			GroundedVar var = null;
			for (GroundedVar v: gTask.getVars())
			if (v.getFuctionName().equals(function) &&
				java.util.Arrays.equals(v.getParams(), fncParams)) {
				var = v;
				break;
			}
			GroundedEff gEff = gTask.createGroundedEffect(var, value);
			return gEff;
		}*/
    }

    public static class POpenCondition implements Serializable {
        private static final long serialVersionUID = 7396924233555014626L;
        private int stepIndex;
        private PGroundedCond cond;

        public POpenCondition(OpenCondition oc) {
            stepIndex = oc.getStep().getIndex();
            cond = new PGroundedCond(oc.getCondition());
        }

        public String toString() {
            return "[" + stepIndex + "] " + cond;
        }
    }

    public static class PCausalLink implements Serializable {
        private static final long serialVersionUID = -7424617979375788553L;
        private int step1, step2;
        private PGroundedCond cond;

        public PCausalLink(CausalLink c) {
            step1 = c.getIndex1();
            step2 = c.getIndex2();
            cond = new PGroundedCond(c.getCondition());
        }

        private static Step findStep(int s, POPIncrementalPlan basePlan) {
            if (basePlan.getStep().getIndex() == s) return basePlan.getStep();
            return findStep(s, basePlan.getFather());
        }

        public Ordering toOrdering(PlannerFactory pf) {
            return pf.createOrdering(step1, step2);
        }

        public CausalLink toCausalLink(PlannerFactoryImp pf, POPIncrementalPlan basePlan,
                                       Step newStep) {
            Condition gc = cond.toCondition();
            Step s1 = step1 == 0 ? basePlan.getInitialStep() : findStep(step1, basePlan);
            Step s2 = newStep != null ? newStep : basePlan.getFinalStep();
            return pf.createCausalLink(gc, s1, s2);
        }
    }

    public static class POrdering implements Serializable {
        private static final long serialVersionUID = 4213546490197217271L;
        private int step1, step2;

        public POrdering(CausalLink c) {
            step1 = c.getIndex1();
            step2 = c.getIndex2();
        }

        public POrdering(Ordering o) {
            step1 = o.getIndex1();
            step2 = o.getIndex2();
        }

        public Ordering toOrdering(PlannerFactory pf) {
            return pf.createOrdering(step1, step2);
        }

        public boolean equals(Object x) {
            POrdering po = (POrdering) x;
            return step1 == po.step1 && step2 == po.step2;
        }
    }

    public static class PStep implements Serializable {
        private static final long serialVersionUID = 2695531841107912873L;
        PGroundedCond prec[], eff[];
        private int index;
        private String agent;
        private String actionName;

        public PStep(Step step) {
            if (step != null) {
                index = step.getIndex();
                agent = step.getAgent();
                actionName = step.getActionName();
                ArrayList<PGroundedCond> aPrecs = new ArrayList<PGroundedCond>();
                for (Condition cond : step.getPrecs())
                    aPrecs.add(new PGroundedCond(cond));
                ArrayList<PGroundedCond> aEffs = new ArrayList<PGroundedCond>();
                for (Condition eff : step.getEffs())
                    aEffs.add(new PGroundedCond(eff));
                prec = aPrecs.toArray(new PGroundedCond[aPrecs.size()]);
                eff = aEffs.toArray(new PGroundedCond[aEffs.size()]);
            } else
                index = -1;
        }

        public Step toStep(PlannerFactoryImp pf) {
            if (index == -1) return null;
            ArrayList<Condition> sPrec = new ArrayList<Condition>(prec.length);
            ArrayList<Condition> sEff = new ArrayList<Condition>(eff.length);
            for (int i = 0; i < prec.length; i++) {
                sPrec.add(prec[i].toCondition());
            }
            for (int i = 0; i < eff.length; i++) {
                sEff.add(eff[i].toCondition());
            }
            return pf.createStep(index, agent, actionName,
                    sPrec.toArray(new Condition[sPrec.size()]),
                    sEff.toArray(new Condition[sEff.size()]));
        }
    }
}
