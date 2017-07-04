package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_grounding.GroundedVar;

import java.util.ArrayList;

/**
 * Defines the POP variables; implements the Function interface.
 *
 * @author Alex
 */
//Variables para el POP; sustituye a la antigua clase PDDLLiteral
public class POPFunction {
    //Parámetros: referencia a la variable original, nombre, parámetros, lista de tipos, valor verdadero inicial, valores falsos iniciales, valores alcanzables
    private GroundedVar var;
    private String name;
    private ArrayList<String> params;
    private ArrayList<String> types;
    private String initialTrueValue;
    private ArrayList<String> initialFalseValues;
    private ArrayList<String> reachableValues;
    private String key;

    public POPFunction(GroundedVar variable) {
        int i;
        this.key = null;
        this.var = variable;

        this.name = var.getFuctionName();
        this.initialTrueValue = var.initialTrueValue();

        this.params = new ArrayList<String>(var.getParams().length);
        for (i = 0; i < var.getParams().length; i++) this.params.add(var.getParams()[i]);

        this.types = new ArrayList<String>(var.getDomainTypes().length);
        for (i = 0; i < var.getDomainTypes().length; i++) this.types.add(var.getDomainTypes()[i]);

        this.initialFalseValues = new ArrayList<String>(var.initialFalseValues().length);
        for (i = 0; i < var.initialFalseValues().length; i++) this.initialFalseValues.add(var.initialFalseValues()[i]);

        this.reachableValues = new ArrayList<String>(var.getReachableValues().length);
        for (i = 0; i < var.getReachableValues().length; i++) this.reachableValues.add(var.getReachableValues()[i]);
    }

    public GroundedVar getVariable() {
        return this.var;
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<String> getParams() {
        return this.params;
    }

    public ArrayList<String> getTypes() {
        return this.types;
    }

    public String getInitialTrueValue() {
        return this.initialTrueValue;
    }

    public ArrayList<String> getInitialFalseValues() {
        return this.initialFalseValues;
    }

    public ArrayList<String> getReachableValues() {
        return this.reachableValues;
    }

    public String toKey() {
        if (this.key == null) {
            key = name;
            for (String param : params)
                key += " " + param;
        }

        return this.key;
    }

    public String toString() {
        String res = name;
        for (String param : params)
            res += " " + param;
        return res;
    }
}
