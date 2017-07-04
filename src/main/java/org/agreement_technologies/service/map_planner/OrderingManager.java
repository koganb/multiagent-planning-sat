package org.agreement_technologies.service.map_planner;

/**
 * Generic methods to be implemented by the ordering manager classes
 *
 * @author Alex
 */
public interface OrderingManager {
    public boolean checkOrdering(int i, int j);

    public void update(POPInternalPlan p);

    public int getSize();

    //public void update(IPlan p);
    public void setSize(int size);

    public void newPlan();

    public void addOrdering(int o1, int o2);

    public void removeOrdering(int o1, int o2);

    public void computeAccessibilityMatrix();

    public void rebuild(POPInternalPlan plan);
}
