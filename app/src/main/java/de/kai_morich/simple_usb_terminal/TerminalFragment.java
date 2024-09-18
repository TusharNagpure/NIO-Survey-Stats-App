package de.kai_morich.simple_usb_terminal;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class TerminalFragment extends Fragment {

    private static final String TAG = "TerminalFragment";
    private static final String ACTION_USB_PERMISSION = "com.example.terminal.USB_PERMISSION";
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private TextView incomingDataText;
    private ScrollView scrollView;
    private Button pauseResumeButton;
    private Button clearButton;
    private boolean isStreaming = false; // Initially streaming is paused

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "USB permission granted for device: " + device.getDeviceName());
                        if (device != null) {
                            openSerialPort(device);
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device: " + device.getDeviceName());
                        Toast.makeText(context, "Permission denied for device", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "USB device attached");
                checkConnectedDevices();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "USB device detached");
                if (serialPort != null) {
                    try {
                        serialPort.close();
                        Log.d(TAG, "Serial port closed");
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing serial port", e);
                    }
                }
                serialPort = null;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);

        // Correctly reference the TextView inside the ScrollView
        incomingDataText = view.findViewById(R.id.incomingDataText);
        scrollView = view.findViewById(R.id.scrollView);
        pauseResumeButton = view.findViewById(R.id.pauseResumeButton);
        clearButton = view.findViewById(R.id.ClearButton);

        pauseResumeButton.setOnClickListener(v -> {
            isStreaming = !isStreaming; // Toggle streaming state
            if (isStreaming) {
                pauseResumeButton.setText("Pause");
                startDataStreaming();
            } else {
                pauseResumeButton.setText("Resume");
                stopDataStreaming();
            }
        });

        clearButton.setOnClickListener(v -> incomingDataText.setText(""));

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getActivity().registerReceiver(usbReceiver, filter);

        checkPermissions();
        checkConnectedDevices();

        return view;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 112);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 112) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "The app needs storage permission to save data.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkConnectedDevices() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "No USB devices found");
            return;
        }

        Log.d(TAG, "USB device(s) found: " + availableDrivers.size());
        UsbSerialDriver driver = availableDrivers.get(0); // Select the first available device
        UsbDevice device = driver.getDevice();
        Log.d(TAG, "Connected device: " + device.getDeviceName());

        PendingIntent permissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
        usbManager.requestPermission(device, permissionIntent);
    }

    private void openSerialPort(UsbDevice device) {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        for (UsbSerialDriver driver : availableDrivers) {
            if (driver.getDevice().equals(device)) {
                serialPort = driver.getPorts().get(0);
                try {
                    serialPort.open(usbManager.openDevice(driver.getDevice()));
                    serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "Serial port opened successfully with parameters: 9600, 8, 1, none");
                    serialPort.setDTR(true); // Data Terminal Ready
                    serialPort.setRTS(true); // Request to Send
                    startDataStreaming(); // Start data streaming after opening the port
                } catch (IOException e) {
                    Log.e(TAG, "Error opening serial port: " + e.getMessage(), e);
                    try {
                        serialPort.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "Error closing serial port", e2);
                    }
                    serialPort = null;
                }
                break;
            }
        }
    }

    private void startDataStreaming() {
        if (serialPort != null) {
            new Thread(new SerialReader()).start();
            Log.d(TAG, "Started data streaming");
        } else {
            Log.d(TAG, "Serial port is not open");
        }
    }

    private void stopDataStreaming() {
        isStreaming = false;
        Log.d(TAG, "Stopped data streaming");
    }

    private class SerialReader implements Runnable {
        private final StringBuilder buffer = new StringBuilder(); // Buffer to accumulate incoming characters

        @Override
        public void run() {
            byte[] buf = new byte[1024]; // Use a buffer of reasonable size
            int len;
            while (isStreaming && serialPort != null) {
                try {
                    len = serialPort.read(buf, 1000); // Read from the serial port
                    if (len > 0) {
                        String data = new String(buf, 0, len); // Convert bytes to string
                        Log.d(TAG, "Data received: " + data); // Log the received data
                        processIncomingData(data); // Process incoming data
                    } else {
                        Log.d(TAG, "No data received");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from serial port", e);
                    break;
                }
            }
        }

        private void processIncomingData(String data) {
            // Accumulate incoming data into the buffer
            buffer.append(data);

            // Check for new lines and process them one by one
            int newlineIndex;
            while ((newlineIndex = buffer.indexOf("\n")) != -1) {  // Look for newline characters
                String line = buffer.substring(0, newlineIndex).trim();  // Extract the line
                buffer.delete(0, newlineIndex + 1);  // Remove the processed line from the buffer
                displayIncomingData(line);  // Display the full line
            }
        }
    }

    private void displayIncomingData(String data) {
        Log.d(TAG, "Displaying line: " + data);
        getActivity().runOnUiThread(() -> {
            incomingDataText.append(data + "\n");  // Add the full line
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));  // Scroll to bottom
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serialPort != null) {
            try {
                serialPort.close();
                Log.d(TAG, "Serial port closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing serial port", e);
            }
        }
        if (getActivity() != null) {
            getActivity().unregisterReceiver(usbReceiver);
        }
    }
}
