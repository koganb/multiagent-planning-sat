package org.agreement_technologies.service.map_landmarks;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Single landmarks transmitted to agents that are not in the label of the associated literal
 *
 * @author Alex
 */
public class MessageContentLandmarkSharing implements Serializable {
    private static final long serialVersionUID = 4010531211040421300L;
    private int literalId;
    private ArrayList<String> agents;

    public MessageContentLandmarkSharing(int l, ArrayList<String> ag) {
        literalId = l;
        agents = ag;
    }

    public int getLiteralId() {
        return literalId;
    }

    public ArrayList<String> getAgents() {
        return agents;
    }
}
