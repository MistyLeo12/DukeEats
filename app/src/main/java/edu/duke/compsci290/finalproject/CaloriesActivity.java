package edu.duke.compsci290.finalproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryApi;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class CaloriesActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
    private String mUid = mUser.getUid();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private Context mContext = this;
    private GoogleApiClient mApiClient;
    private float mCaloriesTotal;
    private ArrayList<BarEntry> mEntries = new ArrayList<>();
    private String[] mLabelStrs;
    private ConnectionResult mConnectionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calories);
        findViewById(R.id.main_layout).setVisibility(View.GONE);

        buildAPIclient();
        mApiClient.connect();

        // Get data from firebase
        getCaloriesFromFirebase();
    }

    private synchronized void buildAPIclient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .useDefaultAccount().build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("HERE", "onConnected: here");
        getGoogleFitData();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("HERE", "onSuspended: here");

    }

    public void getGoogleFitData() {
        if (mApiClient != null && mApiClient.isConnected() && mApiClient.hasConnectedApi(Fitness.HISTORY_API)) {

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            long currtime = cal.getTimeInMillis();

            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startTime = cal.getTimeInMillis();

            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                    .bucketByActivitySegment(1, TimeUnit.MILLISECONDS)
                    .setTimeRange(startTime, currtime, TimeUnit.MILLISECONDS)
                    .build();

            new CaloriesAsyncTask(readRequest, mApiClient).execute();
        } else {
            Log.d("gfitAPI", "Not connected");
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("FAIL", connectionResult.toString());

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, 8);
            } catch (IntentSender.SendIntentException e) {
                mApiClient.connect();
            }
        }

        mConnectionResult = connectionResult;
    }


    public class CaloriesAsyncTask extends AsyncTask<Void, Void, Void> {
        DataReadRequest readRequest;
        GoogleApiClient mApiClient = null;
        public float mCalories = 0;

        CaloriesAsyncTask(DataReadRequest dataReadRequest, GoogleApiClient googleApiClient) {
            this.readRequest = dataReadRequest;
            this.mApiClient = googleApiClient;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mApiClient, readRequest).await(1, TimeUnit.MINUTES);

            for (Bucket bucket : dataReadResult.getBuckets()) {

                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.WALKING) || bucketActivity.contains(FitnessActivities.RUNNING)) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {

                        if (dataSet.getDataType().getName().equals("com.google.calories.expended")) {

                            for (DataPoint dp : dataSet.getDataPoints()) {

                                if (dp.getEndTime(TimeUnit.MILLISECONDS) > dp.getStartTime(TimeUnit.MILLISECONDS)) {
                                    for (Field field : dp.getDataType().getFields()) {
                                        // total calories burned
                                        mCalories = mCalories + dp.getValue(field).asFloat();
                                    }

                                }

                            }

                        }

                    }
                }


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void dataReadResult) {
            super.onPostExecute(dataReadResult);

            Log.e("GoogleFit", "Total cal is " + mCalories);
            int finalcalories = (int) mCalories;
            TextView tcal = findViewById(R.id.calories_burned_today);
            tcal.setText(Integer.toString(finalcalories));
        }
    }


    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 8) {
                mApiClient.connect();
            }
        }
    }


    private void getCaloriesFromFirebase() {
        final DatabaseReference caloriesRef = mDatabase.child("users").child(mUid).child("calories");
        caloriesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.loading_panel).setVisibility(View.GONE);
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.main_layout),
                            "We don't have any data about your calories intake. Start recording your meals to start tracking calories!", Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    return;
                }
                double totalCalories = Double.parseDouble(dataSnapshot.child("total").getValue().toString());

                TextView t1 = findViewById(R.id.average_calories);
                TextView t2 = findViewById(R.id.average_target);
                TextView t3 = findViewById(R.id.average_diff_text);
                TextView t4 = findViewById(R.id.today_calories);
                TextView t5 = findViewById(R.id.today_target);
                TextView t6 = findViewById(R.id.today_diff_text);

                // Average does not count today
                double current = 0;

                SimpleDateFormat d = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                String formattedDate = d.format(new Date());
                long dayCount = dataSnapshot.child("entries").getChildrenCount();


                // Get daily calories entry
                int i = 0;
                ArrayList<String> labels = new ArrayList<>();
                for (DataSnapshot entry : dataSnapshot.child("entries").getChildren()) {
                    double calories = Double.parseDouble(entry.child("calories").getValue().toString());
                    String dates = entry.getKey();
                    String formattedDates = dates.split("-")[0] + "-" + dates.split("-")[1];
                    mEntries.add(new BarEntry(i, (float) calories));
                    labels.add(formattedDates);
                    i++;
                }
                BarChart chart = (BarChart) findViewById(R.id.chart);
                // the labels that should be drawn on the XAxis
                mLabelStrs = new String[labels.size()];
                mLabelStrs = labels.toArray(mLabelStrs);

                IAxisValueFormatter formatter = new IAxisValueFormatter() {

                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        return mLabelStrs[(int) value];
                    }

                };

                XAxis xAxis = chart.getXAxis();
                xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
                xAxis.setValueFormatter(formatter);

                BarDataSet dataset = new BarDataSet(mEntries, "Calories");
                dataset.setColor(Color.parseColor("#7B1FA2"));
                dataset.setValueTextSize(10);
                BarData data = new BarData(dataset);
                data.setBarWidth(0.9f); // set custom bar width
                chart.setData(data);
                chart.animateY(1000, Easing.EasingOption.EaseOutBack);
                chart.setFitBars(true); // make the x-axis fit exactly all bars
                chart.getDescription().setText("Your daily calories intake");
                chart.getDescription().setTextSize(10);
                chart.invalidate(); // refresh

                if (dataSnapshot.child("entries").hasChild(formattedDate)) {
                    // Get current date's calories
                    // Average does not count today
                    dayCount -= 1;
                    current = Double.parseDouble(dataSnapshot.child("entries").child(formattedDate).child("calories").getValue().toString());
                    totalCalories -= current;
                }

                double average = 0;
                if (dayCount > 0) {
                    average = totalCalories / dayCount;
                } else {

                    Snackbar snackbar = Snackbar.make(findViewById(R.id.main_layout),
                            "We do not include today in calculation of the average calories, so the average calories is 0 right now.", Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                }

                t1.setText(Integer.toString((int) average));
                t4.setText(Integer.toString((int) current));
                if (dataSnapshot.child("caloriesneed").exists()) {

                    double caloriesTarget = Double.parseDouble(dataSnapshot.child("caloriesneed").getValue().toString());
                    String targetText = "Daily target: " + (int) caloriesTarget;
                    t2.setText(targetText);
                    t5.setText(targetText);

                    double averageDiff = average - caloriesTarget;
                    double averagePercentage = 100 * averageDiff / caloriesTarget;
                    if (averageDiff > 0) {
                        String averageDiffText = (int) averageDiff + " (" + (int) averagePercentage + "%) above target";
                        t3.setText(averageDiffText);
                    } else {
                        String averageDiffText = (-(int) averageDiff) + " (" + (-(int) averagePercentage) + "%) below target";
                        t3.setText(averageDiffText);
                    }

                    double currentDiff = current - caloriesTarget;
                    double currentpercentage = 100 * currentDiff / caloriesTarget;
                    if (currentDiff > 0) {
                        String currentDiffText = ((int) currentDiff) + " (" + (int) currentpercentage + "%) above target";
                        t6.setText(currentDiffText);
                    } else {
                        String currentDiffText = (-(int) currentDiff) + " (" + (-(int) currentpercentage) + "%) below target";
                        t6.setText(currentDiffText);
                    }
                } else {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.main_layout),
                            "Enter your daily calories target to get more information.", Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("GO", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, ProfileActivity.class);
                            startActivity(intent);
                        }
                    });
                    snackbar.show();
                }

                // Finished loading, show contents
                findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.loading_panel).setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}

