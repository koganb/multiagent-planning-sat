package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_grounding.*;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_negotiation.NegotiationFactory;
import org.agreement_technologies.common.map_planner.*;
import org.agreement_technologies.common.map_planner.Planner;
import org.agreement_technologies.service.map_negotiation.NegotiationFactoryImp;
import org.agreement_technologies.service.tools.CustomArrayList;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Configuration class; from the grounded task, it builds the objects necessary
 * for the planner to work.
 *
 * @author Alex
 */
public class PlannerFactoryImp implements PlannerFactory {
    public static final int EQUAL = 1;
    public static final int DISTINCT = 2;

    private ArrayList<OpenCondition> openConditions;
    private ArrayList<POPPrecEff> goals;

    private String myAgent;
    private ExtendedPlanner planner;

    private GroundedTask task;
    private ArrayList<POPPrecEff> initialState;
    private ArrayList<POPFunction> functions;
    private ArrayList<POPAction> actions;

    private Hashtable<String, POPFunction> hashVars;
    private Hashtable<String, POPPrecEff> hashPrecEffs;
    private Hashtable<String, ArrayList<InternalAction>> hashRequirers;

    private GroundedEff[] groundedInitialState;
    private int totalThreads;

    private NegotiationFactory negotiationFactory;
    private SolutionChecker solutionChecker;

    private Hashtable<String, Integer> hashGlobalIndexesVarCode;
    private Hashtable<String, Integer> hashGlobalIndexesValueCode;

    private Hashtable<Integer, String> hashGlobalIndexesCodeVar;
    private Hashtable<Integer, String> hashGlobalIndexesCodeValue;

    private int numGlobalVariables;

    public PlannerFactoryImp(GroundedTask task, AgentCommunication comm) {
        Hashtable<String, InternalCondition> hashPrecEffs = new Hashtable<String, InternalCondition>();
        ArrayList<InternalCondition> goals = new ArrayList<InternalCondition>();
        ArrayList<InternalCondition> initialState = new ArrayList<InternalCondition>();
        ArrayList<InternalOpenCondition> openConditions = new ArrayList<InternalOpenCondition>();
        ArrayList<InternalAction> actions = new ArrayList<InternalAction>();
        initializeParameters(task, hashPrecEffs, goals, initialState,
                openConditions, actions);
        assignGlobalIndexes(comm);
        translateDataToAttributes(hashPrecEffs, goals, initialState,
                openConditions, actions);
    }

    public int getTotalThreads() {
        return totalThreads;
    }

    public int getNumGlobalVariables() {
        return numGlobalVariables;
    }

    private void translateDataToAttributes(
            Hashtable<String, InternalCondition> hashPrecEffs,
            ArrayList<InternalCondition> goals,
            ArrayList<InternalCondition> initialState,
            ArrayList<InternalOpenCondition> openConditions,
            ArrayList<InternalAction> actions) {
        this.hashPrecEffs = new Hashtable<String, POPPrecEff>(
                hashPrecEffs.size());
        Set<String> keys = hashPrecEffs.keySet();
        for (String key : keys) {
            InternalCondition condition = hashPrecEffs.get(key);
            POPPrecEff eff = condition.toPOPPrecEff(this);
            this.hashPrecEffs.put(eff.toKey(), eff);
        }
        this.goals = new ArrayList<POPPrecEff>(goals.size());
        for (InternalCondition c : goals)
            this.goals.add(c.toPOPPrecEff(this));
        this.initialState = new ArrayList<POPPrecEff>(initialState.size());
        for (InternalCondition c : initialState)
            this.initialState.add(c.toPOPPrecEff(this));
        this.openConditions = new ArrayList<OpenCondition>(
                openConditions.size());
        for (InternalOpenCondition c : openConditions)
            this.openConditions.add(c.toPOPPrecEff(this));
        this.actions = new ArrayList<POPAction>(actions.size());
        for (InternalAction a : actions)
            this.actions.add(a.toPOPAction(this));
    }

    public NegotiationFactory getNegotiationFactory() {
        return negotiationFactory;
    }

    public SolutionChecker getSolutionChecker() {
        return solutionChecker;
    }

    public ArrayList<POPAction> getActions() {
        return this.actions;
    }

    public String getAgent() {
        return myAgent;
    }

    public GroundedTask getGroundedTask() {
        return this.task;
    }

    private void initializeParameters(GroundedTask gTask,
                                      Hashtable<String, InternalCondition> hashPrecEffs,
                                      ArrayList<InternalCondition> goals,
                                      ArrayList<InternalCondition> initialState,
                                      ArrayList<InternalOpenCondition> openConditions,
                                      ArrayList<InternalAction> actions) {
        int i, j;
        POPFunction func;
        InternalCondition pe;
        GroundedCond prec;
        GroundedEff eff;
        InternalAction act;
        // ArrayList<String> agents;
        ArrayList<InternalCondition> precs;
        ArrayList<InternalCondition> effs;
        String key;
        ArrayList<GroundedCond> groundedGoals;
        ArrayList<InternalAction> requirers;
        CustomArrayList<InternalCondition> effects;

        // Guardamos la tarea de planning como miembro de la clase
        this.task = gTask;
        this.myAgent = this.task.getAgentName();
        this.hashVars = new Hashtable<String, POPFunction>(
                gTask.getVars().length);
        this.functions = new ArrayList<POPFunction>();
        effects = new CustomArrayList<InternalCondition>();

        groundedGoals = new ArrayList<GroundedCond>();

        // Procesamos las variables y creamos las POPFunctions
        for (i = 0; i < gTask.getVars().length; i++) {
            func = new POPFunction(gTask.getVars()[i]);
            this.functions.add(func);
            this.hashVars.put(func.toKey(), func);
        }
        // Procesamos las acciones y construimos los POPPrecEff, y los POPAction
        for (i = 0; i < gTask.getActions().size(); i++) {
            precs = new ArrayList<InternalCondition>();
            effs = new ArrayList<InternalCondition>();
            Action action = gTask.getActions().get(i);
            // Procesamos las precondiciones de la accion
            for (j = 0; j < action.getPrecs().length; j++) {
                prec = action.getPrecs()[j];
                key = groundedCondToKey(prec);
                // Si la precondición no existe aún, la creamos
                if (hashPrecEffs.get(key) == null) {
                    // agents = this.getAgents(prec.getVar(), prec.getValue());
                    pe = new InternalCondition(prec, this.hashVars.get(this
                            .groundedVarToKey(prec.getVar())), prec.getValue(),
                            prec.getCondition());
                    // , prec.getVar().getMinTime(prec.getValue()), agents, 0);
                    hashPrecEffs.put(key, pe);
                    precs.add(pe);
                }
                // Si no, la buscamos y la guardamos en la lista de
                // precondiciones
                else {
                    pe = hashPrecEffs.get(key);
                    precs.add(pe);
                    if (pe.prec == null)
                        pe.prec = prec;
                }
            }
            // Procesamos los efectos de la acción
            for (j = 0; j < action.getEffs().length; j++) {
                eff = action.getEffs()[j];
                key = groundedEffToKey(eff);

                if (hashPrecEffs.get(key) == null) {
                    // agents = this.getAgents(eff.getVar(), eff.getValue());
                    pe = new InternalCondition(gTask.createGroundedCondition(
                            EQUAL, eff.getVar(), eff.getValue()),
                            this.hashVars.get(this.groundedVarToKey(eff
                                    .getVar())), eff.getValue(), EQUAL);
                    // , eff.getVar().getMinTime(eff.getValue()), agents, 0);
                    hashPrecEffs.put(key, pe);
                    effects.addNotRepeated(pe);
                    effs.add(pe);
                } else {
                    pe = hashPrecEffs.get(key);
                    effs.add(pe);
                    effects.addNotRepeated(pe);
                }
            }
            // Generamos la acción
            act = new InternalAction(gTask.getActions().get(i), precs, effs);
            actions.add(act);
        }
        // Generamos las metas y las primeras precondiciones abiertas
        // (completaremos su definición al crear el plan inicial)
        for (GroundedCond c : gTask.getGlobalGoals())
            groundedGoals.add(c);
        // for(GroundedCond c: gTask.getPrivateGoals()) groundedGoals.add(c);
        for (GroundedCond c : groundedGoals) {
            key = groundedCondToKey(c);
            if (hashPrecEffs.get(key) == null) {
                // agents = this.getAgents(c.getVar(), c.getValue());
                pe = new InternalCondition(c, this.hashVars.get(this
                        .groundedVarToKey(c.getVar())), c.getValue(),
                        c.getCondition());
                // , c.getVar().getMinTime(c.getValue()), agents, 0);
                hashPrecEffs.put(key, pe);
                openConditions
                        .add(new InternalOpenCondition(pe, null, false/* , null */));
            } else
                openConditions.add(new InternalOpenCondition(hashPrecEffs
                        .get(key), null, false/* , null */));
        }
        // Generamos los POPPrecEff correspondientes al estado inicial
        for (POPFunction f : this.functions) {
            if (f.getInitialTrueValue() != null) {
                key = f.toKey() + " = " + f.getInitialTrueValue();
                if (hashPrecEffs.get(key) == null) {
                    // agents = this.getAgents(f.getVariable(),
                    // f.getInitialTrueValue());
                    pe = new InternalCondition(gTask.createGroundedCondition(
                            EQUAL, f.getVariable(), f.getInitialTrueValue()),
                            f, f.getInitialTrueValue(), EQUAL);
                    // , f.getVariable().getMinTime(f.getInitialTrueValue()),
                    // agents, 0);
                    initialState.add(pe);
                    effects.addNotRepeated(pe);
                    hashPrecEffs.put(pe.toKey(), pe);
                } else
                    initialState.add(hashPrecEffs.get(key));
            } else {
                boolean valueReceived = false;
                GroundedVar v = f.getVariable();
                for (String value : v.getReachableValues())
                    if (v.getMinTime(value) == 0) {
                        valueReceived = true;
                        key = f.toKey() + " = " + value;
                        if (hashPrecEffs.get(key) == null) {
                            // agents = this.getAgents(f.getVariable(), value);
                            pe = new InternalCondition(
                                    gTask.createGroundedCondition(EQUAL,
                                            f.getVariable(), value), f, value,
                                    EQUAL); // ,
                            // f.getVariable().getMinTime(value),agents,
                            // 0);
                            initialState.add(pe);
                            effects.addNotRepeated(pe);
                            hashPrecEffs.put(pe.toKey(), pe);
                        } else
                            initialState.add(hashPrecEffs.get(key));
                        break;
                    }
                // Generamos los efectos especiales de tipo distinct
                // correspondientes al paso inicial (sólo para variables que no
                // tengan valor inicial verdadero)
                if (!valueReceived)
                    for (i = 0; i < f.getInitialFalseValues().size(); i++) {
                        pe = new InternalCondition(
                                gTask.createGroundedCondition(EQUAL,
                                        f.getVariable(), null), f, f
                                .getInitialFalseValues().get(i),
                                DISTINCT);
                        // , 0, null, 0);
                        initialState.add(pe);
                        hashPrecEffs.put(pe.toKey(), pe);
                    }
            }
        }

        ArrayList<GroundedEff> initialStateEffs = new ArrayList<GroundedEff>();
        for (InternalCondition ppe : initialState)
            initialStateEffs.add(this.task.createGroundedEffect(
                    ppe.prec.getVar(), ppe.prec.getValue()));

        this.groundedInitialState = new GroundedEff[initialStateEffs.size()];
        initialStateEffs.toArray(this.groundedInitialState);

        // Guardamos las precondiciones del paso final
        for (InternalOpenCondition oc : openConditions)
            goals.add(oc.condition);

        // Guardamos los índices de los efectos en cada precondición
        for (i = 0; i < effects.size(); i++)
            effects.get(i).setIndex(i);

        // Initialize the requirers hash table
        // For each grounded effect, provides the actions that require it as a
        // precondition
        this.hashRequirers = new Hashtable<String, ArrayList<InternalAction>>();
        for (String spe : hashPrecEffs.keySet()) {
            pe = hashPrecEffs.get(spe);
            requirers = new ArrayList<InternalAction>();
            for (InternalAction ac : actions) {
                for (InternalCondition pre : ac.precs) {
                    if (pre.toKey().equals(pe.toKey()))
                        requirers.add(ac);
                }
            }
            this.hashRequirers.put(pe.toKey(), requirers);
        }
    }

	/*
     * @Override public String getEffectFromCode(int code) { return
	 * this.hashGlobalIndexesCodeEffect.get(code); }
	 */

    @Override
    public int getCodeFromVarName(String var) {
        Integer code = this.hashGlobalIndexesVarCode.get(var);
        return code != null ? code.intValue() : -1;
    }

	/*
	 * @Override public int getCodeFromEffect(String eff) { Integer code =
	 * this.hashGlobalIndexesEffectCode.get(eff); return code != null ?
	 * code.intValue() : -1; }
	 */

    @Override
    public String getVarNameFromCode(int code) {
        return hashGlobalIndexesCodeVar.get(code);
    }

    @Override
    public int getCodeFromVar(GroundedVar var) {
        return getCodeFromVarName(var.toString());
    }

    @Override
    public int getCodeFromValue(String val) {
        Integer code = this.hashGlobalIndexesValueCode.get(val);
        return code != null ? code.intValue() : -1;
    }

    @Override
    public String getValueFromCode(int code) {
        return this.hashGlobalIndexesCodeValue.get(code);
    }

    /**
     * Agents set a global index for each variable and value.
     * Non-visible vars and values are encoded through their global index.
     *
     * @param comm Agent communication object
     */
    private void assignGlobalIndexes(AgentCommunication comm) {
        int globalIdVars = 0, globalIdValues = 0, iter = 0;
        boolean found;

        // Hash tables that contain the global indexes for variables and fluents
        this.hashGlobalIndexesVarCode = new Hashtable<String, Integer>();
        this.hashGlobalIndexesValueCode = new Hashtable<String, Integer>();
        this.hashGlobalIndexesCodeVar = new Hashtable<Integer, String>();
        this.hashGlobalIndexesCodeValue = new Hashtable<Integer, String>();

        ArrayList<ArrayList<GlobalIndexVarValueInfo>> globalIndexesToSend = new ArrayList<ArrayList<GlobalIndexVarValueInfo>>();

        //Iterate until all the agents have played the role of baton agent once
        while (iter < comm.getAgentList().size()) {
            // Baton agent
            if (comm.batonAgent()) {
                //Assign a global index to the values that do not have one already
                for (String val : this.task.getObjects()) {
                    if (this.hashGlobalIndexesValueCode.get(val) == null) {
                        this.hashGlobalIndexesValueCode.put(val, globalIdValues);
                        this.hashGlobalIndexesCodeValue.put(globalIdValues, val);
                        globalIdValues++;
                    }
                }
                //Assign a global index to the vars that do not have one already
                for (GroundedVar var : this.task.getVars()) {
                    String name = var.toString();
                    if (this.hashGlobalIndexesVarCode.get(name) == null) {
                        this.hashGlobalIndexesVarCode.put(name, globalIdVars);
                        this.hashGlobalIndexesCodeVar.put(globalIdVars, name);
                        globalIdVars++;
                    }
                }
                //Prepare the message
                ArrayList<GlobalIndexVarValueInfo> vars = new ArrayList<GlobalIndexVarValueInfo>(),
                        values = new ArrayList<GlobalIndexVarValueInfo>();
                for (Entry<Integer, String> e : this.hashGlobalIndexesCodeVar.entrySet())
                    vars.add(new GlobalIndexVarValueInfo(e.getKey(), e.getValue()));
                for (Entry<Integer, String> e : this.hashGlobalIndexesCodeValue.entrySet())
                    values.add(new GlobalIndexVarValueInfo(e.getKey(), e.getValue()));
                globalIndexesToSend.add(vars);
                globalIndexesToSend.add(values);
                //Send vars and values to the rest of agents
                comm.sendMessage(new MessageContentEncodedVarsValues(globalIndexesToSend, globalIdVars, globalIdValues), false);
            }
            // Non-baton agent
            else {
                // Receive baton agent's global indexes
                MessageContentEncodedVarsValues msg = (MessageContentEncodedVarsValues)
                        comm.receiveMessage(comm.getBatonAgent(), false);
                // Update globalIds
                globalIdVars = msg.getCurrentGlobalIndexVars();
                globalIdValues = msg.getCurrentGlobalIndexValues();

                // Add global indexes to the agent's hash tables
                ArrayList<ArrayList<GlobalIndexVarValueInfo>> indexes = msg.getGlobalIndexes();
                //Add vars info (indexes[0])
                for (GlobalIndexVarValueInfo var : indexes.get(0)) {
                    if (this.hashVars.get(var.getItem()) != null) {
                        if (this.hashGlobalIndexesVarCode.get(var.getItem()) == null) {
                            this.hashGlobalIndexesVarCode.put(var.getItem(), var.getGlobalIndex());
                            this.hashGlobalIndexesCodeVar.put(var.getGlobalIndex(), var.getItem());
                        }
                    }
                }
                //Add values info (indexes[1])
                for (GlobalIndexVarValueInfo val : indexes.get(1)) {
                    //Check if the agent knows the value
                    found = false;
                    for (String s : this.task.getObjects()) {
                        if (s.equals(val.getItem())) {
                            found = true;
                            break;
                        }
                    }
                    //Store the global index if the agent knows the value
                    if (found) {
                        if (this.hashGlobalIndexesValueCode.get(val.getItem()) == null) {
                            this.hashGlobalIndexesValueCode.put(val.getItem(), val.getGlobalIndex());
                            this.hashGlobalIndexesCodeValue.put(val.getGlobalIndex(), val.getItem());
                        }
                    }
                }
            }
            comm.passBaton();
            iter++;
        }
        numGlobalVariables = globalIdVars;
    }

    private String groundedVarToKey(GroundedVar v) {
        String res = v.getFuctionName();
        for (String s : v.getParams()) {
            res += " " + s;
        }
        return res;
    }

    private String groundedCondToKey(GroundedCond c) {
        String res = "(" + groundedVarToKey(c.getVar());
        if (c.getCondition() == EQUAL)
            res += ") = ";
        if (c.getCondition() == DISTINCT)
            res += ") <> ";
        return res + c.getValue();
    }

	/*
	 * private ArrayList<String> getAgents(GroundedVar var, String val) {
	 * ArrayList<String> ag = new ArrayList<String>(); for (String a :
	 * this.task.getAgentNames()) // Si el agente puede conseguir que la
	 * variable tome el valor de la // precondición, lo añadimos if
	 * (var.getMinTime(val, a) != -1) ag.add(a);
	 * 
	 * return ag; }
	 */

    private String groundedEffToKey(GroundedEff e) {
        String res = new String();
        int n = 0;
        res += e.getVar().getFuctionName() + "(";
        for (String s : e.getVar().getParams()) {
            if (n == 0)
                n++;
            else
                res += ", ";
            res += s;
        }
        res += ") = ";
        res += e.getValue();

        return res;
    }

    /**
     * Creates a new plan ordering
     *
     * @param stepIndex1 Index of the first plan step
     * @param stepIndex2 Index of the second plan step
     * @return New ordering
     */
    @Override
    public Ordering createOrdering(int stepIndex1, int stepIndex2) {
        return new POPOrdering(stepIndex1, stepIndex2);
    }

    /**
     * Builds a new causal link
     *
     * @param condition Grounded condition
     * @param step1     First plan step
     * @param step2     Second plan step
     * @return New causal link
     */
    @Override
    public CausalLink createCausalLink(Condition condition, Step step1,
                                       Step step2) {
        POPPrecEff cond = this.condToPrecEff(condition);
        return new POPCausalLink((POPStep) step1, cond, (POPStep) step2);
    }

    private POPPrecEff condToPrecEff(Condition condition) {
        String value = getValueFromCode(condition.getValueCode());
        String varName = getVarNameFromCode(condition.getVarCode());
        POPFunction f = null;
        if (varName != null) {
            GroundedVar gv = task.getVarByName(varName);
            if (gv != null)
                f = new POPFunction(gv);
        }
        if (value == null)
            value = "?";
        return new POPPrecEff(condition, f, value, condition.getType());
    }

    /**
     * Creates a new step
     *
     * @param stepIndex Step index in the plan
     * @param agent     Executor agent
     * @param opName    Operator name
     * @param params    Array of action parameters
     * @param prec      Array of preconditions
     * @param eff       Array of effects
     * @return New plan step
     */
    @Override
    public Step createStep(int stepIndex, String agent, String actionName,
                           Condition[] prec, Condition[] eff) {
        ArrayList<POPPrecEff> precs = new ArrayList<POPPrecEff>();
        ArrayList<POPPrecEff> effs = new ArrayList<POPPrecEff>();
        for (Condition c : prec)
            precs.add(condToPrecEff(c));
        for (Condition c : eff)
            effs.add(condToPrecEff(c));
        POPAction a = new POPAction(actionName, precs, effs);
        return new POPStep(a, stepIndex, agent);
    }

    /**
     * Creates a POPPrecEff object from a GroundedCond
     *
     * @param c
     *            Original GroundedCond
     * @return POPPrecEff created
     *
     *         private POPPrecEff groundedCondToPrecEff(GroundedCond c) {
     *         POPFunction f = new POPFunction(c.getVar()); return new
     *         POPPrecEff(new POPCondition(c, this), f, c.getValue(),
     *         c.getCondition()); }
     */

    /**
     * Creates a POPPrecEff object from a GroundedEff
     *
     * @param c
     *            Original GroundedEff
     * @return POPPrecEff created
     *
     *         private POPPrecEff groundedEffToPrecEff(GroundedEff e) {
     *         POPFunction f = new POPFunction(e.getVar()); POPPrecEff eff = new
     *         POPPrecEff(new POPCondition(e, this), f, e.getValue(),
     *         this.EQUAL); return eff; }
     */

    /**
     * Syncronizes a list of POPPrecEff, replacing them for their counterparts
     * on the grounded task.
     *
     * @param pe
     *            List of POPPrecEff to be syncronized
     * @return New List containing the POPPrecEff syncronized
     *
     *         private ArrayList<POPPrecEff>
     *         synchronizePrecEffs(ArrayList<POPPrecEff> pe) {
     *         ArrayList<POPPrecEff> precEffs = new
     *         ArrayList<POPPrecEff>(pe.size()); for (POPPrecEff ppe : pe) { if
     *         (this.hashPrecEffs.get(ppe.toKey()) != null)
     *         precEffs.add(this.hashPrecEffs.get(ppe.toKey())); else
     *         precEffs.add(ppe); }
     *
     *         return precEffs; }
     */

    /**
     * Synchronizes an array of causal links
     *
     * @param cl
     *            Array of causal links
     *
     *            private void synchronizeCausalLinks(CausalLink[] cl) { int i;
     *            POPCausalLink lnk; for (i = 0; i < cl.length; i++) { lnk =
     *            (POPCausalLink) cl[i]; if
     *            (this.hashPrecEffs.get(lnk.getCondition().toKey()) != null)
     *            lnk
     *            .setCondition(this.hashPrecEffs.get(lnk.getCondition().toKey
     *            ())); // If the PrecEff cannot be sychronized, at least the
     *            variable // should be, in order to detect threats correctly
     *            else if (this.hashVars.get(lnk.getFunction().toKey()) != null)
     *            {
     *            lnk.setFunction(this.hashVars.get(lnk.getFunction().toKey()));
     *            } } }
     */

    /**
     * Synchronizes an array of steps
     *
     * @param st Array of steps
     */
    public void synchronizeStep(Step st) {
        int j;
        POPStep stp;

        stp = (POPStep) st;
        if (stp.getAction().getPrecs() != null) {
            for (j = 0; j < stp.getAction().getPrecs().size(); j++) {
                if (this.hashPrecEffs.get(stp.getAction().getPrecs().get(j)
                        .toKey()) != null)
                    stp.getAction()
                            .getPrecs()
                            .set(j,
                                    this.hashPrecEffs.get(stp.getAction()
                                            .getPrecs().get(j).toKey()));
                else if (this.hashVars.get(stp.getAction().getPrecs().get(j)
                        .getFunction().toKey()) != null)
                    stp.getAction()
                            .getPrecs()
                            .get(j)
                            .setFunction(
                                    this.hashVars.get(stp.getAction()
                                            .getPrecs().get(j).getFunction()
                                            .toKey()));
            }
        }
        if (stp.getAction().getEffects() != null) {
            for (j = 0; j < stp.getAction().getEffects().size(); j++) {
                if (this.hashPrecEffs.get(stp.getAction().getEffects().get(j)
                        .toKey()) != null)
                    stp.getAction()
                            .getEffects()
                            .set(j,
                                    this.hashPrecEffs.get(stp.getAction()
                                            .getEffects().get(j).toKey()));
                else if (this.hashVars.get(stp.getAction().getEffects().get(j)
                        .getFunction().toKey()) != null)
                    stp.getAction()
                            .getEffects()
                            .get(j)
                            .setFunction(
                                    this.hashVars.get(stp.getAction()
                                            .getEffects().get(j).getFunction()
                                            .toKey()));
            }
        }
    }

    @Override
    public Planner createPlanner(GroundedTask gTask, Heuristic h,
                                 AgentCommunication comm, PlanningAgentListener agentListener,
                                 int searchType, int neg, boolean anytime) {
        ArrayList<POPPrecEff> precs = new ArrayList<POPPrecEff>();
        for (POPPrecEff p : goals)
            precs.add(p);
        String[] params = new String[0];
        Action a = task.createAction("Initial", params, new GroundedCond[0],
                groundedInitialState);
        POPAction pa = new POPAction(a, new ArrayList<POPPrecEff>(),
                initialState);
        POPStep initial = new POPStep(pa, 0, null);
        ArrayList<GroundedCond> goalsArray = task.getGlobalGoals();
        a = task.createAction("Final", params,
                goalsArray.toArray(new GroundedCond[goalsArray.size()]),
                new GroundedEff[0]);
        pa = new POPAction(a, precs, null);
        POPStep last = new POPStep(pa, 1, null);
        for (OpenCondition oc : openConditions)
            if (oc.getStep() == null)
                ((POPOpenCondition) oc).setStep(last);

        this.totalThreads = java.lang.Runtime.getRuntime()
                .availableProcessors() / comm.numAgents();
        if (totalThreads <= 0)
            totalThreads = 1;
        if (agentListener != null)
            agentListener.trace(0, "Using " + totalThreads
                    + " thread(s) per agent");

        negotiationFactory = new NegotiationFactoryImp(neg);
        // Solution checker intitialization
        switch (negotiationFactory.getNegotiationType()) {
            case NegotiationFactory.BORDA:
                solutionChecker = new POPSolutionCheckerPrivateGoals(comm, task);
                break;
            default:
                solutionChecker = new POPSolutionCheckerCooperative();
        }

        if (totalThreads == 1) {
            this.planner = new POP(this, initial, last, openConditions, h,
                    comm, agentListener, searchType, anytime);
        } else {
            this.planner = new POPMultiThread(this, initial, last,
                    openConditions, h, comm, agentListener, searchType, anytime);
        }
        return this.planner;
    }

    private static class InternalCondition {
        GroundedCond prec;
        POPFunction popFunction;
        String value;
        int condition;
        int index;

        // ArrayList<String> agents;

        public InternalCondition(GroundedCond prec, POPFunction popFunction,
                                 String value, int condition) {
            // , int minTime, ArrayList<String> agents, int prod) {
            this.prec = prec;
            this.popFunction = popFunction;
            this.value = value;
            this.condition = condition;
            // this.minTime = minTime;
            // this.agents = agents;
            // this.prod = prod;
            this.index = -1;
        }

        public POPPrecEff toPOPPrecEff(PlannerFactory pf) {
            POPCondition pCond = new POPCondition(condition,
                    pf.getCodeFromVar(prec.getVar()), pf.getCodeFromValue(prec
                    .getValue()));
            return new POPPrecEff(pCond, popFunction, value, condition);
        }

        public void setIndex(int i) {
            index = i;
        }

        public String toKey() {
            String res;
            if (prec.getCondition() == EQUAL)
                res = "=";
            if (prec.getCondition() == DISTINCT)
                res = "<>";
            else
                res = "?";
            return prec.getVar().toString() + res + prec.getValue();
        }
    }

    private class InternalAction {
        Action action;
        ArrayList<InternalCondition> precs, effs;

        public InternalAction(Action action,
                              ArrayList<InternalCondition> precs,
                              ArrayList<InternalCondition> effs) {
            this.action = action;
            this.precs = precs;
            this.effs = effs;
        }

        public POPAction toPOPAction(PlannerFactoryImp pf) {
            ArrayList<POPPrecEff> precs = new ArrayList<POPPrecEff>(
                    this.precs.size());
            for (InternalCondition c : this.precs)
                precs.add(c.toPOPPrecEff(pf));
            ArrayList<POPPrecEff> effs = new ArrayList<POPPrecEff>(
                    this.effs.size());
            for (InternalCondition c : this.effs)
                effs.add(c.toPOPPrecEff(pf));
            return new POPAction(action, precs, effs);
        }
    }

    private class InternalOpenCondition {
        InternalCondition condition;
        POPStep step;
        boolean isGoal;

        public InternalOpenCondition(InternalCondition pe, POPStep step,
                                     boolean isGoal) {
            this.condition = pe;
            this.step = step;
            this.isGoal = isGoal;
        }

        public POPOpenCondition toPOPPrecEff(PlannerFactoryImp pf) {
            POPPrecEff cond = condition.toPOPPrecEff(pf);
            return new POPOpenCondition(cond, step, isGoal);
        }
    }
}
