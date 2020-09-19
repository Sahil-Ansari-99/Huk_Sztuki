package com.company.neuralstyletransfer;

import android.app.Application;
import android.util.Log;

import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

public class ServerConnect extends Application {
    private final String IP_ADDRESS = "192.168.1.6"; // 192.168.43.146
    private final int PORT_NUMBER = 8080;
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("Server Connect class initialised");
        connectToServer();
    }

    private void connectToServer() {
        new Thread(new SocketConnector()).start();
    }

    public Socket getSocket() {
        return this.socket;
    }

    public boolean isConnected() {
        if (socket == null) return false;
        return socket.isConnected();
    }

    class SocketConnector implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress serverAddress = InetAddress.getByName(IP_ADDRESS);
                socket = new Socket(serverAddress, PORT_NUMBER);
                Log.d("Socket", String.valueOf(socket.isConnected()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
