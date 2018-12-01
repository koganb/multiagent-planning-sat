package il.ac.bgu.cnfClausesModel.failed;

import il.ac.bgu.variableModel.NoEffectVariableFailureModel;
import il.ac.bgu.variableModel.VariableModelFunction;

public class FailedNoEffectsCnfClauses extends FailureCnfClauses {
    {
        this.failureModel = new NoEffectVariableFailureModel();
    }

    @Override
    public String getName() {
        return "no effects model";
    }

    @Override
    public VariableModelFunction getVariableModel() {
        return failureModel;
    }
}
