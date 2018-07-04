package com.ums.wifiprobeapp;
import com.ums.wifiprobeapp.IProbeInfoCallback;
import com.ums.wifiprobeapp.IProbeStateCallback;
interface IProbeService{
    void startGetProbeState(IProbeStateCallback cb);
    void stopGetProbeState(IProbeStateCallback cb);
    void startGetProbeInfo(IProbeInfoCallback cb);
    void stopGetProbeInfo(IProbeInfoCallback cb);
}