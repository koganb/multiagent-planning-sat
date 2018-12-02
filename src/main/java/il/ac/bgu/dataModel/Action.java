package il.ac.bgu.dataModel;

import com.google.errorprone.annotations.Immutable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static java.lang.String.format;

@EqualsAndHashCode(of = "actionDataFormatted")
@Immutable
public class Action implements Formattable {

    private final String actionName;
    private final String agentName;
    private final Integer stage;
    private final State state;

    private final String actionDataFormatted;
    private final String actionKeyFormatted;
    private final String actionNameFormatted;

    private Action(Step step, Integer stage, State state) {
        this(step.getActionName(), step.getAgent(), stage, state);
    }

    private Action(Step step, Integer stage) {
        this(step.getActionName(), step.getAgent(), stage, null);
    }

    @Builder(toBuilder = true)
    private Action(String actionName, String agentName, Integer stage, State state) {
        assert StringUtils.isNotEmpty(actionName) &&
                stage != null && stage >= -1 &&
                (stage == -1 || StringUtils.isNotEmpty(agentName)) //no agent name for initial state
                : String.format("params: actionName: %s, agentName:%s, stage %s", actionName, agentName, stage);

        this.actionName = actionName;
        this.agentName = agentName;
        this.stage = stage;
        this.state = state;


        this.actionNameFormatted = actionName.replace(" ", "~");
        this.actionKeyFormatted = format("Index:%02d, Agent:%s,Action:%s", stage, agentName, formatActionName());
        this.actionDataFormatted = Optional.ofNullable(state)
                .map(st -> String.format("%s=%s", actionKeyFormatted, st))
                .orElse(actionKeyFormatted);

    }



    public static Action of(Step step, Integer stage) {
        return new Action(step, stage);
    }

    public static Action of(Step step, Integer stage, State state) {
        return new Action(step, stage, state);
    }

    public static Action of(String actionName, String agentName, Integer stage) {
        return new Action(actionName, agentName, stage, null);
    }

    public static Action of(String actionName, String agentName, Integer stage, State state) {
        return new Action(actionName, agentName, stage, state);
    }

    private String formatActionName() {
        return this.actionNameFormatted;
    }


    @Override
    public String formatData() {
        return actionDataFormatted;
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
        return actionKeyFormatted;
    }

    public enum State {HEALTHY, FAILED, CONDITIONS_NOT_MET}
}
