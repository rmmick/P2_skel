package com.example.solution_color;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.library.bitmap_utilities.BitMap_Helpers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity  {

    static final int REQUEST_TAKE_PHOTO = 1;
    private Uri photoURI = null;
    private Uri photo;
    private String mCurrentPhotoPath;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        image = (ImageView) findViewById(R.id.camera_pic);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Toast.makeText(this, "Settings goes here", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.reset:
                doReset();
                return true;

            case R.id.share:
                doSend();
                //Toast.makeText(this, "Sharing goes here", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.sketch:
                image.setImageBitmap(BitMap_Helpers.thresholdBmp(((BitmapDrawable)image.getDrawable()).getBitmap(), 10));
                //image.setImageBitmap(BitMap_Helpers.colorBmp(((BitmapDrawable)image.getDrawable()).getBitmap(), 10));
                return true;

            case R.id.color:
                Bitmap bw = BitMap_Helpers.thresholdBmp(((BitmapDrawable)image.getDrawable()).getBitmap(), 5);
                Bitmap colored = BitMap_Helpers.colorBmp(bw, 10);
                BitMap_Helpers.merge(colored, bw);
                image.setImageBitmap(colored);
                return true;
            default:
                break;
        }
        return true;
    }

    private void doSend() {

        if(photo != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, photo);
            shareIntent.setType("image/jpeg");
            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } else {
            Toast.makeText(this, "No Photo Saved", Toast.LENGTH_SHORT).show();
        }

    }

    private void doHelp() {
        Toast.makeText(this, "Help goes here", Toast.LENGTH_SHORT).show();
    }

    private void doReset() {
        image.setImageResource(R.drawable.gutters);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        Camera_Helpers.delSavedImage(mCurrentPhotoPath);
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

    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        /*File file = new File(Environment.getExternalStorageDirectory(), "implicit.jpg");
        outputFileUri = Uri.fromFile(file);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);*/

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
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void takepicture(int resultCode) {
        if (resultCode == RESULT_OK) {
            //setPic();
            int targetW = image.getWidth();
            int targetH = image.getHeight();
            Bitmap bp = Camera_Helpers.loadAndScaleImage(mCurrentPhotoPath, targetW, targetH);
            photo = getImageUri(getApplicationContext(), bp);
            image.setImageBitmap(bp);
            Camera_Helpers.saveProcessedImage(bp, mCurrentPhotoPath);

            //lets get rid of the image so we dont hog memory
            File file = new File(mCurrentPhotoPath);
            boolean deleted = file.delete();
        } else {
            Toast.makeText(this, "Error Occured", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case (REQUEST_TAKE_PHOTO):
                takepicture(resultCode);
                break;
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    /*public void setPic() {
        // Get the dimensions of the View
        int targetW = image.getWidth();
        int targetH = image.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        image.setImageBitmap(bitmap);
    }*/
}

