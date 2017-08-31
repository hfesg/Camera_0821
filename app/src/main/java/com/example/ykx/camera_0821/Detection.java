package com.example.ykx.camera_0821;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.ykx.camera_0821.view.EcgView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by YKX on 2017/8/22.
 */

public class Detection extends AppCompatActivity{
    private static final String TAG = "Detection";

    private List<Integer> datas = new ArrayList<>();

    private Queue<Integer> dataQ = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        loadDatas();
        simulator();
    }

    private void simulator() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (EcgView.isRunning){
                    if (dataQ.size() > 0){
                        EcgView.addEcgData(dataQ.poll());
                    }
                }
            }
        }, 0, 2);       //第一个参数即实现的方法，具体实现什么
        //第二个参数为schedule()方法实现后,多长时间(ms)开始执行
        //第三个参数为每次执行之间的时间间距(ms)
    }

    /**
     * 加载数据
     */
    private void loadDatas() {
        try {
            String data;
            InputStream inputStream = getResources().openRawResource(R.raw.pulse);
            int length = inputStream.available();
            byte[] buffer = new byte[length];
            inputStream.read(buffer);       //这一句不能省略，省略了之后无法显示波形，测试过了。
            data = new String(buffer);
            inputStream.close();
            String[] dataS = data.split("\r\n");    //注意此处不能写成"/r/n"
            for (String str : dataS){
                double dataTemp = Double.parseDouble(str);
                dataTemp *= 10000;
//                Log.d(TAG, "loadDatas: datatemp" + dataTemp);
                datas.add((int) dataTemp);
            }
            dataQ.addAll(datas);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
