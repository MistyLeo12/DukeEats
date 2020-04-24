package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateMomentActivity extends AppCompatActivity {

    private MomentDatabase mDatabase;
    private Context mContext;
    private String mUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_moment);
        final EditText nameInput = findViewById(R.id.name_field);
        final String uri = getIntent().getExtras().getString("uri");
        Button button = findViewById(R.id.save_button);
        mContext = this;
        mDatabase = MomentDatabase.getInMemoryDatabase(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validate form
                String text = nameInput.getText().toString();
                if (text.equals("")) {
                    text = "Moment";
                }
                if (text.length() > 50) {
                    Toast toast = Toast.makeText(mContext, "Please limit to less than 50 characters", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                final String t = text;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Moment moment = new Moment();
                        moment.setName(t);
                        SimpleDateFormat d = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                        String currentDate = d.format(new Date());
                        moment.setDate(currentDate);
                        moment.setFirebaseId(mUid);
                        moment.setImageUrl(uri);
                        moment.setLocation("/");
                        mDatabase.momentDao().insert(moment);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Moment added", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(mContext, MomentActivity.class);
                                startActivity(intent);

                            }
                        });
                    }
                }).start();
            }
        });
    }
}
