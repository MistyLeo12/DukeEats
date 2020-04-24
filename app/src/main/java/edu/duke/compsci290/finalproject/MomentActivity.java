package edu.duke.compsci290.finalproject;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MomentActivity extends AppCompatActivity {

    private MomentDatabase mDatabase;
    private ArrayList<Moment> mMoments;
    private MomentAdapter mAdapter;
    private String mUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String mCurrentPhotoPath;
    private Uri mPhotoURI;
    private Context mContext = this;
    final private static int REQUEST_IMAGE_CAPTURE = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            galleryAddPic();
            Bundle extras = data.getExtras();
            Intent intent = new Intent(this, CreateMomentActivity.class);
            intent.putExtra("uri",mPhotoURI.toString());
            startActivity(intent);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        mPhotoURI = Uri.fromFile(f);
        mediaScanIntent.setData(mPhotoURI);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moment);
        findViewById(R.id.main_layout).setVisibility(View.GONE);

        RecyclerView rv = findViewById(R.id.moment_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MomentActivity.this);
        rv.setLayoutManager(linearLayoutManager);
        mMoments = new ArrayList<>();
        mAdapter = new MomentAdapter(this, mMoments);
        rv.setAdapter(mAdapter);

        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.add_moment);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(mContext,
                                "edu.duke.compsci290.finalproject.fileprovider",
                                photoFile);
                        Log.d("s",photoURI.toString());
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });


        mDatabase = MomentDatabase.getInMemoryDatabase(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Moment> moments = mDatabase.momentDao().getAllByFirebaseId(mUid);
                Collections.reverse(moments);
                mMoments.addAll(moments);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.loading_panel).setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
