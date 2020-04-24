package edu.duke.compsci290.finalproject;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by isaac on 4/19/18.
 */

public class CaloriesEntry {
    private static String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private static DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    private static DatabaseReference history = database.child("users").child(uid)
            .child("calories");

    private float mCalories;

    public CaloriesEntry(float calories) {
        mCalories = calories;
    }

    public void updateCaloriesToFirebase() {
        // Push to history db
        SimpleDateFormat d = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        final String formattedDate = d.format(new Date());
        // Set total calories
        history.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("total")) {
                    Float total = Float.parseFloat(dataSnapshot.child("total").getValue().toString());
                    total += mCalories;
                    history.child("total").setValue(total);
                } else {
                    history.child("total").setValue(mCalories);
                }

                // Update calories entries
                updateSpentToday(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void updateSpentToday(DataSnapshot snapshot) {

        SimpleDateFormat d = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        String formattedDate = d.format(new Date());

        // This is not the first meal today
        if (snapshot.hasChild("entries") && snapshot.child("entries").hasChild(formattedDate)) {
            float total = Float.parseFloat(snapshot.child("entries").child(formattedDate).child("calories").getValue().toString());
            total += mCalories;
            history.child("entries").child(formattedDate).child("calories").setValue(total);
        } else {
            // Create new entry for today
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", System.currentTimeMillis());
            map.put("calories", mCalories);
            history.child("entries").child(formattedDate).setValue(map);
        }
    }

}
