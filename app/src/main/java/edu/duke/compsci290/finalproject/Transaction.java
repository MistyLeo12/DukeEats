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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class Transaction {
    private static String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private static DatabaseReference database = FirebaseDatabase.getInstance().getReference();

    private float mFp;
    private Date mTime;
    private int mDay;
    private int mMonth;
    private String mPurpose;

    public Transaction(float fp, String purpose) {
        mFp = fp;
        mPurpose = purpose;
        mTime = Calendar.getInstance().getTime();
        mDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        mMonth = Calendar.getInstance().get(Calendar.MONTH);
    }

    public void saveTransactionToFirebase() {
        final Map<String, Object> map = new HashMap<>();
        map.put("amount", mFp);
        map.put("purpose", mPurpose);
        map.put("time", mTime.toString());
        map.put("timestamp", -System.currentTimeMillis());
        DatabaseReference entries = database.child("users").child(uid)
                .child("transactions");
        DatabaseReference pushedRef = entries.push();
        pushedRef.setValue(map);

        // Push to history db
        SimpleDateFormat d = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        final String formattedDate = d.format(new Date());
        final DatabaseReference history = database.child("users").child(uid)
                .child("transactionhistory");
        // If maunally modifiy fund, no need to save total spent
        if (mPurpose.equals("Manually withdrew fund") || mPurpose.equals("Manually added fund")) {
            return;
        }
        // Set total spent
        history.child("total").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    history.child("total").setValue(-mFp);
                } else {
                    Float total = Float.parseFloat(dataSnapshot.getValue().toString());
                    total -= mFp;
                    history.child("total").setValue(total);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        // Retrieve info about last date saved
        history.child("current").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    history.child("current").setValue(formattedDate);
                    updateDays();
                    updateSpentToday(true);
                } else {
                    String curr = dataSnapshot.getValue().toString();
                    // First time saving a record today
                    if (!curr.equals(formattedDate)) {
                        history.child("current").setValue(formattedDate);
                        updateDays();
                        updateSpentToday(true);
                    } else {
                        updateSpentToday(false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


    }

    private void updateDays() {
        final DatabaseReference ref = database.child("users").child(uid)
                .child("transactionhistory").child("days");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    ref.setValue(1);
                } else {
                    int total = Integer.parseInt(dataSnapshot.getValue().toString());
                    total += 1;
                    ref.setValue(total);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void updateSpentToday(final boolean isFirstTime) {
        final DatabaseReference ref = database.child("users").child(uid)
                .child("transactionhistory").child("today");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    ref.setValue(-mFp);
                } else {
                    if (!isFirstTime) {
                        float total = Float.parseFloat(dataSnapshot.getValue().toString());
                        total -= mFp;
                        ref.setValue(total);
                    } else {
                        ref.setValue(-mFp);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }
}
