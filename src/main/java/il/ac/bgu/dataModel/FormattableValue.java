package il.ac.bgu.dataModel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class FormattableValue<T extends Formattable> {
    private T formattable;
    private Boolean value;

    @Override
    public String toString() {
        return String.format("{%s}=%s", formattable, value);
    }
}
