package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class SelectDishActivity extends AppCompatActivity {

    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private Context mContext = this;
    private ArrayList<MealEntry> mMeals;
    private ArrayList<MealEntry> mMealsCopy;
    private MealAdapter mAdapter;
    private int mCount = 0;
    private boolean mSearch = false;
    private static final long FIVE_MEGABYTE = 1024 * 1024 * 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dish);
        Intent receivedIntent = this.getIntent();
        final String restaurant = receivedIntent.getStringExtra("restaurant");
        findViewById(R.id.main_layout).setVisibility(View.GONE);

        configSearchbar();

        // Populate dishes
        RecyclerView rv = findViewById(R.id.dish_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(SelectDishActivity.this);
        rv.setLayoutManager(linearLayoutManager);
        mMeals = new ArrayList<>();
        mAdapter = new MealAdapter(this, mMeals, true);
        rv.setAdapter(mAdapter);
        final FirebaseStorage storage = FirebaseStorage.getInstance();

        mDatabase.child("restaurants").child(restaurant).child("entries").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot dish : snapshot.getChildren()) {
                    mCount++;
                }
                for (DataSnapshot dish : snapshot.getChildren()) {
                    final String name = dish.getKey();
                    final float calories = Float.parseFloat(dish.child("calories").getValue().toString());
                    final float foodpoints = Float.parseFloat(dish.child("foodpoints").getValue().toString());
                    final String img_url = dish.child("image").getValue().toString();
                    StorageReference gsReference = storage.getReferenceFromUrl(img_url);
                    gsReference.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            MealEntry m = new MealEntry(foodpoints, calories, restaurant, name, img_url, image);
                            mMeals.add(m);
                            mCount--;
                            if (mCount == 0) {
                                // Finish loading, display contents
                                mMealsCopy = new ArrayList<>();
                                mMealsCopy.addAll(mMeals);
                                findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                                findViewById(R.id.loading_panel).setVisibility(View.GONE);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast toast = Toast.makeText(mContext, "Can't load image"+img_url, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    public void onBackClickDish(View view) {
        Intent intent = new Intent(this, SelectRestaurantActivity.class);
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
                    mMeals.clear();
                    mMeals.addAll(mMealsCopy);
                    mAdapter.notifyDataSetChanged();
                } else {
                    mSearch = false;
                }

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                Log.d("LOG_TAG", getClass().getSimpleName() + " text changed " + text);
                mMeals.clear();
                for (MealEntry m : mMealsCopy) {
                    if (m.getMeal().toLowerCase().contains(text.toString().toLowerCase())) {
                        mMeals.add(m);
                    }
                }
                if (mMeals.size() == 0) {
                    Toast.makeText(mContext, "Can't find entries. Displaying all.", Toast.LENGTH_SHORT).show();
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
