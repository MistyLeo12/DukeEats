package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CustomMealActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_meal);
        final EditText nameInput = findViewById(R.id.name_field);
        final EditText fpInput = findViewById(R.id.fp_field);
        final EditText caloriesInput = findViewById(R.id.calories_field);
        Button button = findViewById(R.id.profile_save_button);
        final Context mContext = this;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Validate form
                if (nameInput.getText().toString().equals("")) {
                    Toast toast = Toast.makeText(mContext, "Name cannot be empty", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (fpInput.getText().toString().equals("")) {
                    Toast toast = Toast.makeText(mContext, "Price cannot be empty", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (caloriesInput.getText().toString().equals("")) {
                    Toast toast = Toast.makeText(mContext, "Calories cannot be empty", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                try {
                    double d = Double.parseDouble(fpInput.getText().toString());
                } catch (NumberFormatException nfe) {
                    Toast toast = Toast.makeText(mContext, "Please enter a valid number for price", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (!caloriesInput.getText().toString().equals("")) {
                    try {
                        double d = Double.parseDouble(caloriesInput.getText().toString());
                    } catch (NumberFormatException nfe) {
                        Toast toast = Toast.makeText(mContext, "Please enter a valid number for calories", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                }

                // Save meal
                String name = nameInput.getText().toString();
                float price = Float.parseFloat(fpInput.getText().toString());
                float calories = Float.parseFloat(caloriesInput.getText().toString());

                MealEntry m = new MealEntry(price, calories, "", name);
                m.saveEntryToFirebase(MainActivity.class, mContext);
            }
        });
    }
}
