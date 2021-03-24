package com.example.trafficgoggles;

    import android.Manifest;
    import android.app.Activity;
    import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
    import android.os.Build;
    import android.os.Environment;

    import androidx.annotation.RequiresApi;
    import androidx.annotation.UiThread;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;

    import android.os.Bundle;
    import android.os.Handler;
    import android.text.TextUtils;
import android.util.Log;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.*;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;


    import java.time.Duration;
    import java.time.Instant;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
import java.util.Arrays;
    import java.util.Hashtable;
    import java.util.List;
    import java.util.Locale;
    import java.util.Random;
    import java.util.Set;
    import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

    import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    Instant fps;
    BaseLoaderCallback baseLoaderCallback;
    int counter = 0,inputSize = 416;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    float diff;
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    public Set<String> queue;
    private Locale language;
    Canvas canvas;
    Boolean isProcessingFrame = false,once=true;
    private Hashtable<String,Instant> SignLastSeen = new Hashtable<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        language = Locale.US;
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
//      initialize the classifier
        initTensorFlowAndLoadModel();

        baseLoaderCallback = new BaseLoaderCallback(this) {

            @Override
            public void onManagerConnected(int status) {
                if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.CAMERA
                    },REQUEST_CAMERA_PERMISSION);
                }
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }


            }

        };
    }
    //Language menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.lang_menu, menu);
        return true;
    }
    //Language selection operation
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.language_select:
                return true;
            case R.id.eng_us:
                language  = Locale.US;
                return true;
            case R.id.eng_uk:
                language  = Locale.UK;
                return true;
            case R.id.french:
                language  = Locale.FRANCE;
                return true;
            case R.id.hin:
                language = new Locale("hi", "IN");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class DetectThread extends Thread{
        private Mat mImg;

        public DetectThread(Mat img) {
            mImg = img;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            //convert opencv Mat to Bitmap
            Bitmap bitmap = Bitmap.createBitmap(mImg.cols(), mImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mImg, bitmap);

            ArrayList<Classifier.Recognition> results = null;
            //Recognition
            results = classifier.RecognizeImage(bitmap);

            canvas = new Canvas(bitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);
            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                Log.d("MainActivity", result.getTitle()+" "+result.getConfidence());
                if (location != null && result.getConfidence() >= 0.5) {

                    String classFound = result.getTitle();
                    canvas.drawRect(location, paint);
                    //Delay of 1 minute after every sign
                    if ((SignLastSeen.containsKey(classFound) && Duration.between(SignLastSeen.get(classFound), Instant.now()).toMillis() > 60000) || SignLastSeen.containsKey(classFound) == false) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TTS obj = new TTS();
                                obj.speak(getApplicationContext(), result.getTitle(),language);
                            }

                        });

                    }
                    if(SignLastSeen.containsKey(classFound)) {
                        SignLastSeen.replace(classFound, Instant.now());
                    }
                    else{
                        SignLastSeen.put(classFound,Instant.now());
                    }
                }
            }
            isProcessingFrame = false;

        }

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();
//        Core.flip(frame, frame,0);
        if(!isProcessingFrame) {
            isProcessingFrame = true;
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
            Log.d("MainActivity", "detect");
            Mat resizeimage = new Mat();
            Size sz = new Size(inputSize, inputSize);
            //resize input image to the size of input tensor
            Imgproc.resize(frame, resizeimage, sz);
            new DetectThread(resizeimage).start();
            cameraBridgeViewBase.enableFpsMeter();
       }
        return frame;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCameraViewStarted(int width, int height) {

    }


    @Override
    public void onCameraViewStopped() {

    }


    @Override
    protected void onResume() {


        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
        super.onResume();

    }

    @Override
    protected void onPause() {

        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }
        super.onPause();

    }


    @Override
    protected void onDestroy() {

        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
        super.onDestroy();
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = new Yolov3Classifier(getAssets());

                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

}