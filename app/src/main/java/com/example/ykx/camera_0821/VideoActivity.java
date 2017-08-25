package com.example.ykx.camera_0821;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.Process;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.ykx.camera_0821.util.FaceRect;
import com.example.ykx.camera_0821.util.FaceUtil;
import com.example.ykx.camera_0821.util.ParseResult;
import com.iflytek.cloud.FaceDetector;
import com.iflytek.cloud.util.Accelerometer;

import java.io.IOException;


/**
 * Created by YKX on 2017/8/23.
 */

public class VideoActivity extends AppCompatActivity {

    private static final String TAG = "VideoActivity";

    // Camera nv21格式预览帧的尺寸，默认设置640*480
    private final static int PREVIEW_WIDTH = 640;
    private final static int PREVIEW_HEIGHT = 480;

    // 预览帧数据存储数组和缓存数组
    private byte[] nv21;
    private byte[] buffer;

    // 加速度感应器，用于获取手机的朝向
    private Accelerometer mAcc;
    // FaceDetector对象，集成了离线人脸识别：人脸检测、视频流检测功能
    private FaceDetector mFaceDetector;
    private SurfaceView mPreviewSurface;
    private SurfaceView mFaceSurface;

    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private Camera mCamera;
    private boolean mStopTrack;
    private long mLastClickTime;
    private int isAlign;
    // 缩放矩阵
    private Matrix mScaleMatrix = new Matrix();

    private SurfaceHolder.Callback mPreviewCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mScaleMatrix.setScale(width/(float)PREVIEW_HEIGHT, height/(float)PREVIEW_WIDTH);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initUI();

        nv21 = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT *2];
        buffer = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT *2];
        mAcc = new Accelerometer(VideoActivity.this);
        mFaceDetector = FaceDetector.createDetector(VideoActivity.this, null);
    }

    private void initUI() {
        mPreviewSurface = (SurfaceView) findViewById(R.id.sfv_preview);
        mFaceSurface = (SurfaceView) findViewById(R.id.sfv_face);
        RadioGroup alignGroup = (RadioGroup) findViewById(R.id.align_mode);

        mPreviewSurface.getHolder().addCallback(mPreviewCallback);
        mPreviewSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mFaceSurface.setZOrderOnTop(true);
        mFaceSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        //点击SurfaceView切换摄像头
        mFaceSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //如果只有一个摄像头，则不能切换
                if (Camera.getNumberOfCameras() == 1) {
                    Toast.makeText(VideoActivity.this, "只有后置摄像头", Toast.LENGTH_SHORT).show();
                    return;
                }
                closeCamera();
                if (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId) {
                    mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }else {
                    mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                openCamera();
            }
        });

        //长按SurfaceView 500ms后松开，摄像头聚焦
        mFaceSurface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        mLastClickTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - mLastClickTime > 500) {
                            mCamera.autoFocus(null);
                            return true;
                        }
                        break;

                    default:
                        break;
                }
                return false;
            }
        });

        alignGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId){
                    case R.id.detect:
                        isAlign = 0;
                        break;
                    case R.id.align:
                        isAlign = 1;
                        break;
                    default:
                        break;
                }
            }
        });

        setSurfaceSize();
//        mToast = Toast.makeText(VideoDemo.this, "", Toast.LENGTH_SHORT);
    }

    private void setSurfaceSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //此处应该是获取显示图像的大小，并设定surfaceView的显示大小
        int width = metrics.widthPixels;
        int height = (int) (width * PREVIEW_WIDTH / (float) PREVIEW_HEIGHT);       //为何要将PREVIEW_HEIGHT强制转换为float型？
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        mPreviewSurface.setLayoutParams(params);
        mFaceSurface.setLayoutParams(params);
    }


    private void openCamera() {
        if (null != mCamera) {
            return;
        }

        if (!checkCameraPermission()) {
            Toast.makeText(VideoActivity.this, "摄像头权限未打开，请打开后重试", Toast.LENGTH_SHORT).show();
            mStopTrack = true;
            return;
        }

        //如果只有一个摄像头，则打开后置摄像头
        if (Camera.getNumberOfCameras() == 1) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        try {
            mCamera = Camera.open(mCameraId);
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId) {
                Toast.makeText(VideoActivity.this, "前置摄像头已开启，点击可切换", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(VideoActivity.this, "后置摄像头已开启，点击可切换", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeCamera();
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.NV21);
        parameters.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        mCamera.setParameters(parameters);

        //设置显示的偏转角度，大部分机器是顺时针90度，某些机器需要按照情况设置
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                System.arraycopy(data, 0, nv21, 0, data.length);
            }
        });

        try {
            mCamera.setPreviewDisplay(mPreviewSurface.getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mFaceDetector == null) {
            /**
             * 离线视频流检测功能需要单独下载支持离线人脸的SDK
             * 请开发者前往语音云官网下载对应SDK
             */
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            Toast.makeText(VideoActivity.this, "创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkCameraPermission() {
        int status = checkPermission(Manifest.permission.CAMERA, Process.myPid(), Process.myUid());
        if (PackageManager.PERMISSION_GRANTED == status) {
            return true;
        }
        return false;
    }

    private void closeCamera() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (null != mAcc) {
            mAcc.start();
        }

        mStopTrack = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mStopTrack){
                    if (null == nv21) {
                        continue;
                    }

                    //???为何要把nv21中的数据再挪到buffer中？方便nv21再去接收吗？
                    synchronized (nv21){
                        System.arraycopy(nv21, 0, buffer, 0, nv21.length);
                    }

                    //获取手机朝向，返回值0,1,2,3,分别是0,90,180,270度
                    int direction = Accelerometer.getDirection();
                    boolean frontCamera = (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId);
                    // 前置摄像头预览显示的是镜像，需要将手机朝向换算成摄相头视角下的朝向。
                    // 转换公式：a' = (360 - a)%360，a为人眼视角下的朝向（单位：角度）
                    if (frontCamera) {
                        // SDK中使用0,1,2,3,4分别表示0,90,180,270和360度
                        direction = (4 - direction) % 4;
                    }

                    if (mFaceDetector == null) {
                        /**
                         * 离线视频流检测功能需要单独下载支持离线人脸的SDK
                         * 请开发者前往语音云官网下载对应SDK
                         */
                        // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
                        Toast.makeText(VideoActivity.this, "创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    String result = mFaceDetector.trackNV21(buffer, PREVIEW_WIDTH, PREVIEW_HEIGHT, isAlign, direction);
                    Log.d(TAG, "run: " + result);

                    FaceRect[] faces = ParseResult.parseResult(result);

                    Canvas canvas = mFaceSurface.getHolder().lockCanvas();
                    if (null == canvas){
                        continue;
                    }

                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    canvas.setMatrix(mScaleMatrix);

                    if (faces == null || faces.length <= 0) {
                        mFaceSurface.getHolder().unlockCanvasAndPost(canvas);
                        continue;
                    }

                    if (null != faces && frontCamera == (Camera.CameraInfo.CAMERA_FACING_FRONT == mCameraId)) {
                        for (FaceRect face : faces){
                            face.bound = FaceUtil.RotateDeg90(face.bound, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                            if (face.point != null) {
                                for (int i = 0; i <face.point.length; i++){
                                    face.point[i] = FaceUtil.RotateDeg90(face.point[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
                                }
                            }
                            FaceUtil.drawFaceRect(canvas, face, PREVIEW_WIDTH, PREVIEW_HEIGHT, frontCamera, false);
                        }
                    }else {
                        Log.d(TAG, "faces:0");
                    }
                    mFaceSurface.getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mFaceDetector) {
            //销毁对象
            mFaceDetector.destroy();
        }
    }

    //这个showTip可以学习一下，因为一直输入Toast很烦人啊~~
//        private void showTip(final String str) {
//            mToast.setText(str);
//            mToast.show();
//        }
}
