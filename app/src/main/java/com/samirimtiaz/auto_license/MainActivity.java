package com.samirimtiaz.auto_license;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    JavaCameraView javaCameraView;
    private  static String TAG = "MainActivity";
    Mat mRGBA,mRGBAT,mRGBAGREY,mRGBACANNY;
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
        Imgproc.cvtColor(this.mRGBAT, mRGBAGREY, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mRGBAGREY, mRGBAGREY, new Size(5, 5), 0);
        Imgproc.Canny(mRGBAGREY, mRGBACANNY, 50, 150);
        Log.d(TAG, String.valueOf (mRGBACANNY.height ()+" "+ String.valueOf (mRGBACANNY.width ())));
        Log.d(TAG, String.valueOf (mRGBAGREY.height ()+" "+ String.valueOf (mRGBAGREY.width ())));

        Mat lines = new Mat();
        double rho = 1;
        double theta = Math.PI/180;
        int threshold = 50;
        Imgproc.HoughLinesP(mRGBACANNY, lines, rho, theta, threshold, 20, 5);
        for (int x = 0; x < lines.cols(); x++){
            double[] vec = lines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Imgproc.line(mRGBAT, start, end, new Scalar (0, 255, 0), 10);
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
}