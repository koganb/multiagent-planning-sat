package il.ac.bgu.dataModel;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@EqualsAndHashCode(of = "functionDataFormatted")
public class Variable implements Formattable {


    public enum SpecialState {
        LOCKED_FOR_UPDATE,  // used in delay failure model to lock action effects
        // variable in this state cannot be neither in preconditions nor in effects of other actions
        // (not conditioned on and not updated)

        FREEZED,            // used in delay failure model to freeze action preconditions
        // variable in this state cannot be in effects of other actions
        // (can be conditioned but not updated)
    }



    @Nullable
    private Integer stage;
    private String functionKey;
    private String functionValue;


    //cached formatted keys
    private String functionKeyFormatted;
    private String functionKeyWithValueFormatted;
    private String functionDataFormatted;


    private Variable(POPPrecEff eff, @Nullable Integer stage) {
        this(eff);
        this.stage = stage;
        postConstruct();
    }

    private Variable(POPPrecEff eff, String functionValue, @Nullable Integer stage) {
        this(eff, stage);
        this.functionValue = functionValue;
        postConstruct();
    }

    @Builder(toBuilder = true)
    private Variable(String functionKey, String functionValue, @Nullable Integer stage) {
        this(functionKey, functionValue);
        this.stage = stage;
        postConstruct();
    }

    private Variable(String functionKey, String functionValue) {
        this.functionKey = functionKey;
        this.functionValue = functionValue;
        postConstruct();
    }

    private Variable(POPPrecEff eff) {
        this.functionKey = createFunctionKey(eff);
        this.functionValue = eff.getValue();
        postConstruct();
    }

    public static Variable of(Variable variable, Integer stage) {
        return new Variable(variable.functionKey, variable.getValue(), stage);
    }

    public static Variable of(Variable variable, String functionValue, Integer stage) {
        return new Variable(variable.functionKey, functionValue, stage);
    }

    public static Variable of(String functionKey, String functionValue) {
        return new Variable(functionKey, functionValue);
    }

    public static Variable of(String functionKey, String functionValue, Integer stage) {
        return new Variable(functionKey, functionValue, stage);
    }

    private String createFunctionKey(POPPrecEff eff) {
        return Stream.concat(
                Stream.of(eff.getFunction().getName()),
                eff.getFunction().getParams().stream()
        ).collect(Collectors.joining("~"));

    }

    private void postConstruct() {
        this.functionKeyFormatted = StringUtils.replace(functionKey, " ", "~");
        this.functionKeyWithValueFormatted = format("%s=%s", this.functionKeyFormatted, this.functionValue);
        this.functionDataFormatted = format("Stage:%s, State:%s",
                Optional.ofNullable(stage).map(Object::toString).orElse("<NONE>"),
                formatFunctionKeyWithValue());
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
        return functionKeyFormatted;
    }

    public String formatFunctionKeyWithValue() {
        return functionKeyWithValueFormatted;
    }

    public String formatData() {
        return functionDataFormatted;
    }

    @Override
    public String getValue() {
        return functionValue;
    }

    public String getFunctionKey() {
        return functionKey;
    }

    public Optional<Integer> getStage() {
        return Optional.ofNullable(stage);
    }

    @Override
    public String toString() {
        return formatData();
    }
}
