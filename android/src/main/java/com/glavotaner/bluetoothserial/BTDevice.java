package com.glavotaner.bluetoothserial;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.getcapacitor.JSObject;

public class BTDevice implements Parcelable {

    private final String address;
    private final String name;
    private final int deviceClass;

    BTDevice(String address, String name, int deviceClass) {
        this.address = address;
        this.name = name;
        this.deviceClass = deviceClass;
    }

    BTDevice(Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        address = bundle.getString("address");
        name = bundle.getString("name");
        deviceClass = bundle.getInt("deviceClass");
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        bundle.putString("name", name);
        bundle.putInt("deviceClass", deviceClass);
        parcel.writeBundle(bundle);
    }

    public static final Parcelable.Creator<BTDevice> CREATOR = new Parcelable.Creator<>() {
        @Override
        public BTDevice createFromParcel(Parcel parcel) {
            return new BTDevice(parcel);
        }

        @Override
        public BTDevice[] newArray(int i) {
            return new BTDevice[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public JSObject toJSObject() {
        return new JSObject().put("address", address)
                .put("name", name)
                .put("deviceClass", deviceClass);
    }

}
