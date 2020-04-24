package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.ContentValues.TAG;

/**
 * Created by isaac on 4/15/18.
 */

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<MealEntry> mMeals;
    private boolean mSaveMeal;

    public MealAdapter(final Context context, ArrayList<MealEntry> meals, boolean savemeal) {
        this.mContext = context;
        this.mMeals = meals;
        this.mSaveMeal = savemeal;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mName;
        TextView mCalories;
        TextView mPrice;
        ImageView mImg;
        ConstraintLayout mHolder;

        public ViewHolder(View itemView) {
            super(itemView);
            this.mName = itemView.findViewById(R.id.meal_name);
            this.mCalories = itemView.findViewById(R.id.meal_calories);
            this.mPrice = itemView.findViewById(R.id.meal_foodpoints);
            this.mImg = itemView.findViewById(R.id.meal_img);
            this.mHolder = itemView.findViewById(R.id.meal_holder);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = mInflater.inflate(R.layout.meal_holder, parent, false);
        final ViewHolder mealHolder = new ViewHolder(row);
        if (mSaveMeal) {
            mealHolder.mHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mMeals.get(mealHolder.getAdapterPosition()).saveEntryToFirebase(MainActivity.class, mContext);
                }
            });
        }
        return mealHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mName.setText(mMeals.get(position).getMeal());
        String caloriesText = "Calories " + mMeals.get(position).getCalories();
        String priceText = "Price " + mMeals.get(position).getFoodpoints();
        holder.mCalories.setText(caloriesText);
        holder.mPrice.setText(priceText);
        holder.mImg.setImageBitmap((Bitmap) mMeals.get(position).getImg());
    }

    @Override
    public int getItemCount() {
        return mMeals.size();
    }
}
