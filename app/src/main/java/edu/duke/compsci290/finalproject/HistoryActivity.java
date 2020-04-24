package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {
    private FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
    private String mUid = mUser.getUid();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ArrayList<Map<String, Object>> mTransactions;
    private TransactionAdapter mAdapter;
    private long mPageOffset = 0;
    private int mPageLimit = 10;
    private int mCount = 0;
    private boolean allDataLoaded = false;
    private boolean mIsFirst = true;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mContext = this;
        // Populate transactions
        RecyclerView rv = findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        rv.setLayoutManager(linearLayoutManager);
        mTransactions = new ArrayList<>();
        findViewById(R.id.main_layout).setVisibility(View.GONE);

        // Get data from firebase
        getTransactionInfoFromFirebase();
        getTransactionsFromFirebase();
        mAdapter = new TransactionAdapter(this, mTransactions);
        rv.setAdapter(mAdapter);
        rv.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                mIsFirst = false;
                addDataToList();
            }
        });
    }

    private void addDataToList() {
        if (allDataLoaded) {
            return;
        }
        findViewById(R.id.item_progress_bar).setVisibility(View.VISIBLE);
        getTransactionsFromFirebase();
    }

    private void getTransactionInfoFromFirebase() {
        final DatabaseReference fp = mDatabase.child("users").child(mUid).child("foodpoint");
        fp.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    return;
                }
                double balance = Double.parseDouble(dataSnapshot.getValue().toString());
                TextView t = findViewById(R.id.text1);
                String text = "Current balance: $" + (int) balance;
                t.setText(text);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        final DatabaseReference storage = mDatabase.child("users").child(mUid).child("transactionhistory");
        storage.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TextView t2 = findViewById(R.id.text2);
                TextView t3 = findViewById(R.id.text3);
                if (!dataSnapshot.exists()) {
                    t2.setText("Average spent per day: $0");
                    t3.setText("Amount spent today: $0");
                    return;
                }
                int days = Integer.parseInt(dataSnapshot.child("days").getValue().toString());
                String databaseDate = dataSnapshot.child("current").getValue().toString();
                SimpleDateFormat d = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                final String currentDate = d.format(new Date());
                double amountToday = Double.parseDouble(dataSnapshot.child("today").getValue().toString());
                double amountTotal = Double.parseDouble(dataSnapshot.child("total").getValue().toString());

                // If date is not current, spent $0
                if(!databaseDate.equals(currentDate)){
                    double average = amountTotal / days;
                    String t2_t = "Average spent per day: $" + (int) average;
                    String t3_t = "Amount spent today: $0";
                    t2.setText(t2_t);
                    t3.setText(t3_t);
                } else {
                    double average = (amountTotal - amountToday) / days;
                    String t2_t = "Average spent per day: $" + (int) average;
                    String t3_t = "Amount spent today: $" + (int) amountToday;
                    t2.setText(t2_t);
                    t3.setText(t3_t);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getTransactionsFromFirebase() {
        Query q = mDatabase.child("users").child(mUid).child("transactions")
                .orderByChild("timestamp").limitToFirst(30);
        if (!mIsFirst) {
            q = mDatabase.child("users").child(mUid).child("transactions")
                    .orderByChild("timestamp").limitToFirst(mPageLimit).startAt(mPageOffset);
        }
        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot transaction : dataSnapshot.getChildren()) {
                        mCount++;
                    }
                    final int totalCount = mCount;
                    for (DataSnapshot transaction : dataSnapshot.getChildren()) {
                        final String purpose = transaction.child("purpose").getValue().toString();
                        final float amount = Float.parseFloat(transaction.child("amount").getValue().toString());
                        final String time = transaction.child("time").getValue().toString();
                        mPageOffset = Long.parseLong(transaction.child("timestamp").getValue().toString()) + 1;
                        Map<String, Object> m = new HashMap<>();
                        m.put("purpose", purpose);
                        m.put("amount", amount);
                        m.put("time", time);
                        mTransactions.add(m);
                        mCount--;
                        if (mCount == 0) {
                            if (mIsFirst) {
                                if (totalCount < 30) {
                                    allDataLoaded = true;
                                }
                                mAdapter.notifyDataSetChanged();
                                findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                                findViewById(R.id.loading_panel).setVisibility(View.GONE);
                            } else {
                                if (totalCount < mPageLimit) {
                                    allDataLoaded = true;
                                    Toast toast = Toast.makeText(mContext, "All transactions are loaded", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                mAdapter.notifyDataSetChanged();
                                findViewById(R.id.item_progress_bar).setVisibility(View.GONE);
                            }
                        }
                    }
                } else {
                    // Finish loading, display contents
                    if (mIsFirst) {
                        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.loading_panel).setVisibility(View.GONE);
                    } else {
                        mAdapter.notifyDataSetChanged();
                        findViewById(R.id.item_progress_bar).setVisibility(View.GONE);
                        allDataLoaded = true;
                        Toast toast = Toast.makeText(mContext, "All transactions are loaded", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
