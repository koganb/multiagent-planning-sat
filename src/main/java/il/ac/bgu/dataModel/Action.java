package il.ac.bgu.dataModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.agreement_technologies.common.map_planner.Step;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;

import static java.lang.String.format;

@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode
public class Action implements Formattable {

    private final String uuid;
    private final String agentName;
    private String actionName;
    private Integer stage;
    private State state;

    private Action(Step step, Integer stage, State state) {
        this(step, stage);
        this.state = state;
    }


    private Action(Step step, Integer stage) {
        this(step.getActionName(), step.getAgent(), step.getUuid(), stage);
    }

    private Action(String actionName, String agentName, String uuid, Integer stage) {
        this.actionName = actionName;
        this.agentName = agentName;
        this.uuid = uuid;
        this.stage = stage;
    }


    public static Action of(Step step, Integer stage) {
        return new Action(step, stage);
    }

    public static Action of(Step step, Integer stage, State state) {
        return new Action(step, stage, state);
    }

    public static Action of(String actionName, String agentName, String uuid, Integer stage) {
        return new Action(actionName, agentName, uuid, stage);
    }

    public String formatActionName() {
        return actionName.replace(" ", "~");
    }

    @Override
    public String formatData() {
        String format = format("Index:%02d, Agent:%s,Action:%s", stage, agentName, formatActionName());
        return Optional.ofNullable(state).isPresent() ? format + "=" + state.name() : format;
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
        throw new NotImplementedException("implemented for Values only");
    }

    public enum State {HEALTHY, FAILED, CONDITIONS_NOT_MET;}
}
