package com.yunzhiling.yzlconnect.service;

public interface ConnectCallback {
  void connected();
  void sended();
  void fail();
  void timeout();
}
