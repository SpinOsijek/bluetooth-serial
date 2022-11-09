package com.glavotaner.bluetoothserial;

import static com.glavotaner.bluetoothserial.Message.ERROR;
import static com.glavotaner.bluetoothserial.Message.SUCCESS;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
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
    private int mState;

    public BluetoothSerial(Handler connectionHandler, Handler writeHandler) {
        this.connectionHandler = connectionHandler;
        this.writeHandler = writeHandler;
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
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        sendStateToPlugin(state);
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
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
        tryCancelAllThreads();
        setState(ConnectionState.NONE);
    }

    private void startService() {
        BluetoothSerial.this.resetService();
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
     */
    @SuppressLint("MissingPermission")
    public synchronized void startConnectedThread(BluetoothSocket socket, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);
        tryCancelAllThreads();
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
    }

    private void sendConnectionErrorToPlugin(String error) {
        Message message = connectionHandler.obtainMessage(ERROR);
        message.obj = error;
        message.arg1 = ConnectionState.NONE;
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

    private void sendStateToPlugin(int state) {
        Message message = connectionHandler.obtainMessage(SUCCESS);
        message.arg1 = state;
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
        private final String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            mSocketType = secure ? "Secure" : "Insecure";
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            mmSocket = getSocket(device, secure);
        }

        @SuppressLint("MissingPermission")
        private BluetoothSocket getSocket(BluetoothDevice device, boolean secure) {
            BluetoothSocket socket = null;
            try {
                if (secure) {
                    socket = device.createRfcommSocketToServiceRecord(UUID_SPP);
                } else {
                    socket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                }
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

        private void sendConnectedDeviceToPlugin() {
            JSObject device = BluetoothSerialPlugin.deviceToJSON(mmDevice);
            Message message = connectionHandler.obtainMessage(SUCCESS, device);
            message.arg1 = ConnectionState.CONNECTED;
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
                    // TODO send read
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
            Message message = writeHandler.obtainMessage();
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                message.what = SUCCESS;
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                message.what = ERROR;
                message.obj = e.getMessage();
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
