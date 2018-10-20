package il.ac.bgu.dataModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static java.lang.String.format;

@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode
public class Action implements Formattable {

    private final String agentName;
    private String actionName;
    private Integer stage;
    private State state;

    private Action(Step step, Integer stage, State state) {
        this(step, stage);
        this.state = state;
    }


    private Action(Step step, Integer stage) {
        this(step.getActionName(), step.getAgent(), stage);
    }

    private Action(String actionName, String agentName, Integer stage) {
        assert StringUtils.isNotEmpty(actionName) &&
                stage != null && stage >= -1 &&
                (stage == -1 || StringUtils.isNotEmpty(agentName)) //no agent name for initial state
                : String.format("params: actionName: %s, agentName:%s, stage %s", actionName, agentName, stage);

        this.actionName = actionName;
        this.agentName = agentName;
        this.stage = stage;
    }


    public static Action of(Step step, Integer stage) {
        return new Action(step, stage);
    }

    public static Action of(Step step, Integer stage, State state) {
        return new Action(step, stage, state);
    }

    public static Action of(String actionName, String agentName, Integer stage) {
        return new Action(actionName, agentName, stage);
    }

    public String formatActionName() {
        return actionName.replace(" ", "~");
    }


    @Override
    public String formatData() {
        String actionKey = formatFunctionKey();
        return Optional.ofNullable(state).isPresent() ? actionKey + "=" + state.name() : actionKey;
    }

    @Override
    public String toString() {
        return formatData();
    }

    @Override
    public String getValue() {
        return Optional.ofNullable(state).map(Action.State::name).orElse("<NOT_DEF>");
    }

    @Override
    public String formatFunctionKey() {
        return format("Index:%02d, Agent:%s,Action:%s", stage, agentName, formatActionName());
    }

    public enum State {HEALTHY, FAILED, CONDITIONS_NOT_MET;}
}
