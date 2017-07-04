package org.agreement_technologies.service.map_communication;

import org.agreement_technologies.common.map_communication.Message;

import java.io.Serializable;

public class MessageImp implements Message {
    final Serializable content;
    final String sender;

    MessageImp(Serializable obj, String agentName) {
        content = obj;
        sender = agentName;
    }

    @Override
    public Serializable content() {
        return content;
    }

    @Override
    public String sender() {
        return sender;
    }
}
