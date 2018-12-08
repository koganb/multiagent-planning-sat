package il.ac.bgu.planningExtensions;

import org.agreement_technologies.common.map_planner.Step;

import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Boris on 21/06/2017.
 */
public interface SolutionFoundListener {
    void solutionFound(TreeMap<Integer, Set<Step>> solution);
}
