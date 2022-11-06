package com.glavotaner.bluetoothserial;

import android.content.Intent;

public class Intents {
    public static final String CONNECTION_CHANGE = "connectionChange";
    public static final String WRITE = "write";

    public static Intent getConnectionChangeIntent() {
        return new Intent(Intents.CONNECTION_CHANGE);
    }

    public static Intent getWriteIntent() {
        return new Intent(Intents.WRITE);
    }
}
