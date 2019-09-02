package il.ac.bgu.resultsExport;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class ResultFlat {
    @JsonProperty("problem")
    private String problem;

    @JsonProperty("number_of_steps")
    private String numberOfSteps;

    @JsonProperty("number_of_actions")
    private String numberOfActions;

    @JsonProperty("number_of_agents")
    private String numberOfAgents;

    @JsonProperty("failed_actions_cardinality")
    private String failedActionsCardinality;

    @JsonProperty("clauses_num")
    private String clausesNum;

    @JsonProperty("variables_num")
    private String variablesNum;

    @JsonProperty("number_of_solutions")
    private String numberOfSolutions;

    @JsonProperty("solution_index")
    private String solutionIndex;

    @JsonProperty("solution_cardinality")
    private String solutionCardinality;

    @JsonProperty("sat_solving_mils")
    private String satSolvingMils;

    @JsonProperty("correct_solution_findind_mils")
    private String correctSolutionFindingMils;

    @JsonProperty("first_solution_time_mils")
    private String firstSolutionTime;


}
