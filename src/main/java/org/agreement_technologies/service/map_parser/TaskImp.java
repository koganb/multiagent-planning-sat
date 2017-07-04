package org.agreement_technologies.service.map_parser;

import org.agreement_technologies.common.map_parser.*;
import org.agreement_technologies.service.map_parser.SynAnalyzer.Symbol;

import java.text.ParseException;
import java.util.ArrayList;

/**
 * Implementation of an ungrounded planning task Stores the problem and domain
 * information of a parsed planning task
 *
 * @author Oscar Sapena
 * @since Mar 2011
 */
public class TaskImp implements Task {

    static final int OBJECT_TYPE = 0;    // Index of the predefined type 'object'
    static final int BOOLEAN_TYPE = 1;    // Index of the predefined type 'boolean'
    static final int AGENT_TYPE = 2;    // Index of the predefined type 'agent'
    static final int NUMBER_TYPE = 3;    // Index of the predefined type 'number'

    static final int TRUE_VALUE = 0;    // Index of the predefined object 'true'
    static final int FALSE_VALUE = 1;    // Index of the predefined object 'false'

    String domainName;                        // Domain name
    String problemName;                        // Problem name
    ArrayList<String> requirements;            // Requirements list
    ArrayList<Type> types;                // Variable types
    ArrayList<Value> values;                // Objects and constants
    ArrayList<Variable> predicates;            // Predicates
    ArrayList<Function> functions;            // Functions and multi-functions
    ArrayList<Operator> operators;            // Operators list
    ArrayList<SharedData> sharedData;                   // Shared predicates/functions
    ArrayList<Assignment> init;                // Init section
    ArrayList<Operator> beliefs;            // Belief rules
    ArrayList<Assignment> gGoals;            // Global goals
    ArrayList<PreferenceImp> preferences;        // Preferences
    ArrayList<CongestionImp> congestions;                  // Congestions
    MetricImp metric;                                   // Metric
    double selfInterest;                // Self-interest level
    double metricThreshold;                // Metric threshold

    /**
     * Crates an empty planning task
     */
    public TaskImp() {
        types = new ArrayList<Type>();
        types.add(new Type("object"));            // Predefined type
        types.add(new Type("boolean"));            // Predefined type
        types.add(new Type("agent"));            // Predefined type
        types.add(new Type("number"));            // Predefined type
        requirements = new ArrayList<String>();
        values = new ArrayList<Value>();
        Value bv = new Value("true");            // Predefined object "true"
        bv.types.add(types.get(BOOLEAN_TYPE));
        values.add(bv);
        bv = new Value("false");                // Predefined object "false"
        bv.types.add(types.get(BOOLEAN_TYPE));
        values.add(bv);
        predicates = new ArrayList<Variable>();
        functions = new ArrayList<Function>();
        operators = new ArrayList<Operator>();
        sharedData = new ArrayList<SharedData>();
        init = new ArrayList<Assignment>();
        beliefs = new ArrayList<Operator>();
        gGoals = new ArrayList<Assignment>();
        preferences = new ArrayList<PreferenceImp>();
        congestions = new ArrayList<>();
        metric = null;
        selfInterest = 0;
        metricThreshold = 0;
    }

    /**
     * Adds a planning requirement
     *
     * @param reqName Requirement name
     */
    public void addRequirement(String reqName) {
        if (!requirements.contains(reqName)) {
            requirements.add(reqName);
        }
    }

    /**
     * Checks whether the variable is already defined
     *
     * @param v Variable
     * @return True if the variable is already defined
     */
    public boolean existVariable(Variable v) {
        if (predicates.contains(v)) {
            return true;
        }
        return functions.contains(new Function(v, false));
    }

    /**
     * Return the agent type
     *
     * @return Agent type
     */
    public Type getAgentType() {
        int typeIndex = types.indexOf(new Type("agent"));
        if (typeIndex == -1) {
            return null;
        }
        return types.get(typeIndex);
    }

    /**
     * ********************************************************
     */
    /**
     * I N T E R F A C E M E T H O D S             *
     */
    /**
     * ********************************************************
     */
    /**
     * Returns the domain name
     *
     * @return Domain name
     */
    @Override
    public String getDomainName() {
        return domainName;
    }

    /**
     * Return the problem name
     *
     * @return Problem name
     */
    @Override
    public String getProblemName() {
        return problemName;
    }

    /**
     * Returns the requirements list
     *
     * @return Array of strings, each string representing a requirement
     * specified in the domain file. Supported requirements are: strips, typing,
     * negative-preconditions and object-fluents
     */
    @Override
    public String[] getRequirements() {
        String res[] = new String[requirements.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = requirements.get(i);
        }
        return res;
    }

    /**
     * Returns the list of types
     *
     * @return Array of strings, each string is a type defined in the domain
     * file
     */
    @Override
    public String[] getTypes() {
        String res[] = new String[types.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = types.get(i).name;
        }
        return res;
    }

    /**
     * Returns the base types of a given type
     *
     * @param type Name of the type
     * @return Array of strings which contains the super-types for the given
     * type
     */
    @Override
    public String[] getParentTypes(String type) {
        int index = types.indexOf(new Type(type));
        if (index == -1) {
            return null;
        }
        Type t = types.get(index);
        String res[] = new String[t.parentTypes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = t.parentTypes.get(i).name;
        }
        return res;
    }

    /**
     * Returns the list of objects
     *
     * @return Array of string containing the names of the objects declared in
     * the domain (constants section) and problem (objects section) files
     */
    @Override
    public String[] getObjects() {
        String res[] = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = values.get(i).name;
        }
        return res;
    }

    /**
     * Returns the type list of a given object
     *
     * @param objName Object name
     * @return Array of string containing the set of types of the given object
     */
    @Override
    public String[] getObjectTypes(String objName) {
        int index = values.indexOf(new Value(objName));
        if (index == -1) {
            return null;
        }
        Value v = values.get(index);
        String res[] = new String[v.types.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = v.types.get(i).name;
        }
        return res;
    }

    /**
     * Returns the list of functions (predicates are also included as they are
     * considered boolean functions)
     *
     * @return Array of functions defined in the domain file
     */
    @Override
    public org.agreement_technologies.common.map_parser.Function[] getFunctions() {
        TaskTypes.FunctionImp res[] = new TaskTypes.FunctionImp[predicates.size() + functions.size()];
        TaskTypes.FunctionImp f;
        Variable v;
        Value param;
        for (int i = 0; i < predicates.size(); i++) {
            v = predicates.get(i);
            f = new TaskTypes.FunctionImp(v.name, false);
            f.parameters = new TaskTypes.ParameterImp[v.params.size()];
            for (int p = 0; p < f.parameters.length; p++) {
                param = v.params.get(p);
                f.parameters[p] = new TaskTypes.ParameterImp(param.name);
                f.parameters[p].types = new String[param.types.size()];
                for (int t = 0; t < param.types.size(); t++) {
                    f.parameters[p].types[t] = param.types.get(t).name;
                }
            }
            f.domain = new String[1];
            f.domain[0] = types.get(BOOLEAN_TYPE).name;
            res[i] = f;
        }
        for (int i = 0; i < functions.size(); i++) {
            Function fnc = functions.get(i);
            v = fnc.var;
            f = new TaskTypes.FunctionImp(v.name, fnc.multiFunction);
            f.parameters = new TaskTypes.ParameterImp[v.params.size()];
            for (int p = 0; p < f.parameters.length; p++) {
                param = v.params.get(p);
                f.parameters[p] = new TaskTypes.ParameterImp(param.name);
                f.parameters[p].types = new String[param.types.size()];
                for (int t = 0; t < param.types.size(); t++) {
                    f.parameters[p].types[t] = param.types.get(t).name;
                }
            }
            f.domain = new String[fnc.domain.size()];
            for (int t = 0; t < f.domain.length; t++) {
                f.domain[t] = fnc.domain.get(t).name;
            }
            res[i + predicates.size()] = f;
        }
        return res;
    }

    /**
     * Returns the list of operators
     *
     * @return Array of operators defined in the domain file
     */
    @Override
    public org.agreement_technologies.common.map_parser.Operator[] getOperators() {
        TaskTypes.OperatorImp res[] = new TaskTypes.OperatorImp[operators.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = getOperator(operators.get(i));
        }
        return res;
    }

    /**
     * Returns a parsed operator
     *
     * @param operator Stored operator
     * @return Parsed operator
     */
    private TaskTypes.OperatorImp getOperator(Operator operator) {
        TaskTypes.OperatorImp op = new TaskTypes.OperatorImp(operator.name,
                operator.preference);
        op.parameters = new TaskTypes.ParameterImp[operator.params.size()];
        for (int i = 0; i < op.parameters.length; i++) {
            Value p = operator.params.get(i);
            op.parameters[i] = new TaskTypes.ParameterImp(p.name);
            op.parameters[i].types = new String[p.types.size()];
            for (int t = 0; t < p.types.size(); t++) {
                op.parameters[i].types[t] = p.types.get(t).name;
            }
        }
        op.prec = getOperatorCondition(operator.prec, true);
        op.eff = getOperatorCondition(operator.eff, false);
        op.numEff = getOperatorNumericEffects(operator.eff);
        return op;
    }

    /**
     * Returns a list of parsed conditions
     *
     * @param cond Stored condition
     * @param cmp  True if it is a comparison, false for assignments
     * @return Parsed conditions
     */
    private TaskTypes.ConditionImp[] getOperatorCondition(ArrayList<OperatorCondition> cond, boolean cmp) {
        int numConditions = 0;
        for (OperatorCondition oc : cond) {
            if (oc.type != OperatorConditionType.CT_INCREASE)
                numConditions++;
        }
        TaskTypes.ConditionImp[] res = new TaskTypes.ConditionImp[numConditions];
        numConditions = 0;
        for (OperatorCondition oc : cond) {
            if (oc.type == OperatorConditionType.CT_INCREASE) continue;
            Variable varCond = null;
            Function fncCond = null;
            int index = this.predicates.indexOf(new Variable(oc.var.name));
            if (index == -1) {    // Function
                index = this.functions.indexOf(new Function(new Variable(oc.var.name), false));
                fncCond = this.functions.get(index);
            } else {            // Predicate
                varCond = this.predicates.get(index);
            }
            res[numConditions] = new TaskTypes.ConditionImp();
            switch (oc.type) {
                case CT_NONE:    // Predicate
                    if (cmp) {
                        res[numConditions].type = TaskTypes.ConditionImp.EQUAL;
                    } else {
                        res[numConditions].type = TaskTypes.ConditionImp.ASSIGN;
                    }
                    break;
                case CT_EQUAL:
                    if (oc.neg) {
                        res[numConditions].type = TaskTypes.ConditionImp.DISTINCT;
                    } else {
                        res[numConditions].type = TaskTypes.ConditionImp.EQUAL;
                    }
                    break;
                case CT_MEMBER:
                    if (oc.neg) {
                        res[numConditions].type = TaskTypes.ConditionImp.NOT_MEMBER;
                    } else {
                        res[numConditions].type = TaskTypes.ConditionImp.MEMBER;
                    }
                    break;
                case CT_ASSIGN:
                    res[numConditions].type = TaskTypes.ConditionImp.ASSIGN;
                    break;
                case CT_ADD:
                    res[numConditions].type = TaskTypes.ConditionImp.ADD;
                    break;
                case CT_DEL:
                    res[numConditions].type = TaskTypes.ConditionImp.DEL;
                    break;
            }
            res[numConditions].fnc = new TaskTypes.FunctionImp(oc.var.name, fncCond != null && fncCond.multiFunction);
            res[numConditions].fnc.parameters = new TaskTypes.ParameterImp[oc.var.params.size()];
            for (int p = 0; p < oc.var.params.size(); p++) {
                Value param = oc.var.params.get(p);
                res[numConditions].fnc.parameters[p] = new TaskTypes.ParameterImp(param.name);
                res[numConditions].fnc.parameters[p].types = new String[param.types.size()];
                for (int t = 0; t < param.types.size(); t++) {
                    res[numConditions].fnc.parameters[p].types[t] = param.types.get(t).name;
                }
            }
            if (varCond != null) {    // Predicate
                res[numConditions].fnc.domain = new String[1];
                res[numConditions].fnc.domain[0] = this.types.get(BOOLEAN_TYPE).name;
                if (oc.neg) {
                    res[numConditions].value = this.values.get(FALSE_VALUE).name;
                } else {
                    res[numConditions].value = this.values.get(TRUE_VALUE).name;
                }
            } else {                // Function
                res[numConditions].fnc.domain = new String[fncCond.domain.size()];
                for (int t = 0; t < res[numConditions].fnc.domain.length; t++) {
                    res[numConditions].fnc.domain[t] = fncCond.domain.get(t).name;
                }
                res[numConditions].value = oc.value.name;
            }
            numConditions++;
        }
        return res;
    }

    /**
     * Returns the shared data, which defines the information the current agent
     * can share with the other ones
     *
     * @return Array of shared data defined in the problem file
     */
    @Override
    public org.agreement_technologies.common.map_parser.SharedData[] getSharedData() {
        TaskTypes.SharedDataImp[] sd = new TaskTypes.SharedDataImp[sharedData.size()];
        for (int i = 0; i < sd.length; i++) {
            SharedData sData = this.sharedData.get(i);
            sd[i] = new TaskTypes.SharedDataImp();
            Variable v = sData.var != null ? sData.var : sData.fnc.var;
            sd[i].fnc = new TaskTypes.FunctionImp(v.name, sData.fnc != null && sData.fnc.multiFunction);
            sd[i].fnc.parameters = new TaskTypes.ParameterImp[v.params.size()];
            for (int p = 0; p < v.params.size(); p++) {
                Value param = v.params.get(p);
                sd[i].fnc.parameters[p] = new TaskTypes.ParameterImp(param.name);
                sd[i].fnc.parameters[p].types = new String[param.types.size()];
                for (int t = 0; t < param.types.size(); t++) {
                    sd[i].fnc.parameters[p].types[t] = param.types.get(t).name;
                }
            }
            if (sData.fnc == null) {    // Predicate
                sd[i].fnc.domain = new String[1];
                sd[i].fnc.domain[0] = this.types.get(BOOLEAN_TYPE).name;
            } else {                    // Function
                sd[i].fnc.domain = new String[sData.fnc.domain.size()];
                for (int t = 0; t < sd[i].fnc.domain.length; t++) {
                    sd[i].fnc.domain[t] = sData.fnc.domain.get(t).name;
                }
            }
            sd[i].agents = new String[sData.agents.size()];
            for (int a = 0; a < sd[i].agents.length; a++) {
                sd[i].agents[a] = sData.agents.get(a).name;
            }
        }
        return sd;
    }

    /**
     * Returns the initial state information
     *
     * @return Array of facts
     */
    @Override
    public Fact[] getInit() {
        TaskTypes.FactImp res[] = new TaskTypes.FactImp[init.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = getFact(init.get(i));
        }
        return res;
    }

    /**
     * Returns a parsed fact
     *
     * @param as Stored assignment
     * @return Parsed fact
     */
    private TaskTypes.FactImp getFact(Assignment as) {
        Variable v = as.var != null ? as.var : as.fnc.var;
        TaskTypes.FactImp f = new TaskTypes.FactImp(v.name, as.neg);
        f.parameters = new String[as.params.size()];
        for (int i = 0; i < f.parameters.length; i++) {
            Value param = as.params.get(i);
            f.parameters[i] = param.name;
        }
        f.values = new String[as.values.size()];
        for (int i = 0; i < f.values.length; i++) {
            f.values[i] = as.values.get(i).name;
        }
        return f;
    }

    /**
     * Returns the list of belief rules
     *
     * @return Array of belief rules
     */
    @Override
    public org.agreement_technologies.common.map_parser.Operator[] getBeliefs() {
        TaskTypes.OperatorImp res[] = new TaskTypes.OperatorImp[beliefs.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = getOperator(beliefs.get(i));
        }
        return res;
    }

    /**
     * Returns the list of (global or private) goals
     *
     * @param global True to retrieve the global goals, false to retrieve the
     *               private goals
     * @return Array of goals (facts)
     */
    @Override
    public Fact[] getGoals() {
        TaskTypes.FactImp res[] = new TaskTypes.FactImp[gGoals.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = getFact(gGoals.get(i));
        }
        return res;
    }

    /**
     * Returns a description of this task
     *
     * @return String with the task description
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("(domain " + domainName + ")\n");
        s.append("(problem " + problemName + ")\n");
        s.append("(requirements " + toString(getRequirements()) + ")\n");
        s.append("(types\n");
        for (String t : getTypes()) {
            s.append("\t" + t);
            if (getParentTypes(t).length == 0) {
                s.append("\n");
            } else {
                s.append(" - " + toString(getParentTypes(t)) + "\n");
            }
        }
        s.append(")\n(objects\n");
        for (String o : getObjects()) {
            s.append("\t" + o);
            if (getObjectTypes(o).length == 0) {
                s.append("\n");
            } else {
                s.append(" - " + toString(getObjectTypes(o)) + "\n");
            }
        }
        s.append(")\n(functions\n");
        for (org.agreement_technologies.common.map_parser.Function f : getFunctions()) {
            s.append("\t" + f + "\n");
        }
        for (org.agreement_technologies.common.map_parser.Operator o : getOperators()) {
            s.append(o + "\n");
        }
        s.append("(shared-data\n");
        for (org.agreement_technologies.common.map_parser.SharedData d : getSharedData()) {
            s.append("\t" + d + "\n");
        }
        s.append(")\n(init\n");
        for (org.agreement_technologies.common.map_parser.Fact f : getInit()) {
            s.append("\t" + f + "\n");
        }
        s.append(")\n");
        for (org.agreement_technologies.common.map_parser.Operator b : getBeliefs()) {
            s.append(b + "\n");
        }
        s.append("(global-goal\n");
        for (org.agreement_technologies.common.map_parser.Fact f : getGoals()) {
            s.append("\t" + f + "\n");
        }
        s.append(")\n");
        return s.toString();
    }

    /**
     * Displays a String array as a String
     *
     * @param v Array of string
     * @return String with the array description
     */
    private String toString(String[] v) {
        if (v.length == 0) {
            return "";
        }
        String res = v[0];
        for (int i = 1; i < v.length; i++) {
            res += " " + v[i];
        }
        return res;
    }

    private TaskTypes.NumericEffectImp[] getOperatorNumericEffects(ArrayList<OperatorCondition> eff) {
        int numConditions = 0;
        for (OperatorCondition e : eff) {
            if (e.type == OperatorConditionType.CT_INCREASE)
                numConditions++;
        }
        TaskTypes.NumericEffectImp[] res = new TaskTypes.NumericEffectImp[numConditions];
        if (numConditions == 0) return res;
        numConditions = 0;
        for (OperatorCondition e : eff) {
            if (e.type != OperatorConditionType.CT_INCREASE) continue;
            res[numConditions] = new TaskTypes.NumericEffectImp(NumericEffect.INCREASE);
            res[numConditions].var = getOperatorNumericVariable(e.var);
            res[numConditions].exp = getOperatorNumericEffectsExpression(e.exp);
            numConditions++;
        }
        return res;
    }

    private TaskTypes.FunctionImp getOperatorNumericVariable(Variable var) {
        TaskTypes.FunctionImp res = new TaskTypes.FunctionImp(var.name, false);
        int index = this.functions.indexOf(new Function(new Variable(var.name), false));
        Function fncEff = this.functions.get(index);
        res.parameters = new TaskTypes.ParameterImp[var.params.size()];
        for (int p = 0; p < var.params.size(); p++) {
            Value param = var.params.get(p);
            res.parameters[p] = new TaskTypes.ParameterImp(param.name);
            res.parameters[p].types = new String[param.types.size()];
            for (int t = 0; t < param.types.size(); t++) {
                res.parameters[p].types[t] = param.types.get(t).name;
            }
        }
        res.domain = new String[fncEff.domain.size()];
        for (int t = 0; t < res.domain.length; t++) {
            res.domain[t] = fncEff.domain.get(t).name;
        }
        return res;
    }

    private TaskTypes.NumericExpressionImp getOperatorNumericEffectsExpression(NumericExpressionImp exp) {
        int type = -1;
        switch (exp.type) {
            case NET_NUMBER:
                type = org.agreement_technologies.common.map_parser.NumericExpression.NUMBER;
                break;
            case NET_VAR:
                type = org.agreement_technologies.common.map_parser.NumericExpression.VARIABLE;
                break;
            case NET_ADD:
                type = org.agreement_technologies.common.map_parser.NumericExpression.ADD;
                break;
            case NET_DEL:
                type = org.agreement_technologies.common.map_parser.NumericExpression.DEL;
                break;
            case NET_PROD:
                type = org.agreement_technologies.common.map_parser.NumericExpression.PROD;
                break;
            case NET_DIV:
                type = org.agreement_technologies.common.map_parser.NumericExpression.DIV;
                break;
            case NET_USAGE:
                type = org.agreement_technologies.common.map_parser.NumericExpression.USAGE;
                break;
        }
        TaskTypes.NumericExpressionImp e = new TaskTypes.NumericExpressionImp(type);
        if (exp.type == NumericExpressionType.NET_NUMBER) {
            e.value = exp.value;
        } else if (exp.type == NumericExpressionType.NET_VAR) {
            e.var = getOperatorNumericVariable(exp.var);
        } else {
            e.left = getOperatorNumericEffectsExpression(exp.left);
            e.right = getOperatorNumericEffectsExpression(exp.right);
        }
        return e;
    }

    @Override
    public Congestion[] getCongestion() {
        Congestion[] res = new Congestion[congestions.size()];
        for (int i = 0; i < res.length; i++)
            res[i] = congestions.get(i);
        return res;
    }

    @Override
    public NumericFact[] getInitialNumericFacts() {
        ArrayList<NumericFact> res = new ArrayList<>();
        for (Assignment a : init)
            if (a.isNumeric) res.add(a);
        return res.toArray(new NumericFact[res.size()]);
    }


    /**
     * ***********************************************************
     */
    /**
     * I N N E R C L A S S E S                 *
     */
    /**
     * ***********************************************************
     */

    public void addPreference(String name, Assignment a, SynAnalyzer syn) throws ParseException {
        for (PreferenceImp p : preferences) {
            if (p.name.equalsIgnoreCase(name)) {
                syn.notifyError("Preference '" + name + "' redefined");
            }
        }
        preferences.add(new PreferenceImp(name, a));
    }

    @Override
    public double getSelfInterest() {
        return selfInterest;
    }

    @Override
    public double getMetricThreshold() {
        return metricThreshold;
    }

    @Override
    public Fact[] getPreferences() {
        TaskTypes.FactImp res[] = new TaskTypes.FactImp[preferences.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = getFact(preferences.get(i).goal);
        }
        return res;
    }

    @Override
    public String getPreferenceName(int index) {
        return preferences.get(index).name;
    }

    @Override
    public Metric getMetric() {
        return metric;
    }

    public enum NumericExpressionType {
        NET_NUMBER, NET_VAR,
        NET_ADD, NET_DEL, NET_PROD, NET_DIV,
        NET_USAGE
    }

    /**
     * Operator condition types
     */
    public enum OperatorConditionType {
        CT_NONE, CT_EQUAL, CT_MEMBER,
        CT_ASSIGN, CT_ADD, CT_DEL,
        CT_INCREASE
    }

    public enum CongestionUsageType {
        CUT_OR, CUT_AND, CUT_ACTION
    }

    public enum ConditionType {
        CT_EQUAL, CT_GREATER, CT_GREATER_EQ,
        CT_LESS, CT_LESS_EQ, CT_DISTINCT
    }

    /**
     * Variable type
     */
    public class Type {

        String name;                        // Name of the type
        ArrayList<Type> parentTypes;        // Parent types

        /**
         * Constructor
         *
         * @param name Name of the type
         */
        public Type(String name) {
            this.name = name;
            parentTypes = new ArrayList<Type>();
        }

        /**
         * Add a parent type to the current type
         *
         * @param parent Name of the parent type
         * @param syn    Syntactic analyzer
         * @throws ParseException if the parent type is repeated
         */
        public void addParentType(Type parent, SynAnalyzer syn) throws ParseException {
            if (parentTypes.contains(parent)) {
                syn.notifyError("Parent type '" + parent.name
                        + "' already defined for type '" + name + "'");
            }
            parentTypes.add(parent);
        }

        /**
         * Compares two types by their names
         */
        @Override
        public boolean equals(Object x) {
            return name.equals(((Type) x).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        /**
         * Checks if this type is compatible with the given type
         *
         * @param tp Given type
         * @return True if this type is compatible with the given type
         */
        public boolean isCompatible(Type tp) {
            boolean comp = equals(tp);
            if (!comp) {
                for (Type t : parentTypes) {
                    if (t.isCompatible(tp)) {
                        comp = true;
                        break;
                    }
                }
            }
            return comp;
        }

        /**
         * Checks if this type is in the given domain
         *
         * @param domain List of types
         * @return True if this type belongs to the domain
         */
        public boolean isCompatible(ArrayList<Type> domain) {
            for (Type t : domain) {
                if (this.isCompatible(t)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * PDDL objects and constants. Also used for variables in parameters
     */
    public class Value {
        String name;                // Object name
        ArrayList<Type> types;                  // Object types
        boolean isVariable;            // Variable in a parameter list

        /**
         * Constructor
         *
         * @param name Name of the object/constant
         */
        public Value(String name) {
            this.name = name;
            types = new ArrayList<Type>();
            isVariable = false;
        }

        /**
         * Compares two values by their names
         */
        @Override
        public boolean equals(Object x) {
            return name.equals(((Value) x).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        /**
         * Adds a type to this object
         *
         * @param type New type
         * @param syn  Syntactic analyzer
         * @throws ParseException If the type is redefined for this object
         */
        public void addType(Type type, SynAnalyzer syn) throws ParseException {
            if (types.contains(type)) {
                syn.notifyError("Type '" + type.name
                        + "' already defined for object '" + name + "'");
            }
            types.add(type);
        }

        /**
         * Checks if this value is compatible with (at least) one of the types
         * of the parameter
         *
         * @param param Parameter
         * @return True if this value is compatible
         */
        public boolean isCompatible(Value param) {
            boolean comp = false;
            for (Type t : types) {
                for (Type tp : param.types) {
                    if (t.isCompatible(tp)) {
                        comp = true;
                        break;
                    }
                }
                if (comp) {
                    break;
                }
            }
            return comp;
        }

        /**
         * Checks if this value is compatible with (at least) one of the types
         * of the domain
         *
         * @param domain List of types
         * @return True if this value is compatible
         */
        public boolean isCompatible(ArrayList<Type> domain) {
            boolean comp = false;
            for (Type t : types) {
                for (Type td : domain) {
                    if (t.isCompatible(td)) {
                        comp = true;
                        break;
                    }
                }
                if (comp) {
                    break;
                }
            }
            return comp;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Predicates
     */
    public class Variable implements CongestionFluent {
        String name;                // Predicate name
        ArrayList<Value> params;    // Parameters

        /**
         * Constructor
         *
         * @param name Name of the predicate
         */
        public Variable(String name) {
            this.name = name;
            params = new ArrayList<Value>();
        }

        /**
         * Compares two variables by their names
         */
        @Override
        public boolean equals(Object x) {
            return name.equalsIgnoreCase(((Variable) x).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getNumParams() {
            return params.size();
        }

        @Override
        public String getParamName(int index) {
            return params.get(index).name;
        }
    }

    /**
     * Functions
     */
    public class Function {

        Variable var;                // Variable
        ArrayList<Type> domain;        // Domain
        boolean multiFunction;        // Multi-function or function

        /**
         * Constructor
         *
         * @param v             Variable
         * @param multifunction True if this is a multi-function
         */
        public Function(Variable v, boolean multifunction) {
            this.var = v;
            this.multiFunction = multifunction;
            domain = new ArrayList<Type>();
        }

        /**
         * Compares two functions by their variable names
         */
        @Override
        public boolean equals(Object x) {
            return var.equals(((Function) x).var);
        }

        /**
         * Sets the function domain
         *
         * @param domain Function domain
         */
        public void setDomain(ArrayList<Type> domain) {
            for (Type t : domain) {
                this.domain.add(t);
            }
        }

        /**
         * Add a type to the domain
         *
         * @param syn      Syntactic token
         * @param typeName Type name
         * @throws ParseException If the type is not valid
         */
        public void addDomainType(SynAnalyzer syn, String typeName) throws ParseException {
            int typeIndex = types.indexOf(new Type(typeName));
            if (typeIndex == -1) {
                syn.notifyError("Type '" + typeName + "' undefined");
            }
            Type t = types.get(typeIndex);
            if (domain.contains(t)) {
                syn.notifyError("Type '" + typeName + "' duplicated in domain definition");
            }
            domain.add(t);
        }

        boolean isNumeric() {
            Type number = types.get(NUMBER_TYPE);
            for (Type t : domain)
                if (t.isCompatible(number)) return true;
            return false;
        }
    }

    public class NumericExpressionImp implements NumericExpression {
        NumericExpressionType type;
        double value;                       // if type == NET_NUMBER
        Variable var;                       // if type == NET_VAR (numeric variable)
        NumericExpressionImp left;
        NumericExpressionImp right;         // otherwise (operands)


        NumericExpressionImp(double value) {
            type = NumericExpressionType.NET_NUMBER;
            this.value = value;
        }

        NumericExpressionImp(Variable v) {
            type = NumericExpressionType.NET_VAR;
            this.var = v;
        }

        NumericExpressionImp(NumericExpressionType type) {
            this.type = type;
            left = right = null;
        }

        @Override
        public int getType() {
            switch (type) {
                case NET_NUMBER:
                    return NUMBER;
                case NET_VAR:
                    return VARIABLE;
                case NET_ADD:
                    return ADD;
                case NET_DEL:
                    return DEL;
                case NET_PROD:
                    return PROD;
                case NET_DIV:
                    return DIV;
                default:
                    return USAGE;
            }
        }

        @Override
        public double getValue() {
            return value;
        }

        @Override
        public org.agreement_technologies.common.map_parser.Function getNumericVariable() {
            return null;
        }

        @Override
        public NumericExpression getLeftExp() {
            return left;
        }

        @Override
        public NumericExpression getRightExp() {
            return right;
        }

        @Override
        public CongestionFluent getCongestionFluent() {
            return var;
        }
    }

    /**
     * Operator condition
     */
    public class OperatorCondition {
        OperatorConditionType type;        // Condition type
        boolean neg;                            // True if this condition is negated
        Variable var;                // Variable
        Value value;                // Value
        NumericExpressionImp exp;                  // Only for INCREASE operations

        /**
         * Constructor
         *
         * @param type Condition type
         */
        public OperatorCondition(OperatorConditionType type) {
            this.type = type;
            this.neg = false;
        }
    }

    /**
     * Operators
     */
    public class Operator {

        String name;                        // Operator name
        ArrayList<Value> params;            // Parameters
        ArrayList<OperatorCondition> prec;    // Preconditions
        ArrayList<OperatorCondition> eff;    // Effects
        int preference;                        // Preference value (-1 if no set)

        /**
         * Constructor
         *
         * @param name Name of the operator
         */
        public Operator(String name) {
            this.name = name;
            this.params = new ArrayList<Value>();
            this.prec = new ArrayList<OperatorCondition>();
            this.eff = new ArrayList<OperatorCondition>();
            this.preference = -1;
        }

        /**
         * Compares two operators by their names
         */
        @Override
        public boolean equals(Object x) {
            return name.equalsIgnoreCase(((Operator) x).name);
        }
    }

    /**
     * A variable or function to be shared with other agents
     */
    public class SharedData {

        Variable var;                // Predicate
        Function fnc;                // Function
        ArrayList<Value> agents;    // Agents that can observe the predicate/function

        /**
         * Constructor of a shared predicate
         *
         * @param var Predicate
         */
        public SharedData(Variable var) {
            this.var = var;
            this.fnc = null;
            agents = new ArrayList<Value>();
        }

        /**
         * Constructor of a shared function
         *
         * @param fnc Function
         */
        public SharedData(Function fnc) {
            this.var = null;
            this.fnc = fnc;
            agents = new ArrayList<Value>();
        }
    }

    /**
     * A variable assignment in the init or goal section
     */
    public class Assignment implements NumericFact {
        Variable var;                // Predicate if this is a literal
        Function fnc;                // Function if this is a variable
        ArrayList<Value> params;                // Predicate parameters
        ArrayList<Value> values;                // Values assigned. For literals, a true value is inserted
        boolean neg;                // True if the assignment is negated
        boolean isNumeric;                      // Only for numeric assignments
        double value;

        /**
         * Constructor of a literal
         *
         * @param var Predicate
         */
        public Assignment(Variable var, boolean neg) {
            this.var = var;
            this.fnc = null;
            params = new ArrayList<Value>();
            values = new ArrayList<Value>();
            this.neg = neg;
            isNumeric = false;
        }

        /**
         * Constructor of an assignment
         *
         * @param fnc Function
         */
        public Assignment(Function fnc, boolean neg) {
            this.var = null;
            this.fnc = fnc;
            params = new ArrayList<Value>();
            values = new ArrayList<Value>();
            this.neg = neg;
            isNumeric = false;
        }

        @Override
        public String getFunctionName() {
            return fnc.var.getName();
        }

        @Override
        public String[] getParameters() {
            String[] res = new String[this.params.size()];
            for (int i = 0; i < res.length; i++)
                res[i] = this.params.get(i).name;
            return res;
        }

        @Override
        public double getValue() {
            return value;
        }
    }

    public class PreferenceImp {

        String name;
        Assignment goal;

        public PreferenceImp(String name, Assignment a) {
            this.name = name;
            this.goal = a;
        }
    }

    public class MetricImp implements Metric {

        public static final int MT_PREFERENCE = 0;
        public static final int MT_ADD = 1;
        public static final int MT_MULT = 2;
        public static final int MT_NUMBER = 3;
        public static final int MT_TOTAL_TIME = 4;
        int metricType;
        double number;            // if metricType = MT_NUMBER
        String preference;        // if metricType = MT_PREFERENCE
        ArrayList<MetricImp> term;    // otherwise

        public MetricImp(String id, SynAnalyzer syn) throws ParseException {
            metricType = MT_PREFERENCE;
            preference = id;
            boolean found = false;
            for (PreferenceImp p : preferences) {
                if (p.name.equalsIgnoreCase(id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                syn.notifyError("Unknown preference '" + id + "' in metric");
            }
        }

        public MetricImp(Symbol sym) {
            if (sym.equals(Symbol.SS_PLUS)) {
                metricType = MT_ADD;
            } else {
                metricType = MT_MULT;
            }
            term = new ArrayList<MetricImp>();
        }

        public MetricImp(double n) {
            metricType = MT_NUMBER;
            number = n;
        }

        public MetricImp() {
            metricType = MT_TOTAL_TIME;
        }

        public int getMetricType() {
            return metricType;
        }

        public String getPreference() {
            return preference;
        }

        public double getNumber() {
            return number;
        }

        public int getNumTerms() {
            return term.size();
        }

        public MetricImp getTerm(int index) {
            return term.get(index);
        }
    }

    public class CongestionAction {
        Operator op;
        ArrayList<Value> params;

        CongestionAction(Operator op) {
            this.op = op;
            params = new ArrayList<>(op.params.size());
        }

        void addParameter(Value value) {
            params.add(value);
        }
    }

    public class CongestionUsageImp implements CongestionUsage {
        CongestionUsageType type;
        CongestionAction action;               // if type == CUT_ACTION
        ArrayList<CongestionUsageImp> cond;    // otherwise

        CongestionUsageImp(CongestionUsageType type) {
            this.type = type;
            cond = new ArrayList<>();
        }

        CongestionUsageImp(CongestionAction action) {
            type = CongestionUsageType.CUT_ACTION;
            this.action = action;
        }

        void addCondition(CongestionUsageImp condition) {
            cond.add(condition);
        }

        @Override
        public int getType() {
            switch (type) {
                case CUT_OR:
                    return OR;
                case CUT_AND:
                    return AND;
                default:
                    return ACTION;
            }
        }

        @Override
        public int numTerms() {
            return cond.size();
        }

        @Override
        public CongestionUsage getTerm(int index) {
            return cond.get(index);
        }

        @Override
        public String getActionName() {
            return action.op.name;
        }

        @Override
        public int numActionParams() {
            return action.params.size();
        }

        @Override
        public String getParamName(int paramNumber) {
            return action.params.get(paramNumber).name;
        }
    }

    public class CongestionPenaltyImp implements CongestionPenalty {
        ConditionType condition;
        double conditionValue;
        Variable incVariable;
        NumericExpressionImp increment;

        CongestionPenaltyImp(ConditionType conditionType) {
            condition = conditionType;
        }

        void setVariable(Variable var) {
            incVariable = var;
        }

        void setIncrement(NumericExpressionImp inc) {
            increment = inc;
        }

        @Override
        public int getConditionType() {
            switch (condition) {
                case CT_EQUAL:
                    return EQUAL;
                case CT_GREATER:
                    return GREATER;
                case CT_GREATER_EQ:
                    return GREATER_EQ;
                case CT_LESS:
                    return LESS;
                case CT_LESS_EQ:
                    return LESS_EQ;
                default:
                    return DISTINCT;
            }
        }

        @Override
        public CongestionFluent getIncVariable() {
            return incVariable;
        }

        @Override
        public double getConditionValue() {
            return conditionValue;
        }

        @Override
        public NumericExpression getIncExpression() {
            return increment;
        }
    }

    public class CongestionImp implements Congestion {
        String name;
        ArrayList<Value> params;
        ArrayList<Value> vars;
        CongestionUsageImp usage;
        ArrayList<CongestionPenaltyImp> penalty;

        CongestionImp(String name) {
            this.name = name;
            params = new ArrayList<>();
            vars = new ArrayList<>();
            usage = null;
            penalty = new ArrayList<>();
        }

        @Override
        public boolean equals(Object x) {
            return ((CongestionImp) x).name.equalsIgnoreCase(name);
        }

        void addParameter(Value p, SynAnalyzer syn) throws ParseException {
            if (params.contains(p) || vars.contains(p))
                syn.notifyError("Parameter '" + p.name + "' redefined");
            params.add(p);
        }

        void addVariable(Value p, SynAnalyzer syn) throws ParseException {
            if (params.contains(p) || vars.contains(p))
                syn.notifyError("Variable '" + p.name + "' redefined");
            vars.add(p);
        }

        Value getParamOrVar(String name) {
            for (Value v : params)
                if (v.name.equalsIgnoreCase(name)) return v;
            for (Value v : vars)
                if (v.name.equalsIgnoreCase(name)) return v;
            return null;
        }

        void addPenalty(CongestionPenaltyImp p) {
            penalty.add(p);
        }

        @Override
        public int getNumParams() {
            return params.size();
        }

        @Override
        public String[] getParamTypes(int paramNumber) {
            Value v = params.get(paramNumber);
            String[] types = new String[v.types.size()];
            for (int i = 0; i < types.length; i++)
                types[i] = v.types.get(i).name;
            return types;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String[] getVariableNames() {
            String varNames[] = new String[vars.size()];
            for (int i = 0; i < vars.size(); i++)
                varNames[i] = vars.get(i).name;
            return varNames;
        }

        @Override
        public String[] getVarTypes(int varNumber) {
            Value v = vars.get(varNumber);
            String[] types = new String[v.types.size()];
            for (int i = 0; i < types.length; i++)
                types[i] = v.types.get(i).name;
            return types;
        }

        @Override
        public CongestionUsage getUsage() {
            return usage;
        }

        @Override
        public int getParamIndex(String paramName) {
            int index = -1;
            for (int i = 0; i < params.size(); i++)
                if (params.get(i).name.equalsIgnoreCase(paramName)) {
                    index = i;
                    break;
                }
            return index;
        }

        @Override
        public int getNumPenalties() {
            return penalty.size();
        }

        @Override
        public CongestionPenalty getPenalty(int index) {
            return penalty.get(index);
        }
    }
}
