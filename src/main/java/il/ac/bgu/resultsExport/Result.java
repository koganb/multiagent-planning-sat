package il.ac.bgu.resultsExport;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
class Result {
    @JsonProperty("problem")
    private String problem;

    @JsonProperty("plan_properties")
    private PlanProperties planProperties;

    @JsonProperty("simulation")
    private Simulation simulation;

    @JsonProperty("cnf_model_details")
    private CnfModelDetails cnfModelDetails;

    @JsonProperty("cnf")
    private Cnf cnf;

    @JsonProperty("solution")
    private Solution solution;

    @JsonProperty("execution_time")
    private ExecutionTime executionTime;

}


@Setter
@Getter
@NoArgsConstructor
class PlanProperties {
    @JsonProperty("number_of_steps")
    private Long numberOfSteps;

    @JsonProperty("number_of_actions")
    private Long numberOfActions;

    @JsonProperty("number_of_agents")
    private Long numberOfAgents;
}



@Setter
@Getter
@NoArgsConstructor
class Simulation {
    @JsonProperty("failed_actions")
    private String failedActions;

    @JsonProperty("failed_actions_cardinality")
    private Long failedActionsCardinality;
}


@Setter
@Getter
@NoArgsConstructor
class CnfModelDetails {
    @JsonProperty("failure_model")
    private String failureModel;

    @JsonProperty("conflict_model")
    private String conflictModel;

    @JsonProperty("conflict_retries_model")
    private String conflictRetriesModel;

    @JsonProperty("max_failures_restriction")
    private Long maxFailuresRestriction;
}

@Setter
@Getter
@NoArgsConstructor
class Cnf {
    @JsonProperty("clauses_num")
    private Long clausesNum;

    @JsonProperty("variables_num")
    private Long variablesNum;
}


@Setter
@Getter
@NoArgsConstructor
class Solution {
    @JsonProperty("number_of_solutions")
    private Long numberOfSolutions;

    @JsonProperty("solution_index")
    private Long solutionIndex;

    @JsonProperty("solution_cardinality")
    private Long solutionCardinality;
}

@Setter
@Getter
@NoArgsConstructor
class ExecutionTime {
    @JsonProperty("cnf_compilation_mils")
    private Long cnfCompilationMils;

    @JsonProperty("final_var_state_calc_mils")
    private Long finalVarStateCalcMils;

    @JsonProperty("sat_solving_mils")
    private List<SatSolvingMils> SatSolvingMils;
}


@Setter
@Getter
@NoArgsConstructor
class SatSolvingMils {
    @JsonProperty("mils")
    private Long mils;

    @JsonProperty("is_found")
    private Boolean isFound;

    @JsonProperty("solution_size")
    private Long solutionSize;

}



/*


- problem: deports11
  plan_properties:
    number_of_steps: 50
    number_of_actions: 56
    number_of_agents: 7
  simulation:
    failed_actions: "[Index:39, Agent:truck0,Action:Drive~truck0~distributor2~distributor1=FAILED]"
    failed_actions_cardinality: 1
  cnf_model_details:
    failure_model: no effects model
    conflict_model: no effects model
    conflict_retries_model: no retries
    max_failures_restriction: 1
  cnf:
    clauses_num: 27589
    variables_num: 12612
  solution:
    number_of_solutions: 1
    solution_index: 0
    solution_cardinality: 1
  execution_time:
    cnf_compilation_mils: 747
    final_var_state_calc_mils: 616
    sat_solving_mils:
    - mils: 164
      is_found: true
      solution_size: 1
    - mils: 75
      is_found: false
      solution_size: 1

 */