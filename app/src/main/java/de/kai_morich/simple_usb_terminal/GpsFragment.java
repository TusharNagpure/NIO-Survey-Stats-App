package de.kai_morich.simple_usb_terminal;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.SphericalUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;


public class GpsFragment extends Fragment implements OnMapReadyCallback {
    private FusedLocationProviderClient fusedLocationClient;
    private TextView latitudeTextView, longitudeTextView, altitudeTextView, dateTimeTextView, gpsStatusTextView;
    private ToggleButton gpsToggleButton;
    private LocationCallback locationCallback;
    private Handler handler;
    private Runnable updateDateTimeRunnable;
    private boolean isReceivingGPS = true;
    private List<LatLng> polygonPoints = new ArrayList<>();
    private boolean isAreaCalculationMode = false;
    private Polygon polygon;  // For storing the polygon drawn on the map
    private List<Marker> circles = new ArrayList<>();
    private FloatingActionButton areaFab;// For storing the markers used as circles
    private List<LatLng> polylinePoints = new ArrayList<>();
    private com.google.android.gms.maps.model.Polyline polyline;
    private boolean isDistanceCalculationMode = false;
    private FloatingActionButton distanceFab;
    private FloatingActionButton optionsFab;
    private TextView areaTextView;
    private GoogleMap googleMap;
    private boolean isMapReady = false;
    private static final int REQUEST_LOCATION_PERMISSION = 113;
    private ImageView saveIcon;
    private ArrayList<String> gpsDataList;
    private static final int MAX_LIST_SIZE = 20000;
    private ImageView Icon1;
    private ImageView Icon2;
    private ImageView Icon3;
    private ImageView Icon4;
    private boolean addGpsDataList;
    private ImageView myImageView;
    private ListView myListView;
    private ImageView click;
    private boolean isPlacingMarker = false;
    private Marker lastplacedmarker; // Holds the last marker placed
    private ToggleButton pausePlayToggleButton;
    private boolean isReceivingData = false;
    private View recordingIndicator;
    private boolean isRecording = false;
    private FloatingActionButton terrainFab;
    private int currentMapTypeIndex = 0;
    private final int[] mapTypes = {
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_TERRAIN,
            GoogleMap.MAP_TYPE_HYBRID,
    };




    private BroadcastReceiver gpsDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GpsForegroundService.ACTION_GPS_DATA.equals(intent.getAction()) && isReceivingData) {
                ArrayList<String> backgroundGpsData = intent.getStringArrayListExtra(GpsForegroundService.EXTRA_GPS_DATA);
                if (backgroundGpsData != null && !backgroundGpsData.isEmpty()) {
                    if (isReceivingGPS) {
                        gpsDataList.addAll(backgroundGpsData);
                        showCustomToast("Data Receiving", 500);
                        updateRecordingIndicator(true);                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gps, container, false);
        areaTextView = view.findViewById(R.id.areaTextView);
        gpsToggleButton = view.findViewById(R.id.gpsToggleButton);
        saveIcon = view.findViewById(R.id.saveIcon);
        saveIcon.setOnClickListener(v -> showSaveOptionsDialog());
        areaFab = view.findViewById(R.id.areaFab);
        distanceFab = view.findViewById(R.id.distanceFab);
        optionsFab = view.findViewById(R.id.optionsFab);
        recordingIndicator = new View(getContext()); // Create the indicator view
        recordingIndicator.setLayoutParams(new ViewGroup.LayoutParams(40, 40));
        recordingIndicator.setBackgroundResource(R.drawable.circle_background_green);
        terrainFab = view.findViewById(R.id.terrainFab);
        terrainFab.setOnClickListener(v -> cycleMapType());

        ((ViewGroup) view).addView(recordingIndicator);

        view.post(() -> {
            recordingIndicator.setX(50);
            recordingIndicator.setY(20);
        });// Add the indicator to the layout
        // Initialize TextViews and ToggleButton
        initializeUI(view);
        click = view.findViewById(R.id.click);


        // Setup map fragment
        setupMapFragment();

        // Setup location callback
        setupLocationCallback();

        // Initialize handler and runnable for date-time updates
        initializeDateTimeHandler();
        gpsDataList = new ArrayList<>();
        Icon1 = view.findViewById(R.id.tap1);
        Icon1.setOnClickListener(v -> DataTap(1));
        Icon2 = view.findViewById(R.id.tap2);
        Icon2.setOnClickListener(v -> DataTap(2));
        Icon3 = view.findViewById(R.id.tap3);
        Icon3.setOnClickListener(v -> DataTap(3));
        Icon4 = view.findViewById(R.id.tap4);
        Icon4.setOnClickListener(v -> DataTap(4));
        // ToggleButton listener
        gpsToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAreaCalculationMode = false;
            isDistanceCalculationMode = false;
            areaTextView.setVisibility(View.GONE);
            isReceivingGPS = !isChecked;
            recordingIndicator.setBackgroundResource(R.drawable.circle_background_green);

            if (isChecked) {
                stopLocationUpdates();
                gpsStatusTextView.setText("GPS paused");
                showCustomToast("GPS paused", 500);
            } else {
                startLocationUpdates();
                gpsStatusTextView.setText("");
                showCustomToast("GPS Monitoring resumed", 500);
                if(isRecording){
                    recordingIndicator.setBackgroundResource(R.drawable.circle_background);
                }

            }

            if (googleMap != null) {
                clearMarkersAndLines();
            }
        });

        areaFab = view.findViewById(R.id.areaFab);
        areaFab.setOnClickListener(v -> {
            if (gpsToggleButton.isChecked()) {
                if (isDistanceCalculationMode) {
                    deactivateDistCal();

                }
                if (isPlacingMarker) {
                    deactivatePlacedmarker();

                }
                clearMarkersAndLines();
                clearplacedmarker();
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
            if (gpsToggleButton.isChecked()) {
                if (isAreaCalculationMode) {
                    deactivateAreaCal();

                }
                if (isPlacingMarker) {
                    deactivatePlacedmarker();

                }
                clearMarkersAndLines(); // Clear previous lines and markers
                clearplacedmarker();
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

        gpsDataList = new ArrayList<>();
        // Your existing code to initialize UI elements and other components

        // Register the receiver to listen for GPS data broadcasts
        IntentFilter filter = new IntentFilter(GpsForegroundService.ACTION_GPS_DATA);
        getActivity().registerReceiver(gpsDataReceiver, filter);

        // Start the GPS Service when the fragment is created
        Intent serviceIntent = new Intent(getActivity(), GpsForegroundService.class);
        ContextCompat.startForegroundService(getActivity(), serviceIntent);

        optionsFab.setOnClickListener(v -> {
            if (gpsToggleButton.isChecked()) {
                isPlacingMarker = !isPlacingMarker;
                deactivateDistCal();
                deactivateAreaCal();
                clearMarkersAndLines();

                if (isPlacingMarker) {
                    showCustomToast("Tap anywhere on the map to place a marker", 500);
                } else if (lastplacedmarker != null) {
                    lastplacedmarker.remove();

                    showCustomToast("Marker placement mode turned off", 500);
                }
            } else {
                showCustomToast("Please disconnect the device", 500);
            }
        });

        pausePlayToggleButton = view.findViewById(R.id.pausePlayToggleButton);
        pausePlayToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isReceivingData = isChecked;
            isRecording = isChecked;
            if(!gpsToggleButton.isChecked()){
            if (isChecked) {
                pausePlayToggleButton.setBackgroundResource(R.drawable.ic_pause_click_foreground);
                updateRecordingIndicator(true);
                showCustomToast("Data logging started", 500);

            } else {
                pausePlayToggleButton.setBackgroundResource(R.drawable.ic_play_click_foreground);
                updateRecordingIndicator(false);
                showCustomToast("Data logging paused", 500);

            }}
            else {
                showCustomToast("Please connect the device",500);
            }
        });
        // Replace this in your onCreateView or initialization code
        click.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Markers cleared", Toast.LENGTH_SHORT).show();
            googleMap.clear();
        });


        return view;
    }
    private void updateRecordingIndicator(boolean isRecording) {
        if (recordingIndicator != null) {
            if (isRecording) {
                recordingIndicator.setBackgroundResource(R.drawable.circle_background);// Red when recording
            } else {
                recordingIndicator.setBackgroundResource(R.drawable.circle_background_green); // Green when not recording
            }
        }
    }


    private void deactivateDistCal() {
        isDistanceCalculationMode = false;
    }

    private void deactivatePlacedmarker() {
        isPlacingMarker = false;
    }

    private void deactivateAreaCal() {
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

    private void clearplacedmarker(){
        if (lastplacedmarker != null) {
            lastplacedmarker.remove();
        }
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
            // Create a new dialog for entering file name
            AlertDialog.Builder nameDialogBuilder = new AlertDialog.Builder(getActivity());
            View nameDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_enter_filename, null);
            nameDialogBuilder.setView(nameDialogView);


            EditText fileNameInput = nameDialogView.findViewById(R.id.fileNameInput);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            fileNameInput.setText("gps_data_" + timeStamp); // Set default file name
            fileNameInput.setSelection(0, fileNameInput.getText().length()); // Highlight the text
            // Automatically show the keyboard when the dialog is displayed
            fileNameInput.requestFocus();
            nameDialogView.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0); // Forcefully show the keyboard
                }
            }, 200);



            nameDialogBuilder.setPositiveButton("Save", (dialogInterface, which) -> {
                String fileName = fileNameInput.getText().toString();

                if (v == saveTxtLabel) {
                    saveDataToTxtFile(fileName);
                } else if (v == saveKmlLabel) {
                    saveDataToKmlFile(fileName);
                } else if (v == saveBothLabel) {
                    saveBothFiles(fileName);// Save as KML
                }
                dialog.dismiss();
            });
            nameDialogBuilder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());
            nameDialogBuilder.create().show();
        };

        saveTxtLabel.setOnClickListener(saveClickListener);
        saveKmlLabel.setOnClickListener(saveClickListener);
        saveBothLabel.setOnClickListener(saveClickListener);

        // Show the dialog
        dialog.show();
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


    private void initializeUI(View view) {
        latitudeTextView = view.findViewById(R.id.latitudeTextView);
        longitudeTextView = view.findViewById(R.id.longitudeTextView);
        altitudeTextView = view.findViewById(R.id.altitudeTextView);
        dateTimeTextView = view.findViewById(R.id.dateTimeTextView);
        gpsStatusTextView = view.findViewById(R.id.fixStatusTextView);
        gpsToggleButton = view.findViewById(R.id.gpsToggleButton);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !isReceivingGPS) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Check if the location is from the GPS provider and has valid accuracy
                    if (location.hasAccuracy() && location.getAccuracy() < 15) {
                        gpsStatusTextView.setText("Valid fix");
                    } else {
                        gpsStatusTextView.setText("No valid fix");
                    }
                    updateLocationDetails(location);
                }
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        isMapReady = true;
        googleMap.setMapType(mapTypes[currentMapTypeIndex]);

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
            if (isPlacingMarker && !isAreaCalculationMode && !isDistanceCalculationMode) { // Check both modes
                if (lastplacedmarker != null) {
                    lastplacedmarker.remove();
                }
                lastplacedmarker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Get Location marker")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                showCustomToast("Marker placed at: " + latLng.latitude + ", " + latLng.longitude, 5);
            } else if (isAreaCalculationMode) {
                addPointToPolygon(latLng);
            } else if (isDistanceCalculationMode) {
                addPointToPolyline(latLng);
            }
        });
        startLocationUpdates(); // Start location updates immediately when the map is ready
    }

    private void cycleMapType() {
        if (isMapReady && googleMap != null) {
            currentMapTypeIndex = (currentMapTypeIndex + 1) % mapTypes.length;
            googleMap.setMapType(mapTypes[currentMapTypeIndex]);
        }
    }



    private Marker currentLocationMarker; // Store a reference to the current location marker

    private void updateLocationDetails(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.getAltitude();

        String latDirection = latitude >= 0 ? "N" : "S";
        String lonDirection = longitude >= 0 ? "E" : "W";

        latitudeTextView.setText(String.format(Locale.getDefault(), "%.6f %s", Math.abs(latitude), latDirection));
        longitudeTextView.setText(String.format(Locale.getDefault(), "%.6f %s", Math.abs(longitude), lonDirection));
        altitudeTextView.setText(String.format(Locale.getDefault(), "%.1f M", altitude));

        if (gpsDataList.size() >= MAX_LIST_SIZE) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "gps_data_" + timeStamp + ".kml";
            saveDataToTxtFile(fileName);  // Save the data to a file
            gpsDataList.clear();  // Clear the list to free up memory
        }

        // Update the marker on the map
        LatLng currentLocation = new LatLng(latitude, longitude);

        // Check if `currentLocationMarker` already exists
        if (currentLocationMarker != null) {
            // Remove the previous current location marker
            currentLocationMarker.remove();
        }

        // Add a new marker for the updated current location
        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Move the camera to the current location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

    }


    private void initializeDateTimeHandler() {
        handler = new Handler(Looper.getMainLooper());
        updateDateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                dateTimeTextView.setText(dateTime);
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(updateDateTimeRunnable); // Start updating date-time immediately
        if (isReceivingGPS) {
            startLocationUpdates(); // Ensure GPS updates resume if they were previously enabled
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the fragment is destroyed
        getActivity().unregisterReceiver(gpsDataReceiver);

        // Stop the GPS Service when the fragment is destroyed
        Intent serviceIntent = new Intent(getActivity(), GpsForegroundService.class);
        getActivity().stopService(serviceIntent);
    }

    private boolean saveDataToTxtFile(String fileName) {
        OutputStream outputStream = null;
        try {
            // Set up the file details
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");

            // Choose the directory to save in
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NioSurveyStats/InternalGPSDataTxt");
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    outputStream = requireContext().getContentResolver().openOutputStream(uri);
                }
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "GPSData");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                // Write the header with the new "Check" column
                outputStream.write("Date-Time,Latitude,Longitude,Altitude,GPS Fix Status,Check\n".getBytes());

                // Write the collected data
                for (String data : gpsDataList) {
                    outputStream.write((data + "\n").getBytes());
                }

                // Show custom toast with the file path
                showCustomToast("Data saved to " + fileName, 1000);


            }
        } catch (IOException e) {
            e.printStackTrace();
            showCustomToast("Error saving data", 1000);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }


    private boolean saveDataToKmlFile(String fileName) {
        OutputStream outputStream = null;
        try {
            // Set up the file details
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.google-earth.kml+xml");

            // Choose the directory to save in
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NioSurveyStats/InternalGPSDataKml");
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    outputStream = requireContext().getContentResolver().openOutputStream(uri);
                }
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "GPSData");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                // Start the KML document
                outputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes());
                outputStream.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n".getBytes());
                outputStream.write("<Document>\n".getBytes());
                outputStream.write("<name>GPS Data</name>\n".getBytes());

                // Define styles for different check values
                outputStream.write("<Style id=\"markerStyle1\">\n".getBytes());
                outputStream.write("<IconStyle>\n".getBytes());
                outputStream.write("<Icon>\n".getBytes());
                outputStream.write("<href>http://maps.google.com/mapfiles/kml/paddle/blu-circle.png</href>\n".getBytes()); // No newlines, no quotes
                outputStream.write("</Icon>\n".getBytes());
                outputStream.write("<scale>1.2</scale>\n".getBytes());
                outputStream.write("</IconStyle>\n".getBytes());
                outputStream.write("</Style>\n".getBytes());

                outputStream.write("<Style id=\"markerStyle2\">\n".getBytes());
                outputStream.write("<IconStyle>\n".getBytes());
                outputStream.write("<Icon>\n".getBytes());
                outputStream.write("<href>http://maps.google.com/mapfiles/kml/paddle/grn-circle.png</href>\n".getBytes()); // No newlines, no quotes
                outputStream.write("</Icon>\n".getBytes());
                outputStream.write("<scale>1.2</scale>\n".getBytes());
                outputStream.write("</IconStyle>\n".getBytes());
                outputStream.write("</Style>\n".getBytes());

                outputStream.write("<Style id=\"markerStyle3\">\n".getBytes());
                outputStream.write("<IconStyle>\n".getBytes());
                outputStream.write("<Icon>\n".getBytes());
                outputStream.write("<href>http://maps.google.com/mapfiles/kml/paddle/ylw-circle.png</href>\n".getBytes()); // No newlines, no quotes
                outputStream.write("</Icon>\n".getBytes());
                outputStream.write("<scale>1.2</scale>\n".getBytes());
                outputStream.write("</IconStyle>\n".getBytes());
                outputStream.write("</Style>\n".getBytes());

                outputStream.write("<Style id=\"markerStyle4\">\n".getBytes());
                outputStream.write("<IconStyle>\n".getBytes());
                outputStream.write("<Icon>\n".getBytes());
                outputStream.write("<href>http://maps.google.com/mapfiles/kml/paddle/purple-circle.png</href>\n".getBytes()); // No newlines, no quotes
                outputStream.write("</Icon>\n".getBytes());
                outputStream.write("<scale>1.2</scale>\n".getBytes());
                outputStream.write("</IconStyle>\n".getBytes());
                outputStream.write("</Style>\n".getBytes());

                outputStream.write("<Style id=\"defaultMarkerStyle\">\n".getBytes());
                outputStream.write("<IconStyle>\n".getBytes());
                outputStream.write("<Icon>\n".getBytes());
                outputStream.write("<href>http://maps.google.com/mapfiles/kml/paddle/red-circle.png</href>\n".getBytes()); // No newlines, no quotes
                outputStream.write("</Icon>\n".getBytes());
                outputStream.write("<scale>1.2</scale>\n".getBytes());
                outputStream.write("</IconStyle>\n".getBytes());
                outputStream.write("</Style>\n".getBytes());

                // Write each GPS entry as a Placemark with the appropriate style
                for (String data : gpsDataList) {
                    String[] parts = data.split(",");
                    if (parts.length >= 5) { // Ensure there's latitude, longitude, altitude, and check
                        String dateTime = parts[0];
                        String latitude = parts[1];
                        String longitude = parts[2];
                        String altitude = parts[3];
                        String check = (parts.length > 5) ? parts[5] : null; // Safely get the check value if it exists

                        // Determine the style to use
                        String styleUrl = "#defaultMarkerStyle";
                        if ("1".equals(check)) {
                            styleUrl = "#markerStyle1";
                        } else if ("2".equals(check)) {
                            styleUrl = "#markerStyle2";
                        } else if ("3".equals(check)) {
                            styleUrl = "#markerStyle3";
                        } else if ("4".equals(check)) {
                            styleUrl = "#markerStyle4";
                        }

                        outputStream.write("<Placemark>\n".getBytes());
                        outputStream.write(("<name>" + dateTime + "</name>\n").getBytes());
                        outputStream.write(("<styleUrl>" + styleUrl + "</styleUrl>\n").getBytes()); // Apply the determined style
                        outputStream.write("<Point>\n".getBytes());
                        outputStream.write(("<coordinates>" + longitude + "," + latitude + "," + altitude + "</coordinates>\n").getBytes());
                        outputStream.write("</Point>\n".getBytes());
                        outputStream.write("</Placemark>\n".getBytes());
                    }
                }

                // End the KML document
                outputStream.write("</Document>\n".getBytes());
                outputStream.write("</kml>\n".getBytes());


                // Show custom toast with the file path
                showCustomToast("KML data saved to " + fileName, 1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showCustomToast("Error saving KML data", 1000);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public void saveBothFiles(String fileName) {
        boolean save1Success = saveDataToKmlFile(fileName);  // Save to KML
        boolean save2Success = saveDataToTxtFile(fileName);  // Save to another file

        // Clear the list only if both saves were successful
        if (save1Success && save2Success) {
            gpsDataList.clear();
            System.out.println("Both saves completed. gpsDataList cleared.");
        } else {
            System.out.println("Save operation failed for one or both files. gpsDataList not cleared.");
        }
    }




    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateDateTimeRunnable); // Stop updating date-time
    }

    @Override
    public void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    private void showCustomToast(String message, int durationInMillis) {
        final Toast toast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        toast.show();

        // Use a Handler to cancel the Toast after the specified duration
        new Handler(Looper.getMainLooper()).postDelayed(toast::cancel, durationInMillis);
    }

    private void DataTap(Integer counter) {
        // Check if there is any data in gpsDataList
        if (!gpsDataList.isEmpty()) {
            // Get the last entry in gpsDataList
            int lastIndex = gpsDataList.size() - 1;
            String lastEntry = gpsDataList.get(lastIndex);

            // Check if the entry already has a "Check" value
            if (lastEntry.split(",").length == 5) {
                // If not, add the counter value as the "Check" value
                String updatedEntry = lastEntry + "," + counter.toString();
                gpsDataList.set(lastIndex, updatedEntry);
            } else {
                // If it already has a "Check" value, replace it with the new one
                String[] parts = lastEntry.split(",");
                parts[5] = counter.toString();
                String updatedEntry = String.join(",", parts);
                gpsDataList.set(lastIndex, updatedEntry);
            }

            String[] parts = lastEntry.split(",");
            double latitude = Double.parseDouble(parts[1]);
            double longitude = Double.parseDouble(parts[2]);
            LatLng taggedLocation = new LatLng(latitude, longitude);

            // Use Google Maps' built-in colored markers based on the tag counter
            float markerColor;
            switch (counter) {
                case 1:
                    markerColor = BitmapDescriptorFactory.HUE_BLUE;
                    break;
                case 2:
                    markerColor = BitmapDescriptorFactory.HUE_GREEN;
                    break;
                case 3:
                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
                case 4:
                    markerColor = BitmapDescriptorFactory.HUE_VIOLET;
                    break;
                default:
                    markerColor = BitmapDescriptorFactory.HUE_RED;
            }

            googleMap.addMarker(new MarkerOptions()
                    .position(taggedLocation)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                    .title("Tag " + counter)
                    .snippet("Tagged at " + parts[0]));

            showCustomToast("Tagged with " + counter + " at " + parts[0], 1000);
        } else {
            // Handle case where gpsDataList is empty (optional)
            showCustomToast("No GPS data to tag", 1000);
        }
    }

}
