package org.agreement_technologies.service.map_grounding;

import org.agreement_technologies.common.map_grounding.*;
import org.agreement_technologies.common.map_parser.*;

import java.util.*;

/**
 * Implementation of a grounded planning task
 *
 * @author Oscar Sapena
 * @since Mar 2011
 */
public class GroundedTaskImp implements GroundedTask {

    static final int UNDEFINED = 0;
    private static final long serialVersionUID = 9198476578040469582L;
    final boolean negationByFailure;
    String domainName;                        // Domain name
    String problemName;                        // Problem name
    String[] requirements;                    // Requirements
    String[] types;                        // Type names
    boolean[][] typesMatrix;                    // Matrix of types
    Hashtable<String, Integer> typeIndex;            // Type indexes
    int booleanTypeIndex;                    // Index of boolean type
    ArrayList<String> objects;                    // Objects
    Hashtable<String, Integer> objectIndex;            // Object indexes
    ArrayList<ArrayList<Integer>> objectTypes;                  // Types of the objects
    ArrayList<Function> functions;                // Functions
    Hashtable<String, Integer> functionIndex;                   // Function index by name
    ArrayList<Boolean> staticFunction;                // Static functions
    ArrayList<GroundedVarImp> vars;                // List of variables
    ArrayList<GroundedVarImp> numericVars;            // List of numeric variables
    Hashtable<GroundedVarImp, Integer> varIndex;        // Variables indexes
    Hashtable<String, GroundedVarImp> varNames;                 // Variables indexed by name
    ArrayList<Action> actions;                    // Array of actions
    Hashtable<String, Integer> actionIndex;            // Action indexes
    ArrayList<ActionImp> rules;                    // Array of belief rules
    Hashtable<String, Integer> ruleIndex;            // Belief rules indexes
    ArrayList<GroundedValue> values;                // Values
    Hashtable<GroundedValue, Integer> valueIndex;               // Value indexes
    ArrayList<GroundedValue> newValues;                // Only for the re-grounding process
    ArrayList<GroundedCond> globalGoals;            // Set of global goals
    Hashtable<String, Integer> agentIndex;            // Agent indexes
    int thisAgentIndex;                        // Index of this agent
    String agentName;                        // Name of this agent
    ArrayList<ArrayList<GroundedSharedData>> sharedDataByFunction;    // Shared data by function index
    int sameObjects;                    // Same objects filtering
    double selfInterest;                    // Self-interest level
    double metricThreshold;                    // Metric threshold
    ArrayList<GroundedCond> preferences;            // Preferences
    Hashtable<String, Integer> preferenceIndex;
    ArrayList<String> preferenceNames;
    GroundedMetric metric;
    double violatedCost[];
    boolean metricRequiresMakespan;
    ArrayList<GroundedCongestion> congestions;                  // Congestion list

    /**
     * Constructor
     *
     * @param task Parsed planning task
     */
    public GroundedTaskImp(Task task, int sameObjects, boolean negationByFailure) {
        this.domainName = task.getDomainName();
        this.problemName = task.getProblemName();
        this.sameObjects = sameObjects;
        this.negationByFailure = negationByFailure;
        String[] aux = task.getRequirements();
        this.requirements = new String[aux.length];        // Requirements
        System.arraycopy(aux, 0, this.requirements, 0, aux.length);
        aux = task.getTypes();                            // Types
        this.types = new String[aux.length];
        System.arraycopy(aux, 0, this.types, 0, aux.length);
        initTypesMatrix(task);
        aux = task.getObjects();                        // Objects
        this.objects = new ArrayList<String>(aux.length + 1);
        this.objects.add("?");
        for (String obj : aux) {
            this.objects.add(obj);
        }
        initObjects(task);
        vars = new ArrayList<GroundedVarImp>();            // Variables
        numericVars = new ArrayList<>();
        initVariables(task);
        actions = new ArrayList<Action>();                // Actions
        actionIndex = new Hashtable<String, Integer>();
        rules = new ArrayList<ActionImp>();                // Rules
        ruleIndex = new Hashtable<String, Integer>();
        values = new ArrayList<GroundedValue>();        // Values
        valueIndex = new Hashtable<GroundedValue, Integer>();
        newValues = null;
        globalGoals = initGoals(task);                    // Goals
        selfInterest = task.getSelfInterest();
        metricThreshold = task.getMetricThreshold();
        preferences = initPreferences(task);            // Preferences
        metric = new GroundedMetric(task.getMetric());    // Metric
        computeViolatedCosts();
        congestions = new ArrayList<>();
    }

    private void computeViolatedCosts() {
        violatedCost = new double[preferences.size()];
        computeViolatedCosts(metric, null);
    }

    private void computeViolatedCosts(GroundedMetric m, GroundedMetric parent) {
        switch (m.metricType) {
            case GroundedMetric.MT_PREFERENCE:
                int prefIndex = -1;
                for (int i = 0; i < preferences.size() && prefIndex == -1; i++) {
                    if (preferences.get(i) == m.preference) {
                        prefIndex = i;
                    }
                }
                violatedCost[prefIndex] = 1.0;
                if (parent != null) {
                    for (GroundedMetric t : parent.term) {
                        if (t.metricType == GroundedMetric.MT_NUMBER) {
                            violatedCost[prefIndex] *= t.number;
                        }
                    }
                }
                break;
            case GroundedMetric.MT_NUMBER:
            case GroundedMetric.MT_TOTAL_TIME:
            case GroundedMetric.MT_NONE:
                break;
            default:
                for (GroundedMetric t : m.term) {
                    computeViolatedCosts(t, m);
                }
        }
    }

    private ArrayList<GroundedCond> initPreferences(Task task) {
        preferenceIndex = new Hashtable<String, Integer>();
        preferenceNames = new ArrayList<String>();
        ArrayList<GroundedCond> pref = new ArrayList<GroundedCond>();
        Fact[] g = task.getPreferences();
        for (int i = 0; i < g.length; i++) {
            String pname = task.getPreferenceName(i);
            int index = functionIndex.get(g[i].getFunctionName());
            Function func = functions.get(index);
            if (func.isMultifunction() || g[i].getValues().length != 1) {
                throw new RuntimeException("Invalid preference '" + pname + "'");
            }
            GroundedVarImp v = initSingleFunctionVariable(g[i], func, task, false);
            String value = g[i].getValues()[0];
            GroundedValue gv = new GroundedValue(v.varIndex, objectIndex.get(value));
            ActionCondition ac = new ActionCondition(gv, !g[i].negated());
            preferenceIndex.put(pname, pref.size());
            preferenceNames.add(pname);
            pref.add(ac);
        }
        return pref;
    }

    // Optimize structures after grounding
    @Override
    public void optimize() {
        for (Action a : actions) {
            a.optimize();
        }
        varNames = new Hashtable<String, GroundedVarImp>(vars.size());
        for (GroundedVarImp v : vars) {
            varNames.put(v.toString(), v);
        }
    }

    /**
     * Creates a new grounded condition
     *
     * @param condition Condition type (EQUAL or DISTINCT)
     * @param var       Grounded variable
     * @param value     Value
     * @return New grounded condition
     */
    @Override
    public GroundedCond createGroundedCondition(int condition, GroundedVar var, String value) {
        if (var == null) {
            return null;
        }
        Integer varIndex = getVarIndex((GroundedVarImp) var);
        if (varIndex == null) {
            return null;
        }
        Integer valueIndex = getObjectIndex(value);
        if (valueIndex == null) {
            valueIndex = UNDEFINED;
        }
        GroundedValue gVar = new GroundedValue(varIndex, valueIndex);
        ActionCondition ac = new ActionCondition(gVar, condition == GroundedCond.EQUAL);
        return ac;
    }

    /**
     * Creates a new grounded effect
     *
     * @param var   Grounded variable
     * @param value Value
     * @return New grounded effect
     */
    @Override
    public GroundedEff createGroundedEffect(GroundedVar var, String value) {
        if (var == null) {
            return null;
        }
        Integer varIndex = getVarIndex((GroundedVarImp) var);
        if (varIndex == null) {
            return null;
        }
        Integer valueIndex = getObjectIndex(value);
        if (valueIndex == null) {
            valueIndex = UNDEFINED;
        }
        GroundedValue gVar = new GroundedValue(varIndex, valueIndex);
        ActionCondition ac = new ActionCondition(gVar);
        return ac;
    }

    /**
     * Creates a new action
     *
     * @param opName Operator name
     * @param params Action parameters
     * @param prec   Action preconditions
     * @param eff    Action effects
     * @return New action
     */
    @Override
    public Action createAction(String opName, String[] params,
                               GroundedCond[] prec, GroundedEff[] eff) {
        ActionImp a = new ActionImp(opName, params.length, prec.length, eff.length);
        for (int i = 0; i < params.length; i++) {
            Integer objIndex = getObjectIndex(params[i]);
            if (objIndex == null) {
                objIndex = UNDEFINED;
            }
            a.setParam(i, objIndex, params[i]);
        }
        for (int i = 0; i < prec.length; i++) {
            a.prec[i] = new ActionCondition(prec[i]);
        }
        for (int i = 0; i < eff.length; i++) {
            a.eff[i] = new ActionCondition(eff[i]);
        }
        return a;
    }

    /**
     * Initializes the agents data
     *
     * @param agentName
     */
    public void initAgents(Task task, String agentName) {
        this.agentName = agentName;
        int agType[] = new int[1];
        agType[0] = typeIndex.get("agent");
        ArrayList<String> agentList = new ArrayList<String>();
        for (int i = 0; i < objects.size(); i++) {
            if (objectIsCompatible(i, agType)) {
                agentList.add(objects.get(i));
            }
        }
        if (!agentList.contains(agentName)) {
            agentList.add(agentName);
        }
        Collections.sort(agentList);
        thisAgentIndex = agentList.indexOf(agentName);
        agentIndex = new Hashtable<String, Integer>();    // Agents
        for (int i = 0; i < agentList.size(); i++) {
            String agName = agentList.get(i);
            agentIndex.put(agName, i);
        }
        initSharedData(task);                            // Shared data
    }

    /**
     * Initializes the shared data
     *
     * @param task Parsed planning task
     */
    private void initSharedData(Task task) {
        sharedDataByFunction = new ArrayList<ArrayList<GroundedSharedData>>();
        for (int i = 0; i < functions.size(); i++) {
            sharedDataByFunction.add(new ArrayList<GroundedSharedData>());
        }
        for (SharedData sd : task.getSharedData()) {
            GroundedSharedData gsd = new GroundedSharedData(sd);
            sharedDataByFunction.get(gsd.fncIndex).add(gsd);
        }
    }

    /**
     * Initializes the task goals
     *
     * @param task        Parsed planning task
     * @param globalGoals True for grounding the global goals, false for private
     *                    goals
     * @return List of goals
     */
    private ArrayList<GroundedCond> initGoals(Task task) {
        ArrayList<GroundedCond> goals = new ArrayList<GroundedCond>();
        Fact[] g = task.getGoals();
        for (int i = 0; i < g.length; i++) {
            int index = functionIndex.get(g[i].getFunctionName());
            Function func = functions.get(index);
            if (func.isMultifunction()) {
                GroundedVarImp[] varList = initMultiFunctionVariable(g[i], func, task, false);
                for (GroundedVarImp v : varList) {
                    GroundedValue gv = new GroundedValue(v.varIndex, g[i].negated()
                            ? objectIndex.get("false") : objectIndex.get("true"));
                    goals.add(new ActionCondition(gv, true));
                }
            } else {
                GroundedVarImp v = initSingleFunctionVariable(g[i], func, task, false);
                String values[] = g[i].getValues();
                for (String value : values) {
                    GroundedValue gv = new GroundedValue(v.varIndex, objectIndex.get(value));
                    goals.add(new ActionCondition(gv, !g[i].negated()));
                }
            }
        }
        return goals;
    }

    /**
     * Initializes the list of variables through the information in the initial
     * state
     *
     * @param task Parsed planning task
     */
    private void initVariables(Task task) {
        Function[] funcList = task.getFunctions();
        functions = new ArrayList<Function>(funcList.length);
        functionIndex = new Hashtable<String, Integer>(funcList.length);
        for (int i = 0; i < funcList.length; i++) {
            functions.add(funcList[i]);
            functionIndex.put(funcList[i].getName(), i);
        }
        varIndex = new Hashtable<GroundedVarImp, Integer>();
        for (Fact fact : task.getInit()) {
            int index = functionIndex.get(fact.getFunctionName());
            Function func = functions.get(index);
            if (func.isMultifunction()) {
                initMultiFunctionVariable(fact, func, task, true);
            } else {
                initSingleFunctionVariable(fact, func, task, true);
            }
        }
    }

    /**
     * Initializes a single-function variable
     *
     * @param fact         Parsed Fact
     * @param func         Parsed function
     * @param task         Parsed planning task
     * @param initialState True if the fact is in the initial state
     * @return The initialized variables
     */
    private GroundedVarImp[] initMultiFunctionVariable(Fact fact, Function func, Task task,
                                                       boolean initialState) {
        String values[] = fact.getValues();
        GroundedVarImp[] gvList = new GroundedVarImp[values.length];
        for (int i = 0; i < values.length; i++) {
            int fncIndex = getFunctionIndex(fact.getFunctionName());
            GroundedVarImp v = new GroundedVarImp(fact.getFunctionName(), varIndex.size(),
                    fncIndex, fact.getParameters(), values[i]);
            if (varIndex.containsKey(v)) {    // Existing variable
                v = vars.get(varIndex.get(v));
                if (initialState) {
                    updateInitialVariableValues(v, fact, task, true);
                }
            } else {                        // New variable
                if (initialState) {
                    updateInitialVariableValues(v, fact, task, true);
                }
                v.setDomain(new String[]{"boolean"});
                varIndex.put(v, vars.size());
                vars.add(v);
            }
            gvList[i] = v;
        }
        return gvList;
    }

    /**
     * Initializes a single-function variable
     *
     * @param fact         Parsed Fact
     * @param func         Parsed function
     * @param task         Parsed planning task
     * @param initialState True if the fact is in the initial state
     * @return The initialized variable
     */
    private GroundedVarImp initSingleFunctionVariable(Fact fact, Function func, Task task,
                                                      boolean initialState) {
        int fncIndex = getFunctionIndex(fact.getFunctionName());
        GroundedVarImp v = new GroundedVarImp(fact.getFunctionName(), varIndex.size(),
                fncIndex, fact.getParameters());
        if (varIndex.containsKey(v)) {    // Existing variable
            v = vars.get(varIndex.get(v));
            if (initialState) {
                updateInitialVariableValues(v, fact, task, false);
            }
        } else {                        // New variable
            if (initialState) {
                updateInitialVariableValues(v, fact, task, false);
            }
            v.setDomain(func.getDomain());
            varIndex.put(v, vars.size());
            vars.add(v);
        }
        return v;
    }

    /**
     * Sets/updates the initial value of a variable
     *
     * @param v     Variable to update
     * @param fact  Fact with the initial assignment
     * @param task  Parsed planning task
     * @param multi True if the variable comes from a multi-function
     */
    private void updateInitialVariableValues(GroundedVarImp v, Fact fact,
                                             Task task, boolean multi) {
        if (multi) {
            if (fact.negated()) {
                v.setTrueValue("false", objectIndex);
                v.addFalseValue("true", objectIndex);
            } else {
                v.setTrueValue("true", objectIndex);
                v.addFalseValue("false", objectIndex);
            }
        } else {
            String values[] = fact.getValues();
            for (String value : values) {
                if (value.equalsIgnoreCase("true") && values.length == 1) {
                    if (fact.negated()) {
                        v.setTrueValue("false", objectIndex);
                        v.addFalseValue(value, objectIndex);
                    } else {
                        v.setTrueValue(value, objectIndex);
                        v.addFalseValue("false", objectIndex);
                    }
                } else if (value.equalsIgnoreCase("false") && values.length == 1) {
                    if (fact.negated()) {
                        v.setTrueValue("true", objectIndex);
                        v.addFalseValue(value, objectIndex);
                    } else {
                        v.setTrueValue(value, objectIndex);
                        v.addFalseValue("true", objectIndex);
                    }
                } else {
                    if (fact.negated()) {
                        v.addFalseValue(value, objectIndex);
                    } else {
                        v.setTrueValue(value, objectIndex);
                    }
                }
            }
        }
    }

    /**
     * Initializes the information about the planning objects For each object an
     * array-list of type indexes is stored
     *
     * @param task Parsed planning task
     */
    private void initObjects(Task task) {
        int numObjs = objects.size();
        objectIndex = new Hashtable<String, Integer>(numObjs);
        objectTypes = new ArrayList<ArrayList<Integer>>(numObjs);
        String[] types;
        for (int i = 0; i < numObjs; i++) {
            objectIndex.put(objects.get(i), i);
            if (i != UNDEFINED) {
                types = task.getObjectTypes(objects.get(i));
            } else {
                types = new String[0];    // No types for the 'undefined' object
            }
            objectTypes.add(new ArrayList<Integer>(types.length));
            for (int j = 0; j < types.length; j++) {
                objectTypes.get(i).add(typeIndex.get(types[j]));
            }
        }
    }

    /**
     * Creates a new object (received from other agent)
     *
     * @param objName  Object name
     * @param objTypes Object types
     * @return Index of the new object
     */
    public int createNewObject(String objName, String[] objTypes) {
        int index = objects.size();
        objects.add(objName);
        objectIndex.put(objName, index);
        ArrayList<Integer> types = new ArrayList<Integer>();
        for (String t : objTypes) {
            Integer tindex = typeIndex.get(t);
            if (tindex != null) {
                types.add(tindex);
            }
        }
        objectTypes.add(types);
        return index;
    }

    /**
     * Creates the types matrix such that typesMatrix[x][y] == true if type x is
     * equal or a sub-type of y.
     *
     * @param task Parsed planning task
     */
    private void initTypesMatrix(Task task) {
        int numTypes = types.length;
        booleanTypeIndex = -1;
        typeIndex = new Hashtable<String, Integer>(numTypes);
        for (int i = 0; i < numTypes; i++) {
            typeIndex.put(types[i], i);
            if (types[i].equalsIgnoreCase("boolean")) {
                booleanTypeIndex = i;
            }
        }
        ArrayList<Integer> list = new ArrayList<Integer>();
        typesMatrix = new boolean[numTypes][numTypes];    // Initialized to false
        for (int i = 0; i < numTypes; i++) {
            typesMatrix[i][i] = true;
            getParentTypes(task, i, list);
            for (Integer j : list) {
                typesMatrix[i][j] = true;
            }
            list.clear();
        }
    }

    /**
     * Fills a list with all the (indexes of the) parent types of a given type
     *
     * @param task      Parser planning task
     * @param typeIndex Index of the given type
     * @param list      List to be filled
     */
    private void getParentTypes(Task task, int typeIndex, ArrayList<Integer> list) {
        String[] pTypes = task.getParentTypes(types[typeIndex]);
        for (String pType : pTypes) {
            int pTypeIndex = this.typeIndex.get(pType);
            if (!list.contains(pTypeIndex)) {    // New parent type
                list.add(pTypeIndex);
                getParentTypes(task, pTypeIndex, list);
            }
        }
    }

    /**
     * Returns the new values achieved in the grounding process
     *
     * @return Array of reached values
     */
    public ReachedValue[] getNewValues() {
        ArrayList<GroundedValue> nv = newValues == null ? values : newValues;
        int numValues = 0;
        for (GroundedValue v : nv) {
            if (!staticFunction.get(v.var().fncIndex)) {
                numValues++;
            }
        }
        ReachedValue[] res = new ReachedValue[numValues];
        int i = 0;
        for (GroundedValue v : nv) {
            if (!staticFunction.get(v.var().fncIndex)) {
                res[i++] = v;
            }
        }
        return res;
    }

    /**
     * Clears the list of new values. This method must be called before any
     * re-grounding process
     */
    public void resetNewValues() {
        newValues = new ArrayList<GroundedValue>();
    }

    /**
     * Sets the static functions
     *
     * @param staticFunctions Hash table with the name of the static functions
     */
    public void setStaticFunctions(Hashtable<String, Boolean> staticFunctions) {
        staticFunction = new ArrayList<Boolean>(functions.size());
        for (Function f : functions) {
            staticFunction.add(staticFunctions.containsKey(f.getName()));
        }
    }

    /**
     * Gets the domain name
     */
    @Override
    public String getDomainName() {
        return domainName;
    }

    /**
     * Gets the problem name
     */
    @Override
    public String getProblemName() {
        return problemName;
    }

    @Override
    public String getAgentName() {
        return agentName;
    }

    @Override
    public String[] getAgentNames() {
        java.util.Enumeration<String> list = agentIndex.keys();
        String ags[] = new String[agentIndex.size()];
        int n = 0;
        while (list.hasMoreElements()) {
            ags[n++] = list.nextElement();
        }
        return ags;
    }

    /**
     * Gets the requirement list
     */
    @Override
    public String[] getRequirements() {
        return requirements;
    }

    /**
     * Gets the list of types
     */
    @Override
    public String[] getTypes() {
        return types;
    }

    /**
     * Gets the parent types of a given type
     */
    @Override
    public String[] getParentTypes(String type) {
        int n = 0;
        int typeIndex = this.typeIndex.get(type);
        for (int i = 0; i < types.length; i++) {
            if (typesMatrix[typeIndex][i] && typeIndex != i) {
                n++;
            }
        }
        String[] pTypes = new String[n];
        n = 0;
        for (int i = 0; i < types.length; i++) {
            if (typesMatrix[typeIndex][i] && typeIndex != i) {
                pTypes[n++] = types[i];
            }
        }
        return pTypes;
    }

    /**
     * Gets the object list (including 'undefined')
     */
    @Override
    public String[] getObjects() {
        return objects.toArray(new String[objects.size()]);
    }

    /**
     * Gets the list of types for a given object
     */
    @Override
    public String[] getObjectTypes(String objName) {
        int objIndex = objectIndex.get(objName);
        ArrayList<Integer> objTypes = objectTypes.get(objIndex);
        String res[] = new String[objTypes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = this.types[objTypes.get(i)];
        }
        return res;
    }

    /**
     * Gets the list of (non-static) variables
     */
    @Override
    public GroundedVar[] getVars() {
        int numVars = 0;
        for (GroundedVarImp v : vars) {
            if (!staticFunction.get(v.fncIndex)) {
                numVars++;
            }
        }
        GroundedVar[] res = new GroundedVar[numVars];
        int i = 0;
        for (GroundedVarImp v : vars) {
            if (!staticFunction.get(v.fncIndex)) {
                res[i++] = v;
            }
        }
        return res;
    }

    /**
     * Gets the list of grounded actions
     */
    @Override
    public ArrayList<Action> getActions() {
        return actions;
    }

    /**
     * Returns the list of grounded belief rules
     */
    @Override
    public GroundedRule[] getBeliefs() {
        return rules.toArray(new GroundedRule[rules.size()]);
    }

    /**
     * Returns the global goals
     */
    @Override
    public ArrayList<GroundedCond> getGlobalGoals() {
        return globalGoals;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Domain: " + getDomainName() + "\n");
        s.append("Problem: " + getProblemName() + "\n");
        s.append("Agent: " + getAgentName() + "\n");
        for (String a : getAgentNames()) {
            s.append("* " + a + "\n");
        }
        s.append("Requirements:\n");
        for (String r : getRequirements()) {
            s.append("* " + r + "\n");
        }
        s.append("Types:\n");
        for (String t : getTypes()) {
            String pt[] = getParentTypes(t);
            s.append("* " + t + " [ ");
            for (String t2 : pt) {
                s.append(t2 + " ");
            }
            s.append("]\n");
        }
        s.append("Objects:\n");
        for (String t : getObjects()) {
            String pt[] = getObjectTypes(t);
            s.append("* " + t + " [ ");
            for (String t2 : pt) {
                s.append(t2 + " ");
            }
            s.append("]\n");
        }
        s.append("Variables:\n");
        for (GroundedVar gv : getVars()) {
            s.append("* " + gv + "\n");
        }
        s.append("Global goals:\n");
        for (GroundedCond g : getGlobalGoals()) {
            s.append("* " + g + "\n");
        }
        return s.toString();
    }

    /**
     * Returns the index of a given type
     *
     * @param name Type name
     * @return Type index
     */
    public int getTypeIndex(String name) {
        return typeIndex.get(name);
    }

    /**
     * Returns the index of the given function
     *
     * @param name Function name
     * @return Function index
     */
    public int getFunctionIndex(String name) {
        return functionIndex.get(name);
    }

    /**
     * Returns the index of the given object
     *
     * @param name Object name
     * @return Object index
     */
    public Integer getObjectIndex(String name) {
        return objectIndex.get(name);
    }

    /**
     * Returns all objects that can be a value in the given domain
     *
     * @param domainIndex List of (indexes of) types
     * @return List of (indexes of) objects
     */
    public ArrayList<Integer> getAllDomainValues(int[] domainIndex) {
        ArrayList<Integer> dv = new ArrayList<Integer>();
        for (int i = 0; i < objects.size(); i++) {
            ArrayList<Integer> types = objectTypes.get(i);
            boolean isCompatible = false;
            for (Integer type : types) {
                for (int j = 0; j < domainIndex.length; j++) {
                    if (typesMatrix[type][domainIndex[j]]) {
                        isCompatible = true;
                    }
                }
                if (isCompatible) {
                    dv.add(i);
                    break;
                }
            }
        }
        return dv;
    }

    /**
     * Checks if a given object is compatible with (at least) one of the given
     * types
     *
     * @param objIndex Object index
     * @param types    List of types (indexes)
     * @return True if the object is compatible
     */
    public boolean objectIsCompatible(int objIndex, int[] types) {
        ArrayList<Integer> objTypes = objectTypes.get(objIndex);
        for (Integer ot : objTypes) {
            for (int t : types) {
                if (typesMatrix[ot][t]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds a new variable
     *
     * @param var Basic data of the variable to add
     * @return Variable added
     */
    public GroundedVarImp addVariable(GroundedVarImp var) {
        int fncIndex = functionIndex.get(var.name);
        if (fncIndex == -1) {
            throw new RuntimeException("Unknown function: " + var.name);
        }
        String[] params = new String[var.paramIndex.length];
        for (int i = 0; i < params.length; i++) {
            params[i] = this.objects.get(var.paramIndex[i]);
        }
        GroundedVarImp newVar = new GroundedVarImp(var.name, this.vars.size(), fncIndex,
                params);
        newVar.setDomain(var.domain);
        varIndex.put(newVar, newVar.varIndex);
        vars.add(newVar);
        return newVar;
    }

    /**
     * Adds a new value reached by this agent
     *
     * @param varIndex     Variable index
     * @param value        Value (object index)
     * @param currentLevel RPG level
     * @param agIndex      Agent index
     * @return Index of the value
     */
    private int addValue(int varIndex, int value, int currentLevel, int agIndex) {
        GroundedValue gv = new GroundedValue(varIndex, value);
        Integer gvIndex = valueIndex.get(gv);
        if (gvIndex == null) {    // New value
            gv.minTime = new int[agentIndex.size()];
            Arrays.fill(gv.minTime, -1);
            gvIndex = this.values.size();
            this.valueIndex.put(gv, gvIndex);
            this.values.add(gv);
        } else {
            gv = values.get(gvIndex);
        }
        if (currentLevel < gv.minTime[agIndex] || gv.minTime[agIndex] == -1) {
            gv.minTime[agIndex] = currentLevel;
        }
        return gvIndex;
    }

    /**
     * Adds a new value reached by this agent
     *
     * @param varIndex     Variable index
     * @param value        Value (object index)
     * @param currentLevel RPG level
     * @return Index of the value
     */
    public int addValue(int varIndex, int value, int currentLevel) {
        return addValue(varIndex, value, currentLevel, thisAgentIndex);
    }

    /**
     * Returns the index of a given variable
     *
     * @param v Variable
     * @return Variable index
     */
    public Integer getVarIndex(GroundedVarImp v) {
        return varIndex.get(v);
    }

    /**
     * Creates a new variable
     *
     * @param v Incomplete variable (only with its name and parameter indexes)
     * @return Variable index
     */
    public int createNewVariable(GroundedVarImp v) {
        v.varIndex = vars.size();
        v.fncIndex = functionIndex.get(v.name);
        v.paramNames = new String[v.paramIndex.length];
        for (int i = 0; i < v.paramIndex.length; i++) {
            v.paramNames[i] = objects.get(v.paramIndex[i]);
        }
        Function f = functions.get(v.fncIndex);
        String domain[] = f.getDomain();
        v.domain = new String[domain.length];
        v.domainIndex = new int[domain.length];
        for (int i = 0; i < domain.length; i++) {
            v.domain[i] = domain[i];
            v.domainIndex[i] = typeIndex.get(domain[i]);
        }
        v.trueValue = -1;
        v.falseValues = new ArrayList<Integer>();
        vars.add(v);
        varIndex.put(v, v.varIndex);
        return v.varIndex;
    }

    public GroundedVarImp createNewNumericVariable(GroundedVarImp v) {
        v.varIndex = numericVars.size();
        v.fncIndex = functionIndex.get(v.name);
        v.paramNames = new String[v.paramIndex.length];
        for (int i = 0; i < v.paramIndex.length; i++) {
            v.paramNames[i] = objects.get(v.paramIndex[i]);
        }
        Function f = functions.get(v.fncIndex);
        String domain[] = f.getDomain();
        v.domain = new String[domain.length];
        v.domainIndex = new int[domain.length];
        for (int i = 0; i < domain.length; i++) {
            v.domain[i] = domain[i];
            v.domainIndex[i] = typeIndex.get(domain[i]);
        }
        v.trueValue = -1;
        v.falseValues = new ArrayList<Integer>();
        numericVars.add(v);
        return v;
    }

    @Override
    public boolean negationByFailure() {
        return negationByFailure;
    }

    @Override
    public ArrayList<GroundedCongestion> getCongestion() {
        return congestions;
    }

    @Override
    public GroundedVar getVarByName(String varName) {
        return varNames.get(varName);
    }

    @Override
    public double getSelfInterestLevel() {
        return selfInterest;
    }

    @Override
    public double getMetricThreshold() {
        return metricThreshold;
    }

    @Override
    public double evaluateMetric(HashMap<String, String> state, double makespan) {
        return evaluateMetric(metric, state, makespan);
    }

    private double evaluateMetric(GroundedMetric m, HashMap<String, String> state, double makespan) {
        double res;
        switch (m.metricType) {
            case GroundedMetric.MT_PREFERENCE:
                String value = state.get(m.preference.getVar().toString());
                res = value.equals(m.preference.getValue()) ? 0 : 1;
                break;
            case GroundedMetric.MT_ADD:
                res = 0;
                for (GroundedMetric mt : m.term) {
                    res += evaluateMetric(mt, state, makespan);
                }
                break;
            case GroundedMetric.MT_MULT:
                res = evaluateMetric(m.term.get(0), state, makespan);
                for (int i = 1; i < m.term.size(); i++) {
                    res *= evaluateMetric(m.term.get(i), state, makespan);
                }
                break;
            case GroundedMetric.MT_TOTAL_TIME:
                res = makespan;
                break;
            case GroundedMetric.MT_NONE:
                res = 0;
                break;
            default:
                res = m.number;
        }
        return res;
    }

    @Override
    public double evaluateMetricMulti(HashMap<String, ArrayList<String>> state, double makespan) {
        return evaluateMetricMulti(metric, state, makespan);
    }

    private double evaluateMetricMulti(GroundedMetric m, HashMap<String, ArrayList<String>> state,
                                       double makespan) {
        double res;
        switch (m.metricType) {
            case GroundedMetric.MT_PREFERENCE:
                ArrayList<String> values = state.get(m.preference.getVar().toString());
                res = 1;
                if (values != null) {
                    for (String value : values) {
                        if (value.equals(m.preference.getValue())) {
                            res = 0;
                            break;
                        }
                    }
                }
                break;
            case GroundedMetric.MT_ADD:
                res = 0;
                for (GroundedMetric mt : m.term) {
                    res += evaluateMetricMulti(mt, state, makespan);
                }
                break;
            case GroundedMetric.MT_MULT:
                res = evaluateMetricMulti(m.term.get(0), state, makespan);
                for (int i = 1; i < m.term.size(); i++) {
                    res *= evaluateMetricMulti(m.term.get(i), state, makespan);
                }
                break;
            case GroundedMetric.MT_TOTAL_TIME:
                res = makespan;
                break;
            default:
                res = m.number;
        }
        return res;
    }

    @Override
    public ArrayList<GroundedCond> getPreferences() {
        return preferences;
    }

    @Override
    public int getNumPreferences() {
        return preferences.size();
    }

    @Override
    public double getViolatedCost(int prefIndex) {
        return violatedCost[prefIndex];
    }

    @Override
    public boolean metricRequiresMakespan() {
        return metricRequiresMakespan;
    }

    public static class GroundedNumericEffImp implements GroundedNumericEff {
        int type;
        GroundedVar var;
        GroundedNumericExpression exp;

        GroundedNumericEffImp(int type, GroundedVar var, GroundedNumericExpression exp) {
            this.type = type;
            this.var = var;
            this.exp = exp;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public GroundedVar getVariable() {
            return var;
        }

        @Override
        public GroundedNumericExpression getExpression() {
            return exp;
        }
    }

    public static class GroundedNumericExpressionImp implements GroundedNumericExpression {
        int type;
        double value;
        GroundedVar var;
        GroundedNumericExpression left, right;
        GroundedCongestionFluent fluent;

        GroundedNumericExpressionImp(int type, double value) {
            this.type = type;
            this.value = value;
        }

        GroundedNumericExpressionImp(GroundedVar var) {
            type = VARIABLE;
            this.var = var;
            fluent = null;
        }

        GroundedNumericExpressionImp(int type) {
            this.type = type;
        }

        GroundedNumericExpressionImp(GroundedCongestionFluent fluent) {
            type = VARIABLE;
            this.fluent = fluent;
            var = null;
        }

        GroundedNumericExpressionImp(int type, GroundedNumericExpression left, GroundedNumericExpression right) {
            this.type = type;
            this.left = left;
            this.right = right;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public double getValue() {
            return value;
        }

        @Override
        public GroundedVar getVariable() {
            return var;
        }

        @Override
        public GroundedCongestionFluent getFluent() {
            return fluent;
        }

        @Override
        public GroundedNumericExpression getLeftOperand() {
            return left;
        }

        @Override
        public GroundedNumericExpression getRightOperand() {
            return right;
        }

        @Override
        public String toString() {
            switch (type) {
                case NUMBER:
                    return "" + value;
                case VARIABLE:
                    return var == null ? fluent.toString() : var.toString();
                case ADD:
                    return "(+ (" + left + ") (" + right + "))";
                case DEL:
                    return "(- (" + left + ") (" + right + "))";
                case PROD:
                    return "(* (" + left + ") (" + right + "))";
                case DIV:
                    return "(/ (" + left + ") (" + right + "))";
                case USAGE:
                    return "(usage)";
                default:
                    return "<error>";
            }
        }
    }

    /**
     * Implementation of a grounded variable
     *
     * @author Oscar Sapena
     * @since Mar 2011
     */
    public class GroundedVarImp implements GroundedVar {

        private static final long serialVersionUID = -1727789113615885154L;
        int varIndex;                            // Variable index
        String name;                            // Variable name
        int fncIndex;                            // Function index of the variable name
        String[] paramNames;                                            // Parameter names
        int[] paramIndex;                        // Parameter indexes
        String[] domain;                        // Variable domain
        int[] domainIndex;                        // Indexes of the domain types
        int trueValue;                            // Initial true value
        ArrayList<Integer> falseValues;                                 // List of initial false values

        /**
         * Common constructor
         *
         * @param name Variable name
         */
        private GroundedVarImp(String name, int varIndex, int fncIndex) {
            this.name = name;
            this.varIndex = varIndex;
            this.fncIndex = fncIndex;
            trueValue = -1;
            falseValues = new ArrayList<Integer>();
        }

        /**
         * Constructor (only for finding an existing variable)
         */
        public GroundedVarImp(String name, int numParams) {
            this.name = name;
            paramIndex = new int[numParams];
        }

        /**
         * Constructor for a single-function variable
         *
         * @param name   Variable name
         * @param params Variable parameters (names)
         */
        public GroundedVarImp(String name, int varIndex, int fncIndex, String[] params) {
            this(name, varIndex, fncIndex);
            paramNames = new String[params.length];
            paramIndex = new int[params.length];
            for (int i = 0; i < params.length; i++) {
                paramNames[i] = params[i];
                paramIndex[i] = objectIndex.get(params[i]);
            }
        }

        /**
         * Constructor for a multi-function variable
         *
         * @param name   Variable name
         * @param params Variable parameters (names)
         * @param value  Additional parameter
         */
        public GroundedVarImp(String name, int varIndex, int fncIndex, String[] params,
                              String value) {
            this(name, varIndex, fncIndex);
            paramNames = new String[params.length + 1];
            paramIndex = new int[params.length + 1];
            for (int i = 0; i < params.length; i++) {
                paramNames[i] = params[i];
                paramIndex[i] = objectIndex.get(params[i]);
            }
            paramNames[params.length] = value;
            paramIndex[params.length] = objectIndex.get(value);
        }

        /**
         * Sets the variable domain
         *
         * @param domain    List of types
         * @param typeIndex Hash table to obtain the types indexes
         */
        public void setDomain(String[] domain) {
            this.domain = new String[domain.length];
            domainIndex = new int[domain.length];
            for (int i = 0; i < domain.length; i++) {
                this.domain[i] = domain[i];
                domainIndex[i] = typeIndex.get(domain[i]);
            }
        }

        /**
         * Sets the true initial value to this variable
         *
         * @param value       New value
         * @param objectIndex Hash table to obtain the value index
         */
        public void setTrueValue(String value,
                                 Hashtable<String, Integer> objectIndex) {
            Integer valueIndex = objectIndex.get(value);
            if (trueValue == -1) {
                int falseIndex = falseValues.indexOf(valueIndex);
                if (falseIndex == -1) {
                    trueValue = valueIndex;
                } else {    // Contradiction
                    falseValues.remove(falseIndex);
                }
            } else {
                throw new RuntimeException("True value already set for variable: " + toString());
            }
        }

        /**
         * Adds a false initial value to this variable
         *
         * @param value       New value
         * @param objectIndex Hash table to obtain the value index
         */
        public void addFalseValue(String value,
                                  Hashtable<String, Integer> objectIndex) {
            Integer valueIndex = objectIndex.get(value);
            if (!falseValues.contains(valueIndex)) {
                if (trueValue != valueIndex) {
                    falseValues.add(valueIndex);
                } else {    // Contradiction
                    trueValue = -1;
                }
            }
        }

        /**
         * Returns a String representation of this variable
         */
        public String toString() {
            String res = name;
            for (int i = 0; i < paramNames.length; i++) {
                res += " " + paramNames[i];
            }
            return res;
        }

        /**
         * Compares two variables by their names and parameters
         */
        public boolean equals(Object v) {
            GroundedVarImp gv = (GroundedVarImp) v;
            return name.equals(gv.name) && Arrays.equals(paramIndex, gv.paramIndex);
        }

        /**
         * Returns the hash code of this variable
         */
        public int hashCode() {
            return (name + Arrays.toString(paramIndex)).hashCode();
        }

        // Returns the function name
        @Override
        public String getFuctionName() {
            return name;
        }

        // Returns the function parameters (list of object names)
        @Override
        public String[] getParams() {
            return paramNames;
        }

        // Returns the function domain types
        @Override
        public String[] getDomainTypes() {
            return domain;
        }

        // Returns the initial true value (object name) or null if it has none
        @Override
        public String initialTrueValue() {
            return trueValue != -1 ? objects.get(trueValue) : null;
        }

        // Returns the initial false values for this variable (list of objects)
        @Override
        public String[] initialFalseValues() {
            String res[] = new String[falseValues.size()];
            for (int i = 0; i < res.length; i++) {
                res[i] = objects.get(falseValues.get(i));
            }
            return res;
        }

        // Minimum time, according to the disRPG, in which the variable can get the
        // given value (objName). Returns -1 if the given value is not reachable
        @Override
        public int getMinTime(String objName) {
            GroundedValue gv = new GroundedValue(varIndex, objectIndex.get(objName));
            Integer index = valueIndex.get(gv);
            if (index == null) {
                return -1;
            }
            gv = values.get(index);
            int time = gv.getMinTime();
            return time;
        }

        // Minimum time, according to the disRPG, in which a given agent can get this
        // variable to have a given value (objName). Returns -1 if the given agent
        // cannot assign the given value to this variable
        @Override
        public int getMinTime(String objName, String agent) {
            GroundedValue gv = new GroundedValue(varIndex, objectIndex.get(objName));
            Integer index = valueIndex.get(gv);
            Integer agIndex = agentIndex.get(agent);
            if (index == null || agIndex == null) {
                return -1;
            }
            gv = values.get(index);
            int time = gv.minTime[agIndex];
            return time;
        }

        // Checks whether the given value for this variable can be shared with the given agent
        @Override
        public boolean shareable(String objName, String agent) {
            boolean res = false;
            int agIndex = agentIndex.get(agent);
            if (!objectIndex.containsKey(objName)) {
                return false;
            }
            int valueIndex = objectIndex.get(objName);
            ArrayList<GroundedSharedData> sdList = sharedDataByFunction.get(fncIndex);
            for (GroundedSharedData sd : sdList) {
                if (sd.isShareable(this, valueIndex, agIndex)) {
                    res = true;
                    break;
                }
            }
            return res;
        }

        // Checks whether the given variable can be shared with the given agent
        @Override
        public boolean shareable(String agent) {
            boolean res = false;
            int agIndex = agentIndex.get(agent);
            for (GroundedSharedData sd : sharedDataByFunction.get(fncIndex)) {
                if (sd.isShareable(this, agIndex)) {
                    res = true;
                    break;
                }
            }
            return res;
        }

        // List of reachable values for this variable
        @Override
        public String[] getReachableValues() {
            ArrayList<Integer> pv = getAllDomainValues(domainIndex);
            ArrayList<String> rv = new ArrayList<String>(pv.size());
            for (Integer objIndex : pv) {
                Integer gvIndex = valueIndex.get(new GroundedValue(varIndex, objIndex));
                if (gvIndex != null) {
                    rv.add(objects.get(objIndex));
                }
            }
            return rv.toArray(new String[rv.size()]);
        }

        // Returns the list of types for a given parameter (0 .. getParams().length - 1)
        @Override
        public String[] getParamTypes(int paramNumber) {
            Function f = functions.get(fncIndex);
            Parameter p = f.getParameters()[paramNumber];
            return p.getTypes();
        }

        @Override
        public boolean isBoolean() {
            return domainIndex[0] == booleanTypeIndex;
        }
    }

    /**
     * Pair (variable index, value index) for the RPG
     *
     * @author Oscar
     * @since April 2011
     */
    public class GroundedValue implements ReachedValue {

        private static final long serialVersionUID = -9105490290945167341L;
        int varIndex;
        int valueIndex;
        int minTime[];        // Minimum time in which this variable can have this value for each agent

        // Initializes a pair (variable, value)
        public GroundedValue(int varIndex, int valueIndex) {
            this.varIndex = varIndex;
            this.valueIndex = valueIndex;
        }

        // Check if two pairs are equal
        public boolean equals(Object x) {
            GroundedValue v = (GroundedValue) x;
            return varIndex == v.varIndex && valueIndex == v.valueIndex;
        }

        // Hash code
        public int hashCode() {
            return varIndex * 131071 + valueIndex;
        }

        // Returns the variable
        GroundedVarImp var() {
            return vars.get(varIndex);
        }

        // Returns the minimum time in which the variable can reach this value
        @Override
        public int getMinTime() {
            int min = minTime[0];
            for (int i = 1; i < minTime.length; i++) {
                if (minTime[i] != -1 && (min == -1 || minTime[i] < min)) {
                    min = minTime[i];
                }
            }
            return min;
        }

        // Gets the involved variable
        @Override
        public GroundedVar getVar() {
            return var();
        }

        // Gets the value for this variable
        @Override
        public String getValue() {
            return objects.get(valueIndex);
        }

        // Returns a description of this value
        public String toString() {
            return "(= " + var() + " " + getValue() + ")[" + getMinTime() + "]";
        }

        // Checks if this value can be shared to another agent
        @Override
        public boolean shareable(String agName) {
            return var().shareable(getValue(), agName);
        }
    }

    /**
     * Action condition or effect (also used for goals)
     *
     * @author Oscar Sapena
     * @since April 2011
     */
    public class ActionCondition implements GroundedCond, GroundedEff {
        private static final long serialVersionUID = -8669437306147623737L;
        GroundedValue gv;
        int condition;        // EQUAL/DISTINCT for conditions (-1 for effects)

        // Creates a new effect
        public ActionCondition(GroundedValue gv) {
            this.gv = gv;
            condition = -1;
        }

        // Creates a new precondition
        public ActionCondition(GroundedValue gv, boolean isEqual) {
            this.gv = gv;
            if (isEqual) {
                condition = GroundedCond.EQUAL;
            } else {
                condition = GroundedCond.DISTINCT;
            }
        }

        public ActionCondition(GroundedCond cond) {
            this(cond.getCondition(), cond.getVar(), cond.getValue());
        }

        public ActionCondition(GroundedEff eff) {
            this(-1, eff.getVar(), eff.getValue());
        }

        public ActionCondition(int condition, GroundedVar var, String value) {
            this.condition = condition;
            int valueIndex = getObjectIndex(value);
            /*String params[] = var.getParams();
             GroundedVarImp gVar = new GroundedVarImp(var.getFuctionName(), params.length);
             for (int i = 0; i < params.length; i++)
             gVar.paramIndex[i] = getObjectIndex(params[i]);
             int variableIndex = varIndex.get(gVar);
             */
            int variableIndex = varIndex.get(var);
            gv = new GroundedValue(variableIndex, valueIndex);
        }

        @Override
        public int getCondition() {
            return condition;
        }

        @Override
        public GroundedVar getVar() {
            return gv.var();
        }

        @Override
        public String getValue() {
            return gv.getValue();
        }

        // Returns a description of this condition
        public String toString() {
            String res = condition != GroundedCond.DISTINCT ? "=" : "<>";
            return res + " (" + gv.var() + ") " + gv.getValue();
        }

        //@Sergio
        public boolean equals(GroundedCond e) {
            if (this.getVar().toString().equals(e.getVar().toString()) && this.getValue().equals(e.getValue())) {
                return true;
            } else {
                return false;
            }
        }

        //@Sergio
        public boolean equals(GroundedEff e) {
            if (this.getVar().toString().equals(e.getVar().toString()) && this.getValue().equals(e.getValue())) {
                return true;
            } else {
                return false;
            }
        }

        public int hasCode() {
            return (this.getValue().toString().hashCode());
        }
    }

    /**
     * Planning action (grounded operator)
     *
     * @author Oscar Sapena
     * @since April 2011
     */
    public class ActionImp implements Action, GroundedRule {
        private static final long serialVersionUID = 379765009600369268L;
        String opName;
        String params[];
        int paramIndex[];
        ActionCondition prec[];
        ActionCondition eff[];
        GroundedNumericEff[] numEff;
        ArrayList<GroundedVar> mutexVar;

        // Creates a new action
        public ActionImp(String opName, int numParams, int numPrecs, int numEffs) {
            this.opName = opName;
            params = new String[numParams];
            paramIndex = new int[numParams];
            prec = new ActionCondition[numPrecs];
            eff = new ActionCondition[numEffs];
            mutexVar = null;
        }

        /**
         * @param effects_Step
         * @Sergio: Lo necesito para crear una regla defeasible ficticia a
         * partir de los efectos de una acción.
         */
        public void setPrec(ActionCondition[] prec) {
            this.prec = prec;
        }

        // Sets a parameter value
        public void setParam(int paramIndex, int objIndex, String objName) {
            params[paramIndex] = objName;
            this.paramIndex[paramIndex] = objIndex;
        }

        // Returns the action description
        public String toString() {
            String res = opName;
            for (String param : params) {
                res += " " + param;
            }
            return res;
        }

        // Returns the operator name
        @Override
        public String getOperatorName() {
            return opName;
        }

        // Returns the list of parameters (list of objects)
        @Override
        public String[] getParams() {
            return params;
        }

        // Optimize structures after grounding
        @Override
        public void optimize() {
            int n = 0;
            for (ActionCondition c : prec) {
                if (!staticFunction.get(c.gv.var().fncIndex)) {
                    n++;
                }
            }
            ActionCondition[] auxP = new ActionCondition[n];
            n = 0;
            for (ActionCondition c : prec) {
                if (!staticFunction.get(c.gv.var().fncIndex)) {
                    auxP[n++] = c;
                }
            }
            prec = auxP;
            n = 0;
            for (ActionCondition c : eff) {
                if (!staticFunction.get(c.gv.var().fncIndex)) {
                    n++;
                }
            }
            ActionCondition[] auxE = new ActionCondition[n];
            n = 0;
            for (ActionCondition c : eff) {
                if (!staticFunction.get(c.gv.var().fncIndex)) {
                    auxE[n++] = c;
                }
            }
            eff = auxE;
        }

        // Action preconditions
        @Override
        public GroundedCond[] getPrecs() {
            return prec;
        }

        // Action effects
        @Override
        public GroundedEff[] getEffs() {
            return eff;
        }

        @Override
        public String getRuleName() {
            return opName;
        }

        @Override
        public GroundedCond[] getBody() {
            return getPrecs();
        }

        @Override
        public GroundedEff[] getHead() {
            return getEffs();
        }

        // Minimum time, according to the disRPG, in which the action can be executed
        @Override
        public int getMinTime() {
            int t = 0;
            for (ActionCondition c : prec) {
                if (c.condition == ActionCondition.EQUAL) {
                    int vt = c.getVar().getMinTime(c.getValue());
                    if (vt > t) {
                        t = vt;
                    }
                }
            }
            return t;
        }

        // Add an effect to the action
        // Returns false if a contradictory effect is found
        public boolean addEffect(int effIndex, GroundedValue gv) {
            if ((sameObjects & SAME_OBJECTS_REP_PARAMS) != 0) {
                for (String param : gv.getVar().getParams()) {
                    if (param.equals(gv.getValue())) {
                        return false;
                    }
                }
            }
            if ((sameObjects & SAME_OBJECTS_PREC_EQ_EFF) != 0) {
                for (ActionCondition c : prec) {
                    if (c.condition == ActionCondition.EQUAL
                            && c.gv.varIndex == gv.varIndex
                            && c.gv.valueIndex == gv.valueIndex) {
                        return false;
                    }
                }
            }
            eff[effIndex] = new ActionCondition(gv);
            return true;
        }

        // Add a precondition to the action
        public void addPrecondition(int precIndex, GroundedValue gv, boolean truePrec) {
            prec[precIndex] = new ActionCondition(gv, truePrec);
        }

        public void addAdditionalPrecondition(int precIndex, GroundedValue gv, boolean truePrec) {
            ActionCondition aux[] = new ActionCondition[prec.length + 1];
            for (int i = 0; i < precIndex; i++) {
                aux[i] = prec[i];
            }
            for (int i = precIndex; i < prec.length; i++) {
                aux[i + 1] = prec[i];
            }
            prec = aux;
            prec[precIndex] = new ActionCondition(gv, truePrec);
        }

        public boolean requiresVar(GroundedVar var) {
            for (GroundedCond p : this.prec) {
                if (p.getVar().equals(var)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public GroundedNumericEff[] getNumEffs() {
            return numEff;
        }

        void addMutexVariable(GroundedVar var) {
            if (mutexVar == null) mutexVar = new ArrayList<>(1);
            if (!mutexVar.contains(var))
                mutexVar.add(var);
        }

        // If this action deletes a precondition or add effect of "a", or vice versa
        @Override
        public boolean isMutex(Action a) {
            ActionImp ai = (ActionImp) a;
            return this.deletesPrecOrEff(ai) || ai.deletesPrecOrEff(this);
        }

        private boolean deletesPrecOrEff(ActionImp a) {
            for (ActionCondition e : this.eff) {
                GroundedVar v = e.getVar();
                String value = e.getValue();
                for (ActionCondition prec : a.prec) {
                    if (prec.getVar().equals(v)) {
                        if (prec.getCondition() == ActionCondition.EQUAL) {
                            if (!value.equalsIgnoreCase(prec.getValue()))
                                return true;
                        } else {
                            if (value.equalsIgnoreCase(prec.getValue()))
                                return true;
                        }
                    }
                }
                for (ActionCondition eff2 : a.eff) {
                    if (eff2.getVar().equals(v) && !value.equalsIgnoreCase(eff2.getValue()))
                        return true;
                }
                if (a.mutexVar != null && a.mutexVar.contains(v))
                    return true;
            }
            if (mutexVar != null) {
                for (GroundedVar v : mutexVar) {
                    if (a.requiresVar(v)) return true;
                    for (ActionCondition e : a.eff)
                        if (e.getVar().equals(v))
                            return true;
                    if (a.mutexVar != null && a.mutexVar.contains(v))
                        return true;
                }
            }
            return false;
        }
    }

    /**
     * Grounded shared data
     */
    public class GroundedSharedData implements java.io.Serializable {

        private static final long serialVersionUID = -2147732192053461311L;
        int agents[];            // Data shared with these agents
        int fncIndex;            // Function index
        int params[];            // Object index (or -1 it it is a variable)
        int paramTypes[][];        // If the parameter is a variable, this array stores its types
        int valueTypes[];        // Domain types

        public GroundedSharedData(SharedData sd) {
            String ag[] = sd.getAgents();                // Agents
            agents = new int[ag.length];
            for (int i = 0; i < ag.length; i++) {
                agents[i] = agentIndex.get(ag[i]);
            }
            Function f = sd.getFunction();                // Function
            fncIndex = functionIndex.get(f.getName());
            Parameter fparams[] = f.getParameters();    // Parameters
            int numParams = fparams.length;
            if (f.isMultifunction()) {
                numParams++;
            }
            params = new int[numParams];
            paramTypes = new int[numParams][];
            for (int i = 0; i < fparams.length; i++) {
                Parameter fparam = fparams[i];
                if (fparam.getName().startsWith("?")) {
                    params[i] = -1;
                    String types[] = fparam.getTypes();
                    paramTypes[i] = new int[types.length];
                    for (int j = 0; j < types.length; j++) {
                        paramTypes[i][j] = typeIndex.get(types[j]);
                    }
                } else {
                    params[i] = objectIndex.get(fparam.getName());
                }
            }                            // Domain
            if (f.isMultifunction()) {    // The value is the last parameter in multi-functions
                int i = params.length - 1;
                String types[] = f.getDomain();
                paramTypes[i] = new int[types.length];
                for (int j = 0; j < types.length; j++) {
                    paramTypes[i][j] = typeIndex.get(types[j]);
                }
                valueTypes = new int[1];
                valueTypes[0] = typeIndex.get("boolean");
            } else {
                String types[] = f.getDomain();
                valueTypes = new int[types.length];
                for (int j = 0; j < types.length; j++) {
                    valueTypes[j] = typeIndex.get(types[j]);
                }
            }
        }

        public boolean isShareable(GroundedVarImp v, int agIndex) {
            if (fncIndex != v.fncIndex) {
                return false;
            }
            boolean validAgent = false;
            for (int i = 0; i < agents.length; i++) {
                if (agents[i] == agIndex) {
                    validAgent = true;
                    break;
                }
            }
            if (!validAgent) {
                return false;
            }
            for (int i = 0; i < params.length; i++) {
                if (params[i] == -1) {
                    if (!objectIsCompatible(v.paramIndex[i], paramTypes[i])) {
                        return false;
                    }
                } else if (params[i] != v.paramIndex[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns true if the given variable value can be shared with the given
         * agent
         *
         * @param v          Variable
         * @param valueIndex Value
         * @param agIndex    Agent
         * @return true if the given value can be shared (false otherwise)
         */
        public boolean isShareable(GroundedVarImp v, int valueIndex, int agIndex) {
            if (fncIndex != v.fncIndex) {
                return false;
            }
            boolean validAgent = false;
            for (int i = 0; i < agents.length; i++) {
                if (agents[i] == agIndex) {
                    validAgent = true;
                    break;
                }
            }
            if (!validAgent) {
                return false;
            }
            for (int i = 0; i < params.length; i++) {
                if (params[i] == -1) {
                    if (!objectIsCompatible(v.paramIndex[i], paramTypes[i])) {
                        return false;
                    }
                } else if (params[i] != v.paramIndex[i]) {
                    return false;
                }
            }
            return objectIsCompatible(valueIndex, valueTypes);
        }
    }

    public class GroundedMetric {

        public static final int MT_PREFERENCE = 0;
        public static final int MT_ADD = 1;
        public static final int MT_MULT = 2;
        public static final int MT_NUMBER = 3;
        public static final int MT_TOTAL_TIME = 4;
        public static final int MT_NONE = 5;
        int metricType;
        double number;                    // if metricType = MT_NUMBER
        GroundedCond preference;        // if metricType = MT_PREFERENCE
        ArrayList<GroundedMetric> term;    // otherwise

        public GroundedMetric(Metric m) {
            metricRequiresMakespan = false;
            if (m == null) {
                metricType = MT_NONE;
            } else {
                metricType = m.getMetricType();
                switch (metricType) {
                    case MT_PREFERENCE:
                        int prefIndex = preferenceIndex.get(m.getPreference());
                        preference = preferences.get(prefIndex);
                        break;
                    case MT_TOTAL_TIME:
                        metricRequiresMakespan = true;
                        break;
                    case MT_NUMBER:
                        number = m.getNumber();
                        break;
                    default:
                        int n = m.getNumTerms();
                        term = new ArrayList<GroundedMetric>();
                        for (int i = 0; i < n; i++) {
                            term.add(new GroundedMetric(m.getTerm(i)));
                        }

                }
            }
        }
    }
}
