package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.PlannerFactory;

//Interfaz para métodos que comprueban si un plan dado es solución
public interface SolutionChecker {

    Boolean isSolution(POPIncrementalPlan candidate, PlannerFactory pf);

    //Comprueba si el plan cumple restricciones adicionales impuestas (como no superar un límite de pasos fijado a priori)
    Boolean keepsConstraints(POPInternalPlan incrementalCandidate, POPStep step);
}
