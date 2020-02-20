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

import com.amazon.alexa.avs.wakeword.WakeWordIPC.IPCCommand;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


// this class represents a single wake word agent that is connected
// over some form of IPC.  This agent is expected to notify us
// when the wake-word is detected from the audio input.
public class WakeWordIPCConnectedClient implements Runnable {

    private WakeWordIPCSocket wakeWordIPCSocket = null;
    private Thread connectionThread = null;
    private Socket clientSocket = null;
    private DataInputStream input = null;
    private DataOutputStream output = null;
    private volatile boolean running = true;


    public WakeWordIPCConnectedClient(Socket clientSocket, WakeWordIPCSocket wakeWordIPCSocket)
            throws IOException {
        this.wakeWordIPCSocket = wakeWordIPCSocket;
        this.clientSocket = clientSocket;
        input = new DataInputStream(clientSocket.getInputStream());
        output = new DataOutputStream(clientSocket.getOutputStream());
    }

    public void init() {
        if (connectionThread == null) {
            connectionThread = new Thread(this);
            connectionThread.start();
        }
    }
    private static void log(String message) {
    	
    	System.out.println("<WakeWordIPCConnectedClient> "  + message);
    }

    @Override
    public void run() {
        while (running) {
            try {
                int receivedCommand = input.readInt();
                if (IPCCommand.IPC_DISCONNECT.getValue() == receivedCommand) {
                    terminate();
                } else if (IPCCommand.IPC_WAKE_WORD_DETECTED.getValue() == receivedCommand) {
                    log("Received wake word detected");
                    wakeWordIPCSocket.processWakeWordDetected();
                }
            } catch (Exception e) {
                log("Could not read/process the command received:");
                terminate();
            }
        }
    }

    void terminate() {
        log("Terminating/Disconneting a Wake Word Agent, Bye!");
        running = false;
        wakeWordIPCSocket.unregisterClient(this);
        try {
            if (input != null) {
                input.close();
                if (clientSocket != null)
                    clientSocket.close();
            }
        } catch (IOException e) {
            log("Could not close streams and socket:");
        }
    }

    public void send(IPCCommand command) throws IOException {
        output.writeInt(command.getValue());
    }
}
