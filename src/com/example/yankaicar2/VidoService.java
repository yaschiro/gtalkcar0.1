package com.example.yankaicar2;

import android.app.Activity;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

//拍照,伪视频专用类,取得各种状态
public class VidoService {

	public Activity activity;
	// 摄像使用
	public Button btn_capture;
	public Button btn_shotcut;
	public Camera camera = null;
	public SurfaceHolder mSurfaceHolder;
	public SurfaceView mSurfaceView01;
	public boolean bPreviewing = false;
	public int intScreenWidth;
	public int intScreenHeight;
	
	//缓冲图像帧用
	public byte[] frameData;
	public VidoService(Activity _activity){
		
		activity=_activity;
		/* 取得屏幕解析像素 */
		getDisplayMetrics();
		//findViews();
		//getSurfaceHolder();
		
	}
	
	/* func:获取屏幕分辨率 */
	private void getDisplayMetrics() {
		
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		intScreenWidth = dm.widthPixels;
		intScreenHeight = dm.heightPixels;
		//Log.i(TAG, Integer.toString(intScreenWidth));
	}
 
}
