package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import static android.content.ContentValues.TAG;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MealEntry {
    private static String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private static DatabaseReference database = FirebaseDatabase.getInstance().getReference();

    private float mFp;
    private float mCalories;
    private Date mTime;
    private String mRestaurant;
    private String mMeal;
    private String mImgURL;
    private Bitmap mImage;
    private boolean finishedOneSave;

    // For select dish activity
    public MealEntry(float fp, float calories, String restaurant, String meal, String imgurl, Bitmap image) {
        mFp = fp;
        mRestaurant = restaurant;
        mMeal = meal;
        mCalories = calories;
        mImgURL = imgurl;
        mImage = image;

        // Save the time when the entry is created
        mTime = Calendar.getInstance().getTime();
    }

    // For main activity
    public MealEntry(float fp, float calories, String restaurant, String meal, Bitmap image) {
        mFp = fp;
        mRestaurant = restaurant;
        mMeal = meal;
        mCalories = calories;
        mImage = image;
    }

    // For custom meal
    public MealEntry(float fp, float calories, String restaurant, String meal) {
        mFp = fp;
        mRestaurant = restaurant;
        mMeal = meal;
        mCalories = calories;
        mImgURL = "gs://dukeworkout.appspot.com/restaurants/costofmeal.png";

        // Save the time when the entry is created
        mTime = Calendar.getInstance().getTime();
    }

    public String getRestaurant() {
        return mRestaurant;
    }

    public String getMeal() {
        return mMeal;
    }

    public String getImgUrl() {
        return mImgURL;
    }

    public Bitmap getImg() {
        return mImage;
    }

    public void setImg(Bitmap img) {
        mImage = img;
    }

    public float getFoodpoints() {
        return mFp;
    }

    public float getCalories() {
        return mCalories;
    }

    public void saveEntryToFirebase(final Class activity, final Context context) {
        Map<String, Object> map = new HashMap<>();
        map.put("foodpoint", mFp);
        map.put("restaurant", mRestaurant);
        map.put("meal", mMeal);
        map.put("calories", mCalories);
        map.put("image", mImgURL);
        map.put("time", mTime.toString());
        map.put("timestamp", -System.currentTimeMillis());
        DatabaseReference entries = database.child("users").child(uid).child("entries");
        DatabaseReference pushedRef = entries.push();
        pushedRef.setValue(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    Log.d(TAG, error.getMessage());
                    Toast toast = Toast.makeText(context, "Data could not be saved, please try later", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    if (finishedOneSave) {
                        Toast toast = Toast.makeText(context, "Successfully saved", Toast.LENGTH_SHORT);
                        toast.show();
                        // Redirect to home screen
                        Intent i = new Intent(context, activity);
                        context.startActivity(i);
                    }
                    finishedOneSave = true;
                }
            }
        });
        // Modify foodpoints
        final DatabaseReference foodpoint = database.child("users").child(uid).child("foodpoint");
        foodpoint.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Double fp = Double.parseDouble(new DecimalFormat("##.##").format(Double.parseDouble(dataSnapshot.getValue().toString())));
                    fp -= mFp;
                    foodpoint.setValue(fp);
                }
                if (finishedOneSave) {
                    Toast toast = Toast.makeText(context, "Successfully saved", Toast.LENGTH_SHORT);
                    toast.show();
                    // Redirect to home screen
                    Intent i = new Intent(context, activity);
                    context.startActivity(i);
                }
                finishedOneSave = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // Save transaction
        Transaction t = new Transaction(-mFp, mMeal);
        t.saveTransactionToFirebase();
        CaloriesEntry c = new CaloriesEntry(mCalories);
        c.updateCaloriesToFirebase();
    }
}
