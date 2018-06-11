package com.project.hackathon.Potholeinformer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
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
import android.Manifest;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

// class to show the potholes on the map and calculate a route
public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener, SensorEventListener {
    private MapView mapView;
    private MapboxMap map;
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
    private Button button,process;
    private EditText initial_location,final_location;
    //Firebase instances
    private DatabaseReference mDatabase;
    private DatabaseReference mRef;
    private DatabaseReference dbRef;
    private int x=0;
    Button addpotholebtn,addpotholelese,signout;
    ArrayList<coordinate> list=new ArrayList<coordinate>();
    private LatLng initial_lat_lon=new LatLng();
    private LatLng final_lat_lon=new LatLng();
    FloatingActionButton floatingActionButton,floating_cancel,floating_mylocation;
    RelativeLayout relativeLayout;
    //Mobile Accelerometer reading variables
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, 1,0);

        mapView = findViewById(R.id.mapView);
        button = findViewById(R.id.startButton);
        floatingActionButton=(FloatingActionButton) findViewById(R.id.floating_button);
        floating_cancel=(FloatingActionButton) findViewById(R.id.floating_cancel);
        floating_mylocation=(FloatingActionButton)findViewById(R.id.floating_mylocation);
        mapView.onCreate(savedInstanceState);
        relativeLayout=(RelativeLayout)findViewById(R.id.relative_layout);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mRef =  FirebaseDatabase.getInstance().getReference();
        dbRef =  FirebaseDatabase.getInstance().getReference();
        initial_location = (EditText) findViewById(R.id.txt_initial_location);
        final_location = (EditText) findViewById(R.id.txt_final_location);
        process=(Button)findViewById(R.id.process);
        addpotholebtn=(Button)findViewById(R.id.addpotholebtn);
        addpotholelese=(Button)findViewById(R.id.addpoholeelse);
        signout = (Button)findViewById(R.id.signoutbtn);


        long unixTime = System.currentTimeMillis() / 1000L;
        coordinate co;
        String key = dbRef.push().getKey();
        co = new coordinate(unixTime, "False", 0, 0.0, 0.0,key);

        mRef.child(key).setValue(co);
        get_data();


        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map=mapboxMap;
                enableLocationPlugin();

                map.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        floating_cancel.setVisibility(View.VISIBLE);
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
                        //button.setEnabled(true);
                        //button.setBackgroundResource(R.color.mapboxBlue);
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
                                    getRoute(originPosition, destinationPosition);
                                }catch (Exception e){

                                }
                            }
                        }
                    }
                });
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Point origin = originPosition;
                Point destination = destinationPosition;

                String awsPoolId = null;
                boolean simulateRoute = true;
                NavigationViewOptions options = NavigationViewOptions.builder()
                        .origin(origin)
                        .destination(destination)
                        .awsPoolId(awsPoolId)
                        .shouldSimulateRoute(simulateRoute)
                        .build();

                NavigationLauncher.startNavigation(MainActivity.this, options);
            }
        });

        process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View view1=MainActivity.this.getCurrentFocus();
                if(view1!=null){
                    InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view1.getWindowToken(),0);
                }
                if((initial_location.getText().toString().trim().length()>0)&&(final_location.getText().toString().trim().length()>0)) {
                    x = 1;
                    simple_location_search_initial(initial_location.getText().toString());
                    simple_location_search_final(final_location.getText().toString());
                }else {
                    Toast.makeText(MainActivity.this,"Enter source/destination",Toast.LENGTH_SHORT).show();
                }
            }
        });



        //   ******************************************** Start line  **************************************//

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //map.moveCamera(CameraUpdateFactory.newLatLng(originCoord));
                //map.setZoom(17);



                String awsPoolId = null;
                boolean simulateRoute = true;
                if(originPosition==null |  destinationPosition ==null) {

                    Toast.makeText(MainActivity.this,"Choose source/destination",Toast.LENGTH_SHORT).show();
                }

                if(originPosition!=null & destinationPosition !=null) {
                    Point origin = originPosition;
                    Point destination = destinationPosition;
                    NavigationViewOptions options = NavigationViewOptions.builder()
                            .origin(origin)
                            .destination(destination)
                            .awsPoolId(awsPoolId)
                            .shouldSimulateRoute(simulateRoute)
                            .build();

                    NavigationLauncher.startNavigation(MainActivity.this, options);
                    relativeLayout.setVisibility(View.INVISIBLE);
                    addpotholebtn.setVisibility(View.INVISIBLE);
                    addpotholelese.setVisibility(View.INVISIBLE);
                    signout.setVisibility(View.INVISIBLE);
                    floating_cancel.setVisibility(View.VISIBLE);
                }
                else if (destinationPosition==null){
                    Toast.makeText(MainActivity.this,"Please choose your destination",Toast.LENGTH_SHORT);
                }
            }
        });
        //   ******************************************** End line  **************************************//

        floating_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floating_cancel.setVisibility(View.INVISIBLE);
                relativeLayout.setVisibility(View.VISIBLE);
                addpotholebtn.setVisibility(View.VISIBLE);
                addpotholelese.setVisibility(View.VISIBLE);
                signout.setVisibility(View.VISIBLE);
                if (navigationMapRoute != null) {
                    navigationMapRoute.removeRoute();
                }if(destinationMarker!=null){
                    map.removeMarker(destinationMarker);
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

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
            try{
                originPosition = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                getRoute(originPosition, destinationPosition);
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
        locationEngine = new LostLocationEngine(MainActivity.this);
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
    private void simple_location_search_initial(String location){
        MapboxGeocoder client=new MapboxGeocoder.Builder()
                .setAccessToken(getString(R.string.access_token))
                .setLocation(location)
                .build();
        retrofit.Response<GeocoderResponse> response;
        client.enqueue(new retrofit.Callback<GeocoderResponse>() {
            @Override
            public void onResponse(retrofit.Response<GeocoderResponse> response, Retrofit retrofit) {
                Double latitude=response.body().getFeatures().get(0).getLatitude();
                Double longitude=response.body().getFeatures().get(0).getLongitude();
                LatLng latLng=new LatLng(latitude,longitude);
                set_lat_lon(latLng);
                //   ******************************************** Start line  **************************************//
                originPosition = Point.fromLngLat(initial_lat_lon.getLongitude(), initial_lat_lon.getLatitude());
                //   ******************************************** End line **************************************//
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Place Name","Failed");
            }
        });
    }

    private void set_lat_lon(LatLng latLng) {
        initial_lat_lon=latLng;

    }

    private void simple_location_search_final(String location){
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
                Point i_point = Point.fromLngLat(initial_lat_lon.getLongitude(), initial_lat_lon.getLatitude());
                Point f_point = Point.fromLngLat(final_lat_lon.getLongitude(), final_lat_lon.getLatitude());
                //   ******************************************** Start line  **************************************//
                originPosition = Point.fromLngLat(initial_lat_lon.getLongitude(), initial_lat_lon.getLatitude());
                destinationPosition=Point.fromLngLat(final_lat_lon.getLongitude(), final_lat_lon.getLatitude());
                //   ******************************************** End line  **************************************//
                getRoute(i_point, f_point);
                map.moveCamera(CameraUpdateFactory.newLatLng(initial_lat_lon));
                map.setZoom(17);
                Log.e("Problem",""+final_lat_lon);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Place Name","Failed");
            }
        });
    }

    private void set_final_lat_lon(LatLng latLng) {
        final_lat_lon=latLng;
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
    long time = System.currentTimeMillis();

    public void get_data(){

        time = time/1000;

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
                        DatabaseReference ref = noteSnapshot.getRef();
                        note.setTimestamp(time);
                        note.setValue("False");
                        ref.setValue(note);
                    }
                   check_duplicacy(note,mmap_layout,noteSnapshot.getKey());



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

    private void check_duplicacy(coordinate note, ArrayList<coordinate> mmap_layout,String key) {
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
        list = mmap_layout;
        if(map!=null) {
            set_data_on_map();
        }
    }

    private void set_data_on_map() {
        if (list != null) {
            Log.e("data", "" + list.size());

            IconFactory mIconFactory = IconFactory.getInstance(MainActivity.this);
            Icon icon = mIconFactory.fromResource(R.drawable.warning);

            for (int i = 0; i < list.size(); i++) {
                coordinate data = list.get(i);

                if (data.getValue().equals("True")) {
                    LatLng sydney = new LatLng(data.getLattitude(), data.getLongitude());
                    MarkerOptions marker = new MarkerOptions().position(new LatLng(data.getLattitude(), data.getLongitude()));

                    marker.icon(icon);

                    map.addMarker(marker);
                }
            }

        }
        else {
            Toast.makeText(MainActivity.this,"Loading Potholes...",LENGTH_SHORT).show();
        }


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


    public void addPothole(View view) throws ParseException {



        final double[] longitude = {0};
        final double[] latitude = { 0 };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            longitude[0] = location.getLongitude();
            latitude[0] = location.getLatitude();


        }

        String key = dbRef.push().getKey();
        long unixTime = System.currentTimeMillis() / 1000L;
        coordinate co;
        if (latitude[0]== 0. & longitude[0]==0.0) {
            co = new coordinate(unixTime, "False", 0,originPosition.latitude(), originPosition.longitude(),key);

        }
        else {
            co = new coordinate(unixTime, "False", 0, latitude[0], longitude[0],key);
        }

        dbRef.child(key).setValue(co).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Pothole stored at your current location", LENGTH_SHORT).show();


                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        longitude[0] = location.getLongitude();
                        latitude[0] = location.getLatitude();
                    }



                    Log.e("check","substract");



                }
                else{
                    Toast.makeText(MainActivity.this, "Couldn't add at the moment ", LENGTH_SHORT).show();

                }
            }
        });

        get_data();
    }

    long delay = 0;
    static long timestamp = 0;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {



        long time = System.currentTimeMillis()/1000;

        final double[] longitude = new double[1];
        final double[] latitude = new double[1];
        Sensor mySensor = sensorEvent.sensor;



        if (Math.abs(time - timestamp) > 3) {


            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                long curTime = System.currentTimeMillis();

                if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;
                }
                if (z > 20 || z < 2) {

                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        longitude[0] = location.getLongitude();
                        latitude[0] = location.getLatitude();


                    } else {

                        final LocationListener locationListener = new LocationListener() {
                            public void onLocationChanged(Location location) {
                                longitude[0] = location.getLongitude();
                                latitude[0] = location.getLatitude();
                            }

                            @Override
                            public void onStatusChanged(String s, int i, Bundle bundle) {

                            }

                            @Override
                            public void onProviderEnabled(String s) {

                            }

                            @Override
                            public void onProviderDisabled(String s) {

                            }
                        };

                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
                    }
                    String key = dbRef.push().getKey();
                    long unixTime = System.currentTimeMillis() / 1000L;
                    timestamp = unixTime;
                    coordinate co;
                    if (latitude[0]== 0. & longitude[0]==0.0) {
                        co = new coordinate(unixTime, "False", 0,originPosition.latitude(), originPosition.longitude(),key);

                    }
                    else {
                        co = new coordinate(unixTime, "False", 0, latitude[0], longitude[0],key);
                    }


                    final int[] flag = {1};
                    dbRef.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            coordinate c = dataSnapshot.getValue(coordinate.class);
                            if (Math.abs(c.lattitude - latitude[0]) <= 0.00002 || Math.abs(c.longitude - longitude[0]) <= 0.00002)
                                flag[0] = 0;

                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    if (flag[0] == 1) {

                        dbRef.child(key).setValue(co).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    get_data();
                                } else {
                                    // Toast.makeText(MainActivity.this, "Couldn't add at the moment ", LENGTH_SHORT).show();

                                }
                            }
                        });

                    }  // Toast.makeText(getApplicationContext(), longitude[0] + " " + latitude[0], LENGTH_SHORT).show();
                }

            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void addPotholeelse(View view) {

        final double[] longitude = {0};
        final double[] latitude = { 0 };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // destinationPosition=null;
        if (destinationPosition==null){
            Toast.makeText(MainActivity.this, "Please select the pothole", Toast.LENGTH_SHORT).show();

        }

        if (destinationPosition!=null) {
            long unixTime = System.currentTimeMillis() / 1000L;
            coordinate co;
            String key = dbRef.push().getKey();
            co = new coordinate(unixTime, "False", 0, destinationPosition.latitude(), destinationPosition.longitude(),key);

            dbRef.child(key).setValue(co).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Pothole stored at your chosen location", LENGTH_SHORT).show();


                        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            longitude[0] = location.getLongitude();
                            latitude[0] = location.getLatitude();
                        }


                        Log.e("check","substract");


                    } else {
                        Toast.makeText(MainActivity.this, "Couldn't add at the moment ", LENGTH_SHORT).show();

                    }
                }
            });
        }
    }
    public void signOut(View view){

        mAuth.signOut();
        Toast.makeText(this,"Signing out..",LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this,Signin.class);
        startActivity(intent);
    }
    @Override
    public void onBackPressed()
    {
        Toast.makeText(this,"Please Sign out to go back to Sign in Page",LENGTH_SHORT).show();
    }
}

