package org.agreement_technologies.service.map_communication;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.Message;
import org.agreement_technologies.common.map_communication.MessageFilter;
import org.agreement_technologies.common.map_parser.AgentList;
import org.agreement_technologies.common.map_parser.Task;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

/**
 * Agent communication utilities
 *
 * @author Oscar Sapena
 * @since April 2011
 */
public class AgentCommunicationImp implements AgentCommunication {
    private static final int WAIT_TIME_MESSAGE = 1;
    private static final int WAIT_TIMEOUT_MESSAGE = 30000;    // 30 seg.

    private final String agentName;            // Name of this agent (without suffix)
    private final ArrayList<AgentIP> otherAgents;        // Other agents in the task
    private final ArrayList<AgentIP> allAgents;          // All agents in the task
    private int batonAgent;                // Baton agent index
    private int thisAgentIndex;                         // Index of this agent
    private Vector<Message> msgQueue;                   // Message queue
    private int numMessages;                            // Number of sent messages
    private String senderAgent;                         // Agent that sent the last received message

    private CommunicationServer server;
    private HashMap<String, String> ipAddress;
    private HashMap<String, Integer> portIndex;
    private HashMap<String, Integer> agentIndex;
    private ArrayList<String> agentNames;
    private ArrayList<String> otherAgentNames;
    private Socket clientSockets[];

    private AgentCommunicationImp(String agentName) {
        this.agentName = agentName.toLowerCase();
        otherAgents = new ArrayList<>();
        allAgents = new ArrayList<>();
        msgQueue = new Vector<Message>();
        ipAddress = new HashMap<>();
        portIndex = new HashMap<>();
        agentIndex = new HashMap<>();
        agentNames = new ArrayList<>();
        otherAgentNames = new ArrayList<>();
        numMessages = 0;
        batonAgent = 0;
    }

    /**
     * Creates an agent communication utility
     */
    public AgentCommunicationImp(String agentName, Task task) throws IOException {
        this(agentName);
        // Search for agents
        String[] objs = task.getObjects();
        for (String obj : objs) {
            if (isAgent(obj, task)) {
                if (!obj.equalsIgnoreCase(agentName)) {
                    otherAgents.add(new AgentIP(obj.toLowerCase(), "127.0.0.1"));
                }
                allAgents.add(new AgentIP(obj.toLowerCase(), "127.0.0.1"));
            }
        }
        // Sorting agents by name and setting the baton agent as the first one
        Collections.sort(allAgents);
        Collections.sort(otherAgents);
        if (allAgents.isEmpty()) {
            allAgents.add(new AgentIP(this.agentName, "127.0.0.1"));
        }
        thisAgentIndex = allAgents.indexOf(new AgentIP(this.agentName));
        for (int i = 0; i < allAgents.size(); i++) {
            ipAddress.put(allAgents.get(i).name, allAgents.get(i).ip);
            portIndex.put(allAgents.get(i).name, BASE_PORT + i);
            agentIndex.put(allAgents.get(i).name, i);
            agentNames.add(allAgents.get(i).name);
        }
        for (int i = 0; i < otherAgents.size(); i++)
            otherAgentNames.add(otherAgents.get(i).name);
        clientSockets = new Socket[allAgents.size()];
        server = new CommunicationServer(this, thisAgentIndex, otherAgentNames.size());
        server.start();
    }

    public AgentCommunicationImp(String agentName, AgentList agList) throws IOException {
        this(agentName);
        for (int i = 0; i < agList.numAgents(); i++) {
            String agName = agList.getName(i).toLowerCase();
            if (!agName.equalsIgnoreCase(agentName)) {
                otherAgents.add(new AgentIP(agName, agList.getIP(i)));
            }
            allAgents.add(new AgentIP(agName, agList.getIP(i)));
        }
        Collections.sort(allAgents);
        Collections.sort(otherAgents);
        if (allAgents.isEmpty()) {
            allAgents.add(new AgentIP(this.agentName, "127.0.0.1"));
        }
        thisAgentIndex = allAgents.indexOf(new AgentIP(this.agentName));
        for (int i = 0; i < allAgents.size(); i++) {
            ipAddress.put(allAgents.get(i).name, allAgents.get(i).ip);
            portIndex.put(allAgents.get(i).name, BASE_PORT + i);
            agentIndex.put(allAgents.get(i).name, i);
            agentNames.add(allAgents.get(i).name);
        }
        for (int i = 0; i < otherAgents.size(); i++)
            otherAgentNames.add(otherAgents.get(i).name);
        clientSockets = new Socket[allAgents.size()];
        server = new CommunicationServer(this, thisAgentIndex, otherAgentNames.size());
        server.start();
    }

    @Override
    public void enqueueMsg(Message message) {
        msgQueue.add(message);
    }

    @Override
    public boolean registeredAgent(String ag) {
        int socketIndex = agentIndex.get(ag);
        try {
            if (clientSockets[socketIndex] == null)
                clientSockets[socketIndex] = new Socket(ipAddress.get(ag), portIndex.get(ag));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Checks if a given object is an agent
     *
     * @param obj  Object
     * @param task Planning task
     * @return True if the object is an agent
     */
    private boolean isAgent(String obj, Task task) {
        String types[] = task.getObjectTypes(obj);
        for (String t : types) {
            if (t.equalsIgnoreCase("agent")) {
                return true;
            }
        }
        for (String t : types) {
            if (isAgentType(t, task)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a given type is a sub-type of agent
     *
     * @param type Type
     * @param task Planning task
     * @return True if type if a sub-type of agent
     */
    private boolean isAgentType(String type, Task task) {
        String ptypes[] = task.getParentTypes(type);
        for (String t : ptypes) {
            if (t.equalsIgnoreCase("agent")) {
                return true;
            }
        }
        for (String t : ptypes) {
            if (isAgentType(t, task)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the index of this agent
     *
     * @return This agent index
     */
    @Override
    public int getThisAgentIndex() {
        return thisAgentIndex;
    }

    /**
     * Returns the name of this agent
     *
     * @return This agent name
     */
    @Override
    public String getThisAgentName() {
        return agentName;
    }

    /**
     * Retrieves the list of all agents in the task
     *
     * @return Agent list
     */
    @Override
    public ArrayList<String> getAgentList() {
        return agentNames;
    }

    /**
     * Returns the number of agents in the task
     *
     * @return Number of agents
     */
    @Override
    public int numAgents() {
        return allAgents.size();
    }

    @Override
    public String getSenderAgent() {
        return senderAgent;
    }

    /*
     * Returns true if this agent has the baton
     */
    @Override
    public boolean batonAgent() {
        return batonAgent == thisAgentIndex;
    }

    /**
     * Returns the name of the baton agent
     *
     * @return Name of the baton agent
     */
    @Override
    public String getBatonAgent() {
        return agentNames.get(batonAgent);
    }

    /**
     * Returns the list of agents, excluding itself
     *
     * @return List of the other agents in the task
     */
    @Override
    public ArrayList<String> getOtherAgents() {
        return otherAgentNames;
    }

    /**
     * Returns the index of the given agent
     *
     * @param agName Agent name
     * @return Agent index
     */
    @Override
    public int getAgentIndex(String agName) {
        return agentNames.indexOf(agName);
    }

    /**
     * Returns the number of sent messages
     */
    @Override
    public int getNumMessages() {
        return numMessages;
    }

    /**
     * Sends a message to all other agents and waits an acknowledgment
     *
     * @param obj     Message content
     * @param waitACK Waits for a receipt confirmation
     */
    @Override
    public void sendMessage(java.io.Serializable obj, boolean waitACK) {
        for (String toAgent : otherAgentNames) {
            sendMessage(toAgent, obj, waitACK);
        }
    }

    /**
     * Sends a message to other agent and waits an acknowledgment
     *
     * @param toAgent Receiver
     * @param obj     Message content
     * @param waitACK Waits for a receipt confirmation
     */
    @Override
    public void sendMessage(String toAgent, java.io.Serializable obj, boolean waitACK) {
        int socketIndex = agentIndex.get(toAgent);
        try {
            if (clientSockets[socketIndex] == null)
                clientSockets[socketIndex] = new Socket(ipAddress.get(toAgent), portIndex.get(toAgent));
            Socket socket = clientSockets[socketIndex];
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new MessageImp(obj, agentName));
            out.flush();
        } catch (IOException ex) {
            throw new CommunicationException(ex.toString());
        }
        if (waitACK) {
            Message resp;
            int time = 0;
            do {
                resp = checkQueue(toAgent, ACK_MESSAGE);
                wait(WAIT_TIME_MESSAGE);
                time += WAIT_TIME_MESSAGE;
                if (time >= WAIT_TIMEOUT_MESSAGE) {
                    throw new RuntimeException("Message timeout while waiting an ACK from agent " + toAgent);
                }
            } while (resp == null);
        }
        numMessages++;
    }

    /**
     * Searches a message in the queue
     *
     * @param fromAgent Sender
     * @param cont      Message content
     * @return The message if found, otherwise null
     */
    private Message checkQueue(String fromAgent, java.io.Serializable cont) {
        int index = -1;
        Message res = null;
        for (int i = 0; i < msgQueue.size(); i++) {
            Message msg = msgQueue.get(i);
            if (msg.sender().equalsIgnoreCase(fromAgent) && cont.equals(msg.content())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            res = msgQueue.get(index);
            msgQueue.remove(index);
        }
        return res;
    }

    /**
     * Searches a message in the queue
     *
     * @param fromAgent Sender
     * @return The message if found, otherwise null
     */
    private Message checkQueue(String fromAgent) {
        int index = -1;
        Message res = null;
        for (int i = 0; i < msgQueue.size(); i++) {
            Message msg = msgQueue.get(i);
            if (msg.sender().equalsIgnoreCase(fromAgent) && !ACK_MESSAGE.equals(msg.content())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            res = msgQueue.get(index);
            msgQueue.remove(index);
        }
        return res;
    }

    private Message checkQueue(MessageFilter filter) {
        int index = -1;
        Message res = null;
        for (int i = 0; i < msgQueue.size(); i++) {
            Message m = msgQueue.get(i);
            if (filter.validMessage(m)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            res = msgQueue.get(index);
            msgQueue.remove(index);
        }
        return res;
    }

    /**
     * Receives a message from other agent
     *
     * @param fromAgent Message sender
     * @param sendACK   Send a confirmation when the message is received
     * @return Content of the message
     */
    @Override
    public java.io.Serializable receiveMessage(String fromAgent, boolean sendACK) {
        Message msg;
        int time = 0;
        do {
            msg = checkQueue(fromAgent);
            if (msg == null) {
                wait(WAIT_TIME_MESSAGE);
                time += WAIT_TIME_MESSAGE;
                if (time >= WAIT_TIMEOUT_MESSAGE) {
                    throw new RuntimeException("Message timeout while waiting a message from agent " + fromAgent);
                }
            }
        } while (msg == null);
        senderAgent = fromAgent.toLowerCase();
        java.io.Serializable obj = msg.content();
        if (sendACK)
            sendMessage(msg.sender(), ACK_MESSAGE, false);
        return obj;
    }

    @Override
    public Serializable receiveMessage(MessageFilter filter, boolean sendACK) {
        Message msg;
        int time = 0;
        do {
            msg = checkQueue(filter);
            if (msg == null) {
                wait(WAIT_TIME_MESSAGE);
                time += WAIT_TIME_MESSAGE;
                if (time >= WAIT_TIMEOUT_MESSAGE) {
                    throw new RuntimeException("Message timeout while waiting a filtered message");
                }
            }
        } while (msg == null);
        senderAgent = msg.sender();
        java.io.Serializable obj = msg.content();
        if (sendACK)
            sendMessage(msg.sender(), ACK_MESSAGE, false);
        return obj;
    }

    @Override
    public Serializable receiveMessage(boolean sendACK) {
        Message msg;
        int time = 0;
        while (msgQueue.isEmpty()) {
            wait(WAIT_TIME_MESSAGE);
            time += WAIT_TIME_MESSAGE;
            if (time >= WAIT_TIMEOUT_MESSAGE) {
                throw new RuntimeException("Message timeout while waiting a message");
            }
        }
        msg = msgQueue.get(0);
        msgQueue.remove(0);
        senderAgent = msg.sender();
        java.io.Serializable obj = msg.content();
        if (sendACK)
            sendMessage(msg.sender(), ACK_MESSAGE, false);
        return obj;
    }

    @Override
    public void passBaton() {
        batonAgent++;
        if (batonAgent >= numAgents()) {
            batonAgent = 0;
        }
    }

    private void wait(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void sendAck(String toAgent) {
        sendMessage(toAgent, ACK_MESSAGE, false);
    }

    @Override
    public void close() {
        for (Socket s : clientSockets)
            if (s != null) {
                try {
                    s.close();
                } catch (IOException ex) {
                }
            }
    }

    public static class CommunicationException extends RuntimeException {

        private static final long serialVersionUID = -7439092128849900745L;

        public CommunicationException(String msg) {
            super(msg);
        }
    }

    private static class AgentIP implements Comparable<AgentIP> {
        String name;
        String ip;

        private AgentIP(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        private AgentIP(String agentName) {
            this.name = agentName;
        }

        @Override
        public int compareTo(AgentIP a) {
            return name.compareTo(a.name);
        }

        @Override
        public boolean equals(Object x) {
            return ((AgentIP) x).name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
