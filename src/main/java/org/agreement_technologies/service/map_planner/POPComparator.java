package org.agreement_technologies.service.map_planner;

import java.util.Comparator;

/**
 * Comparator interface for IPlans; extends the Comparator interface
 *
 * @author Alex
 */
public interface POPComparator extends Comparator<IPlan> {
    int getPublicValue(IPlan p);
}
