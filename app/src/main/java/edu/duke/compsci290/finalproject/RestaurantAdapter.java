package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by isaac on 4/15/18.
 */

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Map<String, Object>> mRestaurants;

    public RestaurantAdapter(final Context context, ArrayList<Map<String, Object>> restaurants) {
        this.mContext = context;
        this.mRestaurants = restaurants;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mName;
        TextView mLocation;
        TextView mLocationShort;
        LinearLayout mHolder;
        ImageView mImg;

        public ViewHolder(View itemView) {
            super(itemView);
            this.mName = itemView.findViewById(R.id.restaurant_name);
            this.mLocation = itemView.findViewById(R.id.restaurant_location);
            this.mLocationShort = itemView.findViewById(R.id.location_short);
            this.mImg = itemView.findViewById(R.id.restaurant_img);
            this.mHolder = itemView.findViewById(R.id.restaurant_holder);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = mInflater.inflate(R.layout.restaurant_holder, parent, false);
        final ViewHolder quizHolder = new ViewHolder(row);
        quizHolder.mHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(mContext, SelectDishActivity.class);
                i.putExtra("restaurant", (String)mRestaurants.get(quizHolder.getAdapterPosition()).get("name"));
                mContext.startActivity(i);
            }
        });

        return quizHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mName.setText(mRestaurants.get(position).get("name").toString());
        holder.mLocation.setText(mRestaurants.get(position).get("location").toString());
        holder.mLocationShort.setText(mRestaurants.get(position).get("shortlocation").toString());
        holder.mImg.setImageBitmap((Bitmap) mRestaurants.get(position).get("image"));
    }

    @Override
    public int getItemCount() {
        return mRestaurants.size();
    }
}
