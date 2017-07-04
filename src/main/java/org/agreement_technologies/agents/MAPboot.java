package org.agreement_technologies.agents;

import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_heuristic.HeuristicFactory;
import org.agreement_technologies.common.map_negotiation.NegotiationFactory;
import org.agreement_technologies.common.map_parser.AgentList;
import org.agreement_technologies.common.map_parser.PDDLParser;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.service.map_parser.MAPDDLParserImp;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Multi-Agent Planning Process launcher Connects with Magentix platform and
 * creates the planning agents
 *
 * @author Oscar
 */
public class MAPboot {
    public static ArrayList<PlanningAgent> planningAgents;

    /**
     * Main method
     *
     * @param args Command line parameters
     */
    public static void main(String args[]) {
        planningAgents = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        rt.addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }

            ;
        });
        if (args.length == 0)   // Graphical interface
            launchGUI();
        else
            launchCommandLine(args);
    }


    public static Set<Map<Integer, Set<Step>>> runCommandLine(String args[]) {
        planningAgents = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        rt.addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }

            ;
        });
        return launchCommandLine(args);
    }


    private static void launchGUI() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GUIBoot().setVisible(true);
            }
        });
    }

    // Command-line parameters: <agent-name> <domain> <problem> <agent_list>
    public static Set<Map<Integer, Set<Step>>> launchCommandLine(String[] args) {

        //borisk collect plans for all agents
        Set<Map<Integer, Set<Step>>> solutionPlans = new HashSet<>();

        if (args.length < 3) {
            printUsage();
        } else if (args.length <= 4) {
            String agentFile = args.length == 4 ? args[3] : null;
            launchAgent(args[0], args[1], args[2], agentFile);
        } else {    // Several agents at once
            AgentList agList;
            int last = args.length - 1;
            PDDLParser p = new MAPDDLParserImp();
            try {
                if (args.length % 3 == 0) {         // No agents file
                    agList = p.createEmptyAgentList();
                    for (int n = 0; n < last; n += 3)
                        agList.addAgent(args[n].toLowerCase(), "127.0.0.1");
                } else if (args.length % 3 == 1) {  // Agents file
                    agList = p.parseAgentList(args[last]);
                } else {
                    printUsage();
                    return null;
                }
                int n = 0;

                while (n < last) {
                    PlanningAgent ag = new PlanningAgent(args[n].toLowerCase(), args[n + 1], args[n + 2],
                            agList, false, GroundedTask.SAME_OBJECTS_REP_PARAMS, false,
                            HeuristicFactory.LAND_DTG_NORM, 1, NegotiationFactory.COOPERATIVE,
                            false, -1, solutionPlans::add);
                    planningAgents.add(ag);
                    n += 3;
                }
                for (PlanningAgent ag : planningAgents)
                    ag.start();

                //BORISK wait for termination
                for (PlanningAgent ag : planningAgents)
                    ag.join();

            } catch (ParseException ex) {
                System.out.println(ex.getMessage() + ", at line " + ex.getErrorOffset() + " (" + args[last] + ")");
            } catch (IOException ex) {
                System.out.println("Read error: " + ex.getMessage() + " (" + args[last] + ")");
            } catch (Exception ex) {
                System.out.println("Error  " + ex.getMessage());
            }
        }
        return solutionPlans;
    }

    private static void printUsage() {
        System.out.println("Invalid number of parameters.");
        System.out.println("Usage:");
        System.out.println("  java -jar FMAP.jar [<agent-name> <domain-file> <problem-file>]+ <agent-list-file>");
    }

    private static void launchAgent(String agentName, String domainFile, String problemFile, String agentsFile) {
        try {
            PDDLParser p = new MAPDDLParserImp();
            AgentList agList;
            if (agentsFile == null) {
                agList = p.createEmptyAgentList();
                agList.addAgent(agentName.toLowerCase(), "127.0.0.1");
            } else agList = p.parseAgentList(agentsFile);
            PlanningAgent ag = new PlanningAgent(agentName.toLowerCase(), domainFile,
                    problemFile, agList, true, GroundedTask.SAME_OBJECTS_REP_PARAMS,
                    false, HeuristicFactory.LAND_DTG_NORM, 1, NegotiationFactory.COOPERATIVE,
                    false, -1, null);
            MAPboot.planningAgents.add(ag);
            ag.start();
        } catch (ParseException ex) {
            System.out.println(ex.getMessage() + ", at line " + ex.getErrorOffset() + " (" + agentsFile + ")");
        } catch (IOException ex) {
            System.out.println("Read error: " + ex.getMessage() + " (" + agentsFile + ")");
        } catch (Exception ex) {
            System.out.println("Error  " + ex.getMessage());
        }
    }

    public static void shutdown() {
        System.out.println("; Stopping...");
        for (PlanningAgent ag : planningAgents)
            ag.shutdown();
        planningAgents.clear();
    }
}
