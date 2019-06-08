package il.ac.bgu.cnfCompilation;

import com.google.common.collect.Lists;
import il.ac.bgu.dataModel.Variable;
import org.agreement_technologies.service.map_planner.POPFunction;
import org.agreement_technologies.service.map_planner.POPPrecEff;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * Created by borisk on 11/26/2018.
 */
public abstract class AgentPOPPrecEffFactory {


    public static final String READY = "ready";

    public static Variable createConditionOnAgent(String agentName) {
        return Variable.of(String.format(READY + "(%s)", agentName), "true");
    }
}