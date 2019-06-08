package il.ac.bgu.resultsExport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.opendevl.JFlat;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by borisk on 12/5/2018.
 */

public class YmlToCsvConverter {

    private static final Logger log;

    static {
        Configurator.initialize(null, "conf/log4j.properties");
        log = LoggerFactory.getLogger(YmlToCsvConverter.class);

    }


    public static void main(String[] args) throws IOException {

        //for (File ymlFile : FileUtils.listFiles(new File("stats/"), new String[]{"yml"}, false)) {
        for (File ymlFile : Lists.newArrayList(
                new File("stats/stats.NoEffectsFailureModel_NoRetries.yml"),
                new File("stats/stats.NoEffectsFailureModel_OneRetry.yml")

                )) {
            log.info("Start converting {} to csv", ymlFile.getName());

            final Result[] results = new ObjectMapper(new YAMLFactory()).readValue(ymlFile, Result[].class);

            final List<ResultFlat> transformedResults = Arrays.stream(results).map(t -> {
                Long satSolutionTime = t.getExecutionTime().getSatSolvingMils().stream()
                        .mapToLong(SatSolvingMils::getMils)
                        .sum();
                return new ResultFlat(t.getProblem(),
                        t.getPlanProperties().getNumberOfSteps().toString(),
                        t.getPlanProperties().getNumberOfActions().toString(),
                        t.getPlanProperties().getNumberOfAgents().toString(),
                        t.getSimulation().getFailedActionsCardinality().toString(),
                        t.getCnf().getClausesNum().toString(),
                        t.getCnf().getVariablesNum().toString(),
                        t.getSolution().getNumberOfSolutions().toString(),
                        t.getSolution().getSolutionIndex().toString(),
                        t.getSolution().getSolutionCardinality().toString(),
                        satSolutionTime.toString()
                );
            }).collect(Collectors.toList());

            new JFlat(new ObjectMapper().writeValueAsString(transformedResults))
                    .json2Sheet()
                    .write2csv(new File(
                            ymlFile.getParent(), ymlFile.getName().replace("yml", "csv")).getCanonicalPath());

        }
    }

}
