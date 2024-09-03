package de.kai_morich.simple_usb_terminal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.SphericalUtil;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.google.android.gms.maps.model.Marker;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.view.LayoutInflater;
import android.widget.TextView;
public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "HomeFragment";
    private static final String ACTION_USB_PERMISSION = "com.example.nmeadisplay.USB_PERMISSION";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_LOCATION_PERMISSION = 113;
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView altitudeTextView;
    private TextView dateTimeTextView;
    private TextView fixStatusTextView;
    private TextView areaTextView;
    private ImageView saveIcon;
    private ImageView Icon1;
    private ImageView Icon2;
    private ImageView Icon3;
    private ImageView Icon4;
    private GoogleMap googleMap;
    private List<NMEAData> gpggaDataList = new ArrayList<>();
    private List<NMEAData> gprmcDataList = new ArrayList<>();

    private String latestLatitude = "";
    private String latestLongitude = "";
    private String latestAltitude = "";
    private String latestDateTime = "";
    private String latestFixStatus = "";
    private boolean isStreaming = true; // Initially streaming is paused
    private ToggleButton connectButton;
    public String LatDirec = "";
    public String LongDirec = "";
    private boolean isMapReady = false;
    private List<NMEAData> bufferedNMEAData = new ArrayList<>();
    private List<LatLng> polygonPoints = new ArrayList<>();
    private List<Marker> circleMarkers = new ArrayList<>();
    private boolean isAreaCalculationMode = false;
    private Polygon polygon;  // For storing the polygon drawn on the map
    private List<Marker> circles = new ArrayList<>();
    private FloatingActionButton areaFab;// For storing the markers used as circles
    private List<LatLng> polylinePoints = new ArrayList<>();
    private com.google.android.gms.maps.model.Polyline polyline;
    private boolean isDistanceCalculationMode = false;
    private FloatingActionButton distanceFab;



    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            openSerialPort(device);
                        } else {
                            Log.d(TAG, "Permission granted but device is null");
                        }
                    } else {
                        Toast.makeText(context, "Permission denied for device " + device, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Permission denied for device " + device);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                checkConnectedDevices();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                checkConnectedDevices();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        areaTextView = view.findViewById(R.id.areaTextView);
        latitudeTextView = view.findViewById(R.id.latitudeTextView);
        longitudeTextView = view.findViewById(R.id.longitudeTextView);
        altitudeTextView = view.findViewById(R.id.altitudeTextView);
        dateTimeTextView = view.findViewById(R.id.dateTimeTextView);
        fixStatusTextView = view.findViewById(R.id.fixStatusTextView);
        saveIcon = view.findViewById(R.id.saveIcon);
        saveIcon.setOnClickListener(v -> showSaveOptionsDialog());
        Icon1 = view.findViewById(R.id.tap1);
        Icon1.setOnClickListener(v -> DataTap(1));
        Icon2 = view.findViewById(R.id.tap2);
        Icon2.setOnClickListener(v -> DataTap(2));
        Icon3 = view.findViewById(R.id.tap3);
        Icon3.setOnClickListener(v -> DataTap(3));
        Icon4 = view.findViewById(R.id.tap4);
        Icon4.setOnClickListener(v -> DataTap(4));
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getActivity().registerReceiver(usbReceiver, filter);

        checkPermissions();
        checkConnectedDevices();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment is null");
        }
        connectButton = view.findViewById(R.id.connectButton);
        connectButton.setChecked(false); // Ensure the button is off by default

        connectButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAreaCalculationMode = false;
            isDistanceCalculationMode = false;
            areaTextView.setVisibility(View.GONE);
            isStreaming = !isChecked; // Start streaming when checked, stop when unchecked
            if (isStreaming) {
                startDataStreaming();
                showCustomToast("Device Connected", 500);
            } else {
                stopDataStreaming();
                showCustomToast("Device Disconnected", 500);
                areaFab.setEnabled(true);// Enable area calculation button when disconnected
                distanceFab.setEnabled(true);
            }

            if (googleMap != null) {
                googleMap.clear();
                clearMarkersAndLines();
            }
        });


        areaFab = view.findViewById(R.id.areaFab);
        areaFab.setOnClickListener(v -> {
            if (connectButton.isChecked()) {
                if (isDistanceCalculationMode) {
                    deactivateDistCal();
                }
                clearMarkersAndLines();
                    isAreaCalculationMode = !isAreaCalculationMode;  // Toggle the area calculation mode}
                    if (isAreaCalculationMode) {
                        areaTextView.setVisibility(View.VISIBLE);  // Show the area TextView
                        showCustomToast("Area calculation mode on", 500);
                    } else {
                        // Clear the points and remove the polygon
                        polygonPoints.clear();
                        if (polygon != null) {
                            polygon.remove();
                            polygon = null;
                        }

                        // Remove all markers representing circles
                        for (Marker marker : circles) {
                            marker.remove();  // Remove each marker from the map
                        }
                        circles.clear();  // Clear the list of markers

                        areaTextView.setVisibility(View.GONE);
                        areaTextView.setText("Area: 0 sq km"); // Reset the area TextView
                        showCustomToast("Area calculation mode off", 500);
                    }
                } else {
                    showCustomToast("Please disconnect the device", 500);
                }
        });


        distanceFab = view.findViewById(R.id.distanceFab);
        distanceFab.setOnClickListener(v -> {
            if (connectButton.isChecked()) {
                if(isAreaCalculationMode){
                    deactivateAreaCal();
                }
                    clearMarkersAndLines(); // Clear previous lines and markers
                    isDistanceCalculationMode = !isDistanceCalculationMode;  // Toggle the distance calculation mode
                    if (isDistanceCalculationMode) {
                        areaTextView.setVisibility(View.VISIBLE);  // Show the distance TextView
                        showCustomToast("Distance calculation mode on", 500);
                    } else {
                        areaTextView.setVisibility(View.GONE);
                        areaTextView.setText("Distance: 0 km"); // Reset the distance TextView
                        showCustomToast("Distance calculation mode off", 500);
                    }
                } else {
                    showCustomToast("Please disconnect the device", 500);
                }
        });

        return view;
    }

    private void showSaveOptionsDialog() {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.dialog_save_options, null);

        // Initialize the dialog elements
        TextView saveTxtLabel = dialogView.findViewById(R.id.saveTxtLabel);
        TextView saveKmlLabel = dialogView.findViewById(R.id.saveKmlLabel);
        TextView saveBothLabel = dialogView.findViewById(R.id.saveBothLabel);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Set click listeners
        View.OnClickListener saveClickListener = v -> {
            if (v == saveTxtLabel) {
                saveDataToTextFile();
                dialog.dismiss();
            } else if (v == saveKmlLabel) {
                showCustomToast("Save as KML not yet implemented", 1000);
                dialog.dismiss();
            } else if (v == saveBothLabel) {
                saveDataToTextFile(); // Call save as TXT method
                showCustomToast("Save as both TXT and KML not yet implemented", 1000);
                dialog.dismiss();
            }
        };

        saveTxtLabel.setOnClickListener(saveClickListener);
        saveKmlLabel.setOnClickListener(saveClickListener);
        saveBothLabel.setOnClickListener(saveClickListener);

        // Show the dialog
        dialog.show();
    }

    private void deactivateDistCal(){
        isDistanceCalculationMode = false;
    }

    private void deactivateAreaCal(){
        isAreaCalculationMode = false;
    }

    private void clearMarkersAndLines() {
        if (polygon != null) {
            polygon.remove();
            polygon = null;
        }

        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }

        for (Marker marker : circles) {
            marker.remove();
        }
        circles.clear();

        polygonPoints.clear();
        polylinePoints.clear();
    }


    private void addPointToPolygon(LatLng point) {
        polygonPoints.add(point);  // Add the point to the list of polygon points

        // Remove the old polygon if it exists
        if (polygon != null) {
            polygon.remove();
        }

        // Draw the polygon if there are at least 3 points
        if (polygonPoints.size() >= 3) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(polygonPoints)
                    .strokeColor(0xFF00FF00)  // Green stroke color
                    .fillColor(0x5500FF00)    // Transparent green fill color
                    .strokeWidth(5);          // Stroke width
            polygon = googleMap.addPolygon(polygonOptions);  // Draw the polygon on the map

            // Calculate the area of the polygon
            double area = SphericalUtil.computeArea(polygonPoints);

            // Update the area in the TextView
            areaTextView.setText(String.format(Locale.getDefault(), "Area: %.2f sq km", area / 1000000));
        }

        // Add a marker (representing a circle) for each point added to the map
        Bitmap circleBitmap = createCircleBitmap(20, Color.BLUE, Color.WHITE);  // Adjust the size to 20 pixels
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.fromBitmap(circleBitmap))
                .anchor(0.5f, 0.5f));  // Center the marker icon
        circles.add(marker);  // Add the marker to the circles list
    }

    private void addPointToPolyline(LatLng point) {
        polylinePoints.add(point);  // Add the point to the list of polyline points

        // Remove the old polyline if it exists
        if (polyline != null) {
            polyline.remove();
        }

        // Draw the polyline if there are at least 2 points
        if (polylinePoints.size() >= 2) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(polylinePoints)
                    .color(Color.BLUE)   // Polyline color
                    .width(5);           // Polyline width
            polyline = googleMap.addPolyline(polylineOptions);  // Draw the polyline on the map

            // Calculate the distance of the polyline
            double distance = SphericalUtil.computeLength(polylinePoints);

            // Update the distance in the TextView
            areaTextView.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distance / 1000));
        }

        // Add a marker (representing a circle) for each point added to the map
        Bitmap circleBitmap = createCircleBitmap(20, Color.BLUE, Color.WHITE);  // Adjust the size to 20 pixels
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.fromBitmap(circleBitmap))
                .anchor(0.5f, 0.5f));  // Center the marker icon
        circles.add(marker);  // Add the marker to the circles list
    }


    private Bitmap createCircleBitmap(int radius, int strokeColor, int fillColor) {
        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Fill color
        paint.setColor(fillColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(radius, radius, radius, paint);

        // Stroke color
        paint.setColor(strokeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawCircle(radius, radius, radius, paint);

        return bitmap;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "The app needs storage permission to save data.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Location permission granted!", Toast.LENGTH_SHORT).show();
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException: " + e.getMessage());
                    }
                }
            } else {
                Toast.makeText(getActivity(), "The app needs location permission to display the map.", Toast.LENGTH_LONG).show();
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
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();
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
                    Log.d(TAG, "Serial port opened with parameters: 9600, 8, 1, none");
                    new Thread(new SerialReader()).start();
                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Error opening device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error opening device: " + e.getMessage(), e);
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
        // Code to start data streaming
        if (serialPort != null) {
            new Thread(new SerialReader()).start();
        }
        Log.d(TAG, "Data streaming started");
    }

    private void stopDataStreaming() {
        // Code to stop data streaming
        isStreaming = false;
        Log.d(TAG, "Data streaming stopped");
    }

    private class SerialReader implements Runnable {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void run() {
            byte[] buf = new byte[1];
            int len;
            while (isStreaming) { // Check streaming state
                try {
                    len = serialPort.read(buf, 1000);
                    if (len > 0) {
                        char incomingChar = (char) buf[0];
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> processIncomingChar(incomingChar));
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from serial port", e);
                    break;
                }
            }
        }

        private void processIncomingChar(char incomingChar) {
            if (incomingChar == '\n' || incomingChar == '\r') {
                if (buffer.length() > 0) {
                    String data = buffer.toString();
                    buffer.setLength(0);
                    processIncomingData(data);
                }
            } else {
                buffer.append(incomingChar);
            }
        }
    }

    private void processIncomingData(String data) {
        Log.d(TAG, "Processed data: " + data);
        if (data.startsWith("$GPGGA")) {
            if (isValidChecksum(data)) {
                NMEAData nmeaData = parseGPGGAData(data);
                if (nmeaData != null) {
                    gpggaDataList.add(nmeaData);
                    updateTextViews(nmeaData);
                    Log.d(TAG, "GPGGA Data: " + nmeaData.getLatitude() + ", " + nmeaData.getLongitude());
                    updateMapMarker(nmeaData);
                }
            } else {
                Log.d(TAG, "Invalid checksum: " + data);
            }
        } else if (data.startsWith("$GPRMC")) {
            if (isValidChecksum(data)) {
                NMEAData nmeaData = parseGPRMCData(data);
                if (nmeaData != null) {
                    gprmcDataList.add(nmeaData);
                    updateTextViews(nmeaData);
                    Log.d(TAG, "GPRMC Data: " + nmeaData.getLatitude() + ", " + nmeaData.getLongitude());
                    updateMapMarker(nmeaData);
                }
            } else {
                Log.d(TAG, "Invalid checksum: " + data);
            }
        } else {
            Log.d(TAG, "Non-relevant NMEA sentence: " + data);
        }
    }

    private boolean isValidChecksum(String nmeaSentence) {
        int asteriskIndex = nmeaSentence.indexOf('*');
        if (asteriskIndex == -1) {
            return false;
        }

        String checksumString = nmeaSentence.substring(asteriskIndex + 1);
        int checksum;
        try {
            checksum = Integer.parseInt(checksumString, 16);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid checksum format: " + checksumString, e);
            return false;
        }

        int calculatedChecksum = 0;
        for (int i = 1; i < asteriskIndex; i++) {
            calculatedChecksum ^= nmeaSentence.charAt(i);
        }

        return calculatedChecksum == checksum;
    }

    private void updateTextViews(NMEAData nmeaData) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (nmeaData.getLatitude() != null && !nmeaData.getLatitude().isEmpty()) {
                    latestLatitude = nmeaData.getLatitude();
                    latitudeTextView.setText(String.format(Locale.getDefault(), "%.6f", nmeaData.getLatitudeDecimalDegrees()) + " " + LatDirec);
                }
                if (nmeaData.getLongitude() != null && !nmeaData.getLongitude().isEmpty()) {
                    latestLongitude = nmeaData.getLongitude();
                    longitudeTextView.setText(String.format(Locale.getDefault(), "%.6f", nmeaData.getLongitudeDecimalDegrees()) + " " + LongDirec);
                }
                if (nmeaData.getAltitude() != null && !nmeaData.getAltitude().isEmpty()) {
                    latestAltitude = nmeaData.getAltitude();
                    altitudeTextView.setText(latestAltitude);
                }
                if (nmeaData.getDateTime() != null && !nmeaData.getDateTime().isEmpty()) {
                    latestDateTime = nmeaData.getDateTime();
                    dateTimeTextView.setText(latestDateTime);
                }
                latestFixStatus = nmeaData.getFixStatus();
                fixStatusTextView.setText(latestFixStatus);
            });
        }
    }

    public static String addSymbolAtInterval(String str, int interval, char symbol) {
        StringBuilder sb = new StringBuilder(str);

        for (int i = interval; i < sb.length(); i += interval + 1) {
            sb.insert(i, symbol);
        }

        return sb.toString();
    }

    private void updateMapMarker(NMEAData nmeaData) {
        if (isMapReady && googleMap != null && !latestLatitude.isEmpty() && !latestLongitude.isEmpty()) {
            double latitude = nmeaData.getLatitudeDecimalDegrees();
            double longitude = nmeaData.getLongitudeDecimalDegrees();
            Log.d(TAG, "Updating map with coordinates: " + latitude + ", " + longitude);
            LatLng latLng = new LatLng(latitude, longitude);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            addPointToPolygon(latLng);
        } else {
            Log.d(TAG, "GoogleMap is null or coordinates are empty, buffering data");
            bufferedNMEAData.add(nmeaData);
        }
    }

    private void saveDataToTextFile() {
        if (getActivity() == null) {
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "nmea_data_" + timeStamp + ".txt";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri fileUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values);

        if (fileUri == null) {
            Toast.makeText(getActivity(), "Error creating file", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream outputStream = contentResolver.openOutputStream(fileUri)) {
            if (outputStream != null) {
                outputStream.write("Date,Time,Latitude,Longitude,Altitude,Check,Fix Status\n".getBytes());

                int maxSize = Math.max(gpggaDataList.size(), gprmcDataList.size());

                for (int i = 0; i < maxSize; i++) {
                    String time = "";
                    String date = "";
                    String latitude = "";
                    String longitude = "";
                    String altitude = "";
                    String check = "";
                    String fixStatus = "";

                    if (i < gprmcDataList.size()) {
                        NMEAData dateTimeData = gprmcDataList.get(i);
                        time = dateTimeData.getDateTime().substring(0, 10);
                        date = dateTimeData.getDateTime().substring(10);
                        check = String.valueOf(dateTimeData.getCounter());
                        fixStatus = dateTimeData.getFixStatus();
                    }

                    if (i < gpggaDataList.size()) {
                        NMEAData gpggaData = gpggaDataList.get(i);
                        latitude = String.format(Locale.getDefault(), "%.6f", gpggaData.getLatitudeDecimalDegrees()) + LatDirec;
                        longitude = String.format(Locale.getDefault(), "%.6f", gpggaData.getLongitudeDecimalDegrees()) + LongDirec;
                        altitude = gpggaData.getAltitude();
                        check = String.valueOf(gpggaData.getCounter());
                    }

                    String dataLine = date + "," + time + "," + latitude + "," + longitude + "," + altitude + "," + check + "," + fixStatus + "\n";
                    outputStream.write(dataLine.getBytes());
                }

                showCustomToast("Data saved to " + fileName, 1000);
            } else {
                Toast.makeText(getActivity(), "Error opening file output stream", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving data to text file", e);
            Toast.makeText(getActivity(), "Error saving data", Toast.LENGTH_SHORT).show();
        }
    }

    private void DataTap(Integer Counter) {
        Integer counter = Counter;
        // Update the counter for the latest NMEA data in both lists
        if (!gpggaDataList.isEmpty()) {
            gpggaDataList.get(gpggaDataList.size() - 1).setCounter(counter);
        }
        if (!gprmcDataList.isEmpty()) {
            gprmcDataList.get(gprmcDataList.size() - 1).setCounter(counter);
        }
        showCustomToast("Data Tapped " + counter, 50);
    }

    private void showCustomToast(String message, int durationInMillis) {
        final Toast toast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        toast.show();

        // Use a Handler to cancel the Toast after the specified duration
        new Handler(Looper.getMainLooper()).postDelayed(toast::cancel, durationInMillis);
    }

    private NMEAData parseGPGGAData(String data) {
        String[] parts = data.split(",");
        if (parts.length > 9) {
            NMEAData nmeaData = new NMEAData();
            nmeaData.setLatitude(parts[2]);
            nmeaData.setLatitudeDirection(parts[3]);
            nmeaData.setLongitude(parts[4]);
            nmeaData.setLongitudeDirection(parts[5]);
            nmeaData.setAltitude(parts[9] + " " + parts[10]);
            Log.d(TAG, "Parsed GPGGA Data: " + nmeaData.getLatitude() + " " + nmeaData.getLatitudeDirection() + ", " + nmeaData.getLongitude() + " " + nmeaData.getLongitudeDirection());
            return nmeaData;
        }
        return null;
    }

    private NMEAData parseGPRMCData(String data) {
        String[] parts = data.split(",");
        if (parts.length > 9) {
            NMEAData nmeaData = new NMEAData();
            String datestring = parts[9];
            String timestring = parts[1].substring(0, 6);
            nmeaData.setDateTime(addSymbolAtInterval(datestring, 2, '-') + " " + addSymbolAtInterval(timestring, 2, ':'));
            nmeaData.setLatitude(parts[3]);
            nmeaData.setLatitudeDirection(parts[4]);
            LatDirec = parts[4];
            nmeaData.setLongitude(parts[5]);
            nmeaData.setLongitudeDirection(parts[6]);
            LongDirec = parts[6];
            nmeaData.setFixStatus(parts[2].equals("A") ? " Valid Fix" : " No valid Fix");
            Log.d(TAG, "Parsed GPRMC Data: " + nmeaData.getLatitude() + "  " + nmeaData.getLatitudeDirection() + ", " + nmeaData.getLongitude() + " " + nmeaData.getLongitudeDirection());
            return nmeaData;
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        isMapReady = true;
        Log.d(TAG, "Google Map is ready");

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        // Set up a click listener on the map
        googleMap.setOnMapClickListener(latLng -> {
            if (isAreaCalculationMode) {
                addPointToPolygon(latLng);  // Add the clicked point to the polygon
            } else if (isDistanceCalculationMode) {
                addPointToPolyline(latLng);  // Add the clicked point to the polyline
            }
        });



        // Process buffered data
        for (NMEAData nmeaData : bufferedNMEAData) {
            updateMapMarker(nmeaData);
        }
        bufferedNMEAData.clear();
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

    private class NMEAData {
        private String latitude;
        private String latitudeDirection;
        private String longitude;
        private String longitudeDirection;
        private String altitude;
        private String dateTime;
        private String fixStatus;
        private int counter; // Add counter field

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLatitudeDirection() {

            return latitudeDirection;
        }

        public void setLatitudeDirection(String latitudeDirection) {
            this.latitudeDirection = latitudeDirection;
        }

        public String getLongitude() {

            return longitude;
        }

        public void setLongitude(String longitude) {

            this.longitude = longitude;
        }

        public String getLongitudeDirection() {

            return longitudeDirection;
        }

        public void setLongitudeDirection(String longitudeDirection) {
            this.longitudeDirection = longitudeDirection;
        }

        public String getAltitude() {

            return altitude;
        }

        public void setAltitude(String altitude) {

            this.altitude = altitude;
        }

        public String getDateTime() {

            return dateTime;
        }

        public void setDateTime(String dateTime) {

            this.dateTime = dateTime;
        }

        public String getFixStatus() {
            return fixStatus;
        }

        public void setFixStatus(String fixStatus) {
            this.fixStatus = fixStatus;
        }

        public int getCounter() {
            return counter;
        }

        public void setCounter(int counter) {
            this.counter = counter;
        }

        public double getLatitudeDecimalDegrees() {
            return convertToDecimalDegrees(latitude, latitudeDirection);
        }

        public double getLongitudeDecimalDegrees() {
            return convertToDecimalDegrees(longitude, longitudeDirection);
        }

        private double convertToDecimalDegrees(String nmeaCoordinate, String direction) {
            if (nmeaCoordinate == null || nmeaCoordinate.isEmpty()) {
                return 0.0;
            }

            double coordinate = Double.parseDouble(nmeaCoordinate);
            int degrees = (int) (coordinate / 100);
            double minutes = coordinate - (degrees * 100);
            double decimalDegrees = degrees + (minutes / 60);

            // Apply direction
            if (direction.equals("S") || direction.equals("W")) {
                decimalDegrees = -decimalDegrees;
            }

            return decimalDegrees;
        }
    }
}
