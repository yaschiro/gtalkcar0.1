package com.example.yankaicar2;

import android.app.Activity;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

//����,α��Ƶר����,ȡ�ø���״̬
public class VidoService {

	public Activity activity;
	// ����ʹ��
	public Button btn_capture;
	public Button btn_shotcut;
	public Camera camera = null;
	public SurfaceHolder mSurfaceHolder;
	public SurfaceView mSurfaceView01;
	public boolean bPreviewing = false;
	public int intScreenWidth;
	public int intScreenHeight;
	
	//����ͼ��֡��
	public byte[] frameData;
	public VidoService(Activity _activity){
		
		activity=_activity;
		/* ȡ����Ļ�������� */
		getDisplayMetrics();
		//findViews();
		//getSurfaceHolder();
		
	}
	
	/* func:��ȡ��Ļ�ֱ��� */
	private void getDisplayMetrics() {
		
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		intScreenWidth = dm.widthPixels;
		intScreenHeight = dm.heightPixels;
		//Log.i(TAG, Integer.toString(intScreenWidth));
	}
 
}
