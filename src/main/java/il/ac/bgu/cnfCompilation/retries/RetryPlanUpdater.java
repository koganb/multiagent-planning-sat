package il.ac.bgu.cnfCompilation.retries;

import il.ac.bgu.cnfClausesModel.NamedModel;
import il.ac.bgu.dataModel.Action;
import lombok.AllArgsConstructor;
import org.agreement_technologies.common.map_planner.Step;

import java.util.Map;
import java.util.Set;

/**
 * Created by borisk on 11/22/2018.
 */
public interface RetryPlanUpdater extends NamedModel {

    OneRetryPlanUpdater.RetriesPlanCreatorResult updatePlan(Map<Integer, Set<Step>> originalPlan);

    @AllArgsConstructor
    class RetriesPlanCreatorResult {
        public final Map<Integer, Set<Step>> updatedPlan;

        public final Map<Action, Action> actionDependencyMap; //unused
    }
}
