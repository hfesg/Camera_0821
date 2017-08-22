package com.example.ykx.camera_0821;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Created by YKX on 2017/8/22.
 */

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PreviewActivity";

    private Camera mCamera;
    private CameraPreview mPreview;
    private SurfaceView preview;
    private FrameLayout previewLayout;
    private SurfaceHolder previewHolder;
    private Camera camera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        initView();

        mCamera = openFrontCamera();
        mPreview = new CameraPreview(this, mCamera);
        previewLayout.addView(mPreview);
    }

    private void initView() {
        previewLayout = (FrameLayout) findViewById(R.id.camera_preview);
    }

    private Camera openFrontCamera() {
        int cameraCount = 0;
        Camera camera = null;

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();  //获取摄像头数量

        for (int camIdx = 0; camIdx < cameraCount; camIdx++){
            Camera.getCameraInfo(camIdx, cameraInfo);   //获取cameraInfo
            // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    camera = Camera.open(camIdx);
                }catch (RuntimeException e){
                    Log.d(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }
        return camera;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
