package edu.hm.Compressor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.*;
import java.net.Socket;

public class MyActivity extends Activity {

    public Bitmap mBitmap;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
            }
        });

        ((Button) findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBitmap(mBitmap);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && data != null) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                String imagePath = getPath(selectedImageUri);

                Bitmap b = BitmapFactory.decodeFile(imagePath);
                mBitmap = b;

                byte[] bArray2 = bitmapToByteArray(b);
                byte[] bArray = bitmapToByteArray(b, 10);
                Bitmap b2 = byteArrayToBitmap(bArray);

                ((ImageView) findViewById(R.id.image)).setImageBitmap(b2);

                if (b.getHeight() > 4096 || b.getWidth() > 4096) {
                    //TODO Picture to big
                }

                Log.i("Test", String.format("Loaded File %s", imagePath));

                Log.i("Test", String.format("Image %f MB big", bArray.length / 1024.0f / 1024.0f));
                Log.i("Test", String.format("Image %f MB big (no compress)", bArray2.length / 1024.0f / 1024.0f));
            }
        }
        //super.onActivityResult(requestCode,resultCode,data);
    }

    public void sendImage(Bitmap image) { // to ground-station
        try {
            Socket socket = new Socket("localhost", 4242);

            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            // Komprimierung vom Bild
            byte[] bArray = bitmapToByteArray(image, 70);

            out.write(bArray);
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }

    public void saveBitmap(Bitmap bitmap) {
        try {
            for (int i = 10; i <= 100; i += 10) {
                //File file = new File(String.format("/storage/extSdCard/Pictures/Compress/p_%d.jpg", i));
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) +
                        String.format("/Compress/p_%d.jpg", i));
                if (!file.exists())
                    file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, i, out);
                out.close();
                Log.i("Test", String.format(" %d saved", i));
            }

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) +
                    "/Compress/one.jpg");
            if (!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 1, out);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap byteArrayToBitmap(final byte[] bArray) {
        return BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
    }

    public byte[] bitmapToByteArray(final Bitmap bitmap) {
        return bitmapToByteArray(bitmap, 100);
    }

    public byte[] bitmapToByteArray(final Bitmap bitmap, final int compressFactor) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressFactor, baos);
            final byte[] bArray = baos.toByteArray();
            baos.close();
            return bArray;
        } catch (IOException e) {
            return new byte[0];
        }
    }

    public String getPath(Uri uri) {
        // just some safety built in
        if (uri == null) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = {MediaStore.Images.Media.DATA};
        //Cursor cursor = managedQuery(uri, projection, null, null, null);
        Cursor cursor = getContentResolver().query(
                uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        // this is our fallback here
        return uri.getPath();
    }
}
