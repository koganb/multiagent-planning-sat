package org.agreement_technologies.service.map_planner;

//Matriz de orderings; permite averiguar si hay un orden entre dos pasos dados con coste O(1)

import org.agreement_technologies.common.map_planner.CausalLink;
import org.agreement_technologies.common.map_planner.Ordering;

public class POPMatrix implements OrderingManager {
    //Parámetros: matriz, tamaño actual, índice (se asignan índices crecientes a cada nuevo plan base para reutilizar la matriz)
    private int index;
    private int size;
    private int matrix[][];

    public POPMatrix(int x) {
        int i, j;
        this.matrix = new int[x][x];
        this.index = 1;
        this.size = 0;

        for (i = 0; i < x; i++) {
            for (j = 0; j < x; j++) this.matrix[i][j] = 0;
        }
    }

    //Método que incrementa el índice del plan actual
    public void newPlan() {
        this.index++;
    }

    public int getSize() {
        return this.size;
    }

    //Método que fija el número de pasos del plan en la matriz
    public void setSize(int s) {
        this.size = s;
        if (s >= this.matrix.length)
            this.resizeMatrix(java.lang.Math.max(s, this.matrix.length * 2));
    }

    public void update(POPInternalPlan p) {
        POPInternalPlan iter = p;

        this.newPlan();

        this.size = 0;

        while (this.size == 0) {
            if (this.size == 0)
                if (iter.getStep() != null)
                    this.setSize(iter.getStep().getIndex() + 1);
            iter = iter.getFather();
        }

        iter = p;

        while (iter != null) {
            if (iter.getOrdering() != null)
                this.matrix[iter.getOrdering().getIndex1()][iter.getOrdering().getIndex2()] = this.index;
            iter = iter.getFather();
        }
    }

    //Resize the POPMatrix
    private void resizeMatrix(int size) {
        if (size > this.matrix.length) {
            int i, j;
            this.matrix = new int[size][size];
            for (i = 0; i < size; i++) {
                for (j = 0; j < size; j++) this.matrix[i][j] = 0;
            }
        }
    }

    //Método que rellena la matriz a partir de un plan base dado
    /*public void update(IPlan base) {
        POPPlan basePlan = (POPPlan) base;
        //Redimensionamos la matriz si el plan supera el tamaño máximo
        if(basePlan.getSteps().size() == this.matrix.length)
            this.resizeMatrix(this.matrix.length * 2);

        //Incrementamos el índice de la matriz por seguridad
        this.newPlan();

        //Establecemos el tamaño de la matriz
        this.setSize(basePlan.getSteps().size());

        //Volcamos los orderings del plan base en la matriz
        if(!basePlan.getOrderings().isEmpty()) {
            for(Ordering o: basePlan.getOrderings())
                this.addOrdering(o.getStep1(), o.getStep2());
        }

        //Volcamos los orderings asociados a los causal links en la matriz
        for(CausalLink c: basePlan.getCausalLinks()) {
            this.addOrdering(c.getIndex1(), c.getIndex2());
        }

        //Transformamos la matriz de adyacencia en matriz de accesibilidad
        this.computeAccessibilityMatrix();
    }*/

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

        this.computeAccessibilityMatrix();
    }

    //Algoritmo de Warshall para calcular la matriz de accesibilidad a partir de la mariz de adyacencia
    public void computeAccessibilityMatrix() {
        int i, j, k;

        //Generamos k matrices
        for (k = 2; k < this.size; k++) {
            for (i = 2; i < this.size; i++) {
                for (j = 2; j < this.size; j++) {
                    if (this.matrix[i][j] != this.index) {
                        if (this.matrix[i][k] == this.index && this.matrix[k][j] == this.index)
                            this.matrix[i][j] = this.index;
                    }
                }
            }
        }
    }

    //Método que añade a la matriz un ordering i -> j
    public void addOrdering(int step1, int step2) {
        this.matrix[step1][step2] = this.index;
    }

    //Método genérico que comprueba un ordering entre dos pasos
    public boolean checkOrdering(int i, int j) {
        if (i == 0) return true;
        if (j == 1) return true;
        //Si el primer paso del ordering se sale del rango de la matriz (paso nuevo) no hay ordering
        if (i > this.size) return false;

        return this.matrix[i][j] == this.index;
    }

    public void printMatrix() {
        int i, j;
        String res = new String();

        res = "[0]\n";
        res += "[1]\n";
        res += "[2] ";

        for (i = 2; i < this.size; i++) {
            for (j = 2; j < this.size; j++) {
                if (this.matrix[i][j] == this.index) res += "1 ";
                else
                    res += "0 ";
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
                if (this.matrix[i][j] == this.index) res += "1 ";
                else res += "0 ";
            }
            if ((i + 1) < size) res += "\n[" + (i + 1) + "] ";
        }

        return res;
    }

    @Override
    public void removeOrdering(int o1, int o2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
