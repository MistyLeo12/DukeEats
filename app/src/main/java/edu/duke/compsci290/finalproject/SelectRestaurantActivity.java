package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class SelectRestaurantActivity extends AppCompatActivity {

    private RestaurantAdapter mAdapter;
    private ArrayList<Map<String, Object>> mRestaurants;
    private ArrayList<Map<String, Object>> mRestaurantsCopy;
    private Context mContext;
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private static final long FIVE_MEGABYTE = 1024 * 1024 * 5;
    private int downloadCount = 0;
    private boolean mSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_restaurant);
        mContext = this;
        // Populate restaurants
        RecyclerView rv = findViewById(R.id.restaurant_recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rv.setLayoutManager(gridLayoutManager);

        mRestaurants = new ArrayList<>();
        mAdapter = new RestaurantAdapter(this, mRestaurants);
        rv.setAdapter(mAdapter);
        // Set main view to be invisible
        findViewById(R.id.main_layout).setVisibility(View.GONE);
        final FirebaseStorage storage = FirebaseStorage.getInstance();

        // Add snackbar
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main_layout),
                "Can\' find your restaurant?", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Add Custom Meal", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, CustomMealActivity.class);
                startActivity(intent);
            }
        });
        snackbar.show();

        configSearchbar();

        // Populate restaurants
        Query q = mDatabase.child("restaurants").limitToFirst(30);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get children count
                for (DataSnapshot place : dataSnapshot.getChildren()) {
                    downloadCount++;
                }
                for (DataSnapshot restaurant : dataSnapshot.getChildren()) {
                    String name = restaurant.getKey();
                    String location = restaurant.child("location").getValue().toString();
                    String img_url = restaurant.child("image").getValue().toString();
                    final Map<String, Object> m = new HashMap<>();
                    m.put("name", name);
                    m.put("location",
                            location.split("/")[0]);
                    m.put("shortlocation", location.split("/")[1]);
                    StorageReference gsReference = null;
                    if (restaurant.child("image").getValue() != null) {
                        gsReference = storage.getReferenceFromUrl(img_url);
                    } else {
                        gsReference = storage.getReference().child(constructImgUrl(name));
                    }
                    gsReference.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            m.put("image", image);
                            mRestaurants.add(m);
                            downloadCount--;

                            // Finished downloading all images, display contents
                            if (downloadCount == 0) {
                                findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                                findViewById(R.id.loading_panel).setVisibility(View.GONE);
                                // Copy current restaurants to a new arraylist
                                mRestaurantsCopy = new ArrayList<>();
                                mRestaurantsCopy.addAll(mRestaurants);
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast toast = Toast.makeText(mContext, "An error occured, please try again later", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public String constructImgUrl(String name) {
        return "restaurants/" + name.toLowerCase().replaceAll("\\W+", "") + ".png";
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void configSearchbar() {
        final MaterialSearchBar searchBar = findViewById(R.id.search_bar);
        searchBar.setHint("Search Restaurants");
        searchBar.setSpeechMode(false);

        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                if (!enabled && !mSearch) {
                    mRestaurants.clear();
                    mRestaurants.addAll(mRestaurantsCopy);
                    mAdapter.notifyDataSetChanged();
                } else {
                    mSearch = false;
                }

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                Log.d("LOG_TAG", getClass().getSimpleName() + " text changed " + text);
                mRestaurants.clear();
                for (Map m : mRestaurantsCopy) {
                    if (m.get("name").toString().toLowerCase().contains(text.toString().toLowerCase())) {
                        mRestaurants.add(m);
                    }
                }
                if (mRestaurants.size() == 0) {
                    Toast.makeText(mContext, "Can't find restaurants. Displaying all.", Toast.LENGTH_SHORT).show();
                    searchBar.disableSearch();
                    return;
                }
                mAdapter.notifyDataSetChanged();
                mSearch = true;
                searchBar.disableSearch();
            }

            @Override
            public void onButtonClicked(int buttonCode) {
            }

        });
    }
}
