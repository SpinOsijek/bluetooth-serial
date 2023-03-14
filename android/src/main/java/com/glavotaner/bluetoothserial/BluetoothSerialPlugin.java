package com.glavotaner.bluetoothserial;

import static com.glavotaner.bluetoothserial.Message.SUCCESS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.core.location.LocationManagerCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressLint("InlinedApi")
@CapacitorPlugin(name = "BluetoothSerial", permissions = {
        @Permission(strings = {Manifest.permission.BLUETOOTH_SCAN}, alias = BluetoothSerialPlugin.SCAN),
        @Permission(strings = {Manifest.permission.BLUETOOTH_CONNECT}, alias = BluetoothSerialPlugin.CONNECT),
        @Permission(strings = {Manifest.permission.ACCESS_COARSE_LOCATION}, alias = BluetoothSerialPlugin.COARSE_LOCATION),
        @Permission(strings = {Manifest.permission.ACCESS_FINE_LOCATION}, alias = BluetoothSerialPlugin.FINE_LOCATION)
})
public class BluetoothSerialPlugin extends Plugin {

    // Debugging
    private static final String TAG = "BluetoothSerial";

    public static final String CONNECT = "connect";
    public static final String SCAN = "scan";
    public static final String COARSE_LOCATION = "coarseLocation";
    public static final String FINE_LOCATION = "fineLocation";

    private BluetoothSerial implementation;
    private PluginCall connectCall;
    private PluginCall writeCall;
    private PluginCall discoveryCall;
    private BroadcastReceiver discoveryReceiver;
    private List<String> discoveryPermissions;
    private boolean requiresLocationForDiscovery = true;

    StringBuffer buffer = new StringBuffer();

    @Override
    public void load() {
        super.load();
        setDiscoveryPermissions();
        Looper looper = Looper.getMainLooper();
        Handler connectionHandler = new Handler(looper, message -> {
            Bundle data = message.getData();
            ConnectionState connectionState = ConnectionState.values()[data.getInt("state")];
            if (message.what == SUCCESS) {
                JSObject state = new JSObject().put("state", connectionState.value());
                notifyListeners("connectionChange", state);
                if (connectCall != null && connectionState == ConnectionState.CONNECTED) {
                    connectCall.resolve(state);
                    connectCall = null;
                }
                if (connectionState == ConnectionState.NONE) connectCall = null;
            }
            else if (connectCall != null) {
                String error = data.getString("error");
                connectCall.reject(error);
                connectCall = null;
            }
            return false;
        });
        Handler writeHandler = new Handler(looper, message -> {
            if (writeCall == null) return false;
            if (message.what == SUCCESS) {
                writeCall.resolve();
            } else {
                String error = message.getData().getString("error");
                writeCall.reject(error);
            }
            writeCall = null;
           return false;
        });
        Handler readHandler = new Handler(looper, message -> {
            buffer.append(message.getData().getString("data"));
            return false;
        });
        BluetoothAdapter bluetoothAdapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BluetoothManager bluetoothManager = getContext().getSystemService(BluetoothManager.class);
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        implementation = new BluetoothSerial(bluetoothAdapter, connectionHandler, writeHandler, readHandler);
    }

    private void setDiscoveryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // SCAN - required for just invoking discovery methods
            // CONNECT - required for getting data about a discovered device
            discoveryPermissions = List.of(SCAN, CONNECT);
            if (getConfig().getBoolean("neverScanForLocation", false)) {
                requiresLocationForDiscovery = false;
            } else {
                discoveryPermissions.add(FINE_LOCATION);
            }
        } else {
            discoveryPermissions = List.of(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? FINE_LOCATION : COARSE_LOCATION);
        }
    }

    @PluginMethod
    public void echo(@NonNull PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject().put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void connect(@NonNull PluginCall call) {
        connect(call, device -> implementation.connect(device));
    }

    @PluginMethod
    public void connectInsecure(@NonNull PluginCall call) {
        connect(call, device -> implementation.connectInsecure(device));
    }
    
    private void connect(@NonNull PluginCall call, Connector connector) {
        if (rejectIfBluetoothDisabled(call)) return;
        if (hasCompatPermission(CONNECT)) {
            if (connectCall != null) {
                connectCall.reject("Connection interrupted");
                connectCall = null;
            }
            connectToDevice(call, connector);
        } else {
            requestConnectPermission(call);
        }
    }

    private void connectToDevice(@NonNull PluginCall call, Connector connector) {
        String macAddress = call.getString("address");
        BluetoothDevice device;
        try {
            device = implementation.getRemoteDevice(macAddress);
        } catch(IllegalArgumentException error) {
            call.reject(error.getMessage());
            return;
        }
        if (device != null) {
            cancelDiscovery();
            connectCall = call;
            connector.connect(device);
            buffer.setLength(0);
        } else {
            call.reject("Could not connect to " + macAddress);
        }
    }

    @PluginMethod
    public void disconnect(@NonNull PluginCall call) {
        implementation.resetService();
        call.resolve();
    }

    @PluginMethod
    public void write(@NonNull PluginCall call) throws JSONException {
        if (rejectIfBluetoothDisabled(call)) return;
        byte[] data = ((String) call.getData().get("data")).getBytes(StandardCharsets.UTF_8);
        writeCall = call;
        implementation.write(data);
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
        JSObject result = new JSObject().put("isConnected", implementation.isConnected());
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
        if (hasCompatPermission(CONNECT)) {
            enableBluetooth(call);
        } else {
            requestConnectPermission(call);
        }
    }

    private void enableBluetooth(PluginCall call) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(call, enableIntent, "enableBluetoothActivityCallback");
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
        if (rejectIfBluetoothDisabled(call)) return;
        if (hasCompatPermission(CONNECT)) {
            listPairedDevices(call);
        } else {
            requestConnectPermission(call);
        }
    }

    private void listPairedDevices(PluginCall call) {
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> bondedDevices = implementation.getBondedDevices();
        JSONArray devices = new JSONArray();
        for (BluetoothDevice device: bondedDevices) {
            devices.put(deviceToJSON(device));
        }
        JSObject result = new JSObject().put("devices", devices);
        call.resolve(result);
    }

    @PluginMethod
    public void discoverUnpaired(PluginCall call) {
        if (rejectIfBluetoothDisabled(call)) return;
        if (requiresLocationForDiscovery && !isLocationEnabled()) {
            call.reject("Location services are not enabled");
            return;
        }
        if (hasDiscoveryPermissions()) {
            startDiscovery(call);
        } else {
            requestDiscoveryPermissions(call);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        return LocationManagerCompat.isLocationEnabled(lm);
    }

    private boolean hasDiscoveryPermissions() {
        for (String alias: discoveryPermissions) {
            if (getPermissionState(alias) != PermissionState.GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestDiscoveryPermissions(PluginCall call) {
        String[] requiredPermissions = discoveryPermissions.toArray(new String[0]);
        requestPermissionForAliases(requiredPermissions, call, "discoveryPermissionsCallback");
    }

    @PermissionCallback
    private void discoveryPermissionsCallback(PluginCall call) {
        for (String alias: discoveryPermissions) {
            if (getPermissionState(alias) != PermissionState.GRANTED) {
                call.reject(alias + " permission denied");
                return;
            }
        }
        startDiscovery(call);
    }

    @PluginMethod
    public void cancelDiscovery(PluginCall call) {
        if (hasCompatPermission(SCAN)) {
            cancelDiscovery();
            call.resolve();
        } else {
            requestPermissionForAlias(SCAN, call, "scanPermissionCallback");
        }
    }

    private void cancelDiscovery() {
        if (discoveryCall != null) {
            discoveryCall.reject("Discovery cancelled");
            discoveryCall = null;
            implementation.cancelDiscovery();
            getActivity().unregisterReceiver(discoveryReceiver);
            discoveryReceiver = null;
        }
    }
    
    private void startDiscovery(PluginCall call) {
        cancelDiscovery();
        discoveryCall = call;
        discoveryReceiver = new BroadcastReceiver() {

            private final Set<String> foundAddresses = new HashSet<>();
            private final JSONArray unpairedDevices = new JSONArray();
            private final JSObject result = new JSObject().put("devices", unpairedDevices);

            public void onReceive(Context context, @NonNull Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    // for some reason, ACTION_FOUND sometimes finds duplicates
                    if (!foundAddresses.contains(address)) {
                        foundAddresses.add(address);
                        unpairedDevices.put(deviceToJSON(device));
                        result.put("devices", unpairedDevices);
                        notifyListeners("discoverUnpaired", result);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    discoveryCall.resolve(result);
                    getActivity().unregisterReceiver(this);
                    discoveryCall = null;
                }
            }

        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(discoveryReceiver, filter);
        implementation.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    public static JSObject deviceToJSON(@NonNull BluetoothDevice device) {
        BluetoothClass btClass = device.getBluetoothClass();
        return new JSObject()
                .put("address", device.getAddress())
                .put("name", device.getName())
                .put("deviceClass", btClass != null ? btClass.getDeviceClass() : JSObject.NULL);
    }

    @PermissionCallback
    private void connectPermissionCallback(PluginCall call) {
        if (getPermissionState(CONNECT) == PermissionState.GRANTED) {
            switch(call.getMethodName()) {
                case "enable": enableBluetooth(call); break;
                case "list": listPairedDevices(call); break;
                case "connect": connect(call); break;
                case "connectInsecure": connectInsecure(call); break;
            }
        } else {
            call.reject("Connect permission denied");
        }
    }

    @PermissionCallback
    private void scanPermissionCallback(PluginCall call) {
        if (getPermissionState(SCAN) == PermissionState.GRANTED) {
            cancelDiscovery(call);
        } else {
            call.reject("Scan permission denied");
        }
    }

    @PluginMethod
    @Override
    public void checkPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            super.checkPermissions(call);
        } else {
            checkCompatPermissions(call);
        }
    }

    private void checkCompatPermissions(PluginCall call) {
        // scan and connect don't exist on older versions of Android so we only check location
        JSObject permissions = new JSObject()
                .put(COARSE_LOCATION, getPermissionState(COARSE_LOCATION))
                .put(FINE_LOCATION, getPermissionState(FINE_LOCATION))
                .put(SCAN, PermissionState.GRANTED)
                .put(CONNECT, PermissionState.GRANTED);
        call.resolve(permissions);
    }

    @PluginMethod
    @Override
    public void requestPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            super.requestPermissions(call);
        } else {
            requestCompatPermissions(call);
        }
    }

    private void requestCompatPermissions(PluginCall call) {
        try {
            List<String> requestedPermissions = call.getArray("permissions").toList();
            List<String> locationPermissions = new ArrayList<>();
            JSObject permissions = new JSObject();
            for (String permission: requestedPermissions) {
                if (permission.contains("Location")) {
                    locationPermissions.add(permission);
                } else {
                    permissions.put(permission, PermissionState.GRANTED);
                }
            }
            if (locationPermissions.isEmpty()) {
                call.resolve(permissions);
            } else {
                String[] aliases = locationPermissions.toArray(new String[0]);
                requestPermissionForAliases(aliases, call, "requestCompatPermissionCallback");
            }
        } catch(JSONException exception) {
            call.reject(exception.getMessage());
        }
    }

    @PermissionCallback
    private void requestCompatPermissionCallback(PluginCall call) {
        try {
            List<String> requestedPermissions = call.getArray("permissions").toList();
            JSObject permissions = new JSObject();
            for (String alias: requestedPermissions) {
                permissions.put(alias, alias.contains("Location") ? getPermissionState(alias) : PermissionState.GRANTED);
            }
            call.resolve(permissions);
        } catch(JSONException exception) {
            call.reject(exception.getMessage());
        }
    }

    // This is called only for permissions that may not exist on older Android versions,
    // otherwise getPermissionState(alias) is used
    private boolean hasCompatPermission(String alias) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return getPermissionState(alias) == PermissionState.GRANTED;
        } else {
            return true;
        }
    }

    private boolean rejectIfBluetoothDisabled(PluginCall call) {
        boolean disabled = !implementation.isEnabled();
        if (disabled) {
            call.reject("Bluetooth is not enabled");
        }
        return disabled;
    }

    private void requestConnectPermission(PluginCall call) {
        requestPermissionForAlias(CONNECT, call, "connectPermissionCallback");
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (implementation != null) {
            implementation.resetService();
        }
    }

    private interface Connector {
        void connect(BluetoothDevice device);
    }

}
