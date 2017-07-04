package org.agreement_technologies.service.map_landmarks;

import java.util.ArrayList;

public class RPGMessageContent implements java.io.Serializable {
    private static final long serialVersionUID = 6010531211010261701L;
    private boolean RPGChanged;
    private ArrayList<MessageContentRPG> data;

    RPGMessageContent(ArrayList<MessageContentRPG> dataToSend, boolean changed) {
        RPGChanged = changed;
        data = dataToSend;
    }

    public void adRPGData(MessageContentRPG d) {
        data.add(d);
    }

    public ArrayList<MessageContentRPG> getData() {
        return data;
    }

    public boolean isRPGChanged() {
        return RPGChanged;
    }
}
    
