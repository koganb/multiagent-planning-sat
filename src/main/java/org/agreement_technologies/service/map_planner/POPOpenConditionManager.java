package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.OpenCondition;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Precondition manager interface; stores and managaes the open conditions of a plan.
 *
 * @author Alex
 */
public interface POPOpenConditionManager {
    //Extrae la siguiente precondición
    POPOpenCondition getNextOpenCondition();

    //Consulta la siguiente precondición
    POPOpenCondition checkNextOpenCondition();

    //Añade las precondiciones del plan inicial al gestor
    void addInitialOpenConditions(ArrayList<OpenCondition> precs);

    //Añade las nuevas precondiciones al plan sucesor
    void addOpenConditions(ArrayList<OpenCondition> precs);

    //Limpia las precondiciones del plan
    void clearOpenConditions();

    //Limpia las precondiciones abiertas y restaura las correspondientes al plan base
    void restoreOpenConditions();

    //Obtiene un iterador sobre las precondiciones abiertas
    Iterator<OpenCondition> getIterator();

    //Devuelve el número de precondiciones abiertas del plan
    int size();

    //Devuelve un arraylist con las precondiciones abiertas del plan
    ArrayList<OpenCondition> getList();

    //Reordena las precondiciones abiertas por seguridad
    void update();

    //Obtiene la lista de precondiciones abiertas;
    OpenCondition[] getOpenConditions();
}
