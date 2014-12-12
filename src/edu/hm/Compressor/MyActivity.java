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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class MyActivity extends Activity {

    private Bitmap mBitmap;
    private Bitmap mCompressedBitmap;
    private SeekBar mSeekBar;
    private String mPicName;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);

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

        ((Button) findViewById(R.id.button3)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte [] bArray = bitmapToByteArray(mBitmap, mSeekBar.getProgress());
                mCompressedBitmap = byteArrayToBitmap(bArray);
                ((ImageView) findViewById(R.id.image)).setImageBitmap(mCompressedBitmap);
                ((TextView) findViewById(R.id.filesize_label))
                        .setText("Filesize: " + Integer.toString(bArray.length / 1024) + " kB");
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView)findViewById(R.id.textView)).setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && data != null) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                List<String> seg = selectedImageUri.getPathSegments();
                mPicName = seg.get(seg.size() - 1);
                String imagePath = getPath(selectedImageUri);
                mBitmap = BitmapFactory.decodeFile(imagePath);

                ((TextView) findViewById(R.id.filesize_label))
                        .setText("Filesize: " + Integer.toString(
                                bitmapToByteArray(mBitmap).length / 1024) + " kB");

                ((ImageView) findViewById(R.id.image)).setImageBitmap(mBitmap);

                if (mBitmap.getHeight() > 4096 || mBitmap.getWidth() > 4096) {
                    //TODO Picture to big
                }

                mSeekBar.setProgress(100);
            }
        }
    }

    public void saveBitmap(Bitmap bitmap) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) +
                String.format("/Compress/%s_%d.webp", mPicName, mSeekBar.getProgress()));
            if (!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.WEBP, mSeekBar.getProgress(), out);
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
            bitmap.compress(Bitmap.CompressFormat.WEBP, compressFactor, baos);
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
}
