package com.glavotaner.bluetoothserial;

import static com.glavotaner.bluetoothserial.Message.ERROR;
import static com.glavotaner.bluetoothserial.Message.SUCCESS;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothSerial {

    // Debugging
    private static final String TAG = "BluetoothSerialService";
    private static final boolean D = true;

    // Well known SPP UUID
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private IOThread mIOThread;
    private final Handler connectionHandler;
    private final Handler writeHandler;
    private final Handler readHandler;
    private ConnectionState mState;

    public BluetoothSerial(BluetoothAdapter bluetoothAdapter, Handler connectionHandler, Handler writeHandler, Handler readHandler) {
        this.mAdapter = bluetoothAdapter;
        this.connectionHandler = connectionHandler;
        this.writeHandler = writeHandler;
        this.readHandler = readHandler;
        mState = ConnectionState.NONE;
    }

    public String echo(String value) {
        return value;
    }

    public BluetoothDevice getRemoteDevice(String address) {
        return mAdapter.getRemoteDevice(address);
    }

    public boolean isEnabled() {
        return mAdapter.isEnabled();
    }

    // connect
    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices() {
        return mAdapter.getBondedDevices();
    }

    // scan
    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        mAdapter.startDiscovery();
    }

    // scan
    @SuppressLint("MissingPermission")
    public void cancelDiscovery() {
        mAdapter.cancelDiscovery();
    }

    public boolean isConnected() {
        return mState == ConnectionState.CONNECTED;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(ConnectionState state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        Bundle bundle = new Bundle();
        bundle.putInt("state", state.value());
        sendConnectionStateToPlugin(SUCCESS, bundle);
    }

    public synchronized void resetService() {
        if (D) Log.d(TAG, "start");
        closeRunningThreads();
        setState(ConnectionState.NONE);
    }

    // connect
    @SuppressLint("MissingPermission")
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        connectToSocketOfType("secure", () -> device.createRfcommSocketToServiceRecord(UUID_SPP));
    }

    // connect
    @SuppressLint("MissingPermission")
    public synchronized void connectInsecure(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        connectToSocketOfType("insecure", () -> device.createInsecureRfcommSocketToServiceRecord(UUID_SPP));
    }

    private void connectToSocketOfType(String socketType, SocketCreator socketCreator) {
        try {
            setState(ConnectionState.CONNECTING);
            BluetoothSocket socket = socketCreator.create();
            if (socket != null) connect(socket, socketType);
            else sendConnectionErrorToPlugin("Could not create socket");
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: "+ socketType +" create() failed", e);
            handleConnectionError(e.getMessage());
        }
    }

    private void sendConnectionErrorToPlugin(String error) {
        Bundle bundle = new Bundle();
        bundle.putString("error", error);
        sendConnectionStateToPlugin(ERROR, bundle);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     */
    private synchronized void connect(BluetoothSocket socket, String socketType) {
        // Cancel any thread attempting to make a connection
        closeRunningThreads();
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(socket, socketType);
        mConnectThread.start();
    }

    /**
     * Write to the IOThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see IOThread#write(byte[])
     */
    public void write(byte[] out) {
        if (isConnected()) {
            IOThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) { r = mIOThread; }
            r.write(out);
        } else {
            Message message = writeHandler.obtainMessage(ERROR);
            Bundle bundle = new Bundle();
            bundle.putString("error", "Not connected");
            message.setData(bundle);
            message.sendToTarget();
        }
    }

    private void sendConnectionStateToPlugin(int status, Bundle bundle) {
        Message message = connectionHandler.obtainMessage(status);
        message.setData(bundle);
        message.sendToTarget();
    }

    private void closeRunningThreads() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mIOThread != null) {
            mIOThread.cancel();
            mIOThread = null;
        }
    }

    private void handleConnectionError(String message) {
        sendConnectionErrorToPlugin(message);
        BluetoothSerial.this.resetService();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final String mSocketType;

        public ConnectThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            mSocketType = socketType;
        }

        // connect
        @SuppressLint("MissingPermission")
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);
            try {
                mmSocket.connect();
                Log.i(TAG, "Connected");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                String message = e.getMessage();
                if (message != null) {
                    handleConnectionError(message.equals("read failed, socket might closed or timeout, read ret: -1") ? "Unable to connect" : message);
                } else {
                    handleConnectionError("Unable to connect");
                }
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothSerial.this) {
                mConnectThread = null;
                startIOThread();
            }
        }

        /**
         * Start the IOThread to begin managing a Bluetooth connection
         */
        private void startIOThread() {
            if (D) Log.d(TAG, "connected, Socket Type:" + mSocketType);
            // Start the thread to manage the connection and perform transmissions
            mIOThread = new IOThread(mmSocket, mSocketType);
            mIOThread.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class IOThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public IOThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                setState(ConnectionState.CONNECTED);
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
                handleConnectionError("Could not create sockets");
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            while (mState == ConnectionState.CONNECTED) {
                try {
                    String data = getBufferData(buffer);
                    sendToPlugin(data);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    handleConnectionError("Device connection was lost");
                }
            }
        }

        @NonNull
        private String getBufferData(byte[] buffer) throws IOException {
            int bytes = mmInStream.read(buffer);
            return new String(buffer, 0, bytes);
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            Message message = writeHandler.obtainMessage(SUCCESS);
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                message.what = ERROR;
                Log.e(TAG, "Exception during write", e);
                Bundle bundle = new Bundle();
                bundle.putString("error", e.getMessage());
                message.setData(bundle);
            }
            // Share the sent message back to the UI Activity
            message.sendToTarget();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        private void sendToPlugin(String data) {
            Message message = readHandler.obtainMessage(SUCCESS);
            Bundle bundle = new Bundle();
            bundle.putString("data", data);
            message.setData(bundle);
            message.sendToTarget();
        }
    }

    private interface SocketCreator {
        BluetoothSocket create() throws IOException;
    }

}
