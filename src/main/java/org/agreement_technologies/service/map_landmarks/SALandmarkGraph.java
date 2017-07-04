package org.agreement_technologies.service.map_landmarks;

import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_landmarks.*;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Tree of landmarks
 *
 * @author Alex
 */
public class SALandmarkGraph implements LandmarkGraph {
    private final GroundedTask groundedTask;
    private ArrayList<LandmarkNode> nodes;
    private ArrayList<LandmarkOrdering> edges;
    private RPG r;
    private Boolean[][] matrix;
    private ArrayList<Integer> literalNode;
    private Hashtable<String, Integer> maLiteralNode;
    private ArrayList<ArrayList<LandmarkFluent>> objs;
    private ArrayList<ArrayList<LandmarkSet>> disjObjs;
    private boolean[][] mutexMatrix;
    private boolean[][] reasonableOrderings;
    private ArrayList<LandmarkOrdering> reasonableOrderingsList;
    private ArrayList<LandmarkOrdering> reasonableOrderingsGoalsList;

    public SALandmarkGraph(GroundedTask gt, RPG r, ArrayList<LandmarkFluent> g) {
        groundedTask = gt;
        //N: Landmark tree nodes
        nodes = new ArrayList<LandmarkNode>();
        //E: landmark tree links (necessary orderings)
        edges = new ArrayList<LandmarkOrdering>();
        //A: set of actions that produce a certain literal or disjunction of literals
        ArrayList<LandmarkAction> A;
        //r: RPG associated to the LT
        this.r = r;

        //literalNode maps literals and nodes of the landmark graph
        literalNode = new ArrayList<Integer>(r.getLiterals().size());
        for (int i = 0; i < r.getLiterals().size(); i++)
            literalNode.add(-1);

        //Initializing objs array
        objs = new ArrayList<ArrayList<LandmarkFluent>>(r.getLitLevels().size());
        for (int i = 0; i < r.getLitLevels().size(); i++)
            objs.add(new ArrayList<LandmarkFluent>());
        //Initializing disjObjs array
        disjObjs = new ArrayList<ArrayList<LandmarkSet>>(r.getLitLevels().size());
        for (int i = 0; i < r.getLitLevels().size(); i++)
            disjObjs.add(new ArrayList<LandmarkSet>());

        //Adding goals to N and objs(lvl), where lvl is the RPG level in which each goal first appears
        for (LandmarkFluent goal : g) {
            nodes.add(new LGNode(goal));
            nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
            literalNode.set(goal.getIndex(), nodes.size() - 1);
            objs.get(goal.getLevel()).add(goal);
        }

        //The RPG is explored backwards, beginning from the last literal level
        int level = r.getLitLevels().size() - 1;
        while (level > 0) {
            if (objs.get(level).size() > 0) {
                for (int i = 0; i < objs.get(level).size(); i++) {
                    LandmarkFluent obj = objs.get(level).get(i);
                    //For each literal in objs[level], we calculate the set A of actions that produce the literal
                    A = obj.getProducers();
                    //Once A is calculated, the action processing method is invoked
                    //actionProcessing is only launched if there are producers, that is, if A is not an empty set
                    if (A.size() > 0)
                        actionProcessing(A, this.nodes.get(this.literalNode.get(obj.getIndex())), level);
                }
            }
            if (disjObjs.get(level).size() > 0) {
                for (LandmarkSet disjObj : disjObjs.get(level)) {
                    //For each disjunction of literals in disjObjs[level], we calculate A
                    //as the union of the producer actions of each literal in the disjunction
                    A = new ArrayList<LandmarkAction>();
                    for (LandmarkFluent obj : disjObj.getElements()) {
                        for (LandmarkAction pa : obj.getProducers()) {
                            if (!A.contains(pa))
                                A.add(pa);
                        }
                    }
                    //Once A is calculated, the action processing method is invoked
                    actionProcessing(A, disjObj.getLTNode(), level);
                }
            }
            level--;
        }


        //Creating the adjacency matrix
        matrix = new Boolean[nodes.size()][nodes.size()];
        for (int i = 0; i < nodes.size(); i++)
            for (int j = 0; j < nodes.size(); j++)
                matrix[i][j] = false;
        for (LandmarkOrdering o : edges) {
            matrix[o.getNode1().getIndex()][o.getNode2().getIndex()] = true;
        }

        //Verifying necessary orderings
        postProcessing();

        //Creating the accessibility matrix
        //this.computeAccessibilityMatrix();

        //Calculating reasonable orderings
        //reasonableOrderings = getReasonableOrderings();
        System.out.println(nodes.size() + " landmarks found");
    }

    //Multi-agent constructor - requires only grounded task and goals; uses RPG data calculated during grounding
    /*public LandmarkGraph(GroundedTask gt, RPG r) {
        groundedTask = gt;
        //N: Landmark tree nodes
        nodes = new ArrayList<LandmarkNode>();
        //E: landmark tree links (necessary orderings)
        edges = new ArrayList<LandmarkOrdering>();
        //A: set of actions that produce a certain literal or disjunction of literals
        ArrayList<LandmarkAction> A;
        
        //literalNode maps literals and nodes of the landmark tree
        maLiteralNode = new Hashtable<String,Integer>();
        
        //Initializing objs array
        objs = new ArrayList<ArrayList<LandmarkFluent>>(r.getLitLevels().size());
        for(int i = 0; i < r.getLitLevels().size(); i++)
            objs.add(new ArrayList<LandmarkFluent>());
        //Initializing disjObjs array
        disjObjs = new ArrayList<ArrayList<LandmarkSet>>(r.getLitLevels().size());
        for(int i = 0; i < r.getLitLevels().size(); i++)
            disjObjs.add(new ArrayList<LandmarkSet>());
        
        //Adding goals to N and objs(lvl), where lvl is the RPG level in which each goal first appears
        for(GroundedCond goal: g) {
            nodes.add(new LandmarkNode(goal, true));
            nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
            maLiteralNode.put(new String(goal.getVar()+goal.getValue()), nodes.size() - 1);
            objs.get(goal.getVar().getMinTime(goal.getValue())).add(goal);
        }
    }*/

    public ArrayList<LandmarkNode> getNodes() {
        return nodes;
    }

    private boolean[][] getReasonableOrderings() {
        reasonableOrderingsList = new ArrayList<LandmarkOrdering>();
        reasonableOrderingsGoalsList = new ArrayList<LandmarkOrdering>();

        boolean[][] R = new boolean[nodes.size()][nodes.size()];
        for (int i = 0; i < nodes.size(); i++)
            for (int j = 0; j < nodes.size(); j++)
                R[i][j] = false;
        //ArrayList<LandmarkOrdering> R = new ArrayList<LandmarkOrdering>();
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

        for (LandmarkFluent g1 : r.getGoals()) {
            for (LandmarkFluent g2 : r.getGoals()) {
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
        Integer[] lits = new Integer[r.getLiterals().size()];
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
                if (inconsistent(r.getLiterals().get(i), l1.getLiteral()))
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
        boolean[] visitedActions = new boolean[r.getActions().size()];
        boolean[] facts = new boolean[r.getLiterals().size()];
        boolean[] factsPrev = new boolean[r.getLiterals().size()];
        mutexMatrix = new boolean[r.getLiterals().size()][r.getLiterals().size()];
        boolean mutexPrev[][] = new boolean[r.getLiterals().size()][r.getLiterals().size()];

        ArrayList<LandmarkFluent> pre, add, del, newEff;
        ArrayList<LandmarkFluent> oldEff = new ArrayList<LandmarkFluent>();

        int i, j;
        //Initializing data structures
        for (i = 0; i < r.getActions().size(); i++)
            visitedActions[i] = false;
        for (i = 0; i < r.getLiterals().size(); i++) {
            facts[i] = false;
            factsPrev[i] = false;
            for (j = 0; j < r.getLiterals().size(); j++) {
                mutexMatrix[i][j] = false;
                mutexPrev[i][j] = false;
            }
        }
        for (LandmarkFluent l : r.getInitialState()) {
            facts[l.getIndex()] = true;
        }
        //Main loop - iterate until facts and mutex remain unchanged
        while (notEqual(facts, factsPrev) || notEqualMat(mutexMatrix, mutexPrev)) {
            //Copying the arrays used in the previous iteration
            copy(factsPrev, facts);
            copyMat(mutexPrev, mutexMatrix);

            LandmarkAction a;
            for (i = 0; i < r.getActions().size(); i++) {
                a = r.getActions().get(i);
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
                                for (LandmarkFluent prop3 : r.getLiterals()) {
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
                        for (LandmarkFluent prop2 : r.getLiterals()) {
                            //If there is a literal prop2 that is mutex with prop1, is not in del,
                            //and there is not a literal in pre that is mutex with prop2
                            if (mutexMatrix[prop1.getIndex()][prop2.getIndex()] && !checkMutex(pre, prop2, mutexMatrix) && !del.contains(prop2)) {
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

    private ArrayList<LandmarkFluent> calculateNewEffects(ArrayList<LandmarkFluent> effs, boolean[] facts, ArrayList<LandmarkFluent> old) {
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
            for (LandmarkFluent l : r.getLiterals()) {
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
                            if (!r.verify(A)) {
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

    private ArrayList<LandmarkAction> getActions(LandmarkFluent l1, LandmarkFluent l2) {
        ArrayList<LandmarkAction> A = new ArrayList<LandmarkAction>();

        for (LandmarkAction a : r.getActions()) {
            for (LandmarkFluent pre : a.getPreconditions()) {
                if (l1 == pre && l2.getTotalProducers().contains(a))
                    A.add(a);
            }
        }

        return A;
    }

    private void actionProcessing(ArrayList<LandmarkAction> A, LandmarkNode g, int level) {
        //U: set of grouped literals
        ArrayList<LandmarkSet> D;
        //Calculating I set: preconditions that are common to all the actions in A
        ArrayList<LandmarkFluent> I = new ArrayList<LandmarkFluent>();
        ArrayList<LandmarkFluent> U = new ArrayList<LandmarkFluent>();
        int[] common = new int[r.getLiterals().size()];
        for (int i = 0; i < common.length; i++) common[i] = 0;
        for (LandmarkAction a : A) {
            for (LandmarkFluent l : a.getPreconditions()) {
                common[l.getIndex()] = common[l.getIndex()] + 1;
            }
        }
        if (A.size() > 0) {
            for (int i = 0; i < common.length; i++) {
                if (common[i] == A.size())
                    I.add(r.getIndexLiterals().get(i));
                else if (common[i] > 0)
                    U.add(r.getIndexLiterals().get(i));
            }
        }
        //Exploring candidate landmarks in I
        for (LandmarkFluent p : I) {
            if (r.verify(p)) {
                //Adding landmark p to N, and transition p -> g in E
                //The literal is stored only if it hasn't appeared before (it is ensured by checking literalNode)
                if (literalNode.get(p.getIndex()) == -1) {
                    nodes.add(new LGNode(p));
                    nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
                    literalNode.set(p.getIndex(), nodes.size() - 1);
                }
                //Adding a new transition to E
                edges.add(new LMOrdering(nodes.get(literalNode.get(p.getIndex())), g,
                        LandmarkOrdering.NECESSARY));
                if (!objs.get(p.getLevel()).contains(p))
                    objs.get(p.getLevel()).add(p);
            }
        }
        //Exploring candidate disjunctive landmarks in D
        D = groupLandmarkSet(U, A);
        for (LandmarkSet d : D) {
            //if(d.id.equals("at") && d.set.size()==17)
            //    System.out.println();
            LandmarkSet d1 = findDisjObject(d, level);
            if (d1 == null) {
                nodes.add(new LGNode(d));
                d.setLGNode(nodes.get(nodes.size() - 1));
                nodes.get(nodes.size() - 1).setIndex(nodes.size() - 1);
                edges.add(new LMOrdering(nodes.get(nodes.size() - 1), g,
                        LandmarkOrdering.NECESSARY));
                disjObjs.get(level - 1).add(d);
            } else {
                edges.add(new LMOrdering(d1.getLTNode(), g, LandmarkOrdering.NECESSARY));
            }
        }
    }

    private LandmarkSet findDisjObject(LandmarkSet u, int level) {
        for (int i = disjObjs.size() - 1; i >= level - 1; i--) {
            for (LandmarkSet obj : disjObjs.get(i)) {
                //if(obj.id.equals("at") && obj.set.size()==17)
                //    System.out.println();
                if (u.compareTo(obj) == 0)
                    return obj;
            }
        }
        return null;
    }

    private ArrayList<LandmarkSet> groupLandmarkSet(ArrayList<LandmarkFluent> u, ArrayList<LandmarkAction> A) {
        ArrayList<LandmarkSet> U = new ArrayList<LandmarkSet>();
        Hashtable<String, LandmarkSet> hashU = new Hashtable<String, LandmarkSet>();

        for (LandmarkFluent l : u) {
            if (hashU.get(l.getVar().getFuctionName()) == null) {
                U.add(new uSet(l));
                hashU.put(l.getVar().getFuctionName(), U.get(U.size() - 1));
            } else {
                hashU.get(l.getVar().getFuctionName()).addElement(l);
            }
        }

        for (LandmarkSet us : U)
            us.calculateValue();

        //Verify if the LandmarkSets are correct
        //All the actions must have provided the LandmarkSet with at least a precondition of each type
        int instances, actions;
        boolean visited;
        ArrayList<LandmarkSet> U1 = new ArrayList<LandmarkSet>(U.size());
        for (LandmarkSet s : U) {
            instances = 0;
            actions = 0;
            if (s.getElements().size() == 1) {
                continue;
            }
            for (LandmarkAction a : A) {
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
            //If there is one instance per action, the LandmarkSet is added to u1
            if (actions == A.size() && instances == A.size())
                U1.add(s);
            else if (actions == A.size() && instances > A.size())
                analyzeSet(s, A, U1);
        }

        return U1;
    }

    private void analyzeSet(LandmarkSet s, ArrayList<LandmarkAction> A, ArrayList<LandmarkSet> U1) {
        ArrayList<ArrayList<LandmarkFluent>> literalProducers = new ArrayList<ArrayList<LandmarkFluent>>(A.size());
        int i;
        LandmarkAction a;
        LandmarkSet u;

        for (i = 0; i < A.size(); i++)
            literalProducers.add(new ArrayList<LandmarkFluent>());

        //Grouping the literals in the set according to the actions that generated them
        for (i = 0; i < A.size(); i++) {
            a = A.get(i);
            for (LandmarkFluent p : a.getPreconditions()) {
                if (p.getVar().getFuctionName().equals(s.identify()) &&
                        s.getElements().contains(p)) {
                    literalProducers.get(i).add(p);
                }
            }
        }

        ArrayList<LandmarkFluent> actionLiterals;
        LandmarkFluent similar;
        boolean finish = false, add;
        //An LandmarkSet has only one element per action in A
        for (LandmarkFluent l : literalProducers.get(0)) {
            add = true;
            if (finish)
                break;
            u = new uSet(l);
            for (i = 1; i < literalProducers.size(); i++) {
                if (literalProducers.get(i).isEmpty()) {
                    finish = true;
                    add = false;
                    break;
                }
                actionLiterals = literalProducers.get(i);
                similar = equalParameters(l, actionLiterals);
                if (similar == null) {
                    add = false;
                    break;
                }
                actionLiterals.remove(similar);
                u.addElement(similar);
            }
            if (add)
                U1.add(u);
        }
    }

    private LandmarkFluent equalParameters(LandmarkFluent l, ArrayList<LandmarkFluent> actionLiterals) {
        ArrayList<LandmarkFluent> candidates = new ArrayList<LandmarkFluent>(actionLiterals.size());
        ArrayList<LandmarkFluent> auxCandidates = new ArrayList<LandmarkFluent>(actionLiterals.size());
        String p1[] = l.getVar().getParams(), p2[];
        int equalParameters = 0, min;
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
        if (equalParameters == 0)
            return null;
        return candidate;
    }

    private void computeAccessibilityMatrix() {
        int i, j, k;

        //Generamos k matrices
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

    public LandmarkNode getNode(LandmarkFluent l) {
        if (literalNode.get(l.getIndex()) != -1)
            return nodes.get(literalNode.get(l.getIndex()));
        else
            return null;
    }

    public boolean getReasonableOrdering(LandmarkNode n1, LandmarkNode n2) {
        if (reasonableOrderings[n1.getIndex()][n2.getIndex()])
            return true;
        return false;
        /*for(LandmarkOrdering o: reasonableOrderings) {
            if(o.getNode1().getIndex() == n1.getIndex()) {
                if(o.getNode2().getIndex() == n2.getIndex()) 
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


    public String toString() {
        String res = "";
        for (LandmarkOrdering o : edges)
            res += o.getNode1().toString() + " -> " + o.getNode2().toString() + "\n";

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (matrix[i][j] == true)
                    res += 1 + " ";
                else
                    res += 0 + " ";
            }
            res += "\n";
        }
        res += "\nMutex matrix: \n";

        for (int i = 0; i < r.getLiterals().size(); i++) {
            for (int j = 0; j < r.getLiterals().size(); j++) {
                if (mutexMatrix[i][j] == true)
                    res += r.getLiterals().get(i) + " <-> " + r.getLiterals().get(j) + "\n";
            }
        }

        res += "\nReasonable orderings: \n";

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (reasonableOrderings[i][j] == true)
                    res += nodes.get(i) + " -> " + nodes.get(j) + "\n";
            }
        }

        return res;
    }

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

    @Override
    public int numGlobalNodes() {
        return nodes.size();
    }

    @Override
    public int numTotalNodes() {
        return nodes.size();
    }
}
