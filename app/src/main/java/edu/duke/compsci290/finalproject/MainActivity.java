package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
    private String mUid = mUser.getUid();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ArrayList<MealEntry> mMeals;
    private MealAdapter mAdapter;
    private View mMainContentView;
    private long mPageOffset = -Long.MAX_VALUE;
    private int mPageLimit = 10;
    private int mCount = 0;
    private boolean allDataLoaded = false;
    private Context mContext;
    private static final long FIVE_MEGABYTE = 1024 * 1024 * 5;

    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private boolean mIsRequestingLocationPermission = false;

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        String userId = mUser.getUid();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        TextView name = navigationView.getHeaderView(0).findViewById(R.id.name_text_view);
        TextView email = navigationView.getHeaderView(0).findViewById(R.id.email_text_view);
        name.setText(mUser.getDisplayName());
        email.setText(mUser.getEmail());
        mMainContentView = findViewById(R.id.app_bar_view)
                .findViewById(R.id.main_content_view);

        getDataFromFirebase();

        // Generated code
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, SelectRestaurantActivity.class);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        // Get Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (savedInstanceState != null) {
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
    }

    private void getDataFromFirebase() {
        // Set main view to be invisible
        findViewById(R.id.main_layout).setVisibility(View.GONE);

        final TextView foodpoint = mMainContentView.findViewById(R.id.foodpoint);

        // Get food point from database
        mDatabase.child("users").child(mUid).child("foodpoint").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    String fpText = "Foodpoint Balance: $" + new DecimalFormat("##.##").format(Double.parseDouble(snapshot.getValue().toString()));
                    foodpoint.setText(fpText);
                } else {
                    String fpText = "Click \"Modify\" to enter current foodpoint";
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Populate meals
        RecyclerView rv = findViewById(R.id.main_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        rv.setLayoutManager(linearLayoutManager);
        mMeals = new ArrayList<>();
        // Get data from firebase
        getMealsFromFirebase(true);
        mAdapter = new MealAdapter(this, mMeals, false);
        rv.setAdapter(mAdapter);
        rv.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                addDataToList();
            }
        });
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();


        // Change zoom level
        LatLng duke = new LatLng(36.0014258, -78.9404173);
        googleMap.addMarker(new MarkerOptions().position(duke)
                .title("Duke Chapel"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(duke));

        // Add restaurants here
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));
        LatLng panda = new LatLng(36.00042, -78.940367);
        googleMap.addMarker(new MarkerOptions().position(panda)
                .title("Panda Express"));
        LatLng wu = new LatLng(35.9982508, -78.9396322);
        googleMap.addMarker(new MarkerOptions().position(wu)
                .title("West Union"));

    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d("TAG", "Current location is null. Using defaults.");
                            Log.e("TAG", "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (mIsRequestingLocationPermission) {
            return;
        }
        // Prevent multiple requests
        mIsRequestingLocationPermission = true;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    private void addDataToList() {
        if (allDataLoaded) {
            return;
        }
        mMainContentView.findViewById(R.id.item_progress_bar).setVisibility(View.VISIBLE);
        getMealsFromFirebase(false);
    }

    private void getMealsFromFirebase(boolean isFirstTime) {
        final boolean isFirst = isFirstTime;
        final FirebaseStorage storage = FirebaseStorage.getInstance();
        Query q = mDatabase.child("users").child(mUid).child("entries")
                .orderByChild("timestamp").limitToFirst(30);
        if (!isFirst) {
            q = mDatabase.child("users").child(mUid).child("entries")
                    .orderByChild("timestamp").limitToFirst(mPageLimit).startAt(mPageOffset);
        }

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot meal : dataSnapshot.getChildren()) {
                        mCount++;
                    }
                    final int totalCount = mCount;
                    for (DataSnapshot meal : dataSnapshot.getChildren()) {
                        final String name = meal.child("meal").getValue().toString();
                        final String restaurant = meal.child("restaurant").getValue().toString();
                        final float calories = Float.parseFloat(meal.child("calories").getValue().toString());
                        final float foodpoints = Float.parseFloat(meal.child("foodpoint").getValue().toString());
                        final String imgUrl = meal.child("image").getValue().toString();
                        mPageOffset = Long.parseLong(meal.child("timestamp").getValue().toString()) + 1;

                        // load image
                        StorageReference gsReference = storage.getReferenceFromUrl(imgUrl);
                        final MealEntry m = new MealEntry(foodpoints, calories, restaurant, name, null);
                        mMeals.add(m);
                        gsReference.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                m.setImg(image);
                                mCount--;
                                if (mCount == 0) {
                                    if (isFirst) {
                                        // Finish loading, display contents
                                        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                                        findViewById(R.id.loading_panel).setVisibility(View.GONE);
                                        if (totalCount < 30) {
                                            allDataLoaded = true;
                                        }
                                    } else {
                                        mAdapter.notifyDataSetChanged();
                                        mMainContentView.findViewById(R.id.item_progress_bar).setVisibility(View.GONE);
                                        if (totalCount < mPageLimit) {
                                            allDataLoaded = true;
                                            Toast toast = Toast.makeText(mContext, "All meals are loaded", Toast.LENGTH_SHORT);
                                            toast.show();
                                        }
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast toast = Toast.makeText(mContext, "An error occured, please try again later", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        });
                    }
                } else {
                    // Finish loading, display contents
                    if (isFirst) {
                        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.loading_panel).setVisibility(View.GONE);
                    } else {
                        mAdapter.notifyDataSetChanged();
                        mMainContentView.findViewById(R.id.item_progress_bar).setVisibility(View.GONE);
                        allDataLoaded = true;
                        Toast toast = Toast.makeText(mContext, "All meals are loaded", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("TAG", databaseError.toString());

            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_signout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            // user is now signed out
                            startActivity(new Intent(getApplicationContext(), StartActivity.class));
                            finish();
                        }
                    });
        } else if (id == R.id.nav_manage) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_moments) {
            Intent intent = new Intent(this, MomentActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onModifyClick(View view) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    public void onHistoryClick(View view) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    public void onCaloriesClick(View view) {
        Intent intent = new Intent(this, CaloriesActivity.class);
        startActivity(intent);
    }

}
