package org.agreement_technologies.service.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class Graph<N, E> {
    public static final int INFINITE = Integer.MAX_VALUE / 4;
    private ArrayList<GraphNode<N, E>> nodes;
    private HashMap<N, Integer> labels;
    private boolean visited[];
    private int count;

    public Graph() {
        nodes = new ArrayList<GraphNode<N, E>>();
        labels = new HashMap<N, Integer>();
    }

    public static void sort(int key[], int values[]) {
        for (int i = 1; i < key.length; i++) {
            int pos, elem = key[i], val = values[i];
            for (pos = i; pos > 0 && elem < key[pos - 1]; pos--) {
                key[pos] = key[pos - 1];
                values[pos] = values[pos - 1];
            }
            key[pos] = elem;
            values[pos] = val;
        }
    }

    public int addNode(N data) {
        Integer index = labels.get(data);
        GraphNode<N, E> n;
        if (index == null) {
            n = new GraphNode<N, E>(data);
            index = nodes.size();
            nodes.add(n);
            labels.put(data, index);
        } else
            n = nodes.get(index);
        return index;
    }

    public void addEdge(N node1, N node2, E label) {
        int n1 = addNode(node1), n2 = addNode(node2);
        nodes.get(n1).add(n2, label);
    }

    public N getNode(int index) {
        return nodes.get(index).data;
    }

    public int numNodes() {
        return nodes.size();
    }

    public int numEdges() {
        int n = 0;
        for (GraphNode<N, E> gn : nodes)
            n += gn.adjacents.size();
        return n;
    }

    public ArrayList<Adjacent<E>> getAdjacents(int index) {
        return nodes.get(index).adjacents;
    }

    public int[] sortNodesByIndegree() {
        int[] inDegree = new int[nodes.size()], res = new int[nodes.size()];
        for (GraphNode<N, E> n : nodes)
            for (Adjacent<E> adj : n.adjacents)
                inDegree[adj.dst]++;
        for (int i = 0; i < res.length; i++)
            res[i] = i;
        sort(inDegree, res);
        return res;
    }

    public int maxDistance(int vOrigen, int vDestino) {
        int distanciaMax[] = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            distanciaMax[i] = -1;
        }
        distanciaMax[vOrigen] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        q.add(vOrigen);
        while (!q.isEmpty()) {
            int vActual = q.poll();
            ArrayList<Adjacent<E>> aux = nodes.get(vActual).adjacents;
            for (int i = 0; i < aux.size(); i++) {
                int vSiguiente = aux.get(i).dst;
                if (distanciaMax[vSiguiente] <= distanciaMax[vActual]) {
                    distanciaMax[vSiguiente] = distanciaMax[vActual] + 1;
                    if (distanciaMax[vSiguiente] > nodes.size()) {
                        return INFINITE;
                    }
                    q.add(vSiguiente);
                }
            }
        }
        return distanciaMax[vDestino];
    }

    public int maxDistanceWithCycles(int orig, int dst) {
        visited = new boolean[nodes.size()];
        count = 0;
        return maxDistanceWithCyclesRec(orig, dst);
    }

    private int maxDistanceWithCyclesRec(int orig, int dst) {
        if (orig == dst)
            return 0;
        visited[orig] = true;
        count++;
        if (count > 10000)
            return maxDistance(orig, dst);
        int max = 0;
        for (Adjacent<E> actual : nodes.get(orig).adjacents)
            if (!visited[actual.dst]) {
                int dist = maxDistanceWithCyclesRec(actual.dst, dst);
                if (dist != INFINITE) {
                    dist++;
                    if (dist > max)
                        max = dist;
                }
                visited[actual.dst] = false;
            }
        if (max == 0)
            max = INFINITE;
        return max;
    }

    public boolean isRoot(int node) {
        for (GraphNode<N, E> n : nodes)
            for (Adjacent<E> adj : n.adjacents)
                if (adj.dst == node)
                    return false;
        return true;
    }

    public int inDegree(int node) {
        int d = 0;
        for (GraphNode<N, E> n : nodes)
            for (Adjacent<E> adj : n.adjacents)
                if (adj.dst == node)
                    d++;
        return d;
    }

    public int getNodeIndex(N node) {
        Integer index = labels.get(node);
        return index != null ? index.intValue() : -1;
    }

    public int minDistance(int vOrig, int vDst) {
        int distanceMin[] = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++)
            distanceMin[i] = INFINITE;
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        q.add(vOrig);
        distanceMin[vOrig] = 0;
        while (!q.isEmpty()) {
            int v = q.poll();
            for (Adjacent<E> a : nodes.get(vOrig).adjacents) {
                int w = a.dst;
                if (distanceMin[w] == INFINITE) { // w not visited
                    distanceMin[w] = distanceMin[v] + 1;
                    q.add(w);
                }
            }
        }
        return distanceMin[vDst];
    }

    public boolean isAcyclic() {
        int[] marks = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            if (marks[i] == 0 && !isAcyclic(i, marks))
                return false;
        }
        return true;
    }

    private boolean isAcyclic(int origen, int marks[]) {
        marks[origen] = 2;    // Visitado en la rama actual
        for (Adjacent<E> a : nodes.get(origen).adjacents)
            if (marks[a.dst] == 0) {
                if (!isAcyclic(a.dst, marks)) return false;
            } else if (marks[a.dst] == 2) return false;    // Arista de retorno

        marks[origen] = 1;    // Visitado
        return true;
    }

    public void removeEdge(int orig, int dst) {
        nodes.get(orig).removeAdjacent(dst);
    }

    public static class Adjacent<E> {
        public int dst;
        public E label;

        public Adjacent(int dst, E label) {
            this.dst = dst;
            this.label = label;
        }

        @SuppressWarnings("unchecked")
        public boolean equals(Object x) {
            return dst == ((Adjacent<E>) x).dst;
        }
    }

    private static class GraphNode<N, E> {
        N data;
        ArrayList<Adjacent<E>> adjacents;

        public GraphNode(N data) {
            this.data = data;
            adjacents = new ArrayList<Adjacent<E>>();
        }

        public void add(int nextNode, E label) {
            Adjacent<E> a = new Adjacent<E>(nextNode, label);
            if (!adjacents.contains(a))
                adjacents.add(a);
        }

        public void removeAdjacent(int dst) {
            for (int i = 0; i < adjacents.size(); i++) {
                Adjacent<E> a = adjacents.get(i);
                if (a.dst == dst) {
                    adjacents.remove(i);
                    break;
                }
            }
        }
    }
}
