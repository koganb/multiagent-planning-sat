package org.agreement_technologies.service.map_planner;

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Ordering;
import org.agreement_technologies.service.tools.CustomArrayList;

/**
 * Ordering manager with memorization
 *
 * @author Alejandro Torreño
 */
public class POPOrderingManagerMemorization implements OrderingManager {
    //Parameters: reusable matrix, plan size, indexes for true and false values, adjacency list
    private int indexT, indexF;
    private int size;
    private int matrix[][];
    private CustomArrayList<CustomArrayList<Integer>> list;
    //private Boolean order;

    public POPOrderingManagerMemorization(int x) {
        int i, j;

        this.matrix = new int[x][x];
        this.indexT = -1;
        this.indexF = 0;
        this.size = 0;

        list = new CustomArrayList<CustomArrayList<Integer>>(x);

        for (i = 0; i < x; i++) {
            list.insert(new CustomArrayList<Integer>(x));
            for (j = 0; j < x; j++) this.matrix[i][j] = 0;
        }
    }

    public POPOrderingManagerMemorization() {
        this(20);
    }

    //Checks if there is a transitive ordering between two steps
    public boolean checkOrdering(int i, int j) {
        //Base cases: step 0 is ordered before all the steps, and step 1 is always ordered after the rest of steps
        if (i == 0 || j == 1) return true;
        if (i == 1 || j == 0) return false;
        //If step i is out of range (the step is new), there is not an ordering
        if (i > this.size) return false;
        //If the ordering (or absence of ordering) is stored in the matrix, return the result
        if (matrix[i][j] == this.indexT) return true;
        if (matrix[i][j] == this.indexF) return false;

        return findAntecessors(j, i);
    }

    private boolean findAntecessors(int node, int target) {
        int aux;

        CustomArrayList<Integer> nodes = new CustomArrayList<Integer>();
        CustomArrayList<Integer> memorization = new CustomArrayList<Integer>();

        nodes.add(node);
        memorization.add(node);

        while (!nodes.isEmpty()) {
            //System.out.println(nodes.retrieve());
            aux = nodes.retrieve();
            //Base case
            if (matrix[target][aux] == this.indexT) {
                this.memorize(memorization, node);
                return true;
            }
            //if(matrix[target][aux] == this.indexF) return false;
            //Check if the target node is adjacent to aux
            if (this.list.get(aux).contains(target)) {
                this.memorize(memorization, node);
                return true;
            }
            //Explore the nodes adjacent to aux
            nodes.append(this.list.get(aux));
            memorization.append(this.list.get(aux));
        }
        this.memorize(memorization, node);
        return false;
    }

    private void memorize(CustomArrayList<Integer> mem, int node) {
        int i, j;

        for (i = 0; i < this.size; i++) {
            for (j = 0; j < mem.size(); j++) {
                //System.out.println(i + " " + j + " " + this.size);
                if (matrix[node][i] == this.indexT)
                    matrix[mem.get(j)][i] = this.indexT;
            }
        }
        /*for(int i: mem) {
            matrix[i][node] = this.indexT;
        }*/
    }


    /*private Boolean findAntecessors(int node, int target) {
        Boolean ordering;

        //ArrayList<Integer> antecessors = this.list.get(node);

        //Base case
        if(matrix[target][node] == this.indexT)
            return true;
        if(matrix[target][node] == this.indexF)
            return false;

        for(int a = 2; a < this.size; a++) {
            //Check if the target is one of its antecessors (recursive call)
            if(matrix[a][node] == this.indexT) {
                ordering = findAntecessors(a, target);
                if(ordering) return true;
            }
        }

        return false;
    }*/

    //Updates the manager with a new plan
    public void update(POPInternalPlan p) {
        POPInternalPlan iter = p;

        this.indexF += 2;
        this.indexT += 2;

        this.size = 0;

        while (this.size == 0) {
            if (this.size == 0)
                if (iter.getStep() != null) {
                    this.resize(iter.getStep().getIndex() + 1);
                    this.size = iter.getStep().getIndex() + 1;
                }
            iter = iter.getFather();
        }

        iter = p;

        while (iter != null) {
            if (iter.getOrdering() != null) {
                this.list.get(iter.getOrdering().getIndex2()).addNotRepeated(iter.getOrdering().getIndex1());
                this.matrix[iter.getOrdering().getIndex1()][iter.getOrdering().getIndex2()] = this.indexT;
                this.matrix[iter.getOrdering().getIndex2()][iter.getOrdering().getIndex1()] = this.indexF;
            }
            if (iter.getCausalLink() != null) {
                this.list.get(iter.getCausalLink().getIndex2()).addNotRepeated(iter.getCausalLink().getIndex1());
                this.matrix[iter.getCausalLink().getIndex1()][iter.getCausalLink().getIndex2()] = this.indexT;
                this.matrix[iter.getCausalLink().getIndex2()][iter.getCausalLink().getIndex1()] = this.indexF;
            }
            iter = iter.getFather();
        }
    }

    //Increases the matrix indexes to store the new plan's orderings
    public void newPlan() {
        this.indexF += 2;
        this.indexT += 2;
        for (int i = 0; i < this.list.size(); i++)
            this.list.get(i).clear();
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        if (this.size < size) {
            this.size = size;
            this.resize(size);
        }
    }

    //Resizes the matrix and the array
    public void resize(int s) {
        if (s > this.matrix.length) {
            int i, j, newSize;

            newSize = s + (s >> 1);
            for (i = size; i < newSize; i++)
                this.list.add(new CustomArrayList<Integer>(newSize));
            this.matrix = new int[newSize][newSize];
            for (i = 0; i < newSize; i++)
                for (j = 0; j < newSize; j++) this.matrix[i][j] = 0;
        }
    }


    public void printMatrix() {
        int i, j;
        String res = new String();

        res = "[0]\n";
        res += "[1]\n";
        res += "[2] ";

        for (i = 2; i < this.size; i++) {
            for (j = 2; j < this.size; j++) {
                if (this.matrix[i][j] == this.indexT) res += "1 ";
                else if (this.matrix[i][j] == this.indexF) res += "0 ";
                else res += "8 ";
            }
            if ((i + 1) < size) res += "\n[" + (i + 1) + "] ";
        }
        System.out.print(res + "\n");
    }

    public String toString() {
        int i, j;
        String res = new String();

        res = "[0]\n";
        res += "[1]\n";
        res += "[2] ";

        for (i = 2; i < this.size; i++) {
            for (j = 2; j < this.size; j++) {
                if (this.matrix[i][j] == this.indexT) res += "1 ";
                else if (this.matrix[i][j] == this.indexF) res += "0 ";
            }
            if ((i + 1) < size) res += "\n[" + (i + 1) + "] ";
        }

        return res;
    }

    //Updates the manager with a regular plan
    /*public void update(IPlan p) {
        POPPlan basePlan = (POPPlan) p;
        this.indexF += 2;
        this.indexT += 2;

        this.size = basePlan.getSteps().size();

        this.resize(basePlan.getSteps().size());
        
        for(CustomArrayList<Integer> l: list)
            l.clear();

        for(POPOrdering o: basePlan.getOrderings()) {
            this.list.get(o.getStep2()).addNotRepeated(o.getStep1());
            this.matrix[o.getStep1()][o.getStep2()] = this.indexT;
            this.matrix[o.getStep2()][o.getStep1()] = this.indexF;
        }
        for(POPCausalLink cl: basePlan.getCausalLinks()) {
            this.list.get(cl.getIndex2()).addNotRepeated(cl.getIndex1());
            this.matrix[cl.getIndex1()][cl.getIndex2()] = this.indexT;
            this.matrix[cl.getIndex2()][cl.getIndex1()] = this.indexF;
        }
    }*/

    public void addOrdering(int o1, int o2) {
        this.list.get(o2).addNotRepeated(o1);
        this.matrix[o1][o2] = this.indexT;
        this.matrix[o2][o1] = this.indexF;
    }

    public void computeAccessibilityMatrix() {
    }

    @Override
    public void removeOrdering(int o1, int o2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void rebuild(POPInternalPlan plan) {
        this.newPlan();
        this.setSize(plan.numSteps());

        //Guardamos los orderings del plan en la matriz
        if (!plan.getTotalOrderings().isEmpty())
            for (Ordering o : plan.getTotalOrderings())
                this.addOrdering(o.getIndex1(), o.getIndex2());
        if (!plan.getTotalCausalLinks().isEmpty())
            for (CausalLink l : plan.getTotalCausalLinks())
                this.addOrdering(l.getIndex1(), l.getIndex2());
    }


}
