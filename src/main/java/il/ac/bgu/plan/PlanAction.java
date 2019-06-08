package il.ac.bgu.plan;

import com.google.common.collect.ImmutableList;
import il.ac.bgu.dataModel.Variable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = {"agentName", "index", "actionName"})
public class PlanAction {
    public enum StepType {NORMAL, RETRIED}

    private String agentName;
    private Integer index;
    private String actionName;
    private StepType stepType = StepType.NORMAL;
    private ImmutableList<Variable> preconditions;
    private ImmutableList<Variable> effects;

    public PlanAction(String agentName, Integer index, String actionName) {
        this.agentName = agentName;
        this.index = index;
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return actionName;
    }
}
