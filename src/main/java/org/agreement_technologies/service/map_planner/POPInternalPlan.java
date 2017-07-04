package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.OpenCondition;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;
import java.util.Iterator;

//Plan incremental; almacena el refinamiento realizado sobre un plan base padre
public class POPInternalPlan implements Cloneable {
    private ExtendedPlanner POP;
    private POPInternalPlan father;
    private POPStep step;
    private POPCausalLink causalLink;
    private ArrayList<OpenCondition> openConditions;
    private ArrayList<POPThreat> threats;
    private POPOrdering ordering;
    private int steps;

    //Constructor genérico, válido para resolución de precondiciones con paso nuevo y paso existente, y eliminación de amenazas
    POPInternalPlan(POPInternalPlan basePlan, POPStep newStep, POPCausalLink newLink, POPOrdering newOrdering, ArrayList<OpenCondition> precs, ArrayList<POPThreat> thre, OpenCondition solvedPrec, boolean supportFinalStep, ExtendedPlanner planner) {
        POP = planner;

        if (POP.getAntecessors() != null) {
            steps = POP.getAntecessors().length + 2;
            if (supportFinalStep)
                steps--;
        } else steps = -1;

        this.father = basePlan;
        this.step = newStep;
        this.causalLink = newLink;
        if (causalLink == null)
            this.ordering = newOrdering;
        this.threats = new ArrayList<POPThreat>();

        if (precs != null)
            this.openConditions = new ArrayList<OpenCondition>(precs.size());

        //Añadimos todas las precondiciones excepto la resuelta (si la hay)
        Iterator<OpenCondition> it = null;
        if (basePlan != null)
            if (basePlan.getPreconditions() != null)
                it = basePlan.getPreconditions().iterator();
        if (it == null) {
            if (precs != null)
                for (OpenCondition o : precs)
                    this.openConditions.add(o);
        } else {
            POPOpenCondition prec;
            while (it.hasNext()) {
                prec = (POPOpenCondition) it.next();
                //Si la precondición actual no coincide con la resuelta, la guardamos en el plan hijo
                //Si la precondición actual coincide con la resuelta pero los pasos son distintos, la guardamos en el plan hijo
                if ((solvedPrec != null && prec.getCondition() != ((POPOpenCondition) solvedPrec).getCondition()) ||
                        (solvedPrec != null && prec.getCondition() == ((POPOpenCondition) solvedPrec).getCondition() && prec.getStep().getIndex() != solvedPrec.getStep().getIndex())
                        || solvedPrec == null)
                    this.openConditions.add(prec);
            }
        }

        //Añadimos las precondiciones correspondientes al nuevo paso (si lo hay)
        if (newStep != null) {
            for (POPPrecEff pe : newStep.getAction().getPrecs()) {
                if (!pe.getCondition().toString().equals(solvedPrec.getCondition().toString()))
                    //Marcamos las nuevas precondiciones como metas (en el caso monoagente no afectará, en MAP es necesario)
                    this.openConditions.add(new POPOpenCondition(pe, newStep, true));
            }
        }

        //Añadimos todas las amenazas que haya; si hemos resuelto una amenaza, no la añadimos
        if (thre != null) {
            for (int i = 0; i < thre.size(); i++) this.threats.add(thre.get(i));
        }
    }

    POPInternalPlan(POPInternalPlan original, ExtendedPlanner thread) {
        POP = thread;
        father = original.father;
        step = original.step;
        causalLink = original.causalLink;
        openConditions = original.openConditions;
        threats = original.threats;
        ordering = original.ordering;
        steps = original.steps;
    }

    public Object clone() {
        POPInternalPlan c = new POPInternalPlan(null, null, null, null, null, null, null, false, this.POP);

        ArrayList<OpenCondition> precs = new ArrayList<OpenCondition>(this.getPreconditions().size());
        for (OpenCondition p : this.getPreconditions()) precs.add((POPOpenCondition) p);
        c.setPreconditions(precs);

        return c;
    }

    public ExtendedPlanner getPlanner() {
        return POP;
    }

    public ArrayList<OpenCondition> getPreconditions() {
        return this.openConditions;
    }

    public void setPreconditions(ArrayList<OpenCondition> p) {
        this.openConditions = p;
    }

    public ArrayList<POPThreat> getThreats() {
        return this.threats;
    }

    public POPInternalPlan getFather() {
        return this.father;
    }

    public POPOrdering getOrdering() {
        return this.ordering;
    }

    public POPStep getStep() {
        return this.step;
    }

    public POPCausalLink getCausalLink() {
        return this.causalLink;
    }

    public void setOrdering(int v1, int v2) {
        this.ordering = new POPOrdering(v1, v2);
    }

    public void setNumSteps(int n) {
        steps = n;
    }

    public Step getFinalStep() {
        return POP.getFinalStep();
    }

    public void addThreats(ArrayList<POPThreat> v) {
        for (POPThreat t : v)
            this.threats.add(t);
    }

    public void restorePlan(ArrayList<OpenCondition> oc) {
        threats = new ArrayList<POPThreat>();
        openConditions = new ArrayList<OpenCondition>();
        for (OpenCondition o : oc)
            openConditions.add(o);
    }

    public void cleanPlan() {
        this.openConditions = null;
        this.threats = null;
    }

    public String toString() {
        String res = "";
        res += "Precs: " + this.openConditions.size() + "\n";
        res += "Threats: " + this.threats.size();

        return res;
    }

    public Step getStep(int index) {
        if (index == 0) return POP.getInitialStep();
        if (index == 1) return POP.getFinalStep();

        if (this.isRoot() || index <= numSteps() - 2)
            return POP.getAntecessors()[index - 1].getStep();

        POPInternalPlan aux = this;
        while (!aux.isRoot()) {
            if (aux.step != null)
                //if(aux.step.getIndex() == index)
                return aux.step;
            aux = aux.father;
        }

        if (POP.getAntecessors()[POP.getAntecessors().length - 1].getStep().getIndex() == index)
            return POP.getAntecessors()[POP.getAntecessors().length - 1].getStep();

        return null;
    }

    public ArrayList<OpenCondition> getTotalOpenConditions() {
        return this.openConditions;
    }

    public void getInternalCausalLinks(CausalLink[] cl) {
        POPInternalPlan p = this;
        int i = cl.length - 1;

        if (this.causalLink != null) {
            cl[i] = causalLink;
            i--;
        }
        while (p.father != null) {
            p = p.father;
            if (p.causalLink != null) {
                cl[i] = p.causalLink;
                i--;
            }
        }
    }

    public void addCausalLinks() {
        if (!POP.getModifiedCausalLinks()) {
            POPInternalPlan p = this;

            if (this.causalLink != null)
                POP.addCausalLink(causalLink);
            while (p.father != null) {
                p = p.father;
                if (p.causalLink != null)
                    POP.addCausalLink(p.causalLink);
            }
            POP.setModifiedCausalLinks(true);
        }
    }

    public ArrayList<Ordering> getInternalOrderings() {
        ArrayList<Ordering> or = new ArrayList<Ordering>(3);
        POPInternalPlan p = this;

        if (this.ordering != null)
            or.add(ordering);
        while (p.father != null) {
            p = p.father;
            if (p.ordering != null)
                or.add(p.ordering);
        }
        return or;
    }

    public void addOrderings() {
        if (!POP.getModifiedOrderings()) {
            POPInternalPlan p = this;

            if (this.ordering != null)
                POP.addOrdering(ordering);
            while (p.father != null) {
                p = p.father;
                if (p.ordering != null)
                    POP.addOrdering(p.ordering);
            }
            POP.setModifiedOrderings(true);
        }
    }

    public CustomArrayList<CausalLink> getTotalCausalLinks() {
        return POP.getTotalCausalLinks();
    }

    public CustomArrayList<Ordering> getTotalOrderings() {
        return POP.getTotalOrderings();
    }

    public int numSteps() {
        if (steps == -1)
            steps = POP.getAntecessors().length + 1;

        return steps;
    }

    public boolean isRoot() {
        return father == null;
    }

    public boolean isSolution() {
        if (step != null && step.getIndex() == 1)
            return true;
        if (isRoot())
            return false;
        return father.isSolution();
    }

    Step getLatestStep() {
        POPInternalPlan aux = this;
        while (aux != null && aux.step == null)
            aux = aux.father;

        //Si aux es null y el plan es root, devolver el último paso de POPIncrementalPlan
        if (aux == null && this.isRoot())
            return POP.getAntecessors()[POP.getAntecessors().length - 1].getStep();
        if (aux == null)
            return this.step;

        return aux.step;
    }
}