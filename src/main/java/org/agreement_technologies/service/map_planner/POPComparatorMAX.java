package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.OpenCondition;

import java.util.Comparator;

/**
 * Precondition comparator; extracts first the precondition marked as goals, then the ones that can only be solved y the planner agent,
 * then the ones that have a lower number of producers, and finally the most costly one in the dis-RPG
 *
 * @author Alex
 */
public class POPComparatorMAX implements Comparator<OpenCondition> {
    public int compare(OpenCondition op1, OpenCondition op2) {
        //Criterio #1: extraer primero la precondición que esté marcada como meta
        POPOpenCondition p1 = (POPOpenCondition) op1;
        POPOpenCondition p2 = (POPOpenCondition) op2;
        if (!p1.isGoal() && p2.isGoal()) {
            return 1;
        } else if (p1.isGoal() && !p2.isGoal()) {
            return -1;
        } else {
            return -1;/*
            //Criterio #2: extraer primero aquellas precondiciones resolubles únicamente por el agente planificador y no por el resto
            if (p1.getCondition().getAgents().size() > 1 && p2.getCondition().getAgents().size() == 1) {
                return 1;
            }
            else if (p1.getCondition().getAgents().size() == 1 && p2.getCondition().getAgents().size() > 1) {
                return -1;
            }
            else {
                //Criterio #3: extraer la precondición que produzca el menor número de ramificaciones
                if (p1.getCondition().getProducers() > p2.getCondition().getProducers()) return 1;
                else if (p1.getCondition().getProducers() < p2.getCondition().getProducers()) return -1;
                else {
                    //Criterio #4: extraer la precondición de mayor coste en el RPG
                    //Revertimos la comparación, dado que queremos extraer siempre la precondición más costosa
                    if (p1.getCondition().getMinTime() >  p2.getCondition().getMinTime())       return -1;
                    else if (p1.getCondition().getMinTime() <  p2.getCondition().getMinTime())  return 1;
                }
                return 0;
            }*/
        }
    }
}
