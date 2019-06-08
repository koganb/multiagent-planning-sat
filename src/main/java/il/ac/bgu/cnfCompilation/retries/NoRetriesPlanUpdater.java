package il.ac.bgu.cnfCompilation.retries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import il.ac.bgu.plan.PlanAction;

import java.util.Map;

/**
 * Created by borisk on 11/22/2018.
 */
public class NoRetriesPlanUpdater implements RetryPlanUpdater {

    @Override
    public RetriesPlanCreatorResult updatePlan(Map<Integer, ImmutableList<PlanAction>> originalPlan) {
        return new RetriesPlanCreatorResult(originalPlan, ImmutableMap.of());
    }

    @Override
    public String getName() {
        return "no retries";
    }
}
