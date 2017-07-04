package org.agreement_technologies.common.map_planner;

/**
 * Common interface for partial order relationships among steps
 *
 * @author Alex
 */
public interface Ordering extends java.io.Serializable {
    int getIndex1();

    int getIndex2();
}
