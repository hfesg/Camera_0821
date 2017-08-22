package com.example.ykx.camera_0821;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by YKX on 2017/8/22.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private SurfaceView preview;

    @SuppressLint("WrongViewCast")
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

//        preview = (SurfaceView) findViewById(R.id.preview);
        //设置一个SurfaceHolder回调，检测其创造和销毁
        mHolder = getHolder();
        mHolder.addCallback(this);

        //弃用的设置，仅在Android3.0以前版本中需要
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Surface已经被创建，现在需要告诉camera，在哪儿显示预览
        try {
            mCamera.setPreviewDisplay(holder);      //绑定SurfaceHolder
            mCamera.setDisplayOrientation(90);      //设置显示旋转角度
            mCamera.startPreview();
            Log.d(TAG, "surfaceCreated: OK");
        } catch (IOException e) {
            Log.d(TAG, "surfaceCreated: error");
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //如果相机预览能变化或者能旋转，就需要在此进行相关设置，确保在重设定大小和格式之前停止预览
        mCamera.release();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //注意释放相机资源

        mCamera.setDisplayOrientation(90);

        //如果预览的surface不存在了,就直接返回
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            //在进行其余操作前要先关闭预览
            mCamera.stopPreview();
        }catch (Exception e) {
            e.printStackTrace();
        }

        //在这中间插入所需要的变化

        //重新打开预览
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            Log.d(TAG, "surfaceDestroyed: ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
