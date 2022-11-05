package com.glavotaner.bluetoothserial;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

@SuppressLint("InlinedApi")
@CapacitorPlugin(name = "BluetoothSerial", permissions = {
        @Permission(strings = {Manifest.permission.BLUETOOTH_SCAN}, alias = BluetoothSerialPlugin.SCAN),
        @Permission(strings = {Manifest.permission.BLUETOOTH_CONNECT}, alias = BluetoothSerialPlugin.CONNECT),
        @Permission(strings = {Manifest.permission.ACCESS_COARSE_LOCATION}, alias = BluetoothSerialPlugin.LOCATION)
})
public class BluetoothSerialPlugin extends Plugin {

    // Debugging
    private static final String TAG = "BluetoothSerial";

    public static final String CONNECT = "connect";
    public static final String SCAN = "scan";
    public static final String LOCATION = "location";

    private BluetoothSerial implementation;
    private PluginCall connectCall;

    StringBuffer buffer = new StringBuffer();

    @Override
    public void load() {
        super.load();
        Handler mHandler = getHandler();
        implementation = new BluetoothSerial(mHandler);
    }

    @PluginMethod
    public void echo(@NonNull PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject().put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void connect(PluginCall call) {
        if (hasCompatPermission(CONNECT)) {
            connectToDevice(call);
        } else {
            requestPermissionForAlias(CONNECT, call, "connectPermsCallback");
        }
    }

    private void connectToDevice(@NonNull PluginCall call) {
        String macAddress = call.getString("address");
        BluetoothDevice device = implementation.getRemoteDevice(macAddress);
        if (device != null) {
            connectCall = call;
            implementation.connect(device, false);
            buffer.setLength(0);
        } else {
            call.reject("Could not connect to " + macAddress);
        }
    }

    @PermissionCallback
    private void connectPermsCallback(PluginCall call) {
        if (getPermissionState(CONNECT) == PermissionState.GRANTED) {
            connectToDevice(call);
        } else {
            call.reject("Connect permission denied");
        }
    }

    @PluginMethod
    public void disconnect(@NonNull PluginCall call) {
        implementation.stop();
        call.resolve();
    }

    @PluginMethod
    public void write(@NonNull PluginCall call) throws JSONException {
        byte[] data = (byte[]) call.getData().get("data");
        implementation.write(data);
        call.resolve();
    }

    @PluginMethod
    public void read(@NonNull PluginCall call) {
        int length = buffer.length();
        String data = buffer.substring(0, length);
        buffer.delete(0, length);
        JSObject result = new JSObject().put("data", data);
        call.resolve(result);
    }

    @PluginMethod
    public void available(@NonNull PluginCall call) {
        JSObject result = new JSObject().put("available", buffer.length());
        call.resolve(result);
    }

    @PluginMethod
    public void isEnabled(@NonNull PluginCall call) {
        JSObject result = new JSObject().put("isEnabled", implementation.isEnabled());
        call.resolve(result);
    }

    @PluginMethod
    public void isConnected(@NonNull PluginCall call) {
        int bluetoothState = implementation.getState();
        JSObject result = new JSObject()
                .put("isConnected", bluetoothState == ConnectionState.CONNECTED);
        call.resolve(result);
    }

    @PluginMethod
    public void clear(@NonNull PluginCall call) {
        buffer.setLength(0);
        call.resolve();
    }

    @PluginMethod
    public void settings(@NonNull PluginCall call) {
        Intent bluetoothSettingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        getActivity().startActivity(bluetoothSettingsIntent);
        call.resolve();
    }

    @PluginMethod
    public void enable(PluginCall call) {
        if (getPermissionState(LOCATION) == PermissionState.GRANTED) {
            enableBluetooth(call);
        } else {
            requestPermissionForAlias(LOCATION, call, "enablePermsCallback");
        }
    }

    private void enableBluetooth(PluginCall call) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(call, enableIntent, "enableBluetoothActivityCallback");
    }

    @PermissionCallback
    private void enablePermsCallback(PluginCall call) {
        if (getPermissionState(LOCATION) == PermissionState.GRANTED) {
            enableBluetooth(call);
        } else {
            call.reject("Location permission denied");
        }
    }

    @ActivityCallback
    private void enableBluetoothActivityCallback(@NonNull PluginCall call, @NonNull ActivityResult activityResult) {
        boolean isEnabled = activityResult.getResultCode() == Activity.RESULT_OK;
        Log.d(TAG, "User enabled Bluetooth: " + isEnabled);
        JSObject result = new JSObject().put("isEnabled", isEnabled);
        call.resolve(result);
    }

    @PluginMethod
    public void list(PluginCall call) {
        if (hasCompatPermission(CONNECT)) {
            listPairedDevices(call);
        } else {
            requestPermissionForAlias(CONNECT, call, "listPermsCallback");
        }
    }

    @PermissionCallback
    private void listPermsCallback(PluginCall call) {
        if (getPermissionState(CONNECT) == PermissionState.GRANTED) {
            listPairedDevices(call);
        } else {
            call.reject("Connect permission denied");
        }
    }

    private void listPairedDevices(PluginCall call) {
        JSONArray deviceList = new JSONArray();
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> bondedDevices = implementation.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            deviceList.put(deviceToJSON(device));
        }
        JSObject result = new JSObject().put("devices", deviceList);
        call.resolve(result);
    }

    @PluginMethod
    public void discover(PluginCall call) {
        if (hasCompatPermission(SCAN)) {
            startDiscovery(call);
        } else {
            requestPermissionForAlias(SCAN, call, "discoverPermsCallback");
        }
    }

    @PermissionCallback
    private void discoverPermsCallback(PluginCall call) {
        if (getPermissionState(SCAN) == PermissionState.GRANTED) {
            startDiscovery(call);
        } else {
            call.reject("Scan permission denied");
        }
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery(PluginCall call) {
        final BroadcastReceiver discoverReceiver = new BroadcastReceiver() {

            private final JSONArray unpairedDevices = new JSONArray();

            public void onReceive(Context context, @NonNull Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    unpairedDevices.put(deviceToJSON(device));
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    JSObject result = new JSObject().put("devices", unpairedDevices);
                    call.resolve(result);
                    getActivity().unregisterReceiver(this);
                }
            }
        };

        Activity activity = getActivity();
        activity.registerReceiver(discoverReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        activity.registerReceiver(discoverReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        implementation.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    private JSObject deviceToJSON(@NonNull BluetoothDevice device) {
        JSObject json = new JSObject()
                .put("name", device.getName())
                .put("address", device.getAddress())
                .put("id", device.getAddress());
        if (device.getBluetoothClass() != null) {
            json.put("class", device.getBluetoothClass().getDeviceClass());
        }
        return json;
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (implementation != null) {
            implementation.stop();
        }
    }

    private boolean hasCompatPermission(String alias) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getPermissionState(alias) == PermissionState.GRANTED;
        } else {
            return true;
        }
    }

    @NonNull
    @Contract(" -> new")
    private Handler getHandler() {
        return new Handler(Looper.myLooper(), message -> {
            Log.d("BT-Message", message.toString());
            switch (message.what) {
                case Messages.READ: buffer.append((String) message.obj); break;
                case Messages.STATE_CHANGE: {
                    if (connectCall != null) {
                        String device = (String) message.obj;
                        connectCall.resolve(new JSObject().put("device", device));
                    }
                }
                case Messages.CONNECTION_ERROR: {
                    if (connectCall != null) {
                        String error = (String) message.obj;
                        connectCall.reject(error);
                    }
                }
            }
            return false;
        });
    }

}
