package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {
    private boolean mIsLoading = false;
    private float mCurrentFp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Button button = findViewById(R.id.profile_save_button);
        final EditText nameInput = findViewById(R.id.name_field);
        final EditText fpInput = findViewById(R.id.fp_field);
        final EditText caloriesInput = findViewById(R.id.calories_field);
        final Context mContext = this;
        final Intent intent = new Intent(this, MainActivity.class);

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = user.getUid();
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        nameInput.setText(user.getDisplayName());
        database.child("users").child(uid).child("foodpoint").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    fpInput.setText(snapshot.getValue().toString());
                    mCurrentFp = Float.parseFloat(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        database.child("users").child(uid).child("calories").child("caloriesneed").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    caloriesInput.setText(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (nameInput.getText().toString().equals("")) {
                    Toast toast = Toast.makeText(mContext, "Name cannot be empty", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (fpInput.getText().toString().equals("")) {
                    Toast toast = Toast.makeText(mContext, "Foodpoints cannot be empty", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                try {
                    double d = Double.parseDouble(fpInput.getText().toString());
                } catch (NumberFormatException nfe) {
                    Toast toast = Toast.makeText(mContext, "Please enter a valid number for food points", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (!caloriesInput.getText().toString().equals("")) {
                    try {
                        double d = Double.parseDouble(caloriesInput.getText().toString());
                    } catch (NumberFormatException nfe) {
                        Toast toast = Toast.makeText(mContext, "Please enter a valid number for daily calories target", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                }
                // Save to database
                if (mIsLoading) {
                    return;
                }
                mIsLoading = true;
                float newFp = Float.parseFloat(fpInput.getText().toString());
                float diff = newFp - mCurrentFp;
                final DatabaseReference foodpoint = database.child("users").child(uid).child("foodpoint");

                // Save transaction
                String purpose = "Manually withdrew fund";
                if (diff > 0) {
                    purpose = "Manually added fund";
                }
                Transaction t = new Transaction(diff, purpose);
                t.saveTransactionToFirebase();

                // Save foodpoints
                foodpoint.setValue(newFp);

                final DatabaseReference caloriesRef = database.child("users").child(uid).child("calories").child("caloriesneed");
                // Update calories need
                if(caloriesInput.getText().toString().length() > 0){
                    float calories = Float.parseFloat(caloriesInput.getText().toString());
                    caloriesRef.setValue(calories);
                }

                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameInput.getText().toString())
                        .build();
                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast toast = Toast.makeText(mContext, "Profile successfully updated", Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    Toast toast = Toast.makeText(mContext, "Profile update failed, please try again later", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                startActivity(intent);
                            }
                        });
            }
        });
    }
}
