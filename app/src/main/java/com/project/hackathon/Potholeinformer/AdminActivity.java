package com.project.hackathon.Potholeinformer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geocoder.MapboxGeocoder;
import com.mapbox.geocoder.service.models.GeocoderResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;


import com.mapbox.api.directions.v5.models.DirectionsResponse;

import retrofit.Retrofit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

// class to show the admin the potholes in a particular location
public class AdminActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener, SensorEventListener {
    private MapView mapView;
    private MapboxMap map;
    FloatingActionButton floatingActionButton,floating_cancel,floating_mylocation;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;
    // variables for adding a marker
    private Marker destinationMarker;
    private AutoCompleteTextView autocomplete;
    private LatLng originCoord;
    private LatLng destinationCoord;
    // variables for calculating and drawing a route
    private Point originPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    private Button  searchbtn;
    private EditText searchtxt;
    private DatabaseReference mDatabase;
    private DatabaseReference mRef;
    private DatabaseReference dbRef;
    private int x=0;
    ArrayList<coordinate> list=new ArrayList<coordinate>();
    private LatLng initial_lat_lon=new LatLng();
    private LatLng final_lat_lon=new LatLng();

    RelativeLayout relativeLayout;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_admin);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, 1);

        mapView = findViewById(R.id.mapView);
        searchbtn = findViewById(R.id.searchbtn);

        floating_mylocation=(FloatingActionButton)findViewById(R.id.floating_mylocation);
        mapView.onCreate(savedInstanceState);
        relativeLayout=(RelativeLayout)findViewById(R.id.relative_layout);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mRef =  FirebaseDatabase.getInstance().getReference();
        dbRef =  FirebaseDatabase.getInstance().getReference();
        searchtxt = (EditText) findViewById(R.id.txt_initial_location);
        floatingActionButton=(FloatingActionButton)findViewById(R.id.floating_button);
        floating_cancel=(FloatingActionButton) findViewById(R.id.floating_cancel);
        floating_mylocation=(FloatingActionButton)findViewById(R.id.floating_mylocation);


        long unixTime = System.currentTimeMillis() / 1000L;
        coordinate co;
        String key = dbRef.push().getKey();
        co = new coordinate(unixTime, "False", 0, 0.0, 0.0,key);

        mRef.child(key).setValue(co);
        get_data();


        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                enableLocationPlugin();

                map.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        if (destinationMarker != null) {
                            map.removeMarker(destinationMarker);
                        }
                        x=0;
                        destinationCoord = point;
                        destinationMarker = map.addMarker(new MarkerOptions().position(destinationCoord)
                        );
                        Log.e("problem","click listener not working");
                        //   ******************************************** Start line  **************************************//
                        destinationPosition = Point.fromLngLat(destinationCoord.getLongitude(), destinationCoord.getLatitude());
                        originPosition = Point.fromLngLat(originCoord.getLongitude(), originCoord.getLatitude());
                        //   ******************************************** End line **************************************//

                        getRoute(originPosition, destinationPosition);
                    }
                });

                map.setOnMyLocationChangeListener(new MapboxMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(@Nullable Location location) {
                        if (location != null) {
                            originLocation = location;
                            setCameraPosition(location);
                            if(x==0){
                                try{
                                    originPosition = Point.fromLngLat(location.getLongitude(), location.getLatitude());

                                }catch (Exception e){

                                }
                            }
                        }
                    }
                });
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {




                String awsPoolId = null;
                boolean simulateRoute = true;
                if(originPosition!=null & destinationPosition !=null) {
                    Point origin = originPosition;
                    Point destination = destinationPosition;
                    NavigationViewOptions options = NavigationViewOptions.builder()
                            .origin(origin)
                            .destination(destination)
                            .awsPoolId(awsPoolId)
                            .shouldSimulateRoute(simulateRoute)
                            .build();

                    NavigationLauncher.startNavigation(AdminActivity.this, options);
                    relativeLayout.setVisibility(View.INVISIBLE);
                    floating_cancel.setVisibility(View.VISIBLE);
                }
                else if (destinationPosition==null){
                    Toast.makeText(AdminActivity.this,"Please choose your destination",Toast.LENGTH_SHORT).show();
                }
            }
        });

        floating_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floating_cancel.setVisibility(View.INVISIBLE);


                relativeLayout.setVisibility(View.VISIBLE);
                if (navigationMapRoute != null) {
                    navigationMapRoute.removeRoute();
                }if(destinationMarker!=null){
                    map.removeMarker(destinationMarker);
                }
            }
        });

        searchbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View view1=AdminActivity.this.getCurrentFocus();
                if(view1!=null){
                    InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view1.getWindowToken(),0);
                }
                if((searchtxt.getText().toString().trim().length()>0)) {
                    Toast.makeText(AdminActivity.this,searchtxt.getText().toString(),LENGTH_SHORT).show();
                    simple_location_search(searchtxt.getText().toString());
                }else {
                    Toast.makeText(AdminActivity.this,"Enter source/destination",Toast.LENGTH_SHORT).show();
                }
            }
        });


        floating_mylocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.moveCamera(CameraUpdateFactory.newLatLng(originCoord));
                map.setZoom(17);
            }
        });
    }
    private void getRoute(Point origin, Point destination) {
        if (origin != null && destination != null) {
            NavigationRoute.builder()
                    .accessToken(Mapbox.getAccessToken())
                    .origin(origin)
                    .destination(destination)
                    .build()
                    .getRoute(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                            // You can get the generic HTTP info about the response
                            Log.d(TAG, "Response code: " + response.code());
                            if (response.body() == null) {
                                Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                                return;
                            } else if (response.body().routes().size() < 1) {
                                Log.e(TAG, "No routes found");
                                return;
                            }

                            currentRoute = response.body().routes().get(0);
                            // Draw the route on the map
                            if (navigationMapRoute != null) {
                                navigationMapRoute.removeRoute();
                            } else {
                                navigationMapRoute = new NavigationMapRoute(null, mapView, map, R.style.NavigationMapRoute);
                            }
                            navigationMapRoute.addRoute(currentRoute);
                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                            Log.e(TAG, "Error: " + throwable.getMessage());
                        }
                    });
        }

    }
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
            try{
                originPosition = Point.fromLngLat(location.getLongitude(), location.getLatitude());

            }catch (Exception e){

            }
        }
    }

    @SuppressLint("MissingPermission")
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();
            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = new LostLocationEngine(AdminActivity.this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }


    private void set_lat_lon(LatLng latLng) {
        initial_lat_lon=latLng;

    }

    private void simple_location_search(String location){
        MapboxGeocoder client=new MapboxGeocoder.Builder()
                .setAccessToken(getString(R.string.access_token))
                .setLocation(location)
                .build();
        client.enqueue(new retrofit.Callback<GeocoderResponse>() {
            @Override
            public void onResponse(retrofit.Response<GeocoderResponse> response, Retrofit retrofit) {
                Double latitude=response.body().getFeatures().get(0).getLatitude();
                Double longitude=response.body().getFeatures().get(0).getLongitude();
                LatLng latLng=new LatLng(latitude,longitude);
                set_final_lat_lon(latLng);

                destinationPosition=Point.fromLngLat(final_lat_lon.getLongitude(), final_lat_lon.getLatitude());
                MarkerOptions marker = new MarkerOptions().position(new LatLng(destinationPosition.latitude(),destinationPosition.longitude()));
                map.addMarker(marker);
                map.moveCamera(CameraUpdateFactory.newLatLng(final_lat_lon));
                map.setZoom(17);
                Log.e("Problem",""+final_lat_lon);
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(AdminActivity.this,"Sorry unable to find your location",LENGTH_SHORT).show();
                Log.e("Place Name","Failed");
            }
        });
    }



    int y=0;
    private void setCameraPosition(Location location) {
        originCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
        if(y<1) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 18));
            y++;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }


    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            finish();
        }
    }
    long time= System.currentTimeMillis();
    public void get_data(){
        time=time/1000;

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<coordinate> mmap_layout = new ArrayList<coordinate>();

                for (DataSnapshot noteSnapshot : dataSnapshot.getChildren()) {
                    coordinate note=noteSnapshot.getValue(coordinate.class);
                    if((((time-note.getTimestamp())>259200))&&(note.getValue().contains("F"))){
                        noteSnapshot.getRef().removeValue();
                    }
                    if((((time-note.getTimestamp())>259200))&&(note.getValue().contains("T"))){
                        DatabaseReference ref=noteSnapshot.getRef();
                        note.setTimestamp(time);
                        note.setValue("False");
                        ref.setValue(note);
                    }
                    check_duplicacy(note,mmap_layout);
                    mmap_layout.add(note);

                }
                Log.e("mmap_layout_length",""+mmap_layout.size());
                set_data(mmap_layout);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void check_duplicacy(coordinate note, ArrayList<coordinate> mmap_layout) {
        for(int i=0;i<mmap_layout.size();i++) {
            coordinate data = mmap_layout.get(i);
            double lat_diff = data.getLattitude()-note.getLattitude();
            double lon_diff = data.getLongitude()-note.getLongitude();
            if(data.getId() == 0) {
                if (Math.abs(lat_diff) < 0.0001 && Math.abs(lon_diff) < 0.0001) {
                    if (data.getValue().equals("False")) {
                        data.setId(1);
                        note.setId(1);
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(note.getKey());
                        note.setValue("True");
                        ref.setValue(note);
                        DatabaseReference mref = FirebaseDatabase.getInstance().getReference(data.getKey());
                        mref.setValue(data);
                    }
                    if (data.getValue().equals("True")) {
                        data.setId(1);
                        note.setId(1);
                        DatabaseReference mref = FirebaseDatabase.getInstance().getReference(data.getKey());
                        data.setValue("False");
                        mref.setValue(data);
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(note.getKey());
                        note.setValue("True");
                        ref.setValue(note);

                    }
                }
            }
            else {

                if (Math.abs(lat_diff) < 0.0001 && Math.abs(lon_diff) < 0.0001) {
                    if(data.getValue().equals("True")){
                        data.setValue("False");
                        note.setValue("True");
                        note.setId(1);
                        DatabaseReference mref = FirebaseDatabase.getInstance().getReference(data.getKey());
                        mref.setValue(data);
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(note.getKey());
                        ref.setValue(note);
                    }

                }
            }
        }
    }

    private void set_data(ArrayList<coordinate> mmap_layout) {
        list=mmap_layout;
        if(map!=null) {
            set_data_on_map();
        }
    }

    private void set_data_on_map() {
        if (list != null) {
            Log.e("datalistsize", "" + list.size());

            IconFactory mIconFactory = IconFactory.getInstance(AdminActivity.this);
            Icon icon = mIconFactory.fromResource(R.drawable.warning);

            for (int i = 0; i < list.size(); i++) {
                coordinate data = list.get(i);

                LatLng sydney = new LatLng(data.getLattitude(), data.getLongitude());
                MarkerOptions marker = new MarkerOptions().position(new LatLng(data.getLattitude(), data.getLongitude()));
                Log.e("check", "substract");
                marker.icon(icon);

                map.addMarker(marker);
            }

        }
        else {
            Toast.makeText(AdminActivity.this,"Loading Potholes....",LENGTH_SHORT).show();
        }

    }
    private void set_final_lat_lon(LatLng latLng) {
        final_lat_lon=latLng;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }




}

