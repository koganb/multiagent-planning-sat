package org.agreement_technologies.service.map_parser;

import org.agreement_technologies.common.map_parser.AgentList;

import java.util.ArrayList;

/**
 * @author Oscar
 */
public class AgentListImp implements AgentList {

    private final ArrayList<Agent> list;

    public AgentListImp() {
        list = new ArrayList<>();
    }

    @Override
    public void addAgent(String name, String ip) {
        list.add(new Agent(name, ip));
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public String toString() {
        String s = "";
        for (Agent a : list) s += "[" + a + "]";
        return s;
    }

    @Override
    public String getIP(int index) {
        return list.get(index).ip;
    }

    @Override
    public int numAgents() {
        return list.size();
    }

    @Override
    public String getName(int index) {
        return list.get(index).name;
    }

    private static class Agent {
        String name;
        String ip;

        private Agent(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        @Override
        public String toString() {
            return name + ":" + ip;
        }
    }
}
