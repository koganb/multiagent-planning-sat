/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.agreement_technologies.common.map_landmarks;

public interface LandmarkOrdering {
    int NECESSARY = 1;
    int REASONABLE = 2;

    LandmarkNode getNode1();

    LandmarkNode getNode2();

    int getType();
}
