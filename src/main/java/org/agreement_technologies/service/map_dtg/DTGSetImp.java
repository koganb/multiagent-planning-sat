package org.agreement_technologies.service.map_dtg;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_dtg.DTG;
import org.agreement_technologies.common.map_dtg.DTGData;
import org.agreement_technologies.common.map_dtg.DTGSet;
import org.agreement_technologies.common.map_dtg.DTGTransition;
import org.agreement_technologies.common.map_grounding.GroundedCond;
import org.agreement_technologies.common.map_grounding.GroundedEff;
import org.agreement_technologies.common.map_grounding.GroundedTask;
import org.agreement_technologies.common.map_grounding.GroundedVar;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class DTGSetImp implements DTGSet {
    private Hashtable<String, DTG> dtgs;

    public DTGSetImp(GroundedTask task) {
        GroundedVar[] vars = task.getVars();
        dtgs = new Hashtable<String, DTG>(vars.length);
        for (GroundedVar v : vars) {
            DTG dtg = new DTGImp(this, v, task);
            dtgs.put(v.toString(), dtg);
        }
    }

    @Override
    public void distributeDTGs(AgentCommunication comm, GroundedTask gTask) {
        if (comm.numAgents() > 1) {
            boolean endDTG[] = new boolean[comm.numAgents()];    // Initialized to false
            do {
                sendDTGTransitions(comm, endDTG);
                receiveDTGTransitions(comm, gTask, endDTG);
            } while (!checkEndDTG(comm, endDTG));
        }
    }

    @Override
    public DTG getDTG(GroundedVar v) {
        return dtgs.get(v.toString());
    }

    @Override
    public DTG getDTG(String varName) {
        return dtgs.get(varName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Enumeration<String> e = dtgs.keys();
        while (e.hasMoreElements()) {
            DTG dtg = dtgs.get(e.nextElement());
            sb.append(dtg.toString() + "\n");
        }
        return sb.toString();
    }

    private void addTransition(String varName, String startValue,
                               String finalValue, GroundedCond[] commonPrecs,
                               GroundedEff[] commonEffs, String fromAgent) {
        DTG dtg = dtgs.get(varName);
        if (dtg == null) return;
        ((DTGImp) dtg).addTransition(startValue, finalValue, commonPrecs, commonEffs, fromAgent);
    }

    private DTGTransition[] getNewTransitions() {
        ArrayList<DTGTransition> newTransitions = new ArrayList<DTGTransition>();
        Enumeration<String> e = dtgs.keys();
        while (e.hasMoreElements()) {
            DTG dtg = dtgs.get(e.nextElement());
            DTGTransition[] tList = ((DTGImp) dtg).getNewTransitions();
            for (DTGTransition t : tList)
                newTransitions.add(t);
        }
        return newTransitions.toArray(new DTGTransition[newTransitions.size()]);
    }

    private void receiveDTGTransitions(AgentCommunication comm, GroundedTask gTask, boolean[] endDTG) {
        for (String ag : comm.getOtherAgents()) {
            java.io.Serializable data = comm.receiveMessage(ag, false);
            if (data instanceof String) {
                if (((String) data).equals(AgentCommunication.END_STAGE_MESSAGE))
                    endDTG[comm.getAgentIndex(ag)] = true;
                else
                    throw new RuntimeException("Agent " + ag + " is not following the DTG protocol");
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<DTGData> dataReceived = (ArrayList<DTGData>) data;
                endDTG[comm.getAgentIndex(ag)] = false;
                for (DTGData d : dataReceived) {
                    addTransition(d.getVarName(), d.getStartValue(),
                            d.getFinalValue(), d.getCommonPrecs(gTask),
                            d.getCommonEffs(gTask), ag);
                }
            }
        }
    }

    private void sendDTGTransitions(AgentCommunication comm, boolean[] endDTG) {
        String myAgent = comm.getThisAgentName();
        DTGTransition[] newTransitions = getNewTransitions();
        boolean somethingToSend = newTransitions.length > 0;
        endDTG[comm.getThisAgentIndex()] = !somethingToSend;
        if (somethingToSend) {
            for (String ag : comm.getOtherAgents()) {
                ArrayList<DTGData> data = new ArrayList<DTGData>(newTransitions.length);
                for (DTGTransition t : newTransitions)
                    if (DTGData.shareable(t, ag) && t.getAgents().contains(myAgent)) {
                        data.add(new DTGData(t, ag));
                    }
                comm.sendMessage(ag, data, false);
            }
        } else {
            comm.sendMessage(AgentCommunication.END_STAGE_MESSAGE, false);
        }
    }

    private boolean checkEndDTG(AgentCommunication comm, boolean[] endDTG) {
        boolean finished = true;
        for (boolean end : endDTG)
            if (!end) {
                finished = false;
                break;
            }
        if (!finished) {    // Synchronization message
            if (comm.batonAgent()) comm.sendMessage(AgentCommunication.SYNC_MESSAGE, true);
            else comm.receiveMessage(comm.getBatonAgent(), true);
        }
        return finished;
    }

    @Override
    public void clearCache(int threadIndex) {
        for (DTG dtg : dtgs.values())
            dtg.clearCache(threadIndex);
    }
}
