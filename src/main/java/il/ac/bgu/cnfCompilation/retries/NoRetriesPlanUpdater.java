package il.ac.bgu.cnfCompilation.retries;

import com.google.common.collect.ImmutableMap;
import org.agreement_technologies.common.map_planner.Step;

import java.util.Map;
import java.util.Set;

/**
 * Created by borisk on 11/22/2018.
 */
public class NoRetriesPlanUpdater implements RetryPlanUpdater {

    @Override
    public RetriesPlanCreatorResult updatePlan(Map<Integer, Set<Step>> originalPlan) {
        return new RetriesPlanCreatorResult(originalPlan, ImmutableMap.of());
    }

    @Override
    public String getName() {
        return "no retries";
    }
}
