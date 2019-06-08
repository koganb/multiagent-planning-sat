package il.ac.bgu.cnfCompilation.retries;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.cnfClausesModel.NamedModel;
import il.ac.bgu.dataModel.Action;
import il.ac.bgu.plan.PlanAction;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Created by borisk on 11/22/2018.
 */
public interface RetryPlanUpdater extends NamedModel {

    OneRetryPlanUpdater.RetriesPlanCreatorResult updatePlan(Map<Integer, ImmutableList<PlanAction>> originalPlan);

    @AllArgsConstructor
    class RetriesPlanCreatorResult {
        public final Map<Integer, ImmutableList<PlanAction>> updatedPlan;

        public final Map<Action, Action> actionDependencyMap; //unused
    }
}
