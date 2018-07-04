package com.ums.wifiprobeapp;
interface IProbeStateCallback{
  void updateProbeState(int status);//0 关闭 1开启
}