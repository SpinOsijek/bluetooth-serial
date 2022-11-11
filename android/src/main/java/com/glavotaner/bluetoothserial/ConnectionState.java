package com.glavotaner.bluetoothserial;

public enum ConnectionState {
    NONE(0),
    CONNECTING(1),
    CONNECTED(2);
    private final int state;
    ConnectionState(int state) {
        this.state = state;
    }

    public int value() {
        return state;
    }
}
