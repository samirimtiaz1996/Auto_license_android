package com.samirimtiaz.auto_license;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;



import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.net.ssl.SSLException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    Bitmap mask,temp;
    JavaCameraView javaCameraView;

    private  static String TAG = "MainActivity";
    Mat mRGBA,mRGBAT,mRGBAGREY,mRGBACANNY,mRGBADisplay;
    Rect roi;
    BaseLoaderCallback baseLoaderCallback= new BaseLoaderCallback (MainActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch ((status)){
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView ();
                    break;
                }
                default:{
                    super.onManagerConnected (status);
                    break;
                }

            }

        }
    };
    static {
        if(OpenCVLoader.initDebug ()){
            Log.d (TAG,"OpenCV successfully loaded,congrates");
        }
        else {
            Log.d (TAG,"OpenCV didn't loaded");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        javaCameraView = (JavaCameraView) findViewById (R.id.my_camera_view);
        javaCameraView.setVisibility (SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener (MainActivity.this);
        mask= BitmapFactory.decodeResource (getResources (),R.drawable.imask);


    }

    /**
     * This method is invoked when camera preview has started. After this method is invoked
     * the frames will start to be delivered to client via the onCameraFrame() callback.
     *
     * @param width  -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA=new Mat (height,width, CvType.CV_8UC1);
        mRGBAGREY=new Mat (height,width, CvType.CV_8UC1);
        mRGBACANNY=new Mat (height,width, CvType.CV_8UC1);
        mRGBADisplay=new Mat (height,width,CvType.CV_8UC1);
    }

    /**
     * This method is invoked when camera preview has been stopped for some reason.
     * No frames will be delivered via onCameraFrame() callback after this method is called.
     */
    @Override
    public void onCameraViewStopped() {
        mRGBA.release ();
    }

    /**
     * This method is invoked when delivery of the frame needs to be done.
     * The returned values - is a modified frame which needs to be displayed on the screen.
     * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
     *
     * @param inputFrame
     * @return
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA=inputFrame.rgba ();



        mRGBAT =mRGBA.t ();
        Core.flip (mRGBA.t (),mRGBAT,1);
        Imgproc.resize (mRGBAT,mRGBAT, mRGBA.size ());
        mRGBAT.copyTo (mRGBADisplay);




        Imgproc.cvtColor(this.mRGBAT, mRGBAGREY, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mRGBAGREY, mRGBAGREY, new Size(5, 5), 0);
        Imgproc.Canny(mRGBAGREY, mRGBACANNY, 50, 150);
        Log.d (TAG,mRGBACANNY.height ()+" "+mRGBACANNY.width ());

//        Rect roi= new Rect (0,0,640,480);
//        Mat cropped=new Mat (mRGBA,roi);

        Bitmap bmp = Bitmap.createBitmap(mRGBACANNY.cols(), mRGBACANNY.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRGBACANNY, bmp);

        bmp=maskingProcess (bmp);

        Utils.bitmapToMat(bmp, mRGBACANNY);
        Imgproc.cvtColor(mRGBACANNY, mRGBACANNY, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(mRGBACANNY, mRGBACANNY, 50, 150);


        //      Log.d(TAG, String.valueOf (mRGBACANNY.height ()+" "+ String.valueOf (mRGBACANNY.width ())));
    //    Log.d(TAG, String.valueOf (mRGBAGREY.height ()+" "+ String.valueOf (mRGBAGREY.width ())));
//




        Mat lines = new Mat();
        double rho = 1;
        double theta = Math.PI/180;
        int threshold = 50;
      Imgproc.HoughLinesP(mRGBACANNY, lines, rho, theta, threshold, 10, 5);
      Log.d (TAG, String.valueOf (lines.cols ()));

          for (int x = 0; x < lines.cols (); x++) {
              double[] vec = lines.get (0, x);
              double x1 = vec[0],
                      y1 = vec[1],
                      x2 = vec[2],
                      y2 = vec[3];
              double angle =Math.atan2 (y2-y1,x2-x1)*180;
              angle=Math.abs (angle);
              Log.d (TAG, String.valueOf (angle));
              if(angle >50 && angle <=90) {
                  Point start = new Point (x1, y1);
                  Point end = new Point (x2, y2);
                  Imgproc.line (mRGBAT, start, end, new Scalar (0, 255, 0), 10);
              }
//             x1 = vec[4];
//                    y1 = vec[5];
//                    x2 = vec[6];
//                    y2 = vec[7];
//            start = new Point(x1, y1);
//            end = new Point(x2, y2);
//            Imgproc.line(mRGBAT, start, end, new Scalar (0, 255, 0), 10);
          }



        return mRGBAT;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy ();
        if(javaCameraView!=null){
            javaCameraView.disableView ();
        }
    }

    @Override
    protected void onPause() {
        super.onPause ();
        if(javaCameraView!=null){
            javaCameraView.disableView ();
        }
    }

    @Override
    protected void onResume() {
        super.onResume ();
        if(OpenCVLoader.initDebug ()){
            Log.d (TAG,"OpenCV successfully loaded,congrates");
            baseLoaderCallback.onManagerConnected (BaseLoaderCallback.SUCCESS);
        }
        else {
            Log.d (TAG,"OpenCV didn't loaded");
            OpenCVLoader.initAsync (OpenCVLoader.OPENCV_VERSION,this,baseLoaderCallback);
        }

    }

    private Bitmap maskingProcess(Bitmap bmp){
        try {

            temp= Bitmap.createBitmap (bmp.getWidth (),bmp.getHeight (),bmp.getConfig ());
            Bitmap maskBitmap=Bitmap.createScaledBitmap (mask,bmp.getWidth (),bmp.getHeight (),true);
            Canvas canvas=new Canvas (temp);
            Paint paint=new Paint (Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode (new PorterDuffXfermode (PorterDuff.Mode.DST_IN));


            canvas.drawBitmap (bmp,0,0,null);

            canvas.drawBitmap (maskBitmap,0,0,paint);

            paint.setXfermode (null);
            paint.setStyle (Paint.Style.STROKE);

        }
        catch (OutOfMemoryError outOfMemoryError){
            outOfMemoryError.printStackTrace ();
        }


    return temp;
    }

}