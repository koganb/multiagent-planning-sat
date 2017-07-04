package org.agreement_technologies.service.map_grounding;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.*;
import org.agreement_technologies.common.map_parser.*;
import org.agreement_technologies.service.map_grounding.GroundedTaskImp.ActionCondition;
import org.agreement_technologies.service.map_grounding.GroundedTaskImp.GroundedValue;
import org.agreement_technologies.service.map_grounding.GroundedTaskImp.GroundedVarImp;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Hashtable;

/**
 * Planning task grounding process
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public class GroundingImp implements Grounding {

    private Hashtable<String, Boolean> staticFunctions;     // Static functions
    private GroundedTaskImp gTask;                          // Grounded planning task
    private OpGrounding[] gOps;                             // Operators for grounding
    private ArrayList<OpGrounding> opRequireFunction[];     // For each function, list of operators that need it (in positive way)
    private ArrayList<ProgrammedValue> newTrueValues;       // Last reached true values
    private ArrayList<ProgrammedValue> auxTrueValues;       // Auxiliary array of true values
    private ArrayList<ArrayList<ProgrammedValue>> trueValuesByFunction;  // Reached true values classified by the variable function index
    private ArrayList<ArrayList<ProgrammedValue>> falseValuesByFunction; // Reached true values classified by the variable function index
    private Hashtable<ProgrammedValue, Boolean> falseValueReached;       // Stores the reached false values for the variables
    private int currentLevel, startNewValues, currentTrueValueIndex,
            currentFalseValueIndex;                                      // Current RPG level
    private int sameObjects;                                // Same objects filtering
    private boolean isReground;

    /**
     * Constructor
     */
    public GroundingImp(int sameObjects) {
        staticFunctions = new Hashtable<String, Boolean>();
        this.sameObjects = sameObjects;
    }

    /**
     * Computes the list of static functions
     */
    @Override
    public void computeStaticFunctions(Task task, AgentCommunication comm) {
        String[] sf = getStaticFunctions(task);
        if (comm.numAgents() == 1) { // Single agent
            for (String s : sf) {
                setStaticFunction(s);
            }
        } else { // Communication protocol to figure out the static functions
            if (comm.batonAgent()) {
                // Retrieve all static functions from all agents
                ArrayList<String> allSF = new ArrayList<String>();
                for (String ag : comm.getOtherAgents()) {
                    String[] osf = (String[]) comm.receiveMessage(ag, true);
                    if (osf != null) {
                        for (String f : osf) {
                            if (!allSF.contains(f)) {
                                allSF.add(f);
                            }
                        }
                    }
                }
                // Check each function to reach a consensus
                for (String f : allSF) {
                    if (checkStaticFunction(task, f, sf)) {
                        comm.sendMessage(f, true);
                        boolean isStatic = true;
                        for (String ag : comm.getOtherAgents()) {
                            String resp = (String) comm
                                    .receiveMessage(ag, true);
                            if (resp.equalsIgnoreCase("no")) {
                                isStatic = false;
                            }
                        }
                        comm.sendMessage(isStatic ? "yes" : "no", true);
                        if (isStatic) {
                            setStaticFunction(f);
                        }
                    }
                }
                comm.sendMessage(AgentCommunication.END_STAGE_MESSAGE, true);
            } else {
                comm.sendMessage(comm.getBatonAgent(), sf, true);
                String f = "";
                while (!f.equals(AgentCommunication.END_STAGE_MESSAGE)) {
                    f = (String) comm
                            .receiveMessage(comm.getBatonAgent(), true);
                    if (!f.equals(AgentCommunication.END_STAGE_MESSAGE)) {
                        comm.sendMessage(
                                comm.getBatonAgent(),
                                checkStaticFunction(task, f, sf) ? "yes" : "no",
                                true);
                        String resp = (String) comm.receiveMessage(
                                comm.getBatonAgent(), true);
                        if (resp.equalsIgnoreCase("yes")) {
                            setStaticFunction(f);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a given function is static
     *
     * @param f  Function name
     * @param sf Set of static functions
     * @return True if f is static. Function f is static if f is not known by
     * this agents or if f is in the set of static functions
     */
    protected boolean checkStaticFunction(Task task, String f, String[] sf) {
        for (String s : sf) {
            if (s.equalsIgnoreCase(f)) {
                return true;
            }
        }
        Function func[] = task.getFunctions();
        for (Function aux : func) {
            if (aux.getName().equalsIgnoreCase(f)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the list of static functions
     */
    private String[] getStaticFunctions(Task task) {
        ArrayList<String> sf = new ArrayList<String>();
        Function func[] = task.getFunctions();
        Operator ops[] = task.getOperators();
        for (Function f : func) {
            boolean isEffect = false, isInRules = false; // Functions in rules
            // are not
            // considered static
            for (Operator rule : task.getBeliefs()) {
                Condition effs[] = rule.getEffect();
                for (Condition e : effs) {
                    if (e.getFunction().getName().equals(f.getName())) {
                        isInRules = true;
                        break;
                    }
                }
                if (!isInRules) {
                    Condition precs[] = rule.getPrecondition();
                    for (Condition p : precs) {
                        if (p.getFunction().getName().equals(f.getName())) {
                            isInRules = true;
                            break;
                        }
                    }
                }
                if (isInRules) {
                    break;
                }
            }
            if (!isInRules) {
                for (Operator op : ops) {
                    Condition effs[] = op.getEffect();
                    for (Condition e : effs) {
                        if (e.getFunction().getName().equals(f.getName())) {
                            isEffect = true;
                            break;
                        }
                    }
                    if (isEffect) {
                        break;
                    }
                }
            }
            if (!isEffect && !isInRules) {
                sf.add(f.getName()); // Static
            }
        }
        String res[] = new String[sf.size()];
        for (int i = 0; i < sf.size(); i++) {
            res[i] = sf.get(i);
        }
        return res;
    }

    /**
     * Sets a function as static
     */
    protected void setStaticFunction(String fnc) {
        staticFunctions.put(fnc, true);
    }

    /**
     * Checks if a function has been set as static
     */
    protected boolean isStaticFunction(String fnc) {
        return staticFunctions.containsKey(fnc);
    }

    /**
     * Grounds a planning task. Also receives the name of the agent
     */
    @Override
    public GroundedTask ground(Task task, AgentCommunication comm, boolean negationByFailure) {
        GroundedTask groundedTask = ground(task, comm.getThisAgentName(), negationByFailure);
        // Communication with the other agents
        if (comm.numAgents() > 1) {
            boolean endRPG[] = new boolean[comm.numAgents()]; // Initialized to
            // false
            do {
                sendRPGFacts(endRPG, groundedTask, comm);
                receiveRPGFacts(endRPG, comm);
            } while (!checkEndRPG(endRPG, comm));
        }
        return groundedTask;
    }

    /**
     * Receives new facts from other agents
     *
     * @param g      Grounding class
     * @param endRPG Semaphore to know the end of the disRPG construction
     */
    protected void receiveRPGFacts(boolean[] endRPG, AgentCommunication comm) {
        ArrayList<ReachedValue> newData = new ArrayList<ReachedValue>();
        for (String ag : comm.getOtherAgents()) {
            java.io.Serializable data = comm.receiveMessage(ag, false);
            if (data instanceof String) {
                if (((String) data)
                        .equals(AgentCommunication.END_STAGE_MESSAGE)) {
                    endRPG[comm.getAgentIndex(ag)] = true;
                } else {
                    throw new RuntimeException("Agent " + ag
                            + " is not following the RPG protocol");
                }
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<RPGData> dataReceived = (ArrayList<RPGData>) data;
                endRPG[comm.getAgentIndex(ag)] = false;
                for (RPGData d : dataReceived) {
                    newData.add(createReachedValue(d));
                }
            }
        }
        reground(newData.toArray(new ReachedValue[newData.size()]));
    }

    /**
     * Sends the new achieved facts to the other agents
     *
     * @param g      Grounding class
     * @param endRPG Semaphore to know the end of the disRPG construction
     */
    protected void sendRPGFacts(boolean[] endRPG, GroundedTask gTask,
                                AgentCommunication comm) {
        ReachedValue[] newValues = getNewValues();
        boolean somethingToSend = false;
        for (String ag : comm.getOtherAgents()) {
            for (ReachedValue v : newValues) {
                if (v.shareable(ag)) {
                    somethingToSend = true;
                    break;
                }
            }
            if (somethingToSend) {
                break;
            }
        }
        endRPG[comm.getThisAgentIndex()] = !somethingToSend;
        if (somethingToSend) {
            for (String ag : comm.getOtherAgents()) {
                ArrayList<RPGData> dataToSend = new ArrayList<RPGData>();
                for (ReachedValue v : newValues) {
                    if (v.shareable(ag)) {
                        dataToSend.add(new RPGData(v, gTask, comm
                                .getAgentList()));
                    }
                }
                comm.sendMessage(ag, dataToSend, false);
            }
        } else {
            comm.sendMessage(AgentCommunication.END_STAGE_MESSAGE, false);
        }
    }

    /**
     * Checks whether the RPG building has finished
     *
     * @param endRPG Semaphore to know the end of the disRPG construction
     * @return True if the RPG building has finished
     */
    protected boolean checkEndRPG(boolean[] endRPG, AgentCommunication comm) {
        boolean finished = true;
        for (boolean end : endRPG) {
            if (!end) {
                finished = false;
                break;
            }
        }
        if (!finished) { // Synchronization message
            if (comm.batonAgent()) {
                comm.sendMessage(AgentCommunication.SYNC_MESSAGE, true);
            } else {
                comm.receiveMessage(comm.getBatonAgent(), true);
            }
        }
        return finished;
    }

    /**
     * Grounds a planning task
     *
     * @param task      Parsed planning task
     * @param agentName Name of this agent
     */
    protected GroundedTask ground(Task task, String agentName, boolean negationByFailure) {
        currentLevel = 0;
        currentTrueValueIndex = 0;
        currentFalseValueIndex = 0;
        startNewValues = 0;
        isReground = false;
        gTask = new GroundedTaskImp(task, sameObjects, negationByFailure);
        gTask.initAgents(task, agentName);
        gTask.setStaticFunctions(staticFunctions);
        initOperators(task);
        initInitialState();

        while (newTrueValues.size() > 0) {
            auxTrueValues = new ArrayList<ProgrammedValue>();
            for (OpGrounding op : gOps) // Operators without positive preconditions
            {
                if (op.numTruePrec == 0) {
                    matchNegativeConditions(op, false);
                }
            }
            for (ProgrammedValue pv : newTrueValues) {
                match(pv, false);
            }
            for (ProgrammedValue pv : auxTrueValues) {
                GroundedTaskImp.GroundedVarImp v = gTask.vars.get(pv.varIndex);
                trueValuesByFunction.get(v.fncIndex).add(pv);
            }
            startNewValues += newTrueValues.size();
            newTrueValues = auxTrueValues;
            currentLevel++;
        }

        groundCongestion(task);
        return gTask;
    }

    /**
     * Obtains a reached value from the received data
     *
     * @return Reached value
     */
    protected ReachedValue createReachedValue(RPGData data) {
        GroundedVarImp v = gTask.new GroundedVarImp(data.varName,
                data.params.length);
        for (int i = 0; i < data.params.length; i++) {
            Integer pIndex = gTask.objectIndex.get(data.params[i]);
            if (pIndex == null) // New object
            {
                pIndex = gTask.createNewObject(data.params[i],
                        data.paramTypes[i]);
            }
            v.paramIndex[i] = pIndex;
        }
        Integer varIndex = gTask.getVarIndex(v);
        if (varIndex == null) // New variable
        {
            varIndex = gTask.createNewVariable(v);
        }
        Integer valueIndex = gTask.objectIndex.get(data.value);
        if (valueIndex == null) // New object
        {
            valueIndex = gTask.createNewObject(data.value, data.valueTypes);
        }
        GroundedValue gv = gTask.new GroundedValue(varIndex, valueIndex);
        gv.minTime = data.minTime;
        return gv;
    }

    /**
     * Re-grounds a planning task by adding new values
     *
     * @param gTask     Grounded planning task
     * @param newValues Array of new values
     */
    protected void reground(ReachedValue[] newValues) {
        isReground = true;
        gTask.resetNewValues();
        newTrueValues = addNewReachedValues(newValues);
        while (newTrueValues.size() > 0) {
            auxTrueValues = new ArrayList<ProgrammedValue>();
            for (OpGrounding op : gOps) // Operators without positive preconditions
            {
                if (op.numTruePrec == 0) {
                    matchNegativeConditions(op, true);
                }
            }
            for (ProgrammedValue pv : newTrueValues) {
                match(pv, true);
            }
            for (ProgrammedValue pv : auxTrueValues) {
                GroundedTaskImp.GroundedVarImp v = gTask.vars.get(pv.varIndex);
                trueValuesByFunction.get(v.fncIndex).add(pv);

            }
            newTrueValues = auxTrueValues;
        }
    }

    /**
     * Adds the new reached values to the RPG
     *
     * @param newValues List of new values
     * @return Set of values that have not been reached before
     */
    private ArrayList<ProgrammedValue> addNewReachedValues(
            ReachedValue[] newValues) {
        ArrayList<ProgrammedValue> res = new ArrayList<ProgrammedValue>();
        for (ReachedValue rv : newValues) {
            GroundedValue gv = (GroundedValue) rv;
            Integer rvIndex = gTask.valueIndex.get(gv);
            if (rvIndex == null) { // New value
                rvIndex = gTask.values.size();
                gTask.valueIndex.put(gv, rvIndex);
                gTask.values.add(gv);
                ProgrammedValue pv = new ProgrammedValue(
                        currentTrueValueIndex++, gv.varIndex, gv.valueIndex);
                trueValuesByFunction.get(gv.var().fncIndex).add(pv);
                res.add(pv);
                // Negative values derived from this new value
                ArrayList<Integer> domainValues = gTask.getAllDomainValues(gv
                        .var().domainIndex);
                for (Integer value : domainValues) {
                    if (value.intValue() != gv.valueIndex) {
                        pv = new ProgrammedValue(currentFalseValueIndex++,
                                gv.varIndex, value);
                        if (!falseValueReached.containsKey(pv)) {
                            falseValueReached.put(pv, true);
                            falseValuesByFunction.get(gv.var().fncIndex)
                                    .add(pv);
                        }
                    }
                }
            } else { // Existing value
                GroundedValue oldGV = gTask.values.get(rvIndex);
                for (int i = 0; i < oldGV.minTime.length; i++) {
                    if (gv.minTime[i] != -1) {
                        if (oldGV.minTime[i] == -1) {
                            oldGV.minTime[i] = gv.minTime[i];
                        } else {
                            oldGV.minTime[i] = Math.min(oldGV.minTime[i],
                                    gv.minTime[i]);
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * Returns the new values achieved in the grounding process
     *
     * @return Array of reached values
     */
    protected ReachedValue[] getNewValues() {
        return gTask.getNewValues();
    }

    /**
     * Matching process for operator grounding
     *
     * @param pv     Programmed value
     * @param isTrue True if the value for the variable is positive (negative
     *               otherwise)
     */
    private void match(ProgrammedValue pv, boolean reGround) {
        GroundedTaskImp.GroundedVarImp v = gTask.vars.get(pv.varIndex);
        ArrayList<OpGrounding> opList = opRequireFunction[v.fncIndex];
        for (OpGrounding op : opList) {
            int precIndex = -1;
            do {
                precIndex = op.matches(v, pv.valueIndex, precIndex + 1);
                if (precIndex != -1
                        && op.stackParameters(precIndex, v, pv.valueIndex, true)) { // Match
                    // found
                    op.indexFirstValue = pv.index;
                    completeMatch(0, op, reGround);
                    op.unstackParameters(precIndex, true);
                }
            } while (precIndex != -1);
        }
    }

    /**
     * Completes the grounding of a partially grounded operator
     *
     * @param op Operator
     */
    private void completeMatch(int startPrec, OpGrounding op, boolean reGround) {
        if (op.numUngroundedTruePrecs == 0) {
            matchNegativeConditions(op, reGround);
            return;
        }
        int numPrec = startPrec;
        while (op.truePrec[numPrec].grounded) {
            numPrec++;
        }
        OpCondGrounding p = op.truePrec[numPrec];
        ArrayList<ProgrammedValue> valueList = trueValuesByFunction
                .get(p.fncIndex);
        for (ProgrammedValue pv : valueList) {
            if ((isReground || pv.index < startNewValues || pv.index >= op.indexFirstValue)
                    && op.precMatches(p, gTask.vars.get(pv.varIndex),
                    pv.valueIndex)) {
                if (op.stackParameters(numPrec, gTask.vars.get(pv.varIndex),
                        pv.valueIndex, true)) {
                    completeMatch(numPrec + 1, op, reGround);
                    op.unstackParameters(numPrec, true);
                }
            }
        }

    }

    /**
     * Grounds the negative conditions of a partially grounded operator
     *
     * @param op Operator
     */
    private void matchNegativeConditions(OpGrounding op, boolean reGround) {
        if (op.numUngroundedFalsePrecs == 0) { // Operator grounding
            groundRemainingParameters(op, reGround);
            return;
        }
        for (int i = 0; i < op.numFalsePrec; i++) {
            OpCondGrounding p = op.falsePrec[i];
            if (!p.grounded) {
                ArrayList<ProgrammedValue> valueList = falseValuesByFunction
                        .get(p.fncIndex);
                for (ProgrammedValue pv : valueList) {
                    if (op.negPrecMatches(p, gTask.vars.get(pv.varIndex),
                            pv.valueIndex)) {
                        if (op.stackParameters(i, gTask.vars.get(pv.varIndex),
                                pv.valueIndex, false)) {
                            matchNegativeConditions(op, reGround);
                            op.unstackParameters(i, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Grounds the remaining parameters of an operator with all possible values
     *
     * @param op Operator
     */
    private void groundRemainingParameters(OpGrounding op, boolean reGround) {
        int pIndex = -1;
        for (int i = 0; i < op.numParams; i++) {
            int objIndex = op.paramValues[i].isEmpty() ? -1 : op.paramValues[i]
                    .peek();
            if (objIndex == -1) {
                pIndex = i;
                break;
            }
        }
        if (pIndex == -1) {
            groundAction(op, reGround);
        } else {
            for (int i = 0; i < gTask.objects.size(); i++) {
                if (gTask.objectIsCompatible(i, op.paramTypes[pIndex])) {
                    op.paramValues[pIndex].push(i);
                    groundRemainingParameters(op, reGround);
                    op.paramValues[pIndex].pop();
                }
            }
        }
    }

    // Grounds an operator as a new action
    private void groundAction(OpGrounding op, boolean reGround) {
        if (op.isRule) {
            if (gTask.ruleIndex.get(op.toString()) != null) {
                return; // Existing rule
            }
        } else {
            if (gTask.actionIndex.get(op.toString()) != null) {
                return; // Existing action
            }
        }
        GroundedTaskImp.ActionImp a = gTask.new ActionImp(op.name,
                op.numParams, op.numTruePrec + op.numFalsePrec, op.eff.length);
        // Action parameters grounding
        for (int i = 0; i < op.numParams; i++) {
            int objIndex = op.paramValues[i].peek();
            a.setParam(i, objIndex, gTask.objects.get(objIndex));
        }
        // Action preconditions grounding
        for (int i = 0; i < op.numTruePrec; i++) {
            GroundedTaskImp.GroundedValue gv = groundActionCondition(a,
                    op.truePrec[i], false);
            a.addPrecondition(i, gv, true);
        }
        for (int i = 0; i < op.numFalsePrec; i++) {
            GroundedTaskImp.GroundedValue gv = groundActionCondition(a,
                    op.truePrec[i], false);
            a.addPrecondition(i + op.numTruePrec, gv, false);
        }
        // Action effects grounding
        for (int i = 0; i < op.eff.length; i++) {
            GroundedTaskImp.GroundedValue gv = groundActionCondition(a,
                    op.eff[i], true);
            if (!a.addEffect(i, gv)) {
                return; // Same objects action -> invalid action
            }			/*
             * if (gv.getVar().isBoolean() && !a.requiresVar(gv.getVar())) {
             * GroundedTaskImp.GroundedValue gv2 = gTask.new
             * GroundedValue(gv.varIndex, gv.getValue().equals("true") ?
             * gTask.getObjectIndex("false") : gTask.getObjectIndex("true"));
             * a.addAdditionalPrecondition(op.numTruePrec, gv2, true); }
             */
        }
        if (!checkContradictoryEffects(a))
            return;
        // Add effects to the RPG
        if (reGround) {
            currentLevel = a.getMinTime();
        }
        for (GroundedTaskImp.ActionCondition eff : a.eff) {
            boolean newValue = !gTask.valueIndex.containsKey(eff.gv);
            int gvIndex = gTask.addValue(eff.gv.varIndex, eff.gv.valueIndex,
                    currentLevel + 1);
            eff.gv = gTask.values.get(gvIndex);
            if (reGround && !gTask.newValues.contains(eff.gv)) {
                gTask.newValues.add(eff.gv);
            }
            if (newValue) { // New value reached
                auxTrueValues.add(new ProgrammedValue(currentTrueValueIndex++,
                        eff.gv.varIndex, eff.gv.valueIndex));
                // Negative values derived from this new value
                ArrayList<Integer> domainValues = gTask
                        .getAllDomainValues(eff.gv.var().domainIndex);
                for (Integer value : domainValues) {
                    if (value.intValue() != eff.gv.valueIndex) {
                        ProgrammedValue pv = new ProgrammedValue(
                                currentFalseValueIndex++, eff.gv.varIndex,
                                value);
                        if (!falseValueReached.containsKey(pv)) {
                            falseValueReached.put(pv, true);
                            falseValuesByFunction.get(eff.gv.var().fncIndex)
                                    .add(pv);
                        }
                    }
                }
            }
        }
        if (op.numEff.length > 0)
            groundActionNumericEffect(a, op);
        // Action storage
        if (op.isRule) {
            gTask.ruleIndex.put(a.toString(), gTask.actions.size());
            gTask.rules.add(a);
        } else {
            gTask.actionIndex.put(a.toString(), gTask.actions.size());
            gTask.actions.add(a);
            //System.out.println(a + " [" + a.getMinTime() + "]");
        }
    }

    /**
     * Grounds an action condition/effect
     *
     * @param a              Action
     * @param cond           Condition to be grounded
     * @param canAddVariable True if new variables can be added to the domain
     * @return Grounded condition
     */
    private GroundedTaskImp.GroundedValue groundActionCondition(
            GroundedTaskImp.ActionImp a, OpCondGrounding cond,
            boolean canAddVariable) {
        Function f = gTask.functions.get(cond.fncIndex);
        GroundedTaskImp.GroundedVarImp var = gTask.new GroundedVarImp(
                f.getName(), cond.numParams);
        for (int j = 0; j < cond.numParams; j++) {
            if (cond.paramConstant[j]) {
                var.paramIndex[j] = cond.paramIndex[j];
            } else {
                var.paramIndex[j] = a.paramIndex[cond.paramIndex[j]];
            }
        }
        Integer varIndex = gTask.varIndex.get(var);
        if (varIndex != null) {
            var = gTask.vars.get(varIndex);
        } else if (canAddVariable) {
            var.domain = f.getDomain();
            var = gTask.addVariable(var);
        } else {
            throw new RuntimeException("Unknown variable in action '" + a + "'");
        }
        int valueIndex = cond.constantValue ? cond.valueIndex
                : a.paramIndex[cond.valueIndex];
        return gTask.new GroundedValue(var.varIndex, valueIndex);
    }

    /**
     * Adds the initial state to the RPG
     */
    private void initInitialState() {
        int booleanTypeIndex = gTask.typeIndex.get("boolean");
        int falseObjIndex = gTask.objectIndex.get("false");
        trueValuesByFunction = new ArrayList<ArrayList<ProgrammedValue>>();
        falseValuesByFunction = new ArrayList<ArrayList<ProgrammedValue>>();
        falseValueReached = new Hashtable<ProgrammedValue, Boolean>();
        for (int i = 0; i < gTask.functions.size(); i++) {
            trueValuesByFunction.add(new ArrayList<ProgrammedValue>());
            falseValuesByFunction.add(new ArrayList<ProgrammedValue>());
        }
        newTrueValues = new ArrayList<ProgrammedValue>();
        for (int i = 0; i < gTask.vars.size(); i++) {
            GroundedTaskImp.GroundedVarImp v = gTask.vars.get(i);
            if (v.trueValue == -1) { // True value is not set for this variable
                if (v.domainIndex.length == 1
                        && v.domainIndex[0] == booleanTypeIndex) {
                    // <> true is the same as == false
                    ProgrammedValue pv = new ProgrammedValue(
                            currentTrueValueIndex++, v.varIndex, falseObjIndex);
                    newTrueValues.add(pv);
                    trueValuesByFunction.get(v.fncIndex).add(pv);
                    gTask.addValue(i, falseObjIndex, currentLevel);
                }
                for (Integer value : v.falseValues) {
                    ProgrammedValue pv = new ProgrammedValue(
                            currentFalseValueIndex++, v.varIndex, value);
                    falseValuesByFunction.get(v.fncIndex).add(pv);
                    falseValueReached.put(pv, true);
                }
            } else {
                ProgrammedValue pv = new ProgrammedValue(
                        currentTrueValueIndex++, v.varIndex, v.trueValue);
                newTrueValues.add(pv);
                trueValuesByFunction.get(v.fncIndex).add(pv);
                gTask.addValue(i, v.trueValue, currentLevel);
                ArrayList<Integer> domainValues = gTask
                        .getAllDomainValues(v.domainIndex);
                for (Integer value : domainValues) {
                    if (value.intValue() != v.trueValue) {
                        pv = new ProgrammedValue(currentFalseValueIndex++,
                                v.varIndex, value);
                        falseValueReached.put(pv, true);
                        falseValuesByFunction.get(v.fncIndex).add(pv);
                        v.addFalseValue(gTask.objects.get(value),
                                gTask.objectIndex);
                    }
                }
            }
        }
    }

    /**
     * Initializes the operators to be grounded
     *
     * @param task Parsed planning task
     */
    @SuppressWarnings("unchecked")
    private void initOperators(Task task) {
        int numFnc = gTask.functions.size();
        opRequireFunction = new ArrayList[numFnc];
        for (int i = 0; i < numFnc; i++) {
            opRequireFunction[i] = new ArrayList<OpGrounding>();
        }
        Operator[] ops = task.getOperators();
        Operator[] b = task.getBeliefs();
        gOps = new OpGrounding[ops.length + b.length];
        for (int i = 0; i < ops.length; i++) {
            gOps[i] = new OpGrounding(ops[i], i, false);
        }
        for (int i = 0; i < b.length; i++) {
            gOps[i + ops.length] = new OpGrounding(b[i], i, true);
        }
    }

    private void groundActionNumericEffect(GroundedTaskImp.ActionImp a, OpGrounding op) {
        a.numEff = new GroundedNumericEff[op.numEff.length];
        int i = 0;
        for (NumericEffect e : op.numEff) {
            int type = e.getType();
            GroundedTaskImp.GroundedVarImp var = groundNumericVariable(e.getNumericVariable(), op);
            GroundedTaskImp.GroundedNumericExpressionImp exp = groundNumericExpression(e.getNumericExpression(), op);
            a.numEff[i++] = new GroundedTaskImp.GroundedNumericEffImp(type, var, exp);
        }
    }

    private GroundedVarImp groundNumericVariable(Function var, OpGrounding op) {
        int numParams = var.getParameters().length;
        GroundedTaskImp.GroundedVarImp gvar = gTask.new GroundedVarImp(var.getName(), numParams);
        int objIndex;
        for (int i = 0; i < numParams; i++) {
            Parameter param = var.getParameters()[i];
            if (param.getName().startsWith("?")) {
                int paramIndex = -1;
                for (int j = 0; j < op.numParams; j++)
                    if (op.paramNames[j].equalsIgnoreCase(param.getName())) {
                        paramIndex = j;
                        break;
                    }
                objIndex = op.paramValues[paramIndex].peek();
            } else {    // Constant
                objIndex = gTask.objectIndex.get(param.getName());
            }
            gvar.paramIndex[i] = objIndex;
        }
        int varIndex = gTask.numericVars.indexOf(gvar);
        if (varIndex >= 0) return gTask.numericVars.get(varIndex);
        return gTask.createNewNumericVariable(gvar);
    }

    private GroundedTaskImp.GroundedNumericExpressionImp groundNumericExpression(NumericExpression exp, OpGrounding op) {
        int type = exp.getType();
        GroundedTaskImp.GroundedNumericExpressionImp res;
        switch (type) {
            case NumericExpression.NUMBER:
                res = new GroundedTaskImp.GroundedNumericExpressionImp(type, exp.getValue());
                break;
            case NumericExpression.VARIABLE:
                GroundedVarImp var = groundNumericVariable(exp.getNumericVariable(), op);
                res = new GroundedTaskImp.GroundedNumericExpressionImp(var);
                break;
            case NumericExpression.USAGE:
                res = new GroundedTaskImp.GroundedNumericExpressionImp(type);
                break;
            default:
                GroundedTaskImp.GroundedNumericExpressionImp left = groundNumericExpression(exp.getLeftExp(), op);
                GroundedTaskImp.GroundedNumericExpressionImp right = groundNumericExpression(exp.getRightExp(), op);
                res = new GroundedTaskImp.GroundedNumericExpressionImp(type, left, right);
        }
        return res;
    }

    private void groundCongestion(Task task) {
        Congestion congestion[] = task.getCongestion();
        for (Congestion c : congestion) {
            groundCongestion(c, new ArrayList<Integer>());
        }
    }

    private void groundCongestion(Congestion congestion, ArrayList<Integer> params) {
        if (congestion.getNumParams() == params.size()) {
            GroundedCongestionImp gcong = new GroundedCongestionImp(congestion.getName(), params, gTask);
            groundCongestion(gcong, congestion);
            gTask.congestions.add(gcong);
        } else {
            int paramNumber = params.size();
            String types[] = congestion.getParamTypes(paramNumber);
            int typeIndexes[] = new int[types.length];
            for (int i = 0; i < types.length; i++)
                typeIndexes[i] = gTask.getTypeIndex(types[i]);
            for (int i = 0; i < gTask.objects.size(); i++) {
                if (gTask.objectIsCompatible(i, typeIndexes)) {
                    params.add(i);
                    groundCongestion(congestion, params);
                    params.remove(params.size() - 1);
                }
            }
        }
    }

    private void groundCongestion(GroundedCongestionImp gcong, Congestion congestion) {
        String[] vars = congestion.getVariableNames();
        for (int i = 0; i < vars.length; i++) {
            String[] types = congestion.getVarTypes(i);
            gcong.addVariable(vars[i], types);
        }
        GroundedCongestionImp.Usage usage = groundCongestionUsage(gcong, congestion.getUsage(), congestion);
        gcong.setUsage(usage);
        int numPenalties = congestion.getNumPenalties();
        for (int i = 0; i < numPenalties; i++) {
            GroundedCongestionImp.Penalty penalty = groundCongestionPenalty(gcong, congestion.getPenalty(i), congestion);
            gcong.addPenalty(penalty);
        }
    }

    private GroundedCongestionImp.Usage groundCongestionUsage(GroundedCongestionImp gcong, CongestionUsage usage, Congestion congestion) {
        int type = usage.getType();
        GroundedCongestionImp.Usage gusage = new GroundedCongestionImp.Usage(type);
        if (type == CongestionUsage.ACTION) {
            String actionName = usage.getActionName();
            gusage.setActionName(actionName);
            int numParams = usage.numActionParams();
            for (int i = 0; i < numParams; i++) {
                String paramName = usage.getParamName(i);
                int paramIndex = congestion.getParamIndex(paramName);
                if (paramIndex >= 0) paramName = gcong.paramNames.get(paramIndex);
                gusage.addActionParameter(paramName, gcong);
            }
        } else {
            for (int i = 0; i < usage.numTerms(); i++) {
                GroundedCongestionImp.Usage term = groundCongestionUsage(gcong, usage.getTerm(i), congestion);
                gusage.addTerm(term);
            }
        }
        return gusage;
    }

    private GroundedCongestionImp.Penalty groundCongestionPenalty(GroundedCongestionImp gcong, CongestionPenalty penalty, Congestion congestion) {
        int condition = penalty.getConditionType();
        double conditionValue = penalty.getConditionValue();
        GroundedCongestionImp.Fluent incVar = groundCongestionFluent(gcong, penalty.getIncVariable(), congestion);
        GroundedTaskImp.GroundedNumericExpressionImp incExp = groundCongestionNumericExpression(gcong, penalty.getIncExpression(), congestion);
        return new GroundedCongestionImp.Penalty(condition, conditionValue, incVar, incExp);
    }

    private GroundedCongestionImp.Fluent groundCongestionFluent(GroundedCongestionImp gcong, CongestionFluent var, Congestion congestion) {
        int numParams = var.getNumParams();
        GroundedCongestionImp.Fluent f = new GroundedCongestionImp.Fluent(var.getName(), numParams);
        for (int i = 0; i < numParams; i++) {
            String paramName = var.getParamName(i);
            int paramIndex = congestion.getParamIndex(paramName);
            if (paramIndex >= 0) paramName = gcong.paramNames.get(paramIndex);
            f.addParameter(paramName, gcong);
        }
        return f;
    }

    private GroundedTaskImp.GroundedNumericExpressionImp groundCongestionNumericExpression(GroundedCongestionImp gcong,
                                                                                           NumericExpression expression, Congestion congestion) {
        GroundedTaskImp.GroundedNumericExpressionImp exp;
        int type = expression.getType();
        switch (type) {
            case NumericExpression.NUMBER:
                exp = new GroundedTaskImp.GroundedNumericExpressionImp(type, expression.getValue());
                break;
            case NumericExpression.VARIABLE:
                CongestionFluent var = expression.getCongestionFluent();
                GroundedCongestionImp.Fluent f = groundCongestionFluent(gcong, var, congestion);
                exp = new GroundedTaskImp.GroundedNumericExpressionImp(f);
                break;
            case NumericExpression.ADD:
            case NumericExpression.DEL:
            case NumericExpression.PROD:
            case NumericExpression.DIV:
                GroundedTaskImp.GroundedNumericExpressionImp left = groundCongestionNumericExpression(gcong, expression.getLeftExp(), congestion);
                GroundedTaskImp.GroundedNumericExpressionImp right = groundCongestionNumericExpression(gcong, expression.getRightExp(), congestion);
                exp = new GroundedTaskImp.GroundedNumericExpressionImp(type, left, right);
                break;
            default:
                exp = new GroundedTaskImp.GroundedNumericExpressionImp(type);
        }
        return exp;
    }

    private boolean checkContradictoryEffects(GroundedTaskImp.ActionImp a) {
        int effIndex = 0;
        while (effIndex < a.eff.length - 1) {
            GroundedVar var = a.eff[effIndex].getVar();
            int effIndex2 = effIndex + 1;
            while (effIndex2 < a.eff.length) {
                if (var.equals(a.eff[effIndex2].getVar())) {
                    int remove;
                    String v1 = a.eff[effIndex].getValue();
                    String v2 = a.eff[effIndex2].getValue();
                    if (v1.equalsIgnoreCase(v2)) { // Redundant effect (duplicated)
                        remove = effIndex2;
                    } else { // Contradictory effects
                        if (!var.isBoolean()) return false;
                        remove = v1.equalsIgnoreCase("false") ? effIndex : effIndex2;
                    }
                    a.addMutexVariable(var);
                    ActionCondition v[] = new ActionCondition[a.eff.length - 1];
                    System.arraycopy(a.eff, 0, v, 0, remove);
                    for (int i = remove + 1; i < a.eff.length; i++) v[i - 1] = a.eff[i];
                    a.eff = v;
                    if (remove == effIndex) {
                        effIndex--;
                        break;
                    }
                } else effIndex2++;
            }
            effIndex++;
        }
        return true;
    }

    /* Class for operator conditions grounding */
    private class OpCondGrounding {

        int fncIndex; // Function index
        int numParams; // Number of parameters
        boolean paramConstant[]; // True if the function parameter is a constant
        int paramIndex[]; // Object index if the parameter is a constant,
        // otherwise the index of the operator parameter
        boolean constantValue; // True if the value is a constant
        int valueIndex; // Object index if the value is a constant,
        // otherwise the index of the operator parameter
        boolean grounded; // If the precondition has been grounded

        // Initializes an operator condition
        public OpCondGrounding(Condition cond, String[] paramNames) {
            Function fnc = cond.getFunction();
            fncIndex = gTask.getFunctionIndex(fnc.getName());
            Parameter params[] = fnc.getParameters();
            numParams = params.length;
            if (fnc.isMultifunction()) {
                numParams++;
            }
            paramConstant = new boolean[numParams];
            paramIndex = new int[numParams];
            for (int i = 0; i < params.length; i++) {
                paramConstant[i] = !params[i].getName().startsWith("?");
                paramIndex[i] = getParamIndex(paramConstant[i],
                        params[i].getName(), paramNames);
            }
            if (fnc.isMultifunction()) {
                paramConstant[numParams - 1] = !cond.getValue().startsWith("?");
                paramIndex[numParams - 1] = getParamIndex(
                        paramConstant[numParams - 1], cond.getValue(),
                        paramNames);
                constantValue = true;
                valueIndex = gTask.getObjectIndex("true");
            } else {
                constantValue = !cond.getValue().startsWith("?");
                valueIndex = getParamIndex(constantValue, cond.getValue(),
                        paramNames);
            }
            grounded = false;
        }

        // Computes the parameter index
        private int getParamIndex(boolean isConstant, String name,
                                  String[] paramNames) {
            if (isConstant) {
                return gTask.getObjectIndex(name);
            }
            int index = -1;
            for (int i = 0; i < paramNames.length && index == -1; i++) {
                if (paramNames[i].equalsIgnoreCase(name)) {
                    index = i;
                }
            }
            return index;
        }
    }

    /* Class for operator grounding */
    private class OpGrounding {

        int opIndex;
        String name;
        String paramNames[];
        int[][] paramTypes;
        Deque<Integer>[] paramValues;
        int numParams;
        int numUngroundedTruePrecs, numUngroundedFalsePrecs;
        int numTruePrec, numFalsePrec;
        OpCondGrounding[] truePrec, falsePrec, eff;
        boolean isRule;
        int indexFirstValue;
        NumericEffect[] numEff;

        // Initializes a grounding operator
        @SuppressWarnings("unchecked")
        public OpGrounding(Operator op, int opIndex, boolean isRule) {
            this.opIndex = opIndex;
            this.isRule = isRule;
            name = op.getName();
            Parameter[] params = op.getParameters();
            numParams = params.length;
            paramNames = new String[numParams];
            paramTypes = new int[numParams][];
            paramValues = (Deque<Integer>[]) new Deque[numParams];
            for (int i = 0; i < params.length; i++) {
                paramNames[i] = params[i].getName();
                String types[] = params[i].getTypes();
                paramTypes[i] = new int[types.length];
                for (int j = 0; j < types.length; j++) {
                    paramTypes[i][j] = gTask.getTypeIndex(types[j]);
                }
                paramValues[i] = new java.util.ArrayDeque<Integer>();
            }
            numTruePrec = numFalsePrec = 0;
            for (int i = 0; i < op.getPrecondition().length; i++) {
                Condition c = op.getPrecondition()[i];
                if (c.getType() == Condition.EQUAL
                        || c.getType() == Condition.MEMBER) {
                    numTruePrec++;
                } else {
                    numFalsePrec++;
                }
            }
            numUngroundedTruePrecs = numTruePrec;
            numUngroundedFalsePrecs = numFalsePrec;
            truePrec = new OpCondGrounding[numTruePrec];
            falsePrec = new OpCondGrounding[numFalsePrec];
            eff = new OpCondGrounding[op.getEffect().length];
            int nt = 0, nf = 0;
            for (int i = 0; i < op.getPrecondition().length; i++) {
                Condition c = op.getPrecondition()[i];
                if (c.getType() == Condition.EQUAL
                        || c.getType() == Condition.MEMBER) {
                    truePrec[nt] = new OpCondGrounding(c, paramNames);
                    setFunctionUsage(truePrec[nt++]);
                } else {
                    falsePrec[nf++] = new OpCondGrounding(c, paramNames);
                }
            }
            for (int i = 0; i < eff.length; i++) {
                eff[i] = new OpCondGrounding(op.getEffect()[i], paramNames);
            }
            numEff = op.getNumericEffects();
        }

        // Unstacks the current parameter values
        public void unstackParameters(int precIndex, boolean isTrue) {
            OpCondGrounding p = isTrue ? truePrec[precIndex]
                    : falsePrec[precIndex];
            for (int i = 0; i < numParams; i++) {
                paramValues[i].pop();
            }
            p.grounded = false;
            if (isTrue) {
                numUngroundedTruePrecs++;
            } else {
                numUngroundedFalsePrecs++;
            }
        }

        // Stacks the matched parameter values. Returns false if it not possible
        public boolean stackParameters(int precIndex,
                                       GroundedTaskImp.GroundedVarImp v, int valueIndex, boolean isTrue) {
            OpCondGrounding p = isTrue ? truePrec[precIndex]
                    : falsePrec[precIndex];
            int currentParams[] = new int[numParams];
            for (int i = 0; i < numParams; i++) {
                currentParams[i] = paramValues[i].isEmpty() ? -1
                        : paramValues[i].peek();
            }
            int objIndex, paramIndex;
            for (int i = 0; i < p.numParams; i++) {
                if (!p.paramConstant[i]) {
                    paramIndex = p.paramIndex[i];
                    objIndex = currentParams[paramIndex];
                    if (objIndex == -1) {
                        currentParams[paramIndex] = v.paramIndex[i];
                    } else if (v.paramIndex[i] != objIndex) {
                        return false;
                    }
                }
            }
            if (!p.constantValue) {
                paramIndex = p.valueIndex;
                objIndex = currentParams[paramIndex];
                if (objIndex == -1) {
                    currentParams[paramIndex] = valueIndex;
                } else if (valueIndex != objIndex) {
                    return false;
                }
            }
            for (int i = 0; i < numParams; i++) {
                paramValues[i].push(currentParams[i]);
            }
            p.grounded = true;
            if (isTrue) {
                numUngroundedTruePrecs--;
            } else {
                numUngroundedFalsePrecs--;
            }
            return true;
        }

        // Checks if a given pair (variable,value) matches an operator
        // precondition
        public int matches(GroundedTaskImp.GroundedVarImp v, int valueIndex,
                           int startPrec) {
            int precIndex = -1;
            OpCondGrounding p;
            for (int i = startPrec; i < numTruePrec; i++) {
                p = truePrec[i];
                if (!p.grounded && p.fncIndex == v.fncIndex
                        && precMatches(p, v, valueIndex)) {
                    precIndex = i;
                    break;
                }
            }
            return precIndex;
        }

        // Checks if a precondition matches a variable
        public boolean precMatches(OpCondGrounding p,
                                   GroundedTaskImp.GroundedVarImp v, int valueIndex) {
            boolean matches = true;
            int paramIndex, objIndex;
            for (int i = 0; i < p.numParams && matches; i++) { // Check the
                // parameters
                paramIndex = p.paramIndex[i];
                if (p.paramConstant[i]) {
                    matches = paramIndex == v.paramIndex[i];
                } else {
                    objIndex = paramValues[paramIndex].isEmpty() ? -1
                            : paramValues[paramIndex].peek();
                    if (objIndex == -1) { // Ungrounded parameter, types should
                        // match
                        matches = gTask.objectIsCompatible(v.paramIndex[i],
                                paramTypes[paramIndex]);
                    } else { // Grounded parameter, objects must coincide
                        matches = objIndex == v.paramIndex[i];
                    }
                }
            }
            if (matches) { // Check the value
                if (p.constantValue) {
                    matches = valueIndex == p.valueIndex;
                } else {
                    paramIndex = p.valueIndex;
                    objIndex = paramValues[paramIndex].isEmpty() ? -1
                            : paramValues[paramIndex].peek();
                    if (objIndex == -1) { // Ungrounded parameter, types should
                        // match
                        matches = gTask.objectIsCompatible(valueIndex,
                                paramTypes[paramIndex]);
                    } else { // Grounded parameter, objects must coincide
                        matches = objIndex == valueIndex;
                    }
                }
            }
            return matches;
        }

        // Checks if a negative precondition matches a variable
        public boolean negPrecMatches(OpCondGrounding p,
                                      GroundedTaskImp.GroundedVarImp v, int valueIndex) {
            boolean matches = true;
            int paramIndex, objIndex;
            for (int i = 0; i < p.numParams && matches; i++) { // Check the
                // parameters
                paramIndex = p.paramIndex[i];
                if (p.paramConstant[i]) {
                    matches = paramIndex == v.paramIndex[i];
                } else {
                    objIndex = paramValues[paramIndex].isEmpty() ? -1
                            : paramValues[paramIndex].peek();
                    if (objIndex == -1) { // Ungrounded parameter, types should
                        // match
                        matches = gTask.objectIsCompatible(v.paramIndex[i],
                                paramTypes[paramIndex]);
                    } else { // Grounded parameter, objects must coincide
                        matches = objIndex == v.paramIndex[i];
                    }
                }
            }
            if (matches) { // Check the value
                if (p.constantValue) {
                    matches = valueIndex != p.valueIndex;
                } else {
                    paramIndex = p.valueIndex;
                    objIndex = paramValues[paramIndex].isEmpty() ? -1
                            : paramValues[paramIndex].peek();
                    if (objIndex == -1) { // Ungrounded parameter, types should
                        // match
                        matches = gTask.objectIsCompatible(valueIndex,
                                paramTypes[paramIndex]);
                    } else { // Grounded parameter, objects must differ
                        matches = objIndex != valueIndex;
                    }
                }
            }
            return matches;
        }

        // States that the function is used by this operator
        private void setFunctionUsage(OpCondGrounding cond) {
            ArrayList<OpGrounding> opList = opRequireFunction[cond.fncIndex];
            if (!opList.contains(this)) {
                opList.add(this);
            }
        }

        // Compares two operators by their indexes
        public boolean equals(Object op) {
            return opIndex == ((OpGrounding) op).opIndex;
        }

        // Returns a description of this operator
        public String toString() {
            String res = name;
            for (int i = 0; i < numParams; i++) {
                int objIndex = paramValues[i].isEmpty() ? -1 : paramValues[i]
                        .peek();
                if (objIndex == -1) {
                    res += " " + paramNames[i];
                } else {
                    res += " " + gTask.objects.get(objIndex);
                }
            }
            return res;
        }
    }

    // Pair (variable,value) for the RPG
    public class ProgrammedValue {

        int index;
        int varIndex;
        int valueIndex;

        // Initializes a pair (variable, value)
        public ProgrammedValue(int index, int varIndex, int value) {
            this.index = index;
            this.varIndex = varIndex;
            this.valueIndex = value;
        }

        // Check if two pairs are equal
        public boolean equals(Object x) {
            ProgrammedValue v = (ProgrammedValue) x;
            return varIndex == v.varIndex && valueIndex == v.valueIndex;
        }

        // Hash code
        public int hashCode() {
            return varIndex * 131071 + valueIndex;
        }
    }
}
