package com.yunzhiling.yzlconnect.service;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPClient {

    private static final String HOST = "192.168.4.1";
    private static final int PORT = 1000;

    private static Socket socket;
    private static Thread sendMessageThread;
    private static Thread receiveMessageThread;
    private static OutputStream outputStream;
    private static InputStream inputStream;
    private static Handler handler;
    private static Runnable checkRunnable;
    private volatile static Boolean isConnected = false;
    private volatile static Boolean isRunning = false;
    private volatile static int stocketConnectTimes = 0;
    private volatile static boolean isReceiveZore = false;

    public static void tcpConnect(final int port, String ssid, String password, double lng, double lat, ConnectCallback connectCallback) {
        Log.d("yzlconnect","----------->tcpConnect");
        new Thread(() -> {
            stocketConnectTimes = 0;
            stocketConnect(connectCallback);
        }).start();
        sendMessageThread = new Thread(() -> {
            String sendMessage = "{\"port\":\"" + port + "\",password\":\"" + password + "\",\"ssid\":\"" + ssid + "\",\"lng\":" + lng + ",\"lat\":" + lat + "}";
            sendConnectMessage(sendMessage, connectCallback);
        });
        receiveMessageThread = new Thread(() -> receive(connectCallback));
        if (handler == null) {
            handler = new Handler(Looper.myLooper());
        }
        if (checkRunnable == null) {
            checkRunnable = () -> {
                Log.d("yzlconnect","----------->tcp connect 15 second");
                if (!isConnected) {
                    Log.d("yzlconnect","----------->tcp timeout");
                    if (connectCallback != null) connectCallback.timeout();
                    close();
                }
            };
        }
    }

    public static void stocketConnect(ConnectCallback connectCallback) {
        try {
            close();
            isRunning = true;
            isConnected = false;
            isReceiveZore = false;
            socket = new Socket(HOST, PORT);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            Log.d("yzlconnect","----------->tcp stocket connect");
            //连接成功后开始发送和接收
            if (receiveMessageThread != null) {
                receiveMessageThread.start();
            }
            if (sendMessageThread != null) {
                sendMessageThread.start();
            }
            if (handler != null && checkRunnable != null) {
                handler.removeCallbacks(checkRunnable);
                handler.postDelayed(checkRunnable, 45000);
            }
        } catch (Exception e) {
            Log.d("yzlconnect","----------->tcp stocket connect error");
            if (isRunning) {
                if (stocketConnectTimes <= 3) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception es) {

                    }
                    Log.d("yzlconnect","----------->tcp stocket connect again times:" + stocketConnectTimes);
                    stocketConnectTimes++;
                    stocketConnect(connectCallback);
                } else {
                    Log.d("yzlconnect","----------->tcp stocket connect fail");
                    close();
                    if (connectCallback != null) connectCallback.fail();
                }
            }
        }
    }

    public static void sendOkMessage(String message, int sendTimes, ConnectCallback connectCallback) {
        try {
            int currentSendTimes = 0;
            while (socket != null && outputStream != null && currentSendTimes < sendTimes) {
                try {
                    Log.d("yzlconnect","----------->tcp send ok times:" + currentSendTimes);
                    outputStream.write(message.getBytes());
                    outputStream.flush();
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
            Log.d("yzlconnect","----------->tcp send ok error");
            close();
            if (connectCallback != null) connectCallback.fail();
        }

    }

    public static void sendConnectMessage(String message, ConnectCallback connectCallback) {
        try {
            int currentSendTimes = 0;
            while (isRunning && !isConnected && outputStream != null && socket != null && !socket.isClosed() && currentSendTimes < 1 && !isReceiveZore) {
                Log.d("yzlconnect","----------->tcp send connect data times:" + currentSendTimes);
                try {
                    outputStream.write(message.getBytes());
                    outputStream.flush();
                } catch (Exception e) {

                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) {

                }
                currentSendTimes++;
            }
        } catch (Exception e) {
            Log.d("yzlconnect","----------->tcp send ok error");
            if (connectCallback != null) connectCallback.fail();
            close();
        }
    }

    public static void receive(ConnectCallback connectCallback) {
        Log.d("yzlconnect","----------->tcp receive");
        try {
            byte[] buffer = new byte[1024];
            int length;
            while (isRunning && socket != null && !socket.isClosed() && inputStream != null && ((length = inputStream.read(buffer)) != -1)) {
                String data = new String(buffer, 0, length);
                String code;
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    code = jsonObject.getString("code");
                } catch (Exception e) {
                    Log.d("yzlconnect","----------->tcp read json error");
                    code = "";
                }
                if (code != null) {
                    Log.d("yzlconnect","----------->tcp receive code:" + code);
                }
                if (TextUtils.equals(code, "0")) {
                    isReceiveZore = true;
                    if (connectCallback != null) connectCallback.sended();
                } else if (TextUtils.equals(code, "2")) {
                    isConnected = true;
                    sendOkMessage(" {\"app\":\"ok\"}", 1, connectCallback);
                    if (!isReceiveZore && connectCallback != null) connectCallback.sended();
                    if (connectCallback != null) connectCallback.connected();
                    break;
                } else if (TextUtils.equals(code, "3")) {
                    if (connectCallback != null) connectCallback.fail();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("yzlconnect","----------->tcp receive error");
        }
    }

    public static void close() {
        Log.d("yzlconnect","----------->tcp close");
        isConnected = false;
        isRunning = false;
        try {
            if (outputStream != null) {
                outputStream.close(); //关闭输出流
                outputStream = null;
            }
        } catch (Exception e) {

        }
        try {
            if (inputStream != null) {
                inputStream.close(); //关闭输入流
                inputStream = null;
            }
        } catch (Exception e) {

        }
        try {
            if (socket != null) {
                socket.close();  //关闭socket
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