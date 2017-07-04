package org.agreement_technologies.common.map_parser;

import java.io.IOException;
import java.text.ParseException;

/**
 * @author Oscar
 */
public interface PDDLParser {

    Task parseDomain(String domainFile) throws ParseException, IOException;

    void parseProblem(String problemFile, Task planningTask, AgentList agList, String agentName) throws ParseException, IOException;

    boolean isMAPDDL(String domainFile) throws IOException;

    AgentList parseAgentList(String agentsFile) throws ParseException, IOException;

    AgentList createEmptyAgentList();

}
