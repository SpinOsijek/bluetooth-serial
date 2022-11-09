package com.glavotaner.bluetoothserial;

import android.os.Parcel;
import android.os.Parcelable;

import com.getcapacitor.JSObject;

public class BTDevice implements Parcelable {

    private final String address;
    private final String name;
    private final String className;

    BTDevice(String address, String name, String className) {
        this.address = address;
        this.name = name;
        this.className = className;
    }

    BTDevice(Parcel in) {
        String[] data = new String[3];
        in.readStringArray(data);
        this.address = data[0];
        this.name = data[1];
        this.className = data[2];
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(new String[] {this.address, this.name, this.className});
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
        return new JSObject().put("address", address).put("name", name).put("class", className);
    }

}
