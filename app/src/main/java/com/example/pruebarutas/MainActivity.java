package com.example.pruebarutas;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private final static int GET_FILE = 0;
    private final static int TAKE_PHOTO = 2;
    private final static int CAMERA_PERMISION = 3;
    Toast toast;
    private ImageView imgView;
    private MediaPlayer mediaP;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        imgView = findViewById(R.id.imageView);
        mediaP = new MediaPlayer();
        videoView = findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(MainActivity.this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.archivo) {
            //crea el intent para seleccionar archivo
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, GET_FILE);
        }
        if (id == R.id.camara) {
            //mira si se han dado los permisos
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISION);
            } else {
                tomarFoto();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int res, String[] perm, int[] grantRes) {
        super.onRequestPermissionsResult(res, perm, grantRes);
        if (res == CAMERA_PERMISION) {
            if (grantRes.length > 0 && grantRes[0] == PackageManager.PERMISSION_GRANTED) {
                tomarFoto();
            } else {
                toast.setText("Se necesita permisos de camara para utilizarla");
                toast.show();
            }
        }

    }

    private void tomarFoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    @Override
    protected void onActivityResult(int res, int resultCode, Intent data) {
        super.onActivityResult(res, resultCode, data);
        toast.setText("¿ERROR?");

        //seteo los elementos a nada
        imgView.invalidate();
        imgView.setImageBitmap(null);
        videoView.invalidate();
        videoView.setVideoURI(null);
        if (mediaP.isPlaying())
            mediaP.stop();

        if (res == GET_FILE && data != null) {
            //mete en un archivo los datos
            Uri uri = data.getData();
            Archivo arch = getArchiveFromURI(uri, getApplicationContext());

            //enseña los datos
            toast.setText(arch.getNombre() + " " + arch.type);

            //si es una imagen los convierte e Bitmap y lo enseña
            if (arch.type.contains("image")) {
                InputStream is = new ByteArrayInputStream(arch.data);
                imgView.setImageBitmap(BitmapFactory.decodeStream(is));
            }

            //si es un audio...
            if (arch.type.contains("audio")) {
                try {
                    //convierte los datos en un archivo temporal
                    File tempMp3 = File.createTempFile("temp", ".mp3");
                    tempMp3.deleteOnExit();
                    FileOutputStream fos = new FileOutputStream(tempMp3);
                    fos.write(arch.data);
                    FileInputStream fis = new FileInputStream(tempMp3);

                    //escuchamos el sonido del archivo temporal
                    mediaP.reset();
                    mediaP.setDataSource(fis.getFD());
                    mediaP.prepare();
                    mediaP.start();

                    fos.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //si es video...
            if (arch.type.contains("video")) {
                try {
                    //convierte los datos en un archivo temporal
                    File tempMp4 = File.createTempFile("temp", ".mp4");
                    tempMp4.deleteOnExit();
                    FileOutputStream fos = new FileOutputStream(tempMp4);
                    fos.write(arch.data);

                    //vemos el video
                    videoView.setVideoURI(Uri.fromFile(tempMp4));

                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (res == TAKE_PHOTO && data != null) {
            //pickeo la foto
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            bitmap.recycle();

            //creo el objeto archivo
            Archivo arch = new Archivo("imagen.jpg", "image/jpeg", byteArray);

            //enseño los datos
            toast.setText(arch.getNombre() + " " + arch.type);

            InputStream is = new ByteArrayInputStream(arch.data);
            imgView.setImageBitmap(BitmapFactory.decodeStream(is));
        }
        toast.show();
    }


    private Archivo getArchiveFromURI(Uri uri, Context context) {
        //busco el archivo por el uri para cojer el nombre
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        //pillo los bytes[] del archivo
        File file = new File(context.getFilesDir(), name);
        byte[] buffers = null;
        String mimeType = getContentResolver().getType(uri);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1024 * 1024 * 1024;
            int bytesAvailable = inputStream.available();
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return new Archivo(file.getName(), mimeType, buffers);
    }
}