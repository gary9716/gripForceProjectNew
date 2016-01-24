package com.mhci.gripandtipforce.model;

/**
 * Created by lab430 on 16/1/22.
 */


public class BTEvent {

    public enum Type {
        None,
        Connecting,
        Connected,
        Disconnected,
        ConnectionFailed,
        EnablingFailed,
        DeviceSelected
    }

    public Type type;
    public String deviceName;
    public String deviceAddr;

    public void BTEvent() {

        this.type = Type.None;
        this.deviceName = null;
        this.deviceAddr = null;

    }


    public void BTEvent(Type type) {

        this.type = type;
        this.deviceName = null;
        this.deviceAddr = null;

    }

    public void BTEvent(String name, String addr) {

        this.type = Type.Connected;
        this.deviceName = name;
        this.deviceAddr = addr;

    }


}
