package com.example.ykx.camera_0821.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by YKX on 2017/7/6.
 */

public class EcgView extends SurfaceView implements SurfaceHolder.Callback {

    private final Context mContext;
    private final SurfaceHolder surfaceHolder;
    private Canvas mCanvas;
    public static boolean isRunning;

    private int wave_speed = 5;    //波速25mm/s  属性：波速变慢，同一屏波峰变少；波速变快，同一屏波峰变多
                                    //波速应该同下面的若干数据统一起来,如:每次锁屏画1个点,则波速应该放慢(目前测试5mm/s),这样波形显示出来是正常的
    private float sleepTime = 33;    //每次锁屏的时间间距，单位：ms
    private float lockWidth;        //每次锁屏需要画的点数
    private String backgroundColor = "#3FB57D";     //设置背景色
    private int ecgPerCount = 1;    //每次画心电数据的个数，我这个一秒钟只有30个数据。
    private float ecgMax = 600000;             //心电的最大值，我是否用得上？-或许需要进行进一步的改进，这是y轴的最大值..每次画心电数据的个数，心电每秒有500个数据包
                                                //1000000是实验出来的,这样波形会出现在中央,比较好看,同时由于是等比例移动,所以对波形本身没有什么变化
                                                //该参数控制Y轴性质
    private float ecgYRatio;          //心电数据在Y轴上的比例

    private static Queue<Integer> ecgDatas = new LinkedList<>();

    private int mWidth;     //控件宽度
    private int mHeight;    //控件高度
    private int startY;
    private Paint mPaint;   //画波形图的画笔
    private Rect rect;

    private int startX;     //每次画线的X坐标起点
    private int blankLineWidth = 6;     //右侧空白点的宽度
    private double ecgXoffset;   //每次X坐标偏移的像素

    private void startDrawWave() {
        rect.set(startX, 0, (int) (startX + lockWidth + blankLineWidth), mHeight);      //设定方形区域，锁屏区域
        mCanvas = surfaceHolder.lockCanvas(rect);       //锁定SurfaceView上Rect划分的区域，SurfaceView指对Rect所圈出来的区域更新，提高画面更新速度
        if (mCanvas == null) return;
        mCanvas.drawColor(Color.parseColor(backgroundColor));   //绘制背景色

        drawWave();     //绘制波形

        surfaceHolder.unlockCanvasAndPost(mCanvas);     //释放绘图、提交所绘制的图形

        startX = (int) (startX + lockWidth);        //重新确定绘图的起点
        if (startX > mWidth) {
            startX = 0;
        }
    }

    /**
     * 绘制波形
     */
    private void drawWave() {
        try {
            float mStartX = startX;
            if (ecgDatas.size() > ecgPerCount) {
                for (int i = 0; i < ecgPerCount; i++){
                    float newX = (float) (mStartX + ecgXoffset);
                    int newY = ecgCover(ecgDatas.poll()) * 3;       //剪切出ecgDatas中的第一个数据，poll:移除并返问队列头部的元素,意思是将头部第一个数据剪切出来。
                    mCanvas.drawLine(mStartX, startY, newX, newY, mPaint);
                    mStartX = newX;
                    startY = newY;
                }
            }else {
                /**
                 * 如果没有数据调用此处
                 * 因为有数据一次画ecgPerCount个数，那么无数据时候就应该画ecgPercount倍数长度的中线
                 */
                int newX = (int) (mStartX + ecgXoffset * ecgPerCount);
                int newY = ecgCover((int) (ecgMax / 2));        //如果没有数据，就继续画水平线
                mCanvas.drawLine(mStartX, startY, newX, newY, mPaint);
                startY = newY;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将心电数据转换成Y坐标
     * @param data：传入心电数据
     * @return 返回Y坐标
     */
    private int ecgCover(int data) {
        data = (int) (ecgMax -data);    //如果将这一句注释了，心电波形就会是反过来的。可能心电测量得到的数据就是反的，这与其本质有关。
        data = (int) (data * ecgYRatio);
        return data;
    }

    /**
     * 创建心电View
     * @param context：当前对话context
     * @param attrs：？？
     */
    public EcgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;                //指定上下文
        this.surfaceHolder = getHolder();       //获取surfaceHolder
        this.surfaceHolder.addCallback(this);   //添加回调
        rect = new Rect();                      //实例化方形区域
        converXOffset();                        //计算每次增加X坐标的像素数
    }

    /**
     * 根据波速计算每次X坐标增加的像素
     * 计算每次锁屏时应画的像素（px）值
     * 作者想表达的是不是每一次刷屏要画多少像素点？-根据作者提出的sleepTime来看，不是这么理解的。。
     * 大概明白了，此处的锁屏应该是指Canvas类中的lockCanvas()方法之后锁定的屏幕范围。
     * --恰恰相反，是锁定屏幕后，没有被锁住的区域。由startDrawWave（)方法中的lockCanvas(rect) 方法可以看出来。
     */
    private void converXOffset() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;         //获取屏幕的宽度，单位：px
        int height = displayMetrics.heightPixels;       //获取屏幕的长度，单位：px
        double diagonalInch = Math.sqrt(width * width + height * height) / displayMetrics.densityDpi;   //单位：英寸
        double diagonalMm = diagonalInch * 2.54 * 10;   //转换单位为：mm
        double diagonalPx = Math.sqrt(width * width + height * height);     //对角线上的像素数
        double pxPerMm = diagonalPx / diagonalMm;   //每毫米像素数
        double pxPerSec = wave_speed * pxPerMm;     //每秒画多少px
        lockWidth = (float) (pxPerSec * (sleepTime / 1000f));
    }

    /**
     * View大小发生变化时调用
     * @param w：新宽度
     * @param h：新高度
     * @param oldw：旧宽度
     * @param oldh：旧高度
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        isRunning = true;
        init();
    }

    private void init() {
        //设置画笔参数，颜色，宽度等
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(3);

        ecgXoffset = lockWidth / ecgPerCount;
        startY = mHeight / 2;       //波的初始Y坐标为控件高度的一半
        ecgYRatio = mHeight  / ecgMax;   //Y方向上的高度，与最大值得比例，确定数值在Y轴的位置比例。
    }

    /**
     * surfacaView 的三个回调方法
     * @param holder：holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();     //SurfaceHolder提供的方法，锁定整个SurfaceView对象，获取该Surface上的Canvas。
        canvas.drawColor(Color.parseColor(backgroundColor));    //绘制背景
        holder.unlockCanvasAndPost(canvas);      //释放绘图、提交所绘制的图形
        startThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopThread();
    }

    /**
     * 添加数据的方法
     * @param data：要添加的数据
     */
    public static void addEcgData(int data){
        ecgDatas.add(data);
    }

    private void stopThread() {
        isRunning = false;
    }

    private void startThread() {
        isRunning = true;
        new Thread(drawRunnable).start();
    }

    private Runnable drawRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRunning) {
                long startTime = System.currentTimeMillis();

                startDrawWave();    //开始绘制波形

                long endTime = System.currentTimeMillis();
                if (endTime - startTime < sleepTime) {
                    try {
                        Thread.sleep((long) (sleepTime - (endTime - startTime)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
