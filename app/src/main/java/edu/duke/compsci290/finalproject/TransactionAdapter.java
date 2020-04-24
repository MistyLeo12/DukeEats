package edu.duke.compsci290.finalproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by isaac on 4/15/18.
 */

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Map<String, Object>> mTransactions;

    public TransactionAdapter(final Context context, ArrayList<Map<String, Object>> mTransactions) {
        this.mContext = context;
        this.mTransactions = mTransactions;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mAmount;
        TextView mPurpose;
        TextView mTime;

        public ViewHolder(View itemView) {
            super(itemView);
            this.mAmount = itemView.findViewById(R.id.transaction);
            this.mPurpose = itemView.findViewById(R.id.purpose);
            this.mTime = itemView.findViewById(R.id.time);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = mInflater.inflate(R.layout.transaction_holder, parent, false);
        final ViewHolder mealHolder = new ViewHolder(row);
        return mealHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        float amount = (float) mTransactions.get(position).get("amount");
        String amountStr = "";
        if (amount >= 0) {
            amountStr = "+$" + Float.toString(amount);
            holder.mAmount.setTextColor(Color.GREEN);
        } else {
            amountStr = "-$" + Float.toString(-amount);
            holder.mAmount.setTextColor(Color.RED);
        }
        holder.mAmount.setText(amountStr);
        holder.mPurpose.setText((String) mTransactions.get(position).get("purpose"));
        holder.mTime.setText((String) mTransactions.get(position).get("time"));
    }

    @Override
    public int getItemCount() {
        return mTransactions.size();
    }
}
