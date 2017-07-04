package org.agreement_technologies.agents;

import com.codepoetics.protonpack.StreamUtils;
import il.ac.bgu.SolutionFoundListener;
import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.PlanningAgentListener;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_grounding.Grounding;
import org.agreement_technologies.common.map_heuristic.Heuristic;
import org.agreement_technologies.common.map_heuristic.HeuristicFactory;
import org.agreement_technologies.common.map_landmarks.Landmarks;
import org.agreement_technologies.common.map_parser.AgentList;
import org.agreement_technologies.common.map_parser.PDDLParser;
import org.agreement_technologies.common.map_parser.Task;
import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.Planner;
import org.agreement_technologies.common.map_planner.PlannerFactory;
import org.agreement_technologies.common.map_planner.Step;
import org.agreement_technologies.common.map_viewer.PlanViewer;
import org.agreement_technologies.service.map_communication.AgentCommunicationImp;
import org.agreement_technologies.service.map_grounding.GroundingImp;
import org.agreement_technologies.service.map_heuristic.HeuristicFactoryImp;
import org.agreement_technologies.service.map_parser.MAPDDLParserImp;
import org.agreement_technologies.service.map_parser.ParserImp;
import org.agreement_technologies.service.map_planner.PlannerFactoryImp;
import org.agreement_technologies.service.map_viewer.PlanViewerImp;
import org.agreement_technologies.service.tools.Redirect;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PlanningAlgorithm {

    public static final int STATUS_STARTING = 0;
    public static final int STATUS_PARSING = 1;
    public static final int STATUS_GROUNDING = 2;
    public static final int STATUS_PLANNING = 3;
    public static final int STATUS_LANDMARKS = 4;
    public static final int STATUS_IDLE = 8;
    public static final int STATUS_ERROR = 9;
    public static final int STATUS_ARGUMENTATION = 10;
    protected static final String[] STATUS_DESC = {"starting", "parsing",
            "grounding", "planning", "landmarks", "undefined", "undefined",
            "undefined", "idle", "error", "arguing"};

    protected String name;                    // Agent name
    protected String domainFile;                // Domain filename
    protected String problemFile;                // Problem filename

    protected int status;                    // Agent status (see constants above)
    protected int sameObjects;
    protected boolean traceOn;
    protected int heuristicType;

    protected AgentCommunication comm;                          // Agent communication utility
    protected Task planningTask;                // Parsed planning task
    protected GroundedTask groundedTask;                        // Grounded planning task
    protected Landmarks landmarks;                // Landmarks information

    protected PlannerFactory plannerFactory;
    protected Planner planner;
    protected PlanningAgentListener paListener;
    protected Plan solutionPlan;
    protected long planningTime;
    protected int iterations;
    protected int searchPerformance;
    protected int negotiation;
    protected boolean isAnytime;
    protected AgentList agList;
    protected boolean waitSynch;
    protected boolean negationByFailure;
    protected boolean isMAPDDL;
    private SolutionFoundListener solutionFoundListener;

    /**
     * Constructor of a planning agent
     */
    public PlanningAlgorithm(String name, String domainFile, String problemFile, AgentList agList,
                             boolean waitSynch, int sameObjects, boolean traceOn, int h, int searchPerformance,
                             int neg, boolean anytime, SolutionFoundListener solutionFoundListener) throws Exception {
        this.solutionFoundListener = solutionFoundListener;
        this.name = name.toLowerCase();
        this.comm = new AgentCommunicationImp(this.name, agList);
        this.waitSynch = waitSynch;
        this.agList = agList;
        this.plannerFactory = null;
        this.domainFile = domainFile;
        this.problemFile = problemFile;
        this.sameObjects = sameObjects; // GroundedTask.SAME_OBJECTS_REP_PARAMS;
        this.traceOn = traceOn; //false;
        this.searchPerformance = searchPerformance; //1;
        this.status = STATUS_STARTING;
        this.heuristicType = h; //HeuristicFactory.LAND_DTG_NORM;
        this.paListener = null;
        this.solutionPlan = null;
        this.landmarks = null;
        this.negotiation = neg; // NegotiationFactory.COOPERATIVE;
        this.isAnytime = anytime; //false;
        this.isMAPDDL = new ParserImp().isMAPDDL(domainFile);
        this.negationByFailure = this.isMAPDDL;
    }

    /**
     * Gets the status description
     *
     * @param status Agent status
     * @return Status description
     */
    public static String getStatusDesc(int status) {
        return STATUS_DESC[status];
    }

    /**
     * Shows a trace message
     *
     * @param indentLevel Indentation level
     * @param msg         Message
     */
    protected void trace(int indentLevel, String msg) {
        if (paListener != null) {
            paListener.trace(indentLevel, msg);
        }
    }

    /**
     * Changes the agent status
     *
     * @param status New status
     */
    protected void changeStatus(int status) {
        this.status = status;
        if (paListener != null) {
            paListener.statusChanged(this.status);
        }
    }

    /**
     * Notifies an error
     *
     * @param msg Error message
     */
    protected void notifyError(String msg) {
        changeStatus(STATUS_ERROR);
        if (paListener != null) {
            paListener.notyfyError(msg);
        } else System.out.println(msg);
    }

    /**
     * Execution code for the planning agent
     */
    protected void execute(int timeout) {    // Time out in seconds
        if (waitSynch)
            executeWithAsynchronousStart(timeout);
        else
            executeWithSynchronousStart(timeout);
        if (comm != null)
            comm.close();
    }

    /**
     * **********************************************************
     */
    /*        P L A N N I N G    T A S K    P A R S I N G        */
    /**
     * **********************************************************
     */

    /**
     * Task parsing from PDDL files
     */
    protected long parseTask() {
        changeStatus(STATUS_PARSING);
        long startTime = System.currentTimeMillis();
        planningTask = null;
        PDDLParser parser = isMAPDDL ? new MAPDDLParserImp() : new ParserImp();
        try {
            planningTask = parser.parseDomain(domainFile);
        } catch (ParseException e) {
            notifyError(e.getMessage() + ", at line " + e.getErrorOffset()
                    + " (" + domainFile + ")");
        } catch (IOException e) {
            notifyError("Read error: " + e.getMessage() + " (" + domainFile + ")");
        }
        if (status != STATUS_ERROR) {
            try {
                parser.parseProblem(problemFile, planningTask, agList, name);
            } catch (ParseException e) {
                notifyError(e.getMessage() + ", at line " + e.getErrorOffset()
                        + " (" + problemFile + ")");
            } catch (IOException e) {
                notifyError("Read error: " + e.getMessage() + " (" + problemFile + ")");
            }
        }
        long endTime = System.currentTimeMillis() - startTime;
        trace(0, "Parsing completed in " + endTime + "ms.");
        return endTime;
    }

    /**
     * **********************************************************
     */
    /*    D I S T R I B U T E D    R E L A X E D    G R A P H    */
    /**
     * **********************************************************
     */
    /**
     * Task grounding from parsed task
     */
    protected long groundTask() throws IOException {
        if (status == STATUS_ERROR) {
            return 0;
        }
        changeStatus(STATUS_GROUNDING);
        long startTime = System.currentTimeMillis();
        Grounding g = new GroundingImp(sameObjects);
        g.computeStaticFunctions(planningTask, comm);
        groundedTask = g.ground(planningTask, comm, negationByFailure);
        groundedTask.optimize();
        long endTime = System.currentTimeMillis() - startTime;
        trace(0, "Grounding completed in " + endTime + "ms. ("
                + comm.getNumMessages() + " messages, " + groundedTask.getActions().size()
                + " actions)");
        return endTime;
    }

    /**
     * **********************************************************
     */
    /*                P L A N N I N G    S T A G E               */
    /**
     * **********************************************************
     */
    /**
     * Planning stage
     */
    protected long planningStage(long timeout) {
        boolean usesLandmarks;
        if (status == STATUS_ERROR) {
            return 0;
        }
        PlanningAgentListener al = traceOn ? paListener : null;
        plannerFactory = new PlannerFactoryImp(groundedTask, comm);
        HeuristicFactory hf = new HeuristicFactoryImp();
        usesLandmarks = hf.getHeuristicInfo(heuristicType, HeuristicFactory.INFO_USES_LANDMARKS).equals("yes");
        if (usesLandmarks) {
            changeStatus(STATUS_LANDMARKS);
        }
        Heuristic heuristic = hf.getHeuristic(heuristicType, comm, groundedTask, plannerFactory);
        landmarks = usesLandmarks ? (Landmarks) heuristic.getInformation(Heuristic.INFO_LANDMARKS) : null;
        changeStatus(STATUS_PLANNING);
        planner = plannerFactory.createPlanner(groundedTask, heuristic, comm, al, searchPerformance, negotiation, isAnytime);
        long startTime = System.currentTimeMillis();
        solutionPlan = planner.computePlan(startTime, timeout);    // Timeout in seconds

        if (solutionFoundListener != null) {
            int[] actionIndexes = solutionPlan.linearizePlan(Plan.CoDMAP_DISTRIBUTED, comm.getAgentList());
            ArrayList<Step> stepsArray = solutionPlan.getStepsArray();

            TreeMap<Integer, Set<Step>> planStepsByIndex = StreamUtils.<Integer, Step, Pair<Integer, Step>>zip(
                    Arrays.stream(actionIndexes).boxed(),
                    stepsArray.stream(),
                    ImmutablePair::new).
                    filter(p -> p.getKey() >= -1).  //filter out final step
                    collect(Collectors.groupingBy(Pair::getLeft, TreeMap::new, Collectors.mapping(
                    Pair::getRight,
                    Collectors.toSet())));


            solutionFoundListener.solutionFound(planStepsByIndex);
        }

        long endTime = System.currentTimeMillis() - startTime;
        trace(0, String.format("Planning completed in %.3f sec.", endTime / 1000.0));
        trace(0, "Used memory: " + (Runtime.getRuntime().totalMemory() / 1024) + "kb.");
        if (solutionPlan != null) {
            trace(0, "Plan length: " + (solutionPlan.countSteps()));
            if (!traceOn && paListener != null) {
                paListener.showPlan(solutionPlan, plannerFactory);
            }
        }
        iterations = planner.getIterations();
        return endTime;
    }

    public boolean solutionFound() {
        return solutionPlan != null;
    }

    public double getSolutionMetric() {
        return solutionPlan.getMetric();
    }

    public int getSolutionLength() {
        return solutionPlan.numSteps() - 2;
    }

    public int getSolutionMakespan() {
        PlanViewer pv = new PlanViewerImp();
        pv.showPlan(solutionPlan, plannerFactory);
        return pv.getMakespan() - 2;
    }

    public double getPlanningTime() {
        return planningTime / 1000.0;
    }

    public int getIterations() {
        return iterations;
    }

    private void executeWithSynchronousStart(int timeout) {
        try {
            long totalTime;
            totalTime = parseTask();                        // Task parsing from PDDL files
            totalTime += groundTask();

            // Task grounding from the parsed task
            planningTime = planningStage(timeout);                // Planning stage
            totalTime += planningTime;
            if (status != STATUS_ERROR) {
                changeStatus(STATUS_IDLE);
                trace(0, "Number of messages: " + comm.getNumMessages());
                trace(0, String.format("Total time: %.3f sec.", totalTime / 1000.0));
            }
        } catch (Throwable e) {
            String error = e.toString() + "\n";
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            notifyError(error + sw.toString());
        }
    }

    private void executeWithAsynchronousStart(int timeout) {
        try {
            parseTask();
            if (waitSynch) waitSynchronization();
            planningTime = maPDDLGroundTask();
            planningTime += planningStage(timeout);
            showResult();
        } catch (Throwable e) {
            String error = e.toString() + "\n";
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(error + sw.toString());
        }
    }

    private void waitSynchronization() {
        System.out.println("; Waiting to start");
        ArrayList<String> agentNames = comm.getAgentList();
        boolean registered[] = new boolean[comm.numAgents()];
        registered[comm.getThisAgentIndex()] = true;
        int activeAgents = 1;
        Redirect red = Redirect.captureOutput();
        do {
            for (int i = 0; i < comm.numAgents(); i++) {
                if (!registered[i]) {
                    if (comm.registeredAgent(agentNames.get(i))) {
                        //System.out.println("[" + comm.getThisAgentName() + "] -> Agent " + agentNames.get(i) + " registered");
                        registered[i] = true;
                        activeAgents++;
                    }
                    ;
                }
            }
        } while (activeAgents < comm.numAgents());
        red.releaseOutput();
    }

    private long maPDDLGroundTask() {
        if (status == STATUS_ERROR) return 0;
        long startTime = System.currentTimeMillis();
        Grounding g = new GroundingImp(sameObjects);
        g.computeStaticFunctions(planningTask, comm);
        groundedTask = g.ground(planningTask, comm, negationByFailure);
        groundedTask.optimize();
        long endTime = System.currentTimeMillis() - startTime;
        System.out.println("; Grounding time: " + String.format("%.3f sec.", endTime / 1000.0));
        return endTime;
    }

    private void showResult() {
        System.out.println("; Planning time: " + String.format("%.3f sec.", planningTime / 1000.0));
        if (solutionPlan != null) {
            System.out.println("; Plan length: " + (solutionPlan.countSteps()));
            solutionPlan.printPlan(Plan.CoDMAP_CENTRALIZED, comm.getThisAgentName(), comm.getAgentList());
        } else System.out.println("; No plan found");
    }

    private void waitMs(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
        }
    }
}
