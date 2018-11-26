package il.ac.bgu.cnfCompilation;

import com.google.common.collect.Lists;
import org.agreement_technologies.service.map_planner.POPFunction;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * Created by borisk on 11/26/2018.
 */
public abstract class AgentPOPPrecEffFactory {


    public static final String FUNCTION_FIELD_NAME = "function";
    public static final String CONDITION_TYPE_FIELD_NAME = "conditionType";
    public static final String NAME_FIELD_NAME = "name";
    public static final String PARAMS_FIELD_NAME = "params";
    public static final String VALUE_FIELD_NAME = "value";


    public static final String ACTION_NAME = "ready";
    public static final String ACTION_VALUE = "true";

    public static POPPrecEff createConditionOnAgent(String agentName) {

        try {
            //very ugly-evil code that creates objects without calling their default constructor
            Objenesis objenesis = new ObjenesisStd();
            POPPrecEff popPrecEff = objenesis.newInstance(POPPrecEff.class);
            POPFunction popFunction = objenesis.newInstance(POPFunction.class);

            //use reflection to set key-value
            FieldUtils.writeField(popPrecEff, FUNCTION_FIELD_NAME, popFunction, true);
            FieldUtils.writeField(popPrecEff, CONDITION_TYPE_FIELD_NAME, 1, true);
            FieldUtils.writeField(popFunction, NAME_FIELD_NAME, ACTION_NAME, true);
            FieldUtils.writeField(popFunction, PARAMS_FIELD_NAME, Lists.newArrayList(agentName), true);
            FieldUtils.writeField(popPrecEff, VALUE_FIELD_NAME, ACTION_VALUE, true);

            return popPrecEff;

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }


    }
}