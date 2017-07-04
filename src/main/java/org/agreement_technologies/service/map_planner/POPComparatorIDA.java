package org.agreement_technologies.service.map_planner;

import java.util.Comparator;

/**
 * Plan comparator for the IDA* search method
 *
 * @author Alex
 */
public class POPComparatorIDA implements Comparator<POPIncrementalPlan> {
    public int compare(POPIncrementalPlan p1, POPIncrementalPlan p2) {
        //Si aplicamos una búsqueda IDA modificada, el criterio de comparación lo marca f(n) = g(n) + h(n)
        //Ordenamos del revés para que primero se extraigan los planes de mayor coste de la cola de sucesores
        //De este modo, en la pila se extraen primero los planes hermanos de mejor f(n) = g(n) + h(n)
        if ((p1.getG() + p1.getH()) < (p2.getG() + p2.getH())) return 1;
        else if ((p1.getG() + p1.getH()) > (p2.getG() + p2.getH())) return -1;
        return 0;
    }
}
