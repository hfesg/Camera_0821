package com.example.ykx.camera_0821.util;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;

public class ParseResult {
	private static final String TAG = "ParseResult";
	/**
	 * 离线人脸框结果解析方法
	 * @param json
	 * @return
	 */
	static public FaceRect[] parseResult(String json){
		FaceRect[] rect = null;
		if(TextUtils.isEmpty(json)) {
			return null;
		}
		try {
			JSONTokener tokener = new JSONTokener(json);		//json就是一段String类型的，符合JSON数据结构的文本，JSONTokener的作用就是将其读出成JSON格式
			JSONObject joResult = new JSONObject(tokener);		//此处根据tokener创建JSONObject对象，方便使用其getType和optType方法。
			int ret = joResult.optInt("ret");					//此处是将joResult中的“ret”解析为Int类型的数，进行存储
			if(ret != 0) {
				return null;
			}
			// 获取每个人脸的结果
			JSONArray items = joResult.getJSONArray("face");
			// 获取人脸数目
			rect = new FaceRect[items.length()];
			for(int i=0; i<items.length(); i++) {
				
				JSONObject position = items.getJSONObject(i).getJSONObject("position");
				// 提取关键点数据
				rect[i] = new FaceRect();
				rect[i].bound.left = position.getInt("left");
				rect[i].bound.top = position.getInt("top");
				rect[i].bound.right = position.getInt("right");
				rect[i].bound.bottom = position.getInt("bottom");

				//下面的代码就是获取面部特征点的代码，他们并没有获取每一个点的详细信息，而是轮询了所有点，并表示
				//而我想要找到鼻子那个点对应的是第几个点，所以需要进一步查询
				try {
					JSONObject landmark = items.getJSONObject(i).getJSONObject("landmark");
					int keyPoint = landmark.length();
					rect[i].point = new Point[keyPoint];
					Iterator it = landmark.keys();
					int point = 0;
					while (it.hasNext() && point < keyPoint) {
						String key = (String) it.next();
						JSONObject postion = landmark.getJSONObject(key);
						rect[i].point[point] = new Point(postion.getInt("x"), postion.getInt("y"));
//						Log.d(TAG, "x = " + position.getInt("x"));
						point++;
					}
				} catch (JSONException e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rect;
	}
}