package com.glavotaner.bluetoothserial;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    /**
     * Constructor. Prepares a new BluetoothSerial session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothSerial(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = ConnectionState.NONE;
        mHandler = handler;
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
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothSerialPlugin.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        tryCancelThread(mConnectThread);

        // Cancel any thread currently running a connection
        tryCancelThread(mConnectedThread);

        setState(ConnectionState.NONE);

    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        tryCancelAllThreads();
        setState(ConnectionState.NONE);
    }

    private void startService() {
        BluetoothSerial.this.start();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == ConnectionState.CONNECTING) {
            tryCancelThread(mConnectThread);
        }

        // Cancel any thread currently running a connection
        tryCancelThread(mConnectedThread);

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(ConnectionState.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @SuppressLint("MissingPermission")
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);
        tryCancelAllThreads();
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BluetoothSerialPlugin.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSerialPlugin.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        setState(ConnectionState.CONNECTED);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothSerialPlugin.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSerialPlugin.TOAST, "Unable to connect to device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Start the service over to restart listening mode
        startService();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothSerialPlugin.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothSerialPlugin.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        startService();
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

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread implements CancellableThread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                if (secure) {
                    // tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                    tmp = device.createRfcommSocketToServiceRecord(UUID_SPP);
                } else {
                    //tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                    tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                Log.i(TAG, "Connecting to socket...");
                mmSocket.connect();
                Log.i(TAG, "Connected");
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                connectionFailed();
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSerial.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
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
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String data = new String(buffer, 0, bytes);

                    // Send the new data String to the UI Activity
                    mHandler.obtainMessage(BluetoothSerialPlugin.MESSAGE_READ, data).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    startService();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluetoothSerialPlugin.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private void tryCancelThread(CancellableThread thread) {
        if (thread != null) {
            thread.cancel();
            thread = null;
        }
    }

    private void tryCancelAllThreads() {
        tryCancelThread(mConnectThread);
        tryCancelThread(mConnectedThread);
    }

}
