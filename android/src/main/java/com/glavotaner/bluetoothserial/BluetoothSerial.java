package com.glavotaner.bluetoothserial;

import static com.glavotaner.bluetoothserial.Message.ERROR;
import static com.glavotaner.bluetoothserial.Message.SUCCESS;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.glavotaner.bluetoothserial.threads.CancellableThread;

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
    private ConnectedThread mConnectedThread;
    private final Handler connectionHandler;
    private final Handler writeHandler;
    private final Handler readHandler;
    private ConnectionState mState;

    public BluetoothSerial(Handler connectionHandler, Handler writeHandler, Handler readHandler) {
        this.connectionHandler = connectionHandler;
        this.writeHandler = writeHandler;
        this.readHandler = readHandler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
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

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices() {
        return mAdapter.getBondedDevices();
    }

    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        mAdapter.startDiscovery();
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(ConnectionState state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        sendStateToPlugin(state);
    }

    /**
     * Return the current connection state.
     */
    public synchronized ConnectionState getState() {
        return mState;
    }

    public synchronized void resetService() {
        if (D) Log.d(TAG, "start");
        tryCancelThread(mConnectThread);
        tryCancelThread(mConnectedThread);
        setState(ConnectionState.NONE);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        tryCancelThread(mConnectThread);
        tryCancelThread(mConnectedThread);
        setState(ConnectionState.NONE);
    }

    private void startService() {
        BluetoothSerial.this.resetService();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        // Cancel any thread attempting to make a connection
        if (mState == ConnectionState.CONNECTING) {
            tryCancelThread(mConnectThread);
        }
        // Cancel any thread currently running a connection
        tryCancelThread(mConnectedThread);
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(ConnectionState.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     */
    @SuppressLint("MissingPermission")
    public synchronized void startConnectedThread(BluetoothSocket socket, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);
        tryCancelThread(mConnectThread);
        tryCancelThread(mConnectedThread);
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
    }

    private void sendConnectionErrorToPlugin(String error) {
        Message message = connectionHandler.obtainMessage(ERROR, error);
        message.arg1 = ConnectionState.NONE.value();
        message.sendToTarget();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != ConnectionState.CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private void sendStateToPlugin(ConnectionState state) {
        Message message = connectionHandler.obtainMessage(SUCCESS);
        Bundle bundle = new Bundle();
        bundle.putInt("state", state.value());
        message.setData(bundle);
        message.sendToTarget();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread implements CancellableThread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String mSocketType = "insecure";

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            mmSocket = getSocket(device);
        }

        @SuppressLint("MissingPermission")
        private BluetoothSocket getSocket(BluetoothDevice device) {
            BluetoothSocket socket = null;
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            return socket;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            connectToSocket();
            // Reset the ConnectThread because we're done
            resetConnectThread();
            startConnectedThread(mmSocket, mSocketType);
            sendConnectedDeviceToPlugin();
        }

        @SuppressLint("MissingPermission")
        private void sendConnectedDeviceToPlugin() {
            Message message = connectionHandler.obtainMessage(SUCCESS);
            BluetoothClass btClass = mmDevice.getBluetoothClass();
            BTDevice device = new BTDevice(
                    mmDevice.getAddress(),
                    mmDevice.getName(),
                    // TODO class
                    btClass != null ? btClass.getDeviceClass() : 0
                    );
            Bundle bundle = new Bundle();
            bundle.putInt("state", ConnectionState.CONNECTED.value());
            bundle.putParcelable("device", device);
            message.setData(bundle);
            message.sendToTarget();
        }

        @SuppressLint("MissingPermission")
        private void connectToSocket() {
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                Log.i(TAG, "Connecting to socket...");
                mmSocket.connect();
                Log.i(TAG, "Connected");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                sendConnectionErrorToPlugin("Unable to connect to device");
                startService();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private void resetConnectThread() {
        synchronized (BluetoothSerial.this) {
            mConnectThread = null;
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread implements CancellableThread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    String data = getBufferData(buffer);
                    // Send the new data String to the UI Activity
                    sendReadData(data);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    sendConnectionErrorToPlugin("Device connection was lost");
                    startService();
                    break;
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
            Message message = writeHandler.obtainMessage(ERROR);
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                message.what = SUCCESS;
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                Bundle bundle = new Bundle();
                bundle.putString("error", e.getMessage());
                message.setData(bundle);
            }
            message.sendToTarget();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private void sendReadData(String data) {
        Message message = readHandler.obtainMessage(SUCCESS);
        Bundle bundle = new Bundle();
        bundle.putString("data", data);
        message.setData(bundle);
        message.sendToTarget();
    }

    private void tryCancelThread(CancellableThread thread) {
        if (thread != null) {
            thread.cancel();
            thread = null;
        }
    }


}
