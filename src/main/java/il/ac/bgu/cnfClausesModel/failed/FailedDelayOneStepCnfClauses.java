package il.ac.bgu.cnfClausesModel.failed;

import il.ac.bgu.variableModel.DelayStageVariableFailureModel;
import il.ac.bgu.variableModel.VariableModelFunction;

public class FailedDelayOneStepCnfClauses extends FailureCnfClauses {
    {
        this.failureModel = new DelayStageVariableFailureModel(1);
    }

    @Override
    public VariableModelFunction getVariableModel() {
        return failureModel;
    }
}
