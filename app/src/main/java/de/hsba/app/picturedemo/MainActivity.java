package de.hsba.app.picturedemo;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private final String TAG = "CameraXBasic";
    private final String FILENAME_FORMAT = "dd.MM.yyyy";

    Button takePhotoButton;
    PreviewView viewFinder;

    ExecutorService cameraExecutor;
    ImageCapture imageCapture;

    File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (allPermissionsGranted()){
            startCamera();
        }else {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);
        }

        takePhotoButton = findViewById(R.id.camera_capture_button);
        viewFinder = findViewById(R.id.viewFinder);

        outputDirectory = getOutputDir();

        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()){
                startCamera();
            }else {
                Toast.makeText(this, "Permission not granted!",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> camProvFuture = ProcessCameraProvider.getInstance(this);
        imageCapture = new ImageCapture.Builder().build();
        camProvFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider camProvider = camProvFuture.get();
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewFinder.createSurfaceProvider());
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    camProvider.unbindAll();
                    camProvider.bindToLifecycle(MainActivity.this, cameraSelector,preview,imageCapture);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private File getOutputDir(){

        String sdCard = Environment.getExternalStorageDirectory().toString();
        File pictures = new File(sdCard+"/Pictures/");
        pictures.mkdir();
        return pictures;
    }

    public void takePhoto(View view) {

        File photoFile = new File(outputDirectory,System.currentTimeMillis()+".jpg");
        ImageCapture.OutputFileOptions fileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(fileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo captured to: "+savedUri.toString();
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                Log.d("Cam",msg);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.d("Cam",photoFile.toString()+exception.toString());
            }
        });
    }
}