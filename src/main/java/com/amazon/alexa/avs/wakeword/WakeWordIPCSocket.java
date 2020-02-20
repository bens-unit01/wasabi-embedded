/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package com.amazon.alexa.avs.wakeword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class WakeWordIPCSocket extends WakeWordIPC implements Runnable {

    private ServerSocket serverSocket = null;
    private Thread ipcThread = null;
    private final Set<WakeWordIPCConnectedClient> connectedClients = new HashSet<>();

    private static void log(String message) {
    	
    	System.out.println("<WakeWordIPCSocket> " + message);
    }
    public WakeWordIPCSocket(WakeWordDetectedHandler handler, int portNumber) throws IOException {
        super(handler);
        serverSocket = new ServerSocket(portNumber, 0, InetAddress.getByName(null));
    }

    public void init() {
         System.out.println("<WakeWordIPCSocket#init> ..."); 
        if (ipcThread == null) {
            ipcThread = new Thread(this);
            ipcThread.start();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
         	System.out.println("<WakeWordIPCSocket#run> .accept ..."); 
                Socket clientSocket = serverSocket.accept();
         	System.out.println("<WakeWordIPCSocket#run> .accepted ..."); 
                WakeWordIPCConnectedClient newConnectedClient =
                        new WakeWordIPCConnectedClient(clientSocket, this);
                newConnectedClient.init();
                registerClient(newConnectedClient);
            } catch (Exception e) {
                log("Could not accept/connect IPC client");
            }
        }
    }

    public synchronized void registerClient(WakeWordIPCConnectedClient newClient) {
        connectedClients.add(newClient);
        log("New IPC client was accepted, current of current clients is "
                + connectedClients.size());
    }

    public synchronized void unregisterClient(WakeWordIPCConnectedClient oldClient) {
        connectedClients.remove(oldClient);
        log("IPC client was removed, current of current clients is "
                        + connectedClients.size());
    }

    @Override
    public synchronized void sendCommand(IPCCommand command) throws IOException {
        log("Sending command " + command + " to all connected clients");
        for (WakeWordIPCConnectedClient client : connectedClients) {
            client.send(command);
        }
    }

    public void processWakeWordDetected() {
        log("Wake Word Detected ......");
        (new Thread() {
            @Override
            public void run() {
                wakeWordDetected();
            }
        }).start();
    }
}
