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

    public static final String LOCKED_FOR_UPDATE = "LOCKED_FOR_UPDATE";

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
