package org.agreement_technologies.service.map_communication;

import org.agreement_technologies.common.map_communication.AgentCommunication;
import org.agreement_technologies.common.map_communication.Message;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author Oscar
 */
public class CommunicationServer extends Thread {
    private final ServerSocket server;
    private final AgentCommunication comm;
    private final int numConnections;

    public CommunicationServer(AgentCommunication comm, int agentIndex, int numConnections) throws IOException {
        server = new ServerSocket(AgentCommunication.BASE_PORT + agentIndex);
        this.comm = comm;
        this.numConnections = numConnections;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < numConnections; i++) {
                try {
                    Socket client = server.accept();
                    Connection newConnection = new Connection(client);
                    newConnection.start();
                } catch (SocketTimeoutException timeout) {
                }
            }
        } catch (Exception e) {
            System.out.println("SERVER ERROR: " + e);
        }
    }

    private class Connection extends Thread {
        private Socket client;

        private Connection(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                    Object obj = in.readObject();
                    if (obj instanceof MessageImp) comm.enqueueMsg((Message) obj);
                    else {
                        ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                        out.writeObject(obj);
                        out.flush();
                    }
                }
            } catch (Exception ex) {
                IOUtils.closeQuietly(server);
            }
        }
    }
}
