package il.ac.bgu.cnfCompilation.retries

import com.google.common.collect.ImmutableMap
import il.ac.bgu.TestUtils
import il.ac.bgu.dataModel.Action
import spock.lang.Specification

import static il.ac.bgu.dataModel.Action.State.CONDITIONS_NOT_MET

/**
 * Created by borisk on 11/22/2018. 
 */
class TestOneRetryPlanUpdater extends Specification {
    def "test retries plan and action dependency are correctly created"() {
        setup:
        def plan = TestUtils.loadPlan("satellite8.problem")
        def retriesPlanCreator = new OneRetryPlanUpdater(plan)

        when:
        def retriesPlanCreatorResult = retriesPlanCreator.updatePlan()


        then:
        retriesPlanCreatorResult.actionDependencyMap == ImmutableMap.of(
                Action.of("take_image satellite1 phenomenon14 instrument5 thermograph2", "satellite1", 11),
                Action.of("take_image satellite1 phenomenon14 instrument5 thermograph2", "satellite1", 10, CONDITIONS_NOT_MET)
        )

    }

}