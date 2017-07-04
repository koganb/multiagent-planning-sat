package org.agreement_technologies.service.map_grounding;

import org.agreement_technologies.common.map_grounding.*;

import java.util.ArrayList;

/**
 * @author Oscar
 */
public class GroundedCongestionImp implements GroundedCongestion {
    String name;
    ArrayList<Integer> paramIndexes;
    ArrayList<String> paramNames;
    ArrayList<Variable> variables;
    Usage usage;
    ArrayList<GroundedCongestionPenalty> penalty;

    GroundedCongestionImp(String name, ArrayList<Integer> params, GroundedTaskImp gTask) {
        this.name = name;
        paramIndexes = params;
        paramNames = new ArrayList<>(params.size());
        for (Integer objIndex : params)
            paramNames.add(gTask.objects.get(objIndex));
        variables = new ArrayList<>();
        penalty = new ArrayList<>();
    }

    @Override
    public String toString() {
        String res = getFullName();
        res += "\n(:usage " + usage.toString() + ")";
        for (GroundedCongestionPenalty p : penalty)
            res += "\n(:penalty " + p.toString() + ")";
        return res;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullName() {
        String res = name;
        for (String p : paramNames)
            res += " " + p;
        return res;
    }

    @Override
    public int getNumParameters() {
        return paramNames.size();
    }

    @Override
    public ArrayList<String> getParameters() {
        return paramNames;
    }

    void addVariable(String varName, String[] types) {
        variables.add(new Variable(varName, types));
    }

    private Variable getVariable(String varName) {
        for (Variable v : variables)
            if (v.name.equalsIgnoreCase(varName))
                return v;
        return null;
    }

    @Override
    public GroundedCongestionUsage getUsage() {
        return usage;
    }

    void setUsage(Usage usage) {
        this.usage = usage;
    }

    void addPenalty(Penalty p) {
        penalty.add(p);
    }

    @Override
    public ArrayList<GroundedCongestionPenalty> getPenalty() {
        return penalty;
    }

    static class Variable {
        String name;
        String types[];

        private Variable(String varName, String[] types) {
            name = varName;
            this.types = types;
        }

        @Override
        public String toString() {
            String res = name + " -";
            for (String t : types)
                res += " " + t;
            return res;
        }
    }

    static class ActionParameter {
        boolean isObject;
        String objectName;  // if isObject
        Variable var;       // if !isObject

        private ActionParameter(String objName) {
            isObject = true;
            objectName = objName;
        }

        private ActionParameter(Variable var) {
            isObject = false;
            this.var = var;
        }

        @Override
        public String toString() {
            return isObject ? objectName : var.toString();
        }
    }

    static class Action {
        String name;
        ArrayList<ActionParameter> params;

        private Action(String actionName) {
            name = actionName;
            params = new ArrayList<>();
        }

        private void addParameter(String paramName, GroundedCongestionImp congestion) {
            Variable var = congestion.getVariable(paramName);
            if (var == null) params.add(new ActionParameter(paramName));
            else params.add(new ActionParameter(var));
        }

        @Override
        public String toString() {
            String res = name;
            for (ActionParameter p : params)
                res += " " + p.toString();
            return res;
        }
    }

    static class Usage implements GroundedCongestionUsage {
        int type;
        ArrayList<Usage> terms;     // if type == OR or type == AND
        Action action;              // if type == ACTION

        Usage(int type) {
            this.type = type;
            if (type != GroundedCongestionUsage.ACTION)
                terms = new ArrayList<>();
        }

        void addTerm(Usage term) {
            terms.add(term);
        }

        void addActionParameter(String paramName, GroundedCongestionImp congestion) {
            action.addParameter(paramName, congestion);
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public int getNumTerms() {
            return terms.size();
        }

        @Override
        public GroundedCongestionUsage getTerm(int termNumber) {
            return terms.get(termNumber);
        }

        @Override
        public String getActionName() {
            return action.name;
        }

        void setActionName(String actionName) {
            action = new Action(actionName);
        }

        @Override
        public int getNumActionParameters() {
            return action.params.size();
        }

        @Override
        public boolean actionParameterIsVariable(int paramIndex) {
            return !action.params.get(paramIndex).isObject;
        }

        @Override
        public String[] actionParameterTypes(int paramIndex) {
            return action.params.get(paramIndex).var.types;
        }

        @Override
        public String actionParameterObject(int paramIndex) {
            return action.params.get(paramIndex).objectName;
        }

        @Override
        public String toString() {
            switch (type) {
                case OR:
                case AND:
                    String res = "(" + (type == OR ? "OR" : "AND");
                    for (Usage u : terms)
                        res += " (" + u.toString() + ")";
                    return res + ")";
                default:
                    return action.toString();
            }
        }
    }

    static class Fluent implements GroundedCongestionFluent {
        String name;
        ArrayList<ActionParameter> params;

        Fluent(String name, int numParams) {
            this.name = name;
            params = new ArrayList<>(numParams);
        }

        void addParameter(String paramName, GroundedCongestionImp congestion) {
            Variable var = congestion.getVariable(paramName);
            if (var == null) params.add(new ActionParameter(paramName));
            else params.add(new ActionParameter(var));
        }

        @Override
        public String toString() {
            String res = name;
            for (ActionParameter p : params)
                res += " " + p.toString();
            return res;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getNumParameters() {
            return params.size();
        }

        @Override
        public boolean parameterIsVariable(int paramIndex) {
            return !params.get(paramIndex).isObject;
        }

        @Override
        public String[] parameterTypes(int paramIndex) {
            return params.get(paramIndex).var.types;
        }

        @Override
        public String parameterObject(int paramIndex) {
            return params.get(paramIndex).objectName;
        }
    }

    static class Penalty implements GroundedCongestionPenalty {
        private static final String[] condNames = {"=", ">", ">=", "<", "<=", "!="};
        int condition;
        double conditionValue;
        Fluent incVar;
        GroundedNumericExpression incExp;

        Penalty(int condition, double conditionValue, Fluent incVar, GroundedNumericExpression incExp) {
            this.condition = condition;
            this.conditionValue = conditionValue;
            this.incVar = incVar;
            this.incExp = incExp;
        }

        @Override
        public String toString() {
            return "(when (" + condNames[condition] + " (usage) " + conditionValue +
                    ") (increase " + incVar + " " + incExp + "))";
        }

        @Override
        public int getCondition() {
            return condition;
        }

        @Override
        public double getConditionValue() {
            return conditionValue;
        }

        @Override
        public GroundedCongestionFluent getIncVariable() {
            return incVar;
        }

        @Override
        public GroundedNumericExpression getIncExpression() {
            return incExp;
        }
    }
}
