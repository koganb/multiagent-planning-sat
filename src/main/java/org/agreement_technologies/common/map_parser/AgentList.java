package org.agreement_technologies.common.map_parser;

/**
 * @author Oscar
 */
public interface AgentList {

    public void addAgent(String name, String ip);

    public boolean isEmpty();

    public String getIP(int index);

    public int numAgents();

    public String getName(int index);

}
