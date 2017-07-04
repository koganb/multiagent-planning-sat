package org.agreement_technologies.common.map_viewer;

import org.agreement_technologies.common.map_planner.Plan;
import org.agreement_technologies.common.map_planner.PlannerFactory;

import java.awt.*;

public interface PlanViewer {

    void setBackground(Color bgColor);

    void setPreferredSize(Dimension dimension);

    Component getComponent();

    void showPlan(Plan plan, PlannerFactory pf);

    int getMakespan();
}
