package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_landmarks.*;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Multi-agent landmark graph
 *
 * @author Alex
 */
public class MALandmarkGraph implements LandmarkGraph {
    private final GroundedTask groundedTask;
    private final AgentCommunication comm;
    //private ArrayList<LMAction> At;
    //Total number of landmarks (single landmarks that are not in the initial state)
    public int totalLandmarks;
    private ArrayList<LandmarkNode> nodes;
    private ArrayList<LandmarkOrdering> edges;
    private RPG RPG;
    private Boolean[][] matrix;
    private ArrayList<Integer> literalNode;
    private ArrayList<ArrayList<LandmarkNode>> objs;
    private boolean[][] mutexMatrix;
    private boolean[][] reasonableOrderings;
    private ArrayList<LandmarkOrdering> reasonableOrderingsList;
    private ArrayList<LandmarkOrdering> reasonableOrderingsGoalsList;
    //Common and non-common precs of a literal
    private ArrayList<LandmarkFluent> I;
    private ArrayList<LandmarkFluent> U;
    private ArrayList<LandmarkSet> D;
    //Common precs to send each agent
    //private ArrayList<ArrayList<LMLiteralInfo>> commonToSend;
    private ArrayList<LMLiteralInfo> commonToSend;
    //Non common precs to send each agent
    //private ArrayList<ArrayList<LMLiteralInfo>> nonCommonToSend;
    private ArrayList<LMLiteralInfo> nonCommonToSend;
    private Hashtable<String, LandmarkNode> hashLGNodes;
    private Hashtable<String, Integer> hashAgents;
    //A: set of actions that produce a certain literal or disjunction of literals (placed before the literal in the RPG)
    //At: set of all the actions that produce a certain literal or disjunction of literals
    private ArrayList<LandmarkAction> A;
    private Hashtable<Integer, ArrayList<String>> otherAgentsLandmarks;
    //Index used to label the landmarkswith a global identifier
    private int globalIndex;

    public MALandmarkGraph(GroundedTask gt, AgentCommunication c, RPG r, ArrayList<LandmarkFluent> g) {
        globalIndex = 0;
        groundedTask = gt;
        comm = c;
        int i, messageType, visited;
        //N: Landmark tree nodes
        nodes = new ArrayList<LandmarkNode>();
        //E: landmark tree links (necessary orderings)
        edges = new ArrayList<LandmarkOrdering>();

        //r: RPG associated to the LT
        this.RPG = r;
        ArrayList<LMLiteralInfo> commonPrecs;
        ArrayList<LMLiteralInfo> nonCommonPrecs;
        D = new ArrayList<LandmarkSet>();

        i = 0;
        hashAgents = new Hashtable<String, Integer>();
        for (String ag : groundedTask.getAgentNames()) {
            hashAgents.put(ag, i);
            i++;
        }

        ArrayList<MessageContentLandmarkGraph> received = new ArrayList<MessageContentLandmarkGraph>();

        //literalNode maps literals and nodes of the landmark graph
        literalNode = new ArrayList<Integer>(r.getLiterals().size());
        for (i = 0; i < r.getLiterals().size(); i++)
            literalNode.add(-1);

        //Initializing objs array
        objs = new ArrayList<ArrayList<LandmarkNode>>(r.getLitLevels().size());
        for (i = 0; i < r.getLitLevels().size(); i++)
            objs.add(new ArrayList<LandmarkNode>());

        //Adding goals to N and objs(lvl), where lvl is the RPG level in which each goal first appears
        for (LandmarkFluent goal : g) {
            //Create the LGNode and set its global identifier
            LGNode gn = new LGNode(goal);
            globalIndex = gn.setGlobalId(globalIndex);
            nodes.add(gn);
            nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
            literalNode.set(goal.getIndex(), nodes.size() - 1);
            if (goal.getLevel() < 0) {
                throw new AssertionError("Goal not achieved: " + goal);
            } else objs.get(goal.getLevel()).add(gn);
        }

        //The RPG is explored backwards, beginning from the last literal level
        int level = r.getLitLevels().size() - 1;

        I = new ArrayList<LandmarkFluent>();
        U = new ArrayList<LandmarkFluent>();
        commonToSend = new ArrayList<LMLiteralInfo>();
        //commonToSend = new ArrayList<ArrayList<LMLiteralInfo>>();
        nonCommonToSend = new ArrayList<LMLiteralInfo>();
        //nonCommonToSend = new ArrayList<ArrayList<LMLiteralInfo>>();

        LandmarkNode obj;

        int remainingObjects = objs.get(level).size();
        MessageContentLandmarkGraph remainingObjectsMsg;
        LandmarkNode newNode;
        while (level > 0) {
            //Baton agent - solves a complete level of the objs structure
            if (comm.batonAgent()) {
                if (objs.get(level).size() > 0) {
                    //System.out.println("Exploring level " + level);
                    //Analyze literals of the current level
                    for (i = 0; i < objs.get(level).size(); i++) {
                        obj = objs.get(level).get(i);
                        //System.out.println("Baton agent " + comm.getThisAgentName() + " analyzing landmark \t(" + obj.toString() + ")");
                        //If the literal is shareable, send a warning to the related agents
                        if (obj.getAgents().size() > 1) {
                            for (String ag : obj.getAgents()) {
                                if (!ag.equals(this.groundedTask.getAgentName())) {
                                    comm.sendMessage(ag, new MessageContentLandmarkGraph(null, obj.identify(), null, null, null, null,
                                            groundedTask.getAgentName(), null, MessageContentLandmarkGraph.COMMON_PRECS_STAGE), false);
                                }
                            }
                        }
                        //Calculate the set A of actions that produce the literal selected in objs[level]
                        this.getProducers(obj, null);
                        //Clear I and U arrays before refilling them
                        I.clear();
                        U.clear();
                        D.clear();
                        //Once A is calculated, I and U are also computed
                        //getCommonNonCommonPrecs is only launched if there are producers, that is, if A is not an empty set
                        if (A.size() > 0)
                            groupCommonNonCommonPrecs(A/*, At*/);
                        //Clear the received structure before refilling it
                        received.clear();
                        //Receive common and non common precs obtained by other agents
                        for (int j = 0; j < obj.getAgents().size() - 1; j++)
                            received.add((MessageContentLandmarkGraph) comm.receiveMessage(false));
                        //Calculate precs that are common to all the agents' actions
                        groupMACommonNonCommonPrecs(received, A.size() != 0, obj.getAgents());
                        //Send common precs to the rest of involved agents
                        for (String ag : obj.getAgents()) {
                            if (!ag.equals(this.groundedTask.getAgentName()))
                                comm.sendMessage(ag, new MessageContentLandmarkGraph(globalIndex, null, commonToSend,//.get(hashAgents.get(ag)), 
                                        nonCommonToSend,/*.get(hashAgents.get(ag)),*/ null, null, groundedTask.getAgentName(), null,
                                        MessageContentLandmarkGraph.COMMON_PRECS_STAGE), false);
                        }
                        //Verification stage: check if the common precs identified are actually landmarks
                        //All the agents take part in the verification stage
                        //Warning the agents that have not participated in the common precs stage
                        for (String ag : comm.getOtherAgents())
                            if (!obj.getAgents().contains(ag))
                                comm.sendMessage(ag, new MessageContentLandmarkGraph(globalIndex, obj.identify(), commonToSend,//.get(hashAgents.get(ag)), 
                                        nonCommonToSend,/*.get(hashAgents.get(ag)),*/ null, null, groundedTask.getAgentName(),
                                        null, MessageContentLandmarkGraph.VERIFICATION_STAGE), false);
                        //Adding nodes and transitions to the landmark graph and precs to the objs structure
                        for (LMLiteralInfo cp : commonToSend) {
                            //System.out.println("Baton agent " + comm.getThisAgentName() + " verifying common prec \t(" + cp.getLiteral() + ")");
                            this.verifyCommonPrec(cp, obj);
                        }
                        //Verification procedure for disjunctive landmark candidates
                        for (LMLiteralInfo ncp : nonCommonToSend) {
                            //System.out.println("Baton agent " + comm.getThisAgentName() + " verifying non common prec \t(" + ncp.getFunction() + ")");
                            this.verifyNonCommonPrec(ncp, commonToSend, obj, level);
                        }
                    }
                    //Clean the objs structure after analyzing the objects of the last level
                    objs.get(level).clear();
                }
            }
            //Non-baton agent - waits for messages and helps the baton agent to analyze shareable fluents
            else {
                if (remainingObjects > 0) {
                    for (i = remainingObjects; i > 0; i--) {
                        //Wait for a message from the baton agent
                        MessageContentLandmarkGraph m = (MessageContentLandmarkGraph) comm.receiveMessage(comm.getBatonAgent(), false);
                        //Classify the message according to its type:
                        //Common precs stage -> help the baton agent to calculate the common precs of a landmark
                        //Verification stage -> verify a set of landmark candidates in cooperation with the rest of agents
                        if (m.getMessageType() == MessageContentLandmarkGraph.COMMON_PRECS_STAGE) {
                            String l = m.getLiteral();

                            //Locate the literal associated to the info received from the baton agent (LMLiteral obj)
                            obj = locateLGNode(l, level);
                            /*if(obj != null)
                                System.out.println("Participant agent " + comm.getThisAgentName() + " analyzing landmark (" + obj.toString() + ")");
                            else
                                System.out.println("Participant agent " + comm.getThisAgentName() + " didn't locate current landmark");
                            */
                            //Obtain the set A of actions of this agent that produce the LGNode obj, whose info has been received
                            this.getProducers(obj, l);

                            //Clear I and U arrays before refilling them
                            I.clear();
                            U.clear();
                            D.clear();
                            //Once A is calculated, I and U are also computed
                            //getCommonNonCommonPrecs is only launched if there are producers, that is, if A is not an empty set
                            //If A is empty, warn the baton agent in the following message
                            if (A.size() > 0) {
                                groupCommonNonCommonPrecs(A/*, At*/);
                                messageType = MessageContentLandmarkGraph.COMMON_PRECS_STAGE;
                            } else
                                messageType = MessageContentLandmarkGraph.NO_PRODUCER_ACTIONS;

                            //Send back the list of common preconditions to the baton agent
                            //Additionally, send a hashmap that maps the preconditions and their variables
                            commonToSend.clear();
                            for (LandmarkFluent li : I)
                                commonToSend.add(new LMLiteralInfo(li, groundedTask.getAgentName(), li.getAgents()));
                            //Send back the list of disjunctions of preconditions to the baton agent
                            nonCommonToSend.clear();
                            for (LandmarkSet u : D) {
                                nonCommonToSend.add(new LMLiteralInfo(u.identify(), obj.getAgents()));
                            }

                            comm.sendMessage(comm.getBatonAgent(), new MessageContentLandmarkGraph(null, null, commonToSend, nonCommonToSend, null,
                                    null, groundedTask.getAgentName(), null, messageType), false);
                            //Receive the actual common and non common precs from baton agent
                            MessageContentLandmarkGraph precsMsg = ((MessageContentLandmarkGraph) comm.receiveMessage(comm.getBatonAgent(), false));
                            //Update global index, synchronizing it with the baton agent's index
                            globalIndex = precsMsg.getGlobalIndex();

                            commonPrecs = precsMsg.getLiterals();
                            nonCommonPrecs = precsMsg.getDisjunctions();
                            //Verification procedure: check if the precs are actually landmarks
                            //Adding nodes and transitions to the landmark graph and precs to the objs structure
                            for (LMLiteralInfo cp : commonPrecs) {
                                //System.out.println("Participant agent " + comm.getThisAgentName() + " verifying common prec \t(" + cp.getLiteral() + ")");
                                this.verifyCommonPrec(cp, obj);
                            }
                            //Verification procedure for disjunctive landmark candidates
                            for (LMLiteralInfo ncp : nonCommonPrecs) {
                                //System.out.println("Participant agent " + comm.getThisAgentName() + " verifying non common prec \t(" + ncp.getFunction() + ")");
                                this.verifyNonCommonPrec(ncp, commonPrecs, obj, level);
                            }

                            //Remove the object that has been already analyzed, in case the participant agent has it
                            removeObject(obj, level);
                        }
                        //If the participant does not know the object analyzed in this iteration,
                        //it goes directly to the verification stage
                        else if (m.getMessageType() == MessageContentLandmarkGraph.VERIFICATION_STAGE) {
                            //Update global index
                            globalIndex = m.getGlobalIndex();
                            commonPrecs = m.getLiterals();
                            nonCommonPrecs = m.getDisjunctions();
                            for (LMLiteralInfo cp : commonPrecs) {
                                //Creating a landmark node for each common prec
                                LMLiteral p = r.getLiteral(cp.getLiteral());
                                if (cp.getLevel() == 0 || cp.isGoal() || r.verifySingleLandmark(p)) {
                                    //Adding a new landmark node, if it does not exist already
                                    //and if the agent knows the literal
                                    if (p != null) {
                                        if (literalNode.get(p.getIndex()) == -1) {
                                            nodes.add(new LGNode(p));
                                            //globalIndex = nodes.get(nodes.size() - 1).setGlobalId(globalIndex);
                                            nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
                                            literalNode.set(p.getIndex(), nodes.size() - 1);
                                            newNode = nodes.get(nodes.size() - 1);
                                        } else
                                            newNode = nodes.get(literalNode.get(p.getIndex()));
                                        //Adding the common prec p to the objs structure
                                        if (!objs.get(p.getLevel()).contains(newNode))
                                            objs.get(p.getLevel()).add(newNode);
                                        //Necessary orderings are not added, as the agent does not know obj
                                    }
                                }
                            }
                            for (LMLiteralInfo ncp : nonCommonPrecs) {
                                String func = ncp.getFunction();
                                //Locate the disjunction associated to variable var
                                LandmarkSet d = locateDisjunction(func, commonPrecs);
                                ArrayList<LandmarkFluent> dlc;
                                if (d != null)
                                    dlc = d.getElements();
                                else
                                    dlc = new ArrayList<LandmarkFluent>();
                                //Verify if the disjunction is actually a landmark
                                if (/*r.verifyDisjunctiveLandmark(dlc) && */d != null) {
                                    //Add the disjunctive landmark if the agent knows it
                                    if (ncp.getAgents().contains(groundedTask.getAgentName())) {
                                        //Adding a new disjunctive landmark node (if it does not exist already)
                                        newNode = findDisjunctiveLandmarkNode(d);
                                        if (newNode == null) {
                                            newNode = new LGNode(d);
                                            nodes.add(newNode);
                                            d.setLGNode(newNode);
                                            newNode.setIndex(nodes.size() - 1);
                                            newNode.setAgents(ncp.getAgents());
                                            newNode = nodes.get(nodes.size() - 1);
                                        }
                                        //Adding the disjunctive landmark to the objs structure
                                        if (!objs.get(level - 1).contains(newNode))
                                            objs.get(level - 1).add(newNode);
                                    }
                                }
                            }

                        }
                    }
                }
            }
            //Pass baton 
            //The new baton agent should inform the participants about the size of the next level to explore
            comm.passBaton();
            visited = 1;
            while (true) {
                if (comm.batonAgent()) {
                    if (objs.get(level).size() > 0) {
                        for (String ag : comm.getOtherAgents())
                            comm.sendMessage(ag, new MessageContentLandmarkGraph(null, null, null, null, null, null, null,
                                    objs.get(level).size(), MessageContentLandmarkGraph.COMMON_PRECS_STAGE), false);
                        break;
                    } else {
                        if (visited < groundedTask.getAgentNames().length) {
                            for (String ag : comm.getOtherAgents())
                                comm.sendMessage(ag, new MessageContentLandmarkGraph(null, null, null, null, null, null, null,
                                        objs.get(level).size(), MessageContentLandmarkGraph.PASS_BATON), false);
                            visited++;
                            comm.passBaton();
                        } else {
                            if (level > 0) {
                                for (String ag : comm.getOtherAgents())
                                    comm.sendMessage(ag, new MessageContentLandmarkGraph(null, null, null, null, null, null, null,
                                            objs.get(level - 1).size(), MessageContentLandmarkGraph.CHANGE_LEVEL), false);
                                level--;
                                if (objs.get(level).size() > 0)
                                    break;
                                else {
                                    visited = 1;
                                    comm.passBaton();
                                }
                            }
                            //Level = 0 and there are no more landmarks; end the procedure
                            else {
                                for (String ag : comm.getOtherAgents())
                                    comm.sendMessage(ag, new MessageContentLandmarkGraph(null, null, null, null, null, null, null,
                                            null, MessageContentLandmarkGraph.END_PROCEDURE), false);
                                break;
                            }
                        }
                    }
                }
                //Participant agent
                else {
                    remainingObjectsMsg = ((MessageContentLandmarkGraph) comm.receiveMessage(comm.getBatonAgent(), false));
                    if (remainingObjectsMsg.getMessageType() == MessageContentLandmarkGraph.END_PROCEDURE)
                        break;
                    remainingObjects = remainingObjectsMsg.getNextLevelSize();
                    if (remainingObjectsMsg.getMessageType() == MessageContentLandmarkGraph.PASS_BATON) {
                        visited++;
                        comm.passBaton();
                    } else if (remainingObjectsMsg.getMessageType() == MessageContentLandmarkGraph.CHANGE_LEVEL) {
                        level--;
                        if (remainingObjects > 0)
                            break;
                        else {
                            visited = 1;
                            comm.passBaton();
                        }
                    } else
                        break;
                }
            }
        }

        //Creating the adjacency matrix
        hashLGNodes = new Hashtable<String, LandmarkNode>();
        matrix = new Boolean[nodes.size()][nodes.size()];
        for (i = 0; i < nodes.size(); i++) {
            //Fill the LGNodes hashtable
            if (nodes.get(i).isSingleLiteral())
                hashLGNodes.put(nodes.get(i).getLiteral().toString(), nodes.get(i));
            for (int j = 0; j < nodes.size(); j++)
                matrix[i][j] = false;
        }
        for (LandmarkOrdering o : edges) {
            matrix[o.getNode1().getIndex()][o.getNode2().getIndex()] = true;
        }

        //Verifying necessary orderings
        MAPostProcessing();

        //Creating the accessibility matrix
        this.computeAccessibilityMatrix();

        //Calculate global indexes for each landmark found
        this.setGlobalIndexes();

        //Set a vector of antecessors for each LGNode (single landmarks that are ordered before the LGNode
        this.setAntecessorLandmarks();
        //Sharing private landmarks to establish the total number of landmarks for this problem
        sharePrivateLandmarks();

        //Calculating reasonable orderings
        /******reasonableOrderings = getReasonableOrderings();******/
        int count = 0;
        for (LandmarkNode n : nodes) {
            if (n.isSingleLiteral() && !n.getLiteral().isGoal() && n.getLiteral().getLevel() != 0)
                count++;
        }
        //System.out.println("Agent " + comm.getThisAgentName() + " found " + count + " single landmarks (non-goal/non-initial state)");

        //System.out.println(nodes.size() + " landmarks found");
        //System.out.println(edges.size() + " necessary orderings found");
    }

    /**
     * Set, for each single landmark that is not part of the initial state nor goal,
     * which single landmarks (not part of the initial state nor goals) precede it
     */
    private void setAntecessorLandmarks() {
        ArrayList<Integer> antecessors;
        for (LandmarkNode n : nodes) {
            if (n.isSingleLiteral() && n.getLiteral().getLevel() != 0) {
                antecessors = new ArrayList<Integer>();
                for (int i = 0; i < nodes.size(); i++) {
                    if (matrix[i][n.getIndex()] == true && nodes.get(i).isSingleLiteral())
                        antecessors.add(i);
                }
                n.setAntecessors(antecessors);
            }
        }
    }

    /**
     * Agents assign a unique index to their landmark nodes
     */
    private void setGlobalIndexes() {
        int iter = 0;

        while (iter < comm.getAgentList().size()) {
            //Baton agent: set identifiers and communicate other agents the id of each shared landmark
            if (comm.batonAgent()) {
                //Initialize arrays of messages
                ArrayList<MessageContentGlobalId> messages = new ArrayList<MessageContentGlobalId>();
                ArrayList<ArrayList<GlobalIdInfo>> contents = new ArrayList<ArrayList<GlobalIdInfo>>();
                for (int i = 0; i < comm.numAgents(); i++) {
                    contents.add(new ArrayList<GlobalIdInfo>());
                }
                for (LandmarkNode n : nodes) {
                    if (n.getGlobalId() == -1 && n.isSingleLiteral()) {
                        globalIndex = n.setGlobalId(globalIndex);
                        if (n.getAgents().size() > 1) {
                            for (String name : n.getAgents()) {
                                if (!name.equals(comm.getThisAgentName())) {
                                    contents.get(comm.getAgentIndex(name)).
                                            add(new GlobalIdInfo(n.getLiteral().toString(), globalIndex - 1));
                                }
                            }
                        }
                    }
                }
                for (ArrayList<GlobalIdInfo> c : contents)
                    messages.add(new MessageContentGlobalId(c));

                for (MessageContentGlobalId m : messages)
                    m.setCurrentGlobalIndex(globalIndex);
                //Send global identifiers of the shared landmarks
                for (String ag : comm.getOtherAgents()) {
                    comm.sendMessage(ag, messages.get(comm.getAgentIndex(ag)), false);
                }
            }
            //Non-baton agent: receive landmarks and update global identifiers
            else {
                MessageContentGlobalId indexes = (MessageContentGlobalId)
                        comm.receiveMessage(comm.getBatonAgent(), false);
                for (GlobalIdInfo m : indexes.getLiterals()) {
                    hashLGNodes.get(m.getLiteral()).setGlobalId(m.getGlobalId());
                }
                //Update global index
                globalIndex = indexes.getCurrentGlobalIndex();
            }
            comm.passBaton();
            iter++;
        }
    }

    /**
     * Agents share their single landmarks, so that all the agents know the complete list of landmarks
     */
    private void sharePrivateLandmarks() {
        int agentsDone = 0;
        boolean alreadySent;
        boolean[] batonAgents = new boolean[comm.getAgentList().size()];
        for (int i = 0; i < batonAgents.length; i++)
            batonAgents[i] = false;

        ArrayList<MessageContentLandmarkSharing> received;
        ArrayList<MessageContentLandmarkSharing> receivedLandmarks = new ArrayList<MessageContentLandmarkSharing>();
        ArrayList<ArrayList<MessageContentLandmarkSharing>> dataToSend = new ArrayList<ArrayList<MessageContentLandmarkSharing>>();
        for (String ag : comm.getAgentList())
            dataToSend.add(new ArrayList<MessageContentLandmarkSharing>());

        while (agentsDone < comm.getAgentList().size()) {
            if (comm.batonAgent()) {
                for (LandmarkNode n : nodes) {
                    //Only single landmarks that are not in the initial state nor goals are considered
                    if (n.isSingleLiteral() && n.getLiteral().getLevel() != 0 && !n.getLiteral().isGoal()) {
                        //Verify if another agent has already sent the landmark
                        alreadySent = false;
                        for (String label : n.getLiteral().getAgents()) {
                            if (batonAgents[comm.getAgentIndex(label)]) {
                                alreadySent = true;
                                break;
                            }
                        }
                        //If the landmark has not been already sent, prepare to send it
                        if (!alreadySent) {
                            for (String ag : comm.getOtherAgents()) {
                                if (!n.getLiteral().getAgents().contains(ag)) {
                                    //Landmark identifier, ?index, where index is a unique integer
                                    dataToSend.get(comm.getAgentIndex(ag)).
                                            add(new MessageContentLandmarkSharing(n.getGlobalId(), n.getLiteral().getAgents()));
                                }
                            }
                        }
                    }
                }
                //Send landmarks to the rest of agents
                for (String ag : comm.getOtherAgents())
                    comm.sendMessage(ag, dataToSend.get(comm.getAgentIndex(ag)), false);
            } else {
                //Receive landmarks from current baton agent
                received = (ArrayList<MessageContentLandmarkSharing>) comm.receiveMessage(comm.getBatonAgent(), false);
                receivedLandmarks.addAll(received);
            }
            //Mark the agent that has performed the baton agent role
            batonAgents[comm.getAgentIndex(comm.getBatonAgent())] = true;
            //Pass the baton agent role
            comm.passBaton();
            //Increase the number of agents that have finished sending landmarks
            agentsDone++;
        }
        //Add landmarks to the landmark tree
        otherAgentsLandmarks = new Hashtable<Integer, ArrayList<String>>();
        for (MessageContentLandmarkSharing m : receivedLandmarks) {
            otherAgentsLandmarks.put(m.getLiteralId(), m.getAgents());
        }
        //Calculate the total number of landmarks to reach (hL value of the initial empty plan)
        totalLandmarks = 0;
        for (LandmarkNode n : nodes) {
            if (n.isSingleLiteral() && n.getLiteral().getLevel() != 0/* && !n.isGoal()*/)
                totalLandmarks++;
        }
        totalLandmarks += otherAgentsLandmarks.keySet().size();
        //System.out.println("Agent " + comm.getThisAgentName() + " found " + totalLandmarks + " relevant simple landmarks");
    }

    public String toString() {
        return "Size: " + objs.get(3).size();
    }

    private void verifyCommonPrec(LMLiteralInfo commonPrec, LandmarkNode obj) {
        LandmarkNode newNode;
        //Creating a landmark node for each common prec
        LMLiteral p = RPG.getLiteral(commonPrec.getLiteral());
        if (commonPrec.getLevel() == 0 || commonPrec.isGoal() || RPG.verifySingleLandmark(p)) {
            //System.out.println("Agent " + comm.getThisAgentName() + " verified landmark " + commonPrec.getLiteral());
            //Adding a new landmark node (if it does not exist already)
            //If the agent does not know the LMLiteral p, just end the procedure
            if (p != null) {
                if (literalNode.get(p.getIndex()) == -1) {
                    nodes.add(new LGNode(p));
                    //globalIndex = nodes.get(nodes.size() - 1).setGlobalId(globalIndex);

                    nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
                    literalNode.set(p.getIndex(), nodes.size() - 1);
                    newNode = nodes.get(nodes.size() - 1);
                } else
                    newNode = nodes.get(literalNode.get(p.getIndex()));
                //Adding a new necessary ordering p -> obj to the list of edges
                if (obj != null)
                    edges.add(new LMOrdering(newNode,
                            nodes.get(obj.getIndex()), LandmarkOrdering.NECESSARY));
                //Adding the common prec p to the objs structure
                if (!objs.get(p.getLevel()).contains(newNode))
                    objs.get(p.getLevel()).add(newNode);
            }
        }
        //System.out.println("Verification finished");
    }

    private void verifyNonCommonPrec(LMLiteralInfo nonCommonPrec, ArrayList<LMLiteralInfo> commonPrecs,
                                     LandmarkNode obj, int level) {
        boolean valid;
        //Locate the disjunction associated to variable var
        LandmarkSet d = locateDisjunction(nonCommonPrec.getFunction(), commonPrecs);
        /*ArrayList<LMLiteral> dlc;
        if(d != null)
            dlc = d.getElements();
        else
            dlc = new ArrayList<LMLiteral>();*/
        //Disjunctive landmarks don't require verification
        //If the agent knows the disjunction, add a node and a transition to the graph
        if (/*RPG.verifyDisjunctiveLandmark(dlc) && */d != null) {
            //Adding a new disjunctive landmark node (if it does not exist already)
            LandmarkNode newNode = findDisjunctiveLandmarkNode(d);

            if (newNode == null) {
                newNode = new LGNode(d);
                nodes.add(newNode);
                d.setLGNode(newNode);
                newNode.setIndex(nodes.size() - 1);
                newNode.setAgents(obj.getAgents());
                newNode = nodes.get(nodes.size() - 1);
                //Adding a new necessary ordering p -> obj to the list of edges
                edges.add(new LMOrdering(newNode,
                        nodes.get(obj.getIndex()), LandmarkOrdering.NECESSARY));
            } else {
                valid = true;
                for (LandmarkOrdering o : edges) {
                    if (o.getNode1().getIndex() == obj.getIndex() &&
                            o.getNode2().getIndex() == newNode.getIndex()) {
                        valid = false;
                        break;
                    }
                }
                if (valid)
                    edges.add(new LMOrdering(newNode,
                            nodes.get(obj.getIndex()), LandmarkOrdering.NECESSARY));
            }
            //Adding the disjunctive landmark to the objs structure
            if (!objs.get(level - 1).contains(newNode))
                objs.get(level - 1).add(newNode);
        }
    }

    private LandmarkNode locateLGNode(String id, int level) {
        for (LandmarkNode l : objs.get(level))
            if (l.identify().equals(id))
                return l;
        return null;
    }

    private LandmarkSet locateDisjunction(String var, ArrayList<LMLiteralInfo> common) {
        //Locate the disjunction referred to the variable received (if any) 
        for (LandmarkSet u : D)
            if (u.identify().equals(var))
                return u;
        //The variable may refer to a precondition initially considered as a common one
        boolean found = false;
        //If a literal in I has the variable var and it has not been received in the common list
        //create a uSet from the literal and return it
        for (LandmarkFluent l : I) {
            if (l.getVar().getFuctionName().equals(var)) {
                for (LMLiteralInfo li : common)
                    if (l.toString().equals(li.getLiteral())) {
                        found = true;
                        break;
                    }
                if (!found)
                    return new uSet(l);
            }
        }
        return null;
    }

    /**
     * Removes an explored object from the objs structure
     *
     * @param lit   Identifier of the literal/disjunction
     * @param level Level of the objs structure where the object is placed
     */
    private void removeObject(LandmarkNode obj, int level) {
        objs.get(level).remove(obj);
    }
    /**
     * Removes an explored object from the objs structure
     * @param lit Identifier of the literal/disjunction
     * @param level Level of the objs structure where the object is placed
     */
    /*private void removeObject(String lit, int level) {
        LGNode l;
        for(int i = 0; i < objs.get(level).size(); i++) {
            l = objs.get(level).get(i);
            if(l.isSingleLiteral()) {
                if(l.getLiteral().toString().equals(lit))
                    objs.get(level).remove(i);
            }
            else 
                if(l.getDisjunction().identify().equals(lit))
                    objs.get(level).remove(i);
        }
    }*/

    /**
     * Find a disjunctive landmark corresponding to a uSet if it is already in the landmark graph
     *
     * @param u uSet identifying the disjunctive landmark
     * @return The disjunctive landmark, if it exists; null otherwise
     */
    private LandmarkNode findDisjunctiveLandmarkNode(LandmarkSet u) {
        if (u == null)
            return null;
        for (LandmarkNode n : this.nodes) {
            if (!n.isSingleLiteral()) {
                if (n.getDisjunction().identify().equals(u.identify())) {
                    if (n.getDisjunction().getElements().size() != u.getElements().size())
                        return null;
                    int found = 0;
                    for (LandmarkFluent ln : n.getDisjunction().getElements()) {
                        for (LandmarkFluent lu : u.getElements()) {
                            if (ln.toString().equals(lu.toString())) {
                                found++;
                                break;
                            }
                        }
                    }
                    if (found == u.getElements().size())
                        return n;
                }
            }
        }
        return null;
    }

    /**
     * Calculates I and U, common and non-common preconditions of a set of actions A.
     *
     * @param A Set of actions to analyze
     */
    private void groupCommonNonCommonPrecs(ArrayList<LandmarkAction> A/*, ArrayList<LMAction> At*/) {
        int[] common = new int[RPG.getLiterals().size()];
        int[] nonCommon = new int[RPG.getLiterals().size()];
        boolean valid;
        //String [] prod = new String[RPG.getLiterals().size()];
        ArrayList<ArrayList<LandmarkAction>> producers = new ArrayList<ArrayList<LandmarkAction>>();
        Hashtable<Integer, ArrayList<LandmarkAction>> prods = new Hashtable<Integer, ArrayList<LandmarkAction>>();

        //Hashtable<Integer,ArrayList<LMAction>> prodsNC = new Hashtable<Integer,ArrayList<LMAction>>();

        for (int i = 0; i < common.length; i++)
            common[i] = 0;
        for (LandmarkAction a : A) {
            for (LandmarkFluent l : a.getPreconditions()) {
                common[l.getIndex()] = common[l.getIndex()] + 1;
                //prod[l.getIndex()] = a.toString();
                if (prods.get(l.getIndex()) == null)
                    prods.put(l.getIndex(), new ArrayList<LandmarkAction>());
                prods.get(l.getIndex()).add(a);
            }
        }
        /*for(LMAction a: At) {
            for(LMLiteral l: a.getPreconditions()) {
                nonCommon[l.getIndex()] = nonCommon[l.getIndex()] + 1;
                //prod[l.getIndex()] = a.toString();
                if(prodsNC.get(l.getIndex()) == null)
                    prodsNC.put(l.getIndex(), new ArrayList<LMAction>());
                prodsNC.get(l.getIndex()).add(a);
            }
        }*/
        for (int i = 0; i < common.length; i++) {
            //If the literal is common, check if all the actions in A do introdce it
            if (common[i] == A.size()) {
                valid = true;
                for (LandmarkAction a : A) {
                    if (!prods.get(i).contains(a)) {
                        valid = false;
                        break;
                    }
                }
                if (valid)
                    I.add(RPG.getIndexLiterals().get(i));
            }
            //Otherwise, prepare the U structure and calculate the producer actions
            /*else if(common[i] > 0) {
                U.add(RPG.getIndexLiterals().get(i));
                producers.add(prods.get(i));
            }*/
        }
        for (int i = 0; i < nonCommon.length; i++) {
            if (nonCommon[i] > 0 && nonCommon[i] < A.size()) {
                U.add(RPG.getIndexLiterals().get(i));
                producers.add(prods.get(i));
            }
        }

        //D stores the uSets found
        if (U.size() > 0)
            D = groupUSet(U, A, producers);
    }

    private ArrayList<LandmarkSet> groupUSet(ArrayList<LandmarkFluent> u, ArrayList<LandmarkAction> A,
                                             ArrayList<ArrayList<LandmarkAction>> producers) {
        ArrayList<LandmarkSet> D = new ArrayList<LandmarkSet>();
        Hashtable<String, uSet> hashU = new Hashtable<String, uSet>();
        Hashtable<String, ArrayList<String>> hashProducers = new Hashtable<String, ArrayList<String>>();
        uSet set;

        //Group the preconditions according to their functions
        for (int i = 0; i < u.size(); i++) {
            LandmarkFluent l = u.get(i);
            if (hashU.get(l.getVar().getFuctionName()) == null) {
                set = new uSet(l);
                hashU.put(l.getVar().getFuctionName(), set);
                //Add the producer action to the set of producers of this disjunction
                hashProducers.put(l.getVar().getFuctionName(), new ArrayList<String>());
                for (LandmarkAction p : producers.get(i))
                    hashProducers.get(l.getVar().getFuctionName()).add(p.toString());
            } else {
                hashU.get(l.getVar().getFuctionName()).addElement(l);
                //Add the producer action to the set of producers of this disjunction
                for (LandmarkAction p : producers.get(i))
                    if (!hashProducers.get(l.getVar().getFuctionName()).contains(p.toString()))
                        hashProducers.get(l.getVar().getFuctionName()).add(p.toString());
                //if(!hashProducers.get(l.getVar().getFuctionName()).contains(producers.get(i)))
                //    hashProducers.get(l.getVar().getFuctionName()).add(producers.get(i).toString());
            }
        }
        //Verify if the uSets are correct
        //All the actions must have provided the uSet with at least a precondition of each type
        for (String s : hashU.keySet()) {
            if (hashProducers.get(s).size() == A.size()/* && hashU.get(s).getElements().size() == A.size()*/)
                D.add(hashU.get(s));
        }
        return D;
    }

    /**
     * Obtains the set of common precs of a literal, considering all the agents that attain it.
     * The method intersects the set of common precs of each agent involved in the literal.
     *
     * @param received Sets of landmarks received from each other participating agent
     */
    private void groupMACommonNonCommonPrecs(ArrayList<MessageContentLandmarkGraph> received, boolean batonHasCommonActions, ArrayList<String> agents) {
        Hashtable<String, Integer> hashCommonPrecs = new Hashtable<String, Integer>();
        Hashtable<String, LMLiteralInfo> hashInfo = new Hashtable<String, LMLiteralInfo>();
        Hashtable<String, Integer> hashNonCommonPrecs = new Hashtable<String, Integer>();
        int baton = 0;

        commonToSend = new ArrayList<LMLiteralInfo>();
        nonCommonToSend = new ArrayList<LMLiteralInfo>();

        //Consider only the agents that have one or more producer actions
        ArrayList<ArrayList<LMLiteralInfo>> receivedLits = new ArrayList<ArrayList<LMLiteralInfo>>();
        ArrayList<ArrayList<LMLiteralInfo>> receivedDisjs = new ArrayList<ArrayList<LMLiteralInfo>>();
        for (MessageContentLandmarkGraph m : received) {
            if (m.getMessageType() != MessageContentLandmarkGraph.NO_PRODUCER_ACTIONS) {
                receivedLits.add(m.getLiterals());
                receivedDisjs.add(m.getDisjunctions());
            }
        }

        //Add this agent's literals to the hashtable
        if (batonHasCommonActions) {
            for (LandmarkFluent l : I) {
                hashInfo.put(l.toString(), new LMLiteralInfo(l, groundedTask.getAgentName(), l.getAgents()));
                hashCommonPrecs.put(l.toString(), 1);
            }
            for (LandmarkSet u : D) {
                hashInfo.put(u.identify(), new LMLiteralInfo(u.identify(), agents));
                hashNonCommonPrecs.put(u.identify(), 1);
            }
            baton = 1;
        }
        //Add the rest of agents' literals
        for (ArrayList<LMLiteralInfo> agLits : receivedLits) {
            for (LMLiteralInfo s : agLits) {
                if (hashCommonPrecs.get(s.getLiteral()) == null) {
                    hashCommonPrecs.put(s.getLiteral(), 1);
                    hashInfo.put(s.getLiteral(), s);
                } else
                    hashCommonPrecs.put(s.getLiteral(), hashCommonPrecs.get(s.getLiteral()) + 1);
            }
        }
        for (ArrayList<LMLiteralInfo> agDisjs : receivedDisjs) {
            for (LMLiteralInfo disj : agDisjs) {
                String su = disj.getFunction();
                if (hashNonCommonPrecs.get(su) == null) {
                    hashInfo.put(su, disj);
                    hashNonCommonPrecs.put(su, 1);
                } else
                    hashNonCommonPrecs.put(su, hashNonCommonPrecs.get(su) + 1);
            }
        }

        //Calculate which preconditions are actually common and prepare the information to be sent to the rest of agents
        for (String key : hashCommonPrecs.keySet()) {
            if (hashCommonPrecs.get(key) == receivedLits.size() + baton) {
                commonToSend.add(hashInfo.get(key));
            }
            //A precondition that is common to all the actions of an agent may actually be a part of a disjoint set
            else {
                //Add up the number of agents that have the literal key as a common precondition
                if (hashNonCommonPrecs.get(hashInfo.get(key).getFunction()) == null) {
                    hashInfo.put(hashInfo.get(key).getFunction(),
                            new LMLiteralInfo(hashInfo.get(key).getFunction(), agents));
                    hashNonCommonPrecs.put(hashInfo.get(key).getFunction(), 1);
                } else
                    hashNonCommonPrecs.put(hashInfo.get(key).getFunction(), hashNonCommonPrecs.get(hashInfo.get(key).getFunction()) + 1);
            }
        }
        //Calculate actual disjunctions and prepare to send their identifiers
        for (String key : hashNonCommonPrecs.keySet()) {
            //If all the agents that have producer actions have a set of non-common preconditions of this type
            //(or a single precondition that has not been confirmed as a single landmark)
            //send back the disjunction as a confirmed disjunctive landmark
            if (hashNonCommonPrecs.get(key) == (receivedLits.size() + baton)) {
                nonCommonToSend.add(hashInfo.get(key));
            }
        }
        //Prepare the list of common literals to be sent to each agent
        /*commonToSend.clear();
        for(String ag: groundedTask.getAgentNames())
            commonToSend.add(new ArrayList<LMLiteralInfo>());
        for(LMLiteralInfo c: common) {
            for(String ag: comm.getOtherAgents()) {
                //Send every literal to the rest of agents
                //The receiver agent will discard the literal if it doesn't find it
                //if(c.getAgents().contains(ag))
                commonToSend.get(hashAgents.get(ag)).add(c);
            }
        }*/
        //Prepare the list of common literals to be sent to each agent
        /*nonCommonToSend.clear();
        for(String ag: groundedTask.getAgentNames())
            nonCommonToSend.add(new ArrayList<LMLiteralInfo>());
        for(LMLiteralInfo c: nonCommon) {
            for(String ag: comm.getOtherAgents()) {
                if(c.getAgents().contains(ag))
                    nonCommonToSend.get(hashAgents.get(ag)).add(c);
            }
        }*/
    }


    private boolean[][] getReasonableOrderings() {
        reasonableOrderingsList = new ArrayList<LandmarkOrdering>();
        reasonableOrderingsGoalsList = new ArrayList<LandmarkOrdering>();

        boolean[][] R = new boolean[nodes.size()][nodes.size()];
        for (int i = 0; i < nodes.size(); i++)
            for (int j = 0; j < nodes.size(); j++)
                R[i][j] = false;
        //ArrayList<LMOrdering> R = new ArrayList<LMOrdering>();
        //Calculating mutex matrix
        mutexMatrix = computeMutexFacts();

        boolean[][] studied = new boolean[nodes.size()][nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                studied[i][j] = false;
            }
        }
        
        /*boolean[][] visited = new boolean[N.size()][N.size()];
        for(int i = 0; i < N.size(); i++)
            for(int j = 0; j < N.size(); j++)
                visited[i][j] = false;*/

        for (LandmarkFluent g1 : RPG.getGoals()) {
            for (LandmarkFluent g2 : RPG.getGoals()) {
                if (g1 != g2)
                    searchReasonableOrdering(nodes.get(literalNode.get(g1.getIndex())), nodes.get(literalNode.get(g2.getIndex())), studied, R);
            }
        }

        for (LandmarkOrdering o : edges) {
            if (o.getNode1().isSingleLiteral() && o.getNode2().isSingleLiteral()) {
                for (LandmarkOrdering o2 : edges) {
                    if (o2.getNode1().isSingleLiteral() && o2.getNode2().isSingleLiteral()) {
                        if (o.getNode2() == o2.getNode2() && o.getNode1() != o2.getNode1())
                            searchReasonableOrdering(o.getNode1(), o2.getNode1(), studied, R);
                    }
                }
            }
        }

        return R;
    }

    private void searchReasonableOrdering(LandmarkNode ln, LandmarkNode l1, boolean[][] studied, boolean[][] R) {
        LandmarkNode li;
        LandmarkOrdering o, ro;
        for (int i = 0; i < edges.size(); i++) {
            o = edges.get(i);
            if (o.getNode2().getIndex() == ln.getIndex()) {
                li = o.getNode1();
                if (li.isSingleLiteral() && li != l1) {
                    //if(matrix[i][ln.getIndex()] && li.isSingleLiteral() && li.getIndex() != literalNode.get(ln.getIndex())) {
                    if (!studied[li.getIndex()][l1.getIndex()]) {
                        //visited[li.getIndex()][l1.getIndex()] = true;//<- PRUEBA!!/////////////////////////
                        searchReasonableOrdering(li, l1, studied, R);
                    }
                    //}
                }
            }
        }

        LandmarkNode l = ln;


        if (!ignore(l, l1, studied, R)) {
            studied[l.getIndex()][l1.getIndex()] = true;
            if (interfere(l, l1)) {
                R[l.getIndex()][l1.getIndex()] = true;
                ro = new LMOrdering(l, l1, LandmarkOrdering.REASONABLE);
                reasonableOrderingsList.add(ro);
                if (l.getLiteral().isGoal() && l1.getLiteral().isGoal())
                    reasonableOrderingsGoalsList.add(ro);
            }
        }//studied[l.getIndex()][l1.getIndex()] = true;
    }

    boolean ignore(LandmarkNode l1, LandmarkNode l2, boolean[][] studied, boolean[][] R) {
        //A pair of literals l1 and l2 should be ignored if:
        //The pair has been studied yet
        if (studied[l1.getIndex()][l2.getIndex()]) return true;
        //l1 <= l2 or l2 <= l1, i.e., there is a reasonable ordering among the nodes
        if (R[l1.getIndex()][l2.getIndex()] || R[l2.getIndex()][l1.getIndex()]) return true;
        //l2 is a collateral effect of l1
        for (LandmarkAction a : l1.getLiteral().getTotalProducers()) {
            for (LandmarkFluent eff : a.getEffects()) {
                if (eff == l2.getLiteral())
                    return true;
            }
        }

        return false;
    }

    boolean interfere(LandmarkNode l, LandmarkNode l1) {
        Integer[] lits = new Integer[RPG.getLiterals().size()];
        for (int i = 0; i < lits.length; i++) lits[i] = 0;
        //Two goals l and l1 interfere if one of the following holds:
        //1 - If there exists a literal x that is an effect in all the actions that produce l; x is inconsistent with l1
        for (LandmarkAction a : l.getLiteral().getTotalProducers()) {
            for (LandmarkFluent eff : a.getEffects()) {
                if (eff != l.getLiteral())
                    lits[eff.getIndex()]++;
            }
        }
        for (int i = 0; i < lits.length; i++) {
            if (lits[i] == l.getLiteral().getTotalProducers().size()) {
                if (inconsistent(RPG.getLiterals().get(i), l1.getLiteral()))
                    return true;
            }
        }
        //2 - If l1 is erased by all the actions that add l
        int count = 0;
        for (LandmarkAction a : l.getLiteral().getTotalProducers()) {
            for (LandmarkFluent eff : a.getEffects()) {
                if (eff.getVar() == l1.getLiteral().getVar() && !eff.getValue().equals(l1.getLiteral().getValue())) {
                    count++;
                    break;
                }
            }
        }
        if (count == l.getLiteral().getTotalProducers().size())
            return true;
        //3 - If there exists a landmark x such as x <=n l1 and x is inconsistent with l
        for (int i = 0; i < nodes.size(); i++) {
            if (matrix[i][l.getIndex()] && nodes.get(i).isSingleLiteral())
                if (inconsistent(nodes.get(i).getLiteral(), l1.getLiteral()))
                    return true;
        }

        return false;
    }

    private boolean inconsistent(LandmarkFluent l1, LandmarkFluent l2) {
        return mutexMatrix[l1.getIndex()][l2.getIndex()];
    }

    private boolean[][] computeMutexFacts() {
        boolean[] visitedActions = new boolean[RPG.getActions().size()];
        boolean[] facts = new boolean[RPG.getLiterals().size()];
        boolean[] factsPrev = new boolean[RPG.getLiterals().size()];
        mutexMatrix = new boolean[RPG.getLiterals().size()][RPG.getLiterals().size()];
        boolean mutexPrev[][] = new boolean[RPG.getLiterals().size()][RPG.getLiterals().size()];

        ArrayList<LandmarkFluent> pre, add, del, newEff;
        ArrayList<LandmarkFluent> oldEff = new ArrayList<LandmarkFluent>();

        int i, j;
        //Initializing data structures
        for (i = 0; i < RPG.getActions().size(); i++)
            visitedActions[i] = false;
        for (i = 0; i < RPG.getLiterals().size(); i++) {
            facts[i] = false;
            factsPrev[i] = false;
            for (j = 0; j < RPG.getLiterals().size(); j++) {
                mutexMatrix[i][j] = false;
                mutexPrev[i][j] = false;
            }
        }
        for (LMLiteral l : RPG.getInitialState()) {
            facts[l.getIndex()] = true;
        }
        //Main loop - iterate until facts and mutex remain unchanged
        while (notEqual(facts, factsPrev) || notEqualMat(mutexMatrix, mutexPrev)) {
            //Copying the arrays used in the previous iteration
            copy(factsPrev, facts);
            copyMat(mutexPrev, mutexMatrix);

            LandmarkAction a;
            for (i = 0; i < RPG.getActions().size(); i++) {
                a = RPG.getActions().get(i);
                if (isApplicable(a, facts) && !hasMutex(a, mutexMatrix)) {
                    //Calculating structures concerning current action a
                    pre = a.getPreconditions();
                    add = a.getEffects();
                    del = calculateDelEffects(a.getEffects());
                    oldEff.clear();
                    newEff = calculateNewEffects(a.getEffects(), facts, oldEff);

                    //Check all the pairs of literals (prop1, prop2) such that prop1 is in newEff and prop2 belongs to del 
                    for (LandmarkFluent prop1 : newEff) {
                        for (LandmarkFluent prop2 : del) {
                            //Mark the pair as mutex
                            mutexMatrix[prop1.getIndex()][prop2.getIndex()] = true;
                            mutexMatrix[prop2.getIndex()][prop1.getIndex()] = true;

                            //If prop2 belongs to pre
                            if (pre.contains(prop2)) {
                                //Check all the literals prop3
                                for (LMLiteral prop3 : RPG.getLiterals()) {
                                    //If prop3 is mutex with prop2 and prop3 is not in del
                                    if (prop1 != prop3 && mutexMatrix[prop2.getIndex()][prop3.getIndex()] && !del.contains(prop3)) {
                                        //Prop3 is also mutex with prop1
                                        mutexMatrix[prop1.getIndex()][prop3.getIndex()] = true;
                                        mutexMatrix[prop3.getIndex()][prop1.getIndex()] = true;
                                    }
                                }
                            }
                        }
                    }
                    //Remove potential mutex
                    //If the action has not been visited yet
                    if (!visitedActions[i]) {
                        //Check pairs of literals (prop1, prop2), where prop1 and prop2 belong to add
                        if (add.size() > 1) {
                            for (LandmarkFluent prop1 : add) {
                                for (LandmarkFluent prop2 : add) {
                                    if (mutexMatrix[prop1.getIndex()][prop2.getIndex()]) {
                                        //If they are marked as mutex, we remove the mutex
                                        //As they are both add effects of the same action, they can occur on the same state
                                        mutexMatrix[prop1.getIndex()][prop2.getIndex()] = false;
                                        mutexMatrix[prop2.getIndex()][prop1.getIndex()] = false;
                                    }
                                }
                            }
                        }
                    }
                    //Processing oldEff, add effects of a that are not new
                    for (LandmarkFluent prop1 : oldEff) {
                        for (LandmarkFluent prop2 : RPG.getLiterals()) {
                            //If there is a literal prop2 that is mutex with prop1, is not in del,
                            //and there is not a literal in pre that is mutex with prop2
                            if (mutexMatrix[prop1.getIndex()][prop2.getIndex()] &&
                                    !checkMutex(pre, prop2, mutexMatrix) && !del.contains(prop2)) {
                                //We erase the mutex (prop1, prop2)
                                mutexMatrix[prop1.getIndex()][prop2.getIndex()] = false;
                                mutexMatrix[prop2.getIndex()][prop1.getIndex()] = false;
                            }
                        }
                    }
                    //Adding the new facts to the list of facts
                    addNewFacts(facts, newEff);
                    //Marking the current action as visited
                    visitedActions[i] = true;
                }
            }
        }
        return mutexMatrix;
    }

    private boolean checkMutex(ArrayList<LandmarkFluent> pre, LandmarkFluent prop, boolean[][] mutex) {
        for (LandmarkFluent p : pre) {
            if (mutex[p.getIndex()][prop.getIndex()])
                return true;
        }
        return false;
    }

    private void addNewFacts(boolean[] facts, ArrayList<LandmarkFluent> newEff) {
        for (LandmarkFluent n : newEff)
            facts[n.getIndex()] = true;
    }

    private ArrayList<LandmarkFluent> calculateNewEffects(ArrayList<LandmarkFluent> effs, boolean[] facts,
                                                          ArrayList<LandmarkFluent> old) {
        ArrayList<LandmarkFluent> newEff = new ArrayList<LandmarkFluent>();

        for (LandmarkFluent e : effs) {
            if (!facts[e.getIndex()])
                newEff.add(e);
            else
                old.add(e);
        }

        return newEff;
    }

    private ArrayList<LandmarkFluent> calculateDelEffects(ArrayList<LandmarkFluent> effs) {
        ArrayList<LandmarkFluent> del = new ArrayList<LandmarkFluent>();
        //The delete effects are all the literals that share their variable with an effect e, and have a different value than e
        for (LandmarkFluent e : effs) {
            for (LandmarkFluent l : RPG.getLiterals()) {
                if (e.getVar() == l.getVar() && !e.getValue().equals(l.getValue())) {
                    del.add(l);
                }
            }
        }

        return del;
    }

    private boolean hasMutex(LandmarkAction a, boolean[][] mutex) {
        for (LandmarkFluent p1 : a.getPreconditions()) {
            for (LandmarkFluent p2 : a.getPreconditions())
                if (mutex[p1.getIndex()][p2.getIndex()])
                    return true;
        }
        return false;
    }

    private boolean isApplicable(LandmarkAction a, boolean[] facts) {
        for (LandmarkFluent p : a.getPreconditions()) {
            if (!facts[p.getIndex()])
                return false;
        }
        return true;
    }

    private void copyMat(boolean[][] v1, boolean[][] v2) {
        for (int i = 0; i < v1.length; i++)
            copy(v1[i], v2[i]);
    }

    private void copy(boolean[] v1, boolean[] v2) {
        for (int i = 0; i < v1.length; i++)
            v1[i] = v2[i];
    }

    private boolean notEqualMat(boolean[][] v1, boolean[][] v2) {
        for (int i = 0; i < v1[0].length; i++) {
            if (notEqual(v1[i], v2[i]))
                return true;
        }
        return false;
    }

    private boolean notEqual(boolean[] v1, boolean[] v2) {
        for (int i = 0; i < v1.length; i++) {
            if (v1[i] != v2[i])
                return true;
        }
        return false;
    }

    /**
     * Postprocessing that verifies the edges of the landmark tree
     */
    private void MAPostProcessing() {
        //A: Actions that produce a landmark g
        ArrayList<LandmarkAction> A = new ArrayList<LandmarkAction>();
        ArrayList<MessageContentPostProcessing> orderings = new ArrayList<MessageContentPostProcessing>();
        int i, j;
        boolean ordering;
        boolean done = false;
        ArrayList<LandmarkOrdering> auxEdges;

        //Edges hashtable: true -> verified edge; false -> non-verified edge
        Hashtable<String, Boolean> hashEdges = new Hashtable<String, Boolean>();
        for (LandmarkOrdering e : edges)
            if (e.getNode1().isSingleLiteral() && e.getNode2().isSingleLiteral())
                hashEdges.put(e.getNode1().getLiteral().toString() + " -> " + e.getNode2().getLiteral().toString(), false);

        while (true) {
            auxEdges = new ArrayList<LandmarkOrdering>();
            for (LandmarkOrdering e : edges)
                auxEdges.add(e);

            if (comm.batonAgent()) {
                orderings.clear();
                done = true;
                for (i = 0; i < nodes.size(); i++) {
                    //Only single literals are processed
                    if (nodes.get(i).isSingleLiteral()) {
                        for (j = 0; j < nodes.size(); j++) {
                            //Check g column of the matrix to find literals l such that l <=n g
                            if (matrix[j][i] == true) {
                                if (nodes.get(j).isSingleLiteral()) {
                                    if (hashEdges.get(nodes.get(j).getLiteral().toString() + " -> " + nodes.get(i).getLiteral().toString()) == false)
                                        orderings.add(new MessageContentPostProcessing(nodes.get(j).getLiteral().toString(), nodes.get(i).getLiteral().toString()));
                                }
                            }
                        }
                    }
                }
                //Send list of orderings to verify
                for (String ag : comm.getOtherAgents())
                    comm.sendMessage(ag, orderings, false);
                //Verify the list of orderings
                for (MessageContentPostProcessing o : orderings) {
                    A = this.getActions(RPG.getHashLiterals().get(o.getLiteral1()),
                            RPG.getHashLiterals().get(o.getLiteral2()));

                    //System.out.println("Baton agent " + comm.getThisAgentName() +  " verifying necessary ordering " + o.getLiteral1() + " -> " + o.getLiteral2());

                    //Mark the necessary ordering as verified
                    hashEdges.put(o.getLiteral1() + " -> " + o.getLiteral2(), true);
                    //Remove the edge in case it is not verified
                    if (!RPG.verifyEdge(A)) {
                        //System.out.println("Necessary ordering " + o.getLiteral1() + " -> " + o.getLiteral2() + " not verified");
                        for (LandmarkOrdering e : edges) {
                            if (e.getNode1().isSingleLiteral() && e.getNode2().isSingleLiteral()) {
                                if (e.getNode1().getLiteral().getIndex() == RPG.getHashLiterals().get(o.getLiteral1()).getIndex() &&
                                        e.getNode2().getLiteral().getIndex() == RPG.getHashLiterals().get(o.getLiteral2()).getIndex()) {
                                    auxEdges.remove(e);
                                }
                            }
                        }
                    }
                    /*else  {
                        System.out.println("Necessary ordering " + o.getLiteral1() + " -> " + o.getLiteral2() + " verified!");
                    }*/
                }
            }
            //Participant agent
            else {
                orderings = (ArrayList<MessageContentPostProcessing>) comm.receiveMessage(comm.getBatonAgent(), false);
                //Verify the list of orderings
                for (MessageContentPostProcessing o : orderings) {
                    A.clear();
                    ordering = false;
                    if (RPG.getHashLiterals().get(o.getLiteral1()) != null) {
                        if (RPG.getHashLiterals().get(o.getLiteral2()) != null) {
                            if (matrix[this.hashLGNodes.get(o.getLiteral1()).getIndex()][this.hashLGNodes.get(o.getLiteral2()).getIndex()] == true) {
                                A = this.getActions(this.hashLGNodes.get(o.getLiteral1()).getLiteral(),
                                        this.hashLGNodes.get(o.getLiteral2()).getLiteral());
                                ordering = true;
                            }
                        }
                    }
                    //Mark the necessary ordering as verified
                    hashEdges.put(o.getLiteral1() + " -> " + o.getLiteral2(), true);
                    if (!RPG.verifyEdge(A)) {
                        if (ordering) {
                            for (LandmarkOrdering e : edges) {
                                if (e.getNode1().isSingleLiteral() && e.getNode2().isSingleLiteral()) {
                                    if (e.getNode1().getLiteral().getIndex() == RPG.getHashLiterals().get(o.getLiteral1()).getIndex() &&
                                            e.getNode2().getLiteral().getIndex() == RPG.getHashLiterals().get(o.getLiteral2()).getIndex()) {
                                        auxEdges.remove(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            edges = auxEdges;
            //Pass baton
            //If the next baton agent has already been the baton agent,
            //warn the rest of agents and finish the procedure
            comm.passBaton();
            if (comm.batonAgent()) {
                if (done) {
                    for (String ag : comm.getOtherAgents())
                        comm.sendMessage(ag, new Boolean(true), false);
                    return;
                } else {
                    for (String ag : comm.getOtherAgents())
                        comm.sendMessage(ag, new Boolean(false), false);
                }
            }
            //Non-baton agent
            if (!comm.batonAgent()) {
                boolean finish = (Boolean) comm.receiveMessage(comm.getBatonAgent(), false);
                if (finish)
                    return;
            }
        }
    }

    private void postProcessing() {
        //P: Candidate landmarks that precede necessarily a given candidate landmark g
        ArrayList<LandmarkFluent> P = new ArrayList<LandmarkFluent>();
        //A: Actions that produce a landmark g
        ArrayList<LandmarkAction> A = new ArrayList<LandmarkAction>();

        //We analyze all the literal nodes g of the Landmark Tree
        for (int i = 0; i < nodes.size(); i++) {
            //Only single literals are processed
            if (nodes.get(i).isSingleLiteral()) {
                for (int j = 0; j < nodes.size(); j++) {
                    //Check g column of the matrix to find literals l such that l <=n g
                    if (matrix[j][i] == true) {
                        if (nodes.get(j).isSingleLiteral()) {
                            A = getActions(nodes.get(j).getLiteral(), nodes.get(i).getLiteral());
                            //We check if the actions in A are necessary to reach the goals
                            if (!RPG.verify(A)) {
                                matrix[j][i] = false;
                                //We also remove the ordering from the orderings list
                                for (int ord = 0; ord < edges.size(); ord++) {
                                    LandmarkOrdering e = edges.get(ord);
                                    if (e.getNode1().isSingleLiteral() && e.getNode2().isSingleLiteral()) {
                                        if (e.getNode1().getIndex() == j && e.getNode2().getIndex() == i) {
                                            edges.remove(ord);
                                            ord--;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates the actions that generate the edge l1 -> l2
     *
     * @param l1
     * @param l2
     * @return
     */
    private ArrayList<LandmarkAction> getActions(LandmarkFluent l1, LandmarkFluent l2) {
        ArrayList<LandmarkAction> A = new ArrayList<LandmarkAction>();

        for (LandmarkAction a : RPG.getActions()) {
            for (LandmarkFluent pre : a.getPreconditions()) {
                if (l1 == pre && l2.getTotalProducers().contains(a))
                    A.add(a);
            }
        }

        return A;
    }

    private ArrayList<uSet> groupUSet(ArrayList<LMLiteral> u, ArrayList<LMAction> A) {
        ArrayList<uSet> U = new ArrayList<uSet>();
        Hashtable<String, uSet> hashU = new Hashtable<String, uSet>();

        for (LMLiteral l : u) {
            if (hashU.get(l.getVar().getFuctionName()) == null) {
                U.add(new uSet(l));
                hashU.put(l.getVar().getFuctionName(), U.get(U.size() - 1));
            } else {
                hashU.get(l.getVar().getFuctionName()).addElement(l);
            }
        }

        for (uSet us : U)
            us.calculateValue();

        //Verify if the uSets are correct
        //All the actions must have provided the uSet with at least a precondition of each type
        int instances, actions;
        boolean visited;
        ArrayList<uSet> U1 = new ArrayList<uSet>(U.size());
        for (uSet s : U) {
            instances = 0;
            actions = 0;
            if (s.getElements().size() == 1) {
                continue;
            }
            for (LMAction a : A) {
                visited = false;
                for (LandmarkFluent p : a.getPreconditions()) {
                    if (s.match(p)) {
                        instances++;
                        if (!visited) {
                            actions++;
                            visited = true;
                        }
                    }
                }
            }
            //If there is one instance per action, the uSet is added to u1
            if (actions == A.size() && instances == A.size())
                U1.add(s);
            else if (actions == A.size() && instances > A.size())
                analyzeSet(s, A, U1);
        }

        return U1;
    }

    private void analyzeSet(uSet s, ArrayList<LMAction> A, ArrayList<uSet> U1) {
        ArrayList<ArrayList<LandmarkFluent>> literalProducers = new ArrayList<ArrayList<LandmarkFluent>>(A.size());
        int i;
        LMAction a;
        uSet u;

        for (i = 0; i < A.size(); i++)
            literalProducers.add(new ArrayList<LandmarkFluent>());

        //Grouping the literals in the set according to the actions that generated them
        for (i = 0; i < A.size(); i++) {
            a = A.get(i);
            for (LandmarkFluent p : a.getPreconditions()) {
                if (p.getVar().getFuctionName().equals(s.identify())) {
                    literalProducers.get(i).add(p);
                }
            }
        }

        ArrayList<LandmarkFluent> actionLiterals;
        LandmarkFluent similar;
        boolean finish = false;
        //An uSet has only one element per action in A
        for (LandmarkFluent l : literalProducers.get(0)) {
            if (finish)
                break;
            u = new uSet(l);
            for (i = 1; i < literalProducers.size(); i++) {
                if (literalProducers.get(i).isEmpty()) {
                    finish = true;
                    break;
                }
                actionLiterals = literalProducers.get(i);
                similar = equalParameters(l, actionLiterals);
                actionLiterals.remove(similar);
                u.addElement(similar);
            }
            U1.add(u);
        }
    }

    private LandmarkFluent equalParameters(LandmarkFluent l, ArrayList<LandmarkFluent> actionLiterals) {
        ArrayList<LandmarkFluent> candidates = new ArrayList<LandmarkFluent>(actionLiterals.size());
        ArrayList<LandmarkFluent> auxCandidates = new ArrayList<LandmarkFluent>(actionLiterals.size());
        String p1[] = l.getVar().getParams(), p2[];
        int equalParameters, min;
        boolean equal;
        LandmarkFluent candidate;

        for (LandmarkFluent al : actionLiterals)
            candidates.add(al);

        //Check if the candidate and the target literal are equal
        for (LandmarkFluent c : candidates) {
            p2 = c.getVar().getParams();
            equal = true;
            for (int i = 0; i < p1.length; i++) {
                if (!p1[i].equals(p2[i])) {
                    equal = false;
                    break;
                }
            }
            if (equal)
                auxCandidates.add(c);
        }

        //If there is only one candidate left, return it
        if (auxCandidates.size() == 1)
            return auxCandidates.get(0);
        else if (auxCandidates.size() > 1) {
            candidates = auxCandidates;
            auxCandidates = new ArrayList<LandmarkFluent>(actionLiterals.size());
        }
        //If there are no candidates left, apply next criteria
        else {
            auxCandidates = new ArrayList<LandmarkFluent>();
        }

        min = Integer.MAX_VALUE;
        candidate = candidates.get(0);
        String[] lt, ct;
        int j;
        //Check which candidate has most parameters of the same type than the target literal
        for (LandmarkFluent c : candidates) {
            equalParameters = 0;
            for (int i = 0; i < c.getVar().getParams().length; i++) {
                ct = this.groundedTask.getObjectTypes(c.getVar().getParams()[i]);
                lt = this.groundedTask.getObjectTypes(l.getVar().getParams()[i]);

                for (j = 0; j < ct.length; j++) {
                    if (!ct[j].equals(lt[j]))
                        break;
                }

                if (j == ct.length - 1)
                    equalParameters++;

                //equalParameters = equalParameters + Math.abs(l.getVar().getParams()[i].compareTo(c.getVar().getParams()[i]));
            }
            if (min > equalParameters) {
                min = equalParameters;
                candidate = c;
            }

        }

        return candidate;
    }

    private void computeAccessibilityMatrix() {
        int i, j, k;

        //Generate accessibility matrix
        for (k = 0; k < nodes.size(); k++) {
            for (i = 0; i < nodes.size(); i++) {
                for (j = 0; j < nodes.size(); j++) {
                    if (this.matrix[i][j] != true) {
                        if (this.matrix[i][k] == true && this.matrix[k][j] == true)
                            this.matrix[i][j] = true;
                    }
                }
            }
        }
    }

    public LandmarkNode getNode(LMLiteral l) {
        if (literalNode.get(l.getIndex()) != -1)
            return nodes.get(literalNode.get(l.getIndex()));
        else
            return null;
    }

    public boolean getReasonableOrdering(LGNode n1, LGNode n2) {
        if (reasonableOrderings[n1.getIndex()][n2.getIndex()])
            return true;
        return false;
        /*for(LMOrdering o: reasonableOrderings) {
            if(o.getIndex1().getIndex() == n1.getIndex()) {
                if(o.getIndex2().getIndex() == n2.getIndex()) 
                    return true;
            }
        }
        return false;*/
    }

    public ArrayList<LandmarkOrdering> getReasonableOrderingList() {
        return this.reasonableOrderingsList;
    }

    public ArrayList<LandmarkOrdering> getReasonableGoalOrderingList() {
        return this.reasonableOrderingsGoalsList;
    }
    
    
    /*public String toString() {
        String res = "";
        for(LMOrdering o: edges)
            res += o.getIndex1().toString() + " -> " + o.getIndex2().toString() + "\n";
        
        for(int i = 0; i < nodes.size(); i++) {
            for(int j = 0; j < nodes.size(); j++) {
                if(matrix[i][j] == true)
                    res += 1 + " ";
                else
                    res += 0 + " ";
            }
            res += "\n";
        }
        res += "\nMutex matrix: \n";
        
        for(int i = 0; i < r.getLiterals().size(); i++) {
            for(int j = 0; j < r.getLiterals().size(); j++) {
                if(mutexMatrix[i][j] == true)
                    res += r.getLiterals().get(i) + " <-> " + r.getLiterals().get(j) + "\n";
            }
        }
        
        res += "\nReasonable orderings: \n";
        
        for(int i = 0; i < nodes.size(); i++) {
            for(int j = 0; j < nodes.size(); j++) {
                if(reasonableOrderings[i][j] == true)
                    res += nodes.get(i) + " -> " + nodes.get(j) + "\n";
            }
        }
        
        return res;
    }*/

    public ArrayList<LandmarkOrdering> getNeccessaryGoalOrderingList() {
        ArrayList<LandmarkOrdering> res = new ArrayList<LandmarkOrdering>();
        for (LandmarkOrdering o : edges) {
            if (o.getNode1().isSingleLiteral() && o.getNode2().isSingleLiteral()) {
                if (o.getNode1().getLiteral().isGoal() && o.getNode2().getLiteral().isGoal())
                    res.add(o);
            }
        }
        return res;
    }

    public ArrayList<LandmarkOrdering> getNeccessaryOrderingList() {
        return edges;
    }

    /**
     * Gets the actions that produce a LGNode
     *
     * @param obj     LGNode to analyze
     * @param literal Literal associated to the LGNode
     */
    private void getProducers(LandmarkNode obj, String literal) {
        if (obj != null) {
            A = obj.getProducers();
            //At = obj.getTotalProducers();
        }
        //Participant agents, in case they don't have the LGNode
        else {
            A = new ArrayList<LandmarkAction>();
            //At = new ArrayList<LMAction>();

            if (RPG.getHashLiterals().get(literal) != null) {
                //System.out.println("Participant agent " + comm.getThisAgentName() + ": LGNode not known, LMLiteral known");
                A = RPG.getHashLiterals().get(literal).getProducers();
                //At = RPG.getHashLiterals().get(literal).getTotalProducers();
            }
        }
    }

    public ArrayList<LandmarkNode> getNodes() {
        return nodes;
    }

    @Override
    public int numGlobalNodes() {
        //System.out.println("Agent " + comm.getThisAgentName() + " has " + globalIndex + " global nodes");
        return this.totalLandmarks;
    }

    @Override
    public int numTotalNodes() {
        return globalIndex;
    }
}
