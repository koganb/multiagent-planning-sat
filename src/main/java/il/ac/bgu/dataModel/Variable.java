package il.ac.bgu.dataModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.lang.String.format;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Variable implements Formattable {

    public enum SpecialState {
        LOCKED_FOR_UPDATE,  // used in delay failure model to lock action effects
        // variable in this state cannot be neither in preconditions nor in effects of other actions
        // (not conditioned on and not updated)

        FREEZED,            // used in delay failure model to freeze action preconditions
        // variable in this state cannot be in effects of other actions
        // (can be conditioned but not updated)

        IN_CONFLICT_RETRY   // used in retry conflict model to signal that variable will be updated in next stages by retry
        // the variable in this state cannot be in effects of other actions

    }

    //public static final String LOCKED_FOR_UPDATE = "LOCKED_FOR_UPDATE";
    //public static final String FREEZED = "FREEZED";


    @Nullable
    private Integer stage;
    private String functionKey;
    private String functionValue;

    private Variable(POPPrecEff eff, @Nullable Integer stage) {
        this(eff);
        this.stage = stage;
    }

    private Variable(POPPrecEff eff, String functionValue, Integer stage) {
        this(eff, stage);
        this.functionValue = functionValue;
    }

    private Variable(POPPrecEff eff) {
        this.functionKey = eff.getFunction().toKey();
        this.functionValue = eff.getValue();
    }

    public static Variable of(POPPrecEff eff) {
        return new Variable(eff);
    }

    public static Variable of(POPPrecEff eff, Integer stage) {
        return new Variable(eff, stage);
    }

    public static Variable of(POPPrecEff eff, String functionValue, Integer stage) {
        return new Variable(eff, functionValue, stage);
    }


    public String formatFunctionKey() {
        return functionKey.replace(" ", "~");
    }

    public String formatFunctionKeyWithValue() {
        return format("%s=%s", formatFunctionKey(), functionValue);
    }

    public String formatData() {
        return format("Stage:%02d, State:%s", stage, formatFunctionKeyWithValue());
    }

    @Override
    public String getValue() {
        return functionValue;
    }

    public Optional<Integer> getStage() {
        return Optional.ofNullable(stage);
    }

    @Override
    public String toString() {
        return formatData();
    }
}
