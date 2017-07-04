package org.agreement_technologies.common.map_communication;

import java.io.Serializable;
import java.util.ArrayList;

public interface AgentCommunication {
    public static final int BASE_PORT = 38000;
    public static final String ACK_MESSAGE = "<ACK>";
    public static final String END_STAGE_MESSAGE = "<END>";
    public static final String SYNC_MESSAGE = "<SYNC>";
    public static final String PASS_BATON_MESSSAGE = "<PASSBATON>";
    public static final String NO_SOLUTION_MESSAGE = "<NOPLAN>";
    public static final String YES_REPLY = "<YES>";
    public static final String NO_REPLY = "<NO>";
    public static final String NO_AGENT = "<NOAGENT>";

    /**
     * Returns the index of this agent
     *
     * @return This agent index
     */
    int getThisAgentIndex();

    /**
     * Returns the name of this agent
     *
     * @return This agent name
     */
    String getThisAgentName();

    /**
     * Retrieves the list of all agents in the task
     *
     * @return Agent list
     */
    ArrayList<String> getAgentList();

    /**
     * Returns the number of agents in the task
     *
     * @return Number of agents
     */
    int numAgents();

    /**
     * Returns the name of the agent which sent the last received message
     *
     * @return Agent name
     */
    String getSenderAgent();

    /**
     * Returns true if this agent has the baton
     */
    boolean batonAgent();

    /**
     * Returns the name of the baton agent
     *
     * @return Name of the baton agent
     */
    String getBatonAgent();

    /**
     * Returns the list of agents, excluding itself
     *
     * @return List of the other agents in the task
     */
    ArrayList<String> getOtherAgents();

    /**
     * Returns the index of the given agent
     *
     * @param agName Agent name
     * @return Agent index
     */
    int getAgentIndex(String agName);

    /**
     * Returns the number of sent messages
     */
    int getNumMessages();

    /**
     * Sends a message to all other agents and waits an acknowledgment
     *
     * @param obj     Message content
     * @param waitACK Waits for a receipt confirmation
     */
    void sendMessage(Serializable obj, boolean waitACK);

    void sendAck(String toAgent);

    /**
     * Sends a message to other agent and waits an acknowledgment
     *
     * @param toAgent Receiver
     * @param obj     Message content
     * @param waitACK Waits for a receipt confirmation
     */
    void sendMessage(String toAgent, Serializable obj, boolean waitACK);

    /**
     * Receives a message from other agent
     *
     * @param fromAgent Message sender
     * @param sendACK   Send a confirmation when the message is received
     * @return Content of the message
     */
    Serializable receiveMessage(String fromAgent, boolean sendACK);

    /**
     * Receives a message from other agent
     *
     * @param sendACK Send a confirmation when the message is received
     * @return Content of the message
     */
    Serializable receiveMessage(boolean sendACK);

    /**
     * Receives a message from other agent that fits the given filter
     *
     * @param filter  Message filter
     * @param sendACK Send a confirmation when the message is received
     * @return Content of the message
     */
    Serializable receiveMessage(MessageFilter filter, boolean sendACK);

    /**
     * Passes the baton to the following agent
     */
    void passBaton();

    public boolean registeredAgent(String ag);

    public void enqueueMsg(Message message);

    public void close();

}
