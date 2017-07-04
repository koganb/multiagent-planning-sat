package org.agreement_technologies.service.map_landmarks;

import java.io.Serializable;

/**
 * Orderings transmitted during the landmark graph postprocessing
 *
 * @author Alex
 */
public class MessageContentPostProcessing implements Serializable {
    private static final long serialVersionUID = 4010531211080423701L;
    private String literal1, literal2;

    public MessageContentPostProcessing(String l1, String l2) {
        literal1 = l1;
        literal2 = l2;
    }

    public String getLiteral1() {
        return literal1;
    }

    public String getLiteral2() {
        return literal2;
    }
}
