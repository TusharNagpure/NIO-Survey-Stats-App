package de.kai_morich.simple_usb_terminal;


import android.Manifest;
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
import android.widget.ImageView;
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
    private TextView areaTextView;
    private GoogleMap googleMap;
    private boolean isMapReady = false;
    private static final int REQUEST_LOCATION_PERMISSION = 113;
    private ImageView saveIcon;
    private ArrayList<String> gpsDataList;
    private static final int MAX_LIST_SIZE = 20000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gps, container, false);
        areaTextView = view.findViewById(R.id.areaTextView);
        gpsToggleButton = view.findViewById(R.id.gpsToggleButton);
        saveIcon = view.findViewById(R.id.saveIcon);
        saveIcon.setOnClickListener(v -> saveDataToTxtFile());
        areaFab = view.findViewById(R.id.areaFab);
        distanceFab = view.findViewById(R.id.distanceFab);
        // Initialize TextViews and ToggleButton
        initializeUI(view);

        // Setup map fragment
        setupMapFragment();

        // Setup location callback
        setupLocationCallback();

        // Initialize handler and runnable for date-time updates
        initializeDateTimeHandler();
        gpsDataList = new ArrayList<>();
        // ToggleButton listener
        gpsToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAreaCalculationMode = false;
            isDistanceCalculationMode = false;
            areaTextView.setVisibility(View.GONE);
            isReceivingGPS = !isChecked;
            if (isChecked) {
                stopLocationUpdates();
                gpsStatusTextView.setText("GPS paused");
                showCustomToast("GPS paused",500);
            } else {
                startLocationUpdates();
                gpsStatusTextView.setText("");
                showCustomToast("GPS Monitoring resumed",500);
            }

            if (googleMap != null) {
                googleMap.clear();
                clearMarkersAndLines();
            }
        });

        areaFab = view.findViewById(R.id.areaFab);
        areaFab.setOnClickListener(v -> {
            if (gpsToggleButton.isChecked()) {
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
            if (gpsToggleButton.isChecked()) {
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
                    if (location.getProvider().equals(LocationManager.GPS_PROVIDER) && location.hasAccuracy() && location.getAccuracy() < 10) {
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
        });startLocationUpdates(); // Start location updates immediately when the map is ready
    }


    private void updateLocationDetails(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.getAltitude();

        String latDirection = latitude >= 0 ? "N" : "S";
        String lonDirection = longitude >= 0 ? "E" : "W";

        latitudeTextView.setText(String.format(Locale.getDefault(), "%.6f %s", Math.abs(latitude), latDirection));
        longitudeTextView.setText(String.format(Locale.getDefault(), "%.6f %s", Math.abs(longitude), lonDirection));
        altitudeTextView.setText(String.format(Locale.getDefault(), "%.1f M", altitude));
        // Store the data in the array
        String dateTime = dateTimeTextView.getText().toString();
        String gpsStatus = gpsStatusTextView.getText().toString();
        String data = String.format("%s,%s,%s,%s,%s", dateTime, latitudeTextView.getText().toString(),
                longitudeTextView.getText().toString(), altitudeTextView.getText().toString(), gpsStatus);
        gpsDataList.add(data);

        if (gpsDataList.size() >= MAX_LIST_SIZE) {
            saveDataToTxtFile();  // Save the data to a file
            gpsDataList.clear();  // Clear the list to free up memory
        }

        // Update the marker on the map
        LatLng currentLocation = new LatLng(latitude, longitude);
        googleMap.clear(); // Clear previous markers
        googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
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

    private void saveDataToTxtFile() {
        // Get the "Documents" directory in the external storage
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "GPSData");
        if (!directory.exists()) {
            directory.mkdirs();  // Create the directory if it doesn't exist
        }

        // Generate a unique filename using a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "nmea_data_" + timeStamp + ".txt";
        File file = new File(directory, fileName);

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);

            // Write the header
            writer.append("Date-Time,Latitude,Longitude,Altitude,GPS Fix Status\n");

            // Write the collected data
            for (String data : gpsDataList) {
                writer.append(data).append("\n");
            }

            showCustomToast("Data saved to " + file.getAbsolutePath(), 1000);
        } catch (IOException e) {
            e.printStackTrace();
            showCustomToast("Error saving data", 1000);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
}

