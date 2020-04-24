package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by isaac on 4/15/18.
 */

public class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Moment> mMoments;

    public MomentAdapter(final Context context, ArrayList<Moment> mMoments) {
        this.mContext = context;
        this.mMoments = mMoments;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mDate;
        TextView mText;
        ImageView mImg;
        Button mDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            this.mText = itemView.findViewById(R.id.moment_text);
            this.mDate = itemView.findViewById(R.id.moment_time);
            this.mImg = itemView.findViewById(R.id.moment_img);
            this.mDelete = itemView.findViewById(R.id.delete_button);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = mInflater.inflate(R.layout.moment_holder, parent, false);
        final ViewHolder holder = new ViewHolder(row);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.mText.setText(mMoments.get(position).getName());
        holder.mDate.setText(mMoments.get(position).getDate());
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MomentDatabase.getInMemoryDatabase(mContext).momentDao().delete(mMoments.get(position));
                        ((MomentActivity)mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMoments.remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(mContext, "Moment deleted", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
        });
        try {
            Uri uri = Uri.parse(mMoments.get(position).getImageUrl());
            Bitmap bitmap = BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri));
            holder.mImg.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return mMoments.size();
    }

}
