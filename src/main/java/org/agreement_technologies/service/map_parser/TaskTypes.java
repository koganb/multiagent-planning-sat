package org.agreement_technologies.service.map_parser;

import org.agreement_technologies.common.map_parser.*;

/**
 * Ungrounded task types
 *
 * @author Oscar Sapena
 * @since Mars 2011
 */
public class TaskTypes {

    /**
     * A parameter represents a name with a list of types
     */
    public static class ParameterImp implements Parameter {

        String name;
        String[] types;

        /**
         * Constructor of a parameter
         *
         * @param name Parameter name
         */
        public ParameterImp(String name) {
            this.name = name;
        }

        // Gets the parameter name
        @Override
        public String getName() {
            return name;
        }

        // Return the parameter type list
        @Override
        public String[] getTypes() {
            return types;
        }

        // Returns a description of this parameter
        public String toString() {
            String res = name;
            if (types.length > 0) {
                res += " -";
                for (String t : types) {
                    res += " " + t;
                }
            }
            return res;
        }
    }

    /**
     * Ungrounded function
     */
    public static class FunctionImp implements Function {

        String name;
        boolean multifunction;
        ParameterImp[] parameters;
        String[] domain;

        /**
         * Constructor of an ungrounded function
         *
         * @param name          Function name
         * @param multifunction True if this is a multi-function
         */
        public FunctionImp(String name, boolean multifunction) {
            this.name = name;
            this.multifunction = multifunction;
        }

        // Retrieves the function name
        @Override
        public String getName() {
            return name;
        }

        // Returns the function parameters
        @Override
        public Parameter[] getParameters() {
            return parameters;
        }

        // Returns the function domain
        @Override
        public String[] getDomain() {
            return domain;
        }

        // Checks if this is a multi-function
        @Override
        public boolean isMultifunction() {
            return multifunction;
        }

        // Returns a description of this function
        public String toString() {
            String res = "((" + name;
            for (Parameter p : parameters) {
                res += " " + p;
            }
            res += ")";
            if (domain.length > 0) {
                res += " -";
                for (String d : domain) {
                    res += " " + d;
                }
            }
            if (multifunction) {
                res += "*";
            }
            return res + ")";
        }
    }

    /**
     * Ungrounded condition
     */
    public static class ConditionImp implements Condition {
        private static final String[] typeName = {"=", "<>", "member",
                "not member", "assign", "add", "del"};
        int type;
        FunctionImp fnc;
        String value;

        // Returns the condition type
        @Override
        public int getType() {
            return type;
        }

        // Returns the condition function
        @Override
        public Function getFunction() {
            return fnc;
        }

        // Returns the condition value
        @Override
        public String getValue() {
            return value;
        }

        // Returns a description of this condition
        @Override
        public String toString() {
            return "(" + typeName[type] + " " + fnc + " " + value + ")";
        }
    }

    public static class NumericExpressionImp implements NumericExpression {
        int type;
        double value;
        FunctionImp var;
        NumericExpressionImp left, right;

        NumericExpressionImp(int type) {
            this.type = type;
        }

        @Override
        public String toString() {
            String res;
            switch (type) {
                case NUMBER:
                    res = "" + value;
                    break;
                case VARIABLE:
                    res = var.toString();
                    break;
                case USAGE:
                    res = "(usage)";
                    break;
                case ADD:
                    res = "(+ " + left + " " + right + ")";
                    break;
                case DEL:
                    res = "(- " + left + " " + right + ")";
                    break;
                case PROD:
                    res = "(* " + left + " " + right + ")";
                    break;
                case DIV:
                    res = "(/ " + left + " " + right + ")";
                    break;
                default:
                    res = "<error>";
            }
            return res;
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
        public Function getNumericVariable() {
            return var;
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
            return null;
        }
    }

    public static class NumericEffectImp implements NumericEffect {
        private static final String[] typeName = {"increase"};
        int type;
        FunctionImp var;
        NumericExpressionImp exp;

        NumericEffectImp(int type) {
            this.type = type;
        }

        // Returns a description of this effect
        @Override
        public String toString() {
            return "(" + typeName[type] + " " + var + " " + exp + ")";
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public Function getNumericVariable() {
            return var;
        }

        @Override
        public NumericExpression getNumericExpression() {
            return exp;
        }
    }

    /**
     * Operator (ungrounded action)
     */
    public static class OperatorImp implements Operator {
        String name;
        ParameterImp[] parameters;
        ConditionImp[] prec, eff;
        NumericEffectImp[] numEff;
        int preference;

        /**
         * Constructor of an operator
         */
        public OperatorImp(String name, int preference) {
            this.name = name;
            this.preference = preference;
        }

        // Returns the operator name
        @Override
        public String getName() {
            return name;
        }

        // Returns the operator parameters
        @Override
        public Parameter[] getParameters() {
            return parameters;
        }

        // Get the operator precondition (list of conditions)
        @Override
        public Condition[] getPrecondition() {
            return prec;
        }

        // Get the operator effect (list of effects)
        @Override
        public Condition[] getEffect() {
            return eff;
        }

        // Returns a description of this operator
        public String toString() {
            String res = "(" + name;
            if (parameters.length > 0) {
                res += "(";
                for (int i = 0; i < parameters.length; i++) {
                    if (i == 0) {
                        res += parameters[i];
                    } else {
                        res += " " + parameters[i];
                    }
                }
                res += ")";
            }
            if (prec.length > 0) {
                res += "\n\tPrec: (and";
                for (Condition c : prec) {
                    res += " " + c;
                }
                res += ")";
            }
            if (eff.length > 0) {
                res += "\n\tEff: (and";
                for (Condition c : eff) {
                    res += " " + c;
                }
                res += ")";
            }
            return res;
        }

        // Returns the preference value. Returns -1 if it is not set
        @Override
        public int getPreferenceValue() {
            return preference;
        }

        @Override
        public NumericEffect[] getNumericEffects() {
            return numEff;
        }
    }

    /**
     * Ungrounded shared data
     */
    public static class SharedDataImp implements SharedData {

        FunctionImp fnc;
        String[] agents;

        // Returns the function
        @Override
        public Function getFunction() {
            return fnc;
        }

        // Get the list of agents that can observe this function
        @Override
        public String[] getAgents() {
            return agents;
        }

        // Returns a description of this shared data
        public String toString() {
            String res = "(" + fnc + " -";
            for (String ag : agents) {
                res += " " + ag;
            }
            return res + ")";
        }
    }

    /**
     * Ungrounded fact
     */
    public static class FactImp implements Fact {

        String name;
        String parameters[];
        String values[];
        boolean neg;

        // Constructor
        public FactImp(String name, boolean neg) {
            this.name = name;
            this.neg = neg;
        }

        // Returns the function name
        @Override
        public String getFunctionName() {
            return name;
        }

        // Returns the function parameters
        @Override
        public String[] getParameters() {
            return parameters;
        }

        // Returns the list of values assigned to the function
        @Override
        public String[] getValues() {
            return values;
        }

        // Returns a description of this fact
        public String toString() {
            String res;
            if (neg) {
                res = "(not (= (" + name;
            } else {
                res = "(= (" + name;
            }
            for (String p : parameters) {
                res += " " + p;
            }
            res += ") ";
            if (values.length != 1) {
                res += "{";
            }
            for (int i = 0; i < values.length; i++) {
                if (i == 0) {
                    res += values[i];
                } else {
                    res += " " + values[i];
                }
            }
            if (values.length != 1) {
                res += "}";
            }
            if (neg) {
                res += ")";
            }
            return res + ")";
        }

        // Checks whether the assignment is negated
        @Override
        public boolean negated() {
            return neg;
        }
    }
}
