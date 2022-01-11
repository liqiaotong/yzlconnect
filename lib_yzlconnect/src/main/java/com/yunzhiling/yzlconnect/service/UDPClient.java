package com.yunzhiling.yzlconnect.service;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class UDPClient {

    private static final String HOST = "192.168.4.1";
    private static final int PORT = 1000;

    private static DatagramSocket socket;
    private static Handler handler;
    private static Runnable checkRunnable;
    private volatile static Boolean isConnected = false;
    private volatile static Boolean isRunning = false;
    private volatile static boolean isReceiveZore = false;

    public static void udpConnect(int port, String ssid, String password, double lng, double lat, ConnectCallback connectCallback) throws UnknownHostException, IOException {
        Log.d("yzlconnect","----------->udpConnect");
        socket = new DatagramSocket(9999);
        socket.setSoTimeout(45000);
        try {
            //重置连接状态
            isConnected = false;
            isRunning = true;
            isReceiveZore = false;
            String message = "{\"port\":\"" + port + "\",\"password\":\"" + password + "\",\"ssid\":\"" + ssid + "\",\"lng\":" + lng + ",\"lat\":" + lat + "}";
            new Thread(() -> sendConnectData(message, connectCallback)).start();
            new Thread(() -> readConnectData(connectCallback)).start();
            if (handler == null) {
                handler = new Handler(Looper.myLooper());
            }
            if (checkRunnable == null) {
                checkRunnable = () -> {
                    Log.d("yzlconnect","----------->udp connect 15 second");
                    if (!isConnected) {
                        Log.d("yzlconnect","----------->udp timeout");
                        if (connectCallback != null) connectCallback.timeout();
                        close();
                    }
                };
            }
            if (handler != null && checkRunnable != null) {
                handler.removeCallbacks(checkRunnable);
                handler.postDelayed(checkRunnable, 47000);
            }
        } catch (Exception e) {
            Log.d("yzlconnect","----------->udp connect fail");
            if (connectCallback != null) connectCallback.fail();
        }
    }

    //连接成功后请求设备，无需回调
    private static void sendOKData(String message, int sendTimes) {
        if (socket != null && !socket.isClosed()) {
            try {
                byte[] buf = message.getBytes();
                InetAddress address = InetAddress.getByName(HOST);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PORT);
                int currentSendTimes = 0;
                while (currentSendTimes < sendTimes) {
                    Log.d("yzlconnect","----------->udp send ok times:" + currentSendTimes);
                    try {
                        socket.send(packet);
                    } catch (Exception e) {

                    }
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {

                    }
                    currentSendTimes++;
                }
                close();
            } catch (Exception e) {
                Log.d("yzlconnect","----------->udp send ok error");
                close();
            }
        }
    }

    private static void sendConnectData(String message, ConnectCallback connectCallback) {
        Log.d("yzlconnect","----------->udp send data");
        try {
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName(HOST);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PORT);
            int currentSendTimes = 0;
            Log.d("yzlconnect","----------->udp send data isRunning:"+isRunning+"  isConnected:"+isConnected+"  socket is not null:"+(socket!=null)+"   isReceiveZore:"+isReceiveZore);
            while (isRunning && !isConnected && socket != null && !socket.isClosed() && currentSendTimes < 50 && !isReceiveZore) {
                Log.d("yzlconnect","----------->udp send times:" + currentSendTimes);
                try {
                    socket.send(packet);
                } catch (Exception e) {

                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) {

                }
                currentSendTimes++;
            }
        } catch (Exception e) {
            Log.d("yzlconnect","----------->udp send error");
            close();
            if (connectCallback != null) connectCallback.fail();
        }
    }

    private static void readConnectData(ConnectCallback connectCallback) {
        Log.d("yzlconnect","----------->udp read");
        if (isRunning) {
            try {
                byte[] recBuf = new byte[1024];
                DatagramPacket recvPacket = new DatagramPacket(recBuf, recBuf.length);
                if (socket != null) {
                    try {
                        socket.receive(recvPacket);
                    } catch (SocketTimeoutException e) {
                        if (connectCallback != null) connectCallback.timeout();
                        close();
                    } catch (SocketException e){
                        if (connectCallback != null) connectCallback.fail();
                        close();
                    } catch (Exception e){

                    }
                }
                String recvContent = new String(recvPacket.getData(), 0, recvPacket.getLength());
                if (isRunning && socket != null && !socket.isClosed()) {
                    if (recvContent != null) {
                        String code;
                        try {
                            JSONObject jsonObject = new JSONObject(recvContent);
                            code = jsonObject.getString("code");
                        } catch (Exception e) {
                            Log.d("yzlconnect","----------->udp read json error");
                            code = "";
                        }
                        if (code != null) {
                            Log.d("yzlconnect","----------->udp read code:" + code);
                        }
                        if (TextUtils.equals(code, "0")) {
                            isReceiveZore = true;
                            if (connectCallback != null) connectCallback.sended();
                        }
                        if (TextUtils.equals(code, "2")) {
                            isConnected = true;
                            sendOKData("{\"app\":\"ok\"}", 5);
                            if (!isReceiveZore && connectCallback != null) connectCallback.sended();
                            if (connectCallback != null) connectCallback.connected();
                        } else if (TextUtils.equals(code, "3")) {
                            if (connectCallback != null) connectCallback.fail();
                        }
                        else {
                            if (isRunning && socket != null && !socket.isClosed() && !isConnected)
                                readConnectData(connectCallback);
                        }
                    }
                }
            } catch (Exception e) {
                Log.d("yzlconnect","----------->udp read error message:" + e.getMessage());
                if (socket != null && !socket.isClosed() && !isConnected)
                    readConnectData(connectCallback);
            }
        }
    }

    public static void close() {
        Log.d("yzlconnect","----------->udp close");
        isConnected = false;
        isRunning = false;
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {

        }
        try {
            if (handler != null && checkRunnable != null) {
                handler.removeCallbacks(checkRunnable);
            }
        } catch (Exception e) {

        }
    }


}