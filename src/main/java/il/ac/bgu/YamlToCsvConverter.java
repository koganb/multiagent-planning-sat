package il.ac.bgu;

import com.github.opendevl.JFlat;

import java.io.IOException;

/**
 * Created by borisk on 12/5/2018.
 */
public class YamlToCsvConverter {


    public static void main(String[] args) throws IOException {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"cnf\": {\n" +
                "      \"variables_num\": 815, \n" +
                "      \"clauses_num\": 2030\n" +
                "    }, \n" +
                "    \"cnf_model_details\": {\n" +
                "      \"failure_model\": \"delay one step\", \n" +
                "      \"conflict_model\": \"no effects model\", \n" +
                "      \"conflict_retries_model\": \"no retries\", \n" +
                "      \"max_failures_restriction\": 1\n" +
                "    }, \n" +
                "    \"execution_time\": {\n" +
                "      \"sat_solving_mils\": [\n" +
                "        40, \n" +
                "        20\n" +
                "      ], \n" +
                "      \"cnf_compilation_mils\": 120, \n" +
                "      \"final_var_state_calc_mils\": 50\n" +
                "    }, \n" +
                "    \"problem\": \"deports1.problem\", \n" +
                "    \"plan_properties\": {\n" +
                "      \"failed_actions\": \"[Index:07, Agent:truck1,Action:Unload~hoist2~crate0~truck1~distributor1=FAILED]\", \n" +
                "      \"number_of_steps\": 9, \n" +
                "      \"number_of_actions\": 11, \n" +
                "      \"number_of_agents\": 4\n" +
                "    }, \n" +
                "    \"solution\": {\n" +
                "      \"solution_index\": 0, \n" +
                "      \"number_of_solutions\": 1\n" +
                "    }\n" +
                "  }, \n" +
                "  {\n" +
                "    \"cnf\": {\n" +
                "      \"variables_num\": 815, \n" +
                "      \"clauses_num\": 2030\n" +
                "    }, \n" +
                "    \"cnf_model_details\": {\n" +
                "      \"failure_model\": \"delay one step\", \n" +
                "      \"conflict_model\": \"no effects model\", \n" +
                "      \"conflict_retries_model\": \"no retries\", \n" +
                "      \"max_failures_restriction\": 1\n" +
                "    }, \n" +
                "    \"execution_time\": {\n" +
                "      \"sat_solving_mils\": [\n" +
                "        10, \n" +
                "        10, \n" +
                "        10\n" +
                "      ], \n" +
                "      \"cnf_compilation_mils\": 120, \n" +
                "      \"final_var_state_calc_mils\": 20\n" +
                "    }, \n" +
                "    \"problem\": \"deports1.problem\", \n" +
                "    \"plan_properties\": {\n" +
                "      \"failed_actions\": \"[Index:03, Agent:truck1,Action:Drive~truck1~distributor1~distributor0=FAILED]\", \n" +
                "      \"number_of_steps\": 9, \n" +
                "      \"number_of_actions\": 11, \n" +
                "      \"number_of_agents\": 4\n" +
                "    }, \n" +
                "    \"solution\": {\n" +
                "      \"solution_index\": 0, \n" +
                "      \"number_of_solutions\": 2\n" +
                "    }\n" +
                "  }, \n" +
                "  {\n" +
                "    \"cnf\": {\n" +
                "      \"variables_num\": 815, \n" +
                "      \"clauses_num\": 2030\n" +
                "    }, \n" +
                "    \"cnf_model_details\": {\n" +
                "      \"failure_model\": \"delay one step\", \n" +
                "      \"conflict_model\": \"no effects model\", \n" +
                "      \"conflict_retries_model\": \"no retries\", \n" +
                "      \"max_failures_restriction\": 1\n" +
                "    }, \n" +
                "    \"execution_time\": {\n" +
                "      \"sat_solving_mils\": [\n" +
                "        0, \n" +
                "        0\n" +
                "      ], \n" +
                "      \"cnf_compilation_mils\": 120, \n" +
                "      \"final_var_state_calc_mils\": 40\n" +
                "    }, \n" +
                "    \"problem\": \"deports1.problem\", \n" +
                "    \"plan_properties\": {\n" +
                "      \"failed_actions\": \"[Index:08, Agent:distributor1,Action:DropP~hoist2~crate0~pallet2~distributor1=FAILED]\", \n" +
                "      \"number_of_steps\": 9, \n" +
                "      \"number_of_actions\": 11, \n" +
                "      \"number_of_agents\": 4\n" +
                "    }, \n" +
                "    \"solution\": {\n" +
                "      \"solution_index\": 0, \n" +
                "      \"number_of_solutions\": 1\n" +
                "    }\n" +
                "  }, \n" +
                "  {\n" +
                "    \"cnf\": {\n" +
                "      \"variables_num\": 815, \n" +
                "      \"clauses_num\": 2030\n" +
                "    }, \n" +
                "    \"cnf_model_details\": {\n" +
                "      \"failure_model\": \"delay one step\", \n" +
                "      \"conflict_model\": \"no effects model\", \n" +
                "      \"conflict_retries_model\": \"no retries\", \n" +
                "      \"max_failures_restriction\": 1\n" +
                "    }, \n" +
                "    \"execution_time\": {\n" +
                "      \"sat_solving_mils\": [\n" +
                "        20, \n" +
                "        10\n" +
                "      ], \n" +
                "      \"cnf_compilation_mils\": 120, \n" +
                "      \"final_var_state_calc_mils\": 20\n" +
                "    }, \n" +
                "    \"problem\": \"deports1.problem\", \n" +
                "    \"plan_properties\": {\n" +
                "      \"failed_actions\": \"[Index:01, Agent:truck1,Action:Load~hoist0~crate1~truck1~depot0=FAILED]\", \n" +
                "      \"number_of_steps\": 9, \n" +
                "      \"number_of_actions\": 11, \n" +
                "      \"number_of_agents\": 4\n" +
                "    }, \n" +
                "    \"solution\": {\n" +
                "      \"solution_index\": 0, \n" +
                "      \"number_of_solutions\": 1\n" +
                "    }\n" +
                "  }]";

        JFlat flatMe = new JFlat(jsonString);


        flatMe.json2Sheet();

        //write2csv("result-json.csv");


    }

}
