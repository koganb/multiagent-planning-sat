package org.agreement_technologies.common.map_communication;

import java.io.Serializable;

public interface Message extends Serializable {

    Serializable content();

    String sender();

}