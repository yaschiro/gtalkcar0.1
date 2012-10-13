package com.example.yankaicar2;


 

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smack.util.StringUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;
 
public class VideoSmackCarControllerActivity extends Activity implements
SurfaceHolder.Callback, PreviewCallback, Runnable  {
	public static final String BroadcastName = "android.lynx.car.intent.INTENT_CAR_CONTROL"; // 蓝牙车控制广播地址
	private static final String TAG = "VideoSmackCarController";
	private static final boolean D = true; // Debug模式开启标识
	private Chat newChat = null;
	private Handler mHandler = new Handler();
	public XMPPConnection connection;
	private ChatManager chatmanager;
	private SettingsDialog mDialog;
	private EditText mRecipient;
	private EditText mSendText;
	private EditText mCycleTime;
	private RadioGroup rg;
	private RadioButton b0;
	private RadioButton b1;
	private RadioButton b2;
	private RadioButton b3;
	private ListView mList;
	private ImageView mImageView;
	public String serverName = "gamil.com";
	private ArrayList<String> messages = new ArrayList<String>();

	// 摄像使用
	private Button btn_capture;
	private Button btn_shotcut;
	private Camera camera = null;
	private SurfaceHolder mSurfaceHolder;
	private SurfaceView mSurfaceView01;
	private boolean bPreviewing = false;
	private int intScreenWidth;
	private int intScreenHeight;
	//缓冲图像帧用
	private byte[] frameData;
	//守护进程用
	private Thread Guarder = null;
	private boolean ThreadFlag = false;
	private int cycleMiliseconds = 2000;  //拍照循环周期，默认设置为2000毫秒（2秒）
	// 扩展数据定义
	public static final String EElementName = "JpegExtension";
	public static final String ENameSpace = "CarExtension";
	public static final String EValueName = "AJpeg";
	public static final String ETimeName = "CreateTime";

	// 用于确定图片的先后时间防止图片数据包分时到达出现显示混乱
	private long lastPhotoTime = 0;

	// 用于存储控制命令的两个时间变量
	private int moveContinueTime = 100;
	private int eyeCycleTime = 1000;

	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		 
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectDiskReads()
	                 .detectDiskWrites()
	                 .detectNetwork()   // or .detectAll() for all detectable problems
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectLeakedSqlLiteObjects()
	                 .detectLeakedClosableObjects()
	                 .penaltyLog()
	                 .penaltyDeath()
	                 .build());
	      
		
		
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE); // 去掉标题栏
		setContentView(R.layout.main);

		initFindsAndListeners();

	}
	/*---------------------------------------------------------视频部分开始----------------------------
	
	/* func:获取屏幕分辨率 */
	private void getDisplayMetrics() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		intScreenWidth = dm.widthPixels;
		intScreenHeight = dm.heightPixels;
		Log.i(TAG, Integer.toString(intScreenWidth));
	}
	
	/* get a fully initialized SurfaceHolder */
	private void getSurfaceHolder() {

		// 使SurfaceView可以被截图
		mSurfaceView01.setDrawingCacheEnabled(true);

		mSurfaceHolder = mSurfaceView01.getHolder();
		mSurfaceHolder.addCallback(this);
		//mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	 
	/**
	 * 转换YUV420SP到rgb的代码
	 * 
	 * @param rgb
	 * @param yuv420sp
	 * @param width
	 * @param height
	 */
	static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width,
			int height) {
		final int frameSize = width * height;
		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}
				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);
				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;
				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}

	private Bitmap makebitmap(Activity activity) {
		// View是你需要截图的View
		View view = activity.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap b1 = view.getDrawingCache();

		Rect frame = new Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
		int statusBarHeight = frame.top;

		int width = activity.getWindowManager().getDefaultDisplay().getWidth();
		int height = activity.getWindowManager().getDefaultDisplay()
				.getHeight();
		Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height
				- statusBarHeight);
		view.destroyDrawingCache();
		return b;
	}
	
	/**
	 * 定时发送位置状态的守护进程动作
	 */
	@Override
	public void run() {
		while (ThreadFlag) {
			
			//每一个周期执行一次截图操作并发送模拟低帧率视频流
			takeAShotCut();
			
			try {
				Thread.sleep(cycleMiliseconds);
			} catch (Exception ex) {
				if (D)
					Log.d(TAG, "睡眠死……这……不可能吧？……");
			}
		}
	}
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		frameData = data.clone();

		// get the prew frame here,the data of default is YUV420_SP

		// you should change YUV420_SP to YUV420_P

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (bPreviewing) {
			camera.stopPreview();
		}
		Camera.Parameters params = camera.getParameters();
		// params.setPreviewSize(width, height);
		camera.setParameters(params);
		try {
			camera.setPreviewDisplay(mSurfaceHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		camera.startPreview();
		bPreviewing = true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!bPreviewing) {
			camera = Camera.open();
		}
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopPreview();
		bPreviewing = false;
		camera.release();
		mSurfaceHolder = null;
	}
	/* func:弹出toast,主要做测试用 */
	private void jumpOutAToast(String string) {
		Toast.makeText(VideoSmackCarControllerActivity.this, string, Toast.LENGTH_SHORT)
				.show();
	}

	/* func:停止preview,释放Camera对象 */
	private void resetCamera() {
		if (camera != null && bPreviewing) {
			camera.stopPreview();
			/* 释放Camera对象 */
			camera.release();
			camera = null;
			bPreviewing = false;
		}
	}

	private void takeAPicture() throws IOException {
		if (camera != null && bPreviewing) {
			/* 调用takePicture()方法拍照 */
			camera.takePicture(null, null, jpegCallback);// 调用PictureCallback
															// interface的对象作为参数
		}else{
			
			//initCamera();
			camera.takePicture(null, null, jpegCallback);// 调用PictureCallback
			
		}
	}

	private PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			/*
			 * resetCamera(); try { initCamera(); } catch(Exception e) {
			 * Log.e(TAG, "initCamera Error after snapping"); }
			 */
			camera.startPreview();

			/*
			 * 发送图片的相关代码
			 */
			String to = mRecipient.getText().toString();
			String text = "Photo-JPEG";
			Message msg = new Message(to, Message.Type.chat);
			msg.setBody(text);

			DefaultPacketExtension jpegExtension = new DefaultPacketExtension(
					EElementName, ENameSpace);
			jpegExtension.setValue(EValueName, Base64.encodeBytes(data));
			jpegExtension.setValue(ETimeName, System.currentTimeMillis()+"");
			msg.addExtension(jpegExtension);

			if (D)
				Log.d(TAG, "+++ Jpeg Extended +++"
						+ Base64.encodeBytes(data).substring(0, 40));

			sendMessage(msg);

		}
	};
	private void takeAShotCut() {
		if (camera != null && bPreviewing) {
			/* 调用截图方法拍照方法拍照 */
			// Bitmap newBitmap = mSurfaceView01.getDrawingCache();
			/*
			 * Bitmap newBitmap = this.makebitmap(this); newBitmap =
			 * BitmapFactory.decodeByteArray(YUVData, 0, YUVData.length);
			 */
			byte[] YUVData = frameData.clone();
			int imageWidth = camera.getParameters().getPreviewSize().width;
			int imageHeight = camera.getParameters().getPreviewSize().height;
			int RGBData[] = new int[imageWidth * imageHeight];
			byte[] mYUVData = new byte[YUVData.length];
			System.arraycopy(YUVData, 0, mYUVData, 0, YUVData.length);
			decodeYUV420SP(RGBData, mYUVData, imageWidth, imageHeight);

			Bitmap newBitmap = Bitmap.createBitmap(imageWidth, imageHeight,
					Bitmap.Config.ARGB_8888);
			newBitmap.setPixels(RGBData, 0, imageWidth, 0, 0, imageWidth,
					imageHeight);

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] jpegArray = stream.toByteArray();

			/*
			 * 发送图片的相关代码
			 */
			String to = mRecipient.getText().toString();
			String text = "ShotCut-JPEG";
			Message msg = new Message(to, Message.Type.chat);
			msg.setBody(text);

			DefaultPacketExtension jpegExtension = new DefaultPacketExtension(
					EElementName, ENameSpace);
			jpegExtension.setValue(EValueName, Base64.encodeBytes(jpegArray));
			jpegExtension.setValue(ETimeName, System.currentTimeMillis()+"");
			msg.addExtension(jpegExtension);
			

			if (D)
				Log.d(TAG,
						"+++ Jpeg Extended +++"
								+ Base64.encodeBytes(jpegArray)
										.substring(0, 40));

			sendMessage(msg);
		}
	}
	/*
	 * function: 非preview时：实例化Camera,开始preview 非preview时and相机打开时：再设置一次preview
	 * preview时：不动作
	 */
	private void initCamera() throws IOException {
		if (!bPreviewing) {
			/* 若相机非在预览模式，则打开相机 */
			camera = Camera.open();
		}
		// 非预览时and相机打开时，开启preview
		if (camera != null && !bPreviewing) {
			Log.i(TAG, "inside the camera");
			/* 创建Camera.Parameters对象 */
			Camera.Parameters parameters = camera.getParameters();
			/* 设置相片格式为JPEG */
			// parameters.setPictureFormat(PixelFormat.JPEG);
			/* 指定preview的屏幕大小 */
			// parameters.setPreviewSize(intScreenWidth, intScreenHeight);
			/* 设置图片分辨率大小 (不能用) */
			// parameters.setPictureSize(intScreenWidth, intScreenHeight);
			/* 将Camera.Parameters设置予Camera */
			camera.setParameters(parameters);
			/* setPreviewDisplay唯一的参数为SurfaceHolder */
			camera.setPreviewDisplay(mSurfaceHolder);
			camera.setPreviewCallback(this);// 设置预览帧的接口,就是通过这个接口，我们来获得预览帧的数据的
			/* 立即运行Preview */
			camera.startPreview();
			bPreviewing = true;
		}
	}
	
	/**
	 * 销毁守护进程
	 */
	public void ThreadDestory()
	{
		ThreadFlag = false;
		Guarder = null;		
	}
	/*----------------------------------------------------------视频部分结束----------------
	/**
	 * 统一的查找窗体和按钮监听事件，独立出来是为了作横屏转换的时候更方便
	 */
	private void initFindsAndListeners() {
		mRecipient = (EditText) this.findViewById(R.id.recipient);
		mSendText = (EditText) this.findViewById(R.id.sendText);
		mList = (ListView) this.findViewById(R.id.listMessages);
		//mImageView = (ImageView) this.findViewById(R.id.imageView1);
		// Dialog for getting the xmpp settings
		mDialog = new SettingsDialog(this);
		mSurfaceView01 = (SurfaceView) findViewById(R.id.surfaceView1);
		// Set a listener to show the settings dialog
		Button setup = (Button) this.findViewById(R.id.setup);
		setup.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mDialog.show();
					}
				});
			}
		});

		Button send = (Button) this.findViewById(R.id.send);
		send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = mSendText.getText().toString();
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);

				while (messages.size() > 50) // 防止消息过多
				{
					messages.remove(messages.size() - 1);
				}
				messages.add(0, text);
				messages.add(0, connection.getUser() + ":");

				// 新建一个会话
				if (newChat == null) {// "test@a-7a511a1a957b4"
					String fName = mRecipient.getText().toString();
					if (!fName.contains("@"))
						fName = fName + "@" + serverName;
					newChat = chatmanager.createChat(fName,
							new MessageListener() {
								public void processMessage(Chat chat,
										Message message) {
									System.out.println("Received from 【"
											+ message.getFrom() + "】 message: "
											+ message.getBody());
								}
							});
				}
				try {
					newChat.sendMessage(msg);
				} catch (XMPPException e) {
					e.printStackTrace();
				}

				// Add the incoming message to the list view
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						setListAdapter();
					}
				});
			}
		});
		setListAdapter();

		/*
		 * 这里开始是横屏各种按钮的初始化
		 */
		Button MoveLF = (Button) this.findViewById(R.id.buttonLF);
		MoveLF.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnL 40 moveF 50 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveF = (Button) this.findViewById(R.id.buttonF);
		MoveF.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnL 0 moveF 50 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveRF = (Button) this.findViewById(R.id.buttonRF);
		MoveRF.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnR 40 moveF 50 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveL = (Button) this.findViewById(R.id.buttonL);
		MoveL.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnL 100 moveF 40 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveM = (Button) this.findViewById(R.id.buttonM);
		MoveM.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnL 0 moveF 0 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveR = (Button) this.findViewById(R.id.buttonR);
		MoveR.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnR 100 moveF 40 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveLB = (Button) this.findViewById(R.id.buttonLB);
		MoveLB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnL 100 moveB 40 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveB = (Button) this.findViewById(R.id.buttonB);
		MoveB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnL 0 moveB 50 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});
		Button MoveRB = (Button) this.findViewById(R.id.buttonRB);
		MoveRB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String to = mRecipient.getText().toString();
				String text = "#command action turnR 100 moveB 40 keep "
						+ moveContinueTime;
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				sendMessage(msg);
			}
		});

		// Eye相关按钮与命令初始化
		mCycleTime = (EditText) this.findViewById(R.id.editTextCycle);

		btn_capture = (Button) this.findViewById(R.id.buttonEyeOn);
		btn_capture.setOnClickListener(new View.OnClickListener() {
			 
			@Override
			public void onClick(View v) {
				jumpOutAToast("拍照");
				try {
					takeAPicture();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		Button EyeOff = (Button) this.findViewById(R.id.buttonEyeOff);
		EyeOff.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				resetCamera();
			}
		});
		 btn_shotcut = (Button) this.findViewById(R.id.buttonPhoto);
		 btn_shotcut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				jumpOutAToast("截图");
				takeAShotCut();
			}
		});

		// 单选按钮调用方法
		rg = (RadioGroup) findViewById(R.id.radioGroup1);
		b0 = (RadioButton) findViewById(R.id.radio0);
		b1 = (RadioButton) findViewById(R.id.radio1);
		b2 = (RadioButton) findViewById(R.id.radio2);
		b3 = (RadioButton) findViewById(R.id.radio3);
		rg.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == b0.getId()) {
					moveContinueTime = 800;
				} else if (checkedId == b1.getId()) {
					moveContinueTime = 200;
				} else if (checkedId == b2.getId()) {
					moveContinueTime = 100;
				} else if (checkedId == b3.getId()) {
					moveContinueTime = 50;
				}
			}

		});
		
		/* 取得屏幕解析像素 */
		getDisplayMetrics();
		//findViews();
		getSurfaceHolder();
	}

	/**
	 * 统一的消息发送方法
	 * 
	 * @param msg
	 *            要发送的消息
	 */
	private void sendMessage(Message msg) {
		while (messages.size() > 50) // 防止消息过多
		{
			messages.remove(messages.size() - 1);
		}
		messages.add(0, msg.getBody());
		messages.add(0, connection.getUser() + ":");

		// 新建一个会话
		if (newChat == null) {// "test@a-7a511a1a957b4"
			String fName = mRecipient.getText().toString();
			if (!fName.contains("@"))
				fName = fName + "@" + serverName;
			newChat = chatmanager.createChat(fName, new MessageListener() {
				public void processMessage(Chat chat, Message message) {
					System.out.println("Received from 【" + message.getFrom()
							+ "】 message: " + message.getBody());
				}
			});
		}
		try {
			newChat.sendMessage(msg);
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		// Add the incoming message to the list view
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				setListAdapter();
			}
		});
	}

	/**
	 * Called by Settings dialog when a connection is establised with the XMPP
	 * server
	 * 
	 * @param connection
	 */
	public void setConnection(XMPPConnection connection) {
		this.connection = (XMPPConnection) connection;
		if (connection != null) {
			try {
				// XMPPConnection.DEBUG_ENABLED = true;
				// //我的电脑IP:10.16.25.90
				// final ConnectionConfiguration connectionConfig = new
				// ConnectionConfiguration("192.168.8.194", 5222,
				// "a-7a511a1a957b4");
				// connectionConfig.setSASLAuthenticationEnabled(false);
				// connection = new XMPPConnection(connectionConfig);
				// connection.connect();//连接
				// connection.login("whb", "874553");//登陆
				chatmanager = connection.getChatManager();

				// 监听被动接收消息，或广播消息监听器
				chatmanager.addChatListener(new ChatManagerListener() {
					@Override
					public void chatCreated(Chat chat, boolean createdLocally) {
						chat.addMessageListener(new MessageListener() {
							@Override
							public void processMessage(Chat chat,
									Message message) {
								if (message.getBody() != null) {

									try {
										MessageHook(message);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} // 用于检测控制命令的消息钩子

									String fromName = StringUtils
											.parseBareAddress(message.getFrom());
									while (messages.size() > 50) // 防止消息过多
									{
										messages.remove(messages.size() - 1);
									}
									messages.add(0, message.getBody());
									messages.add(0, fromName + ":");
									// Add the incoming message to the list view
									mHandler.post(new Runnable() {
										@Override
										public void run() {
											setListAdapter();
										}
									});
								}
							}

						});
					}
				});
				// 发送消息
				// newChat.sendMessage("我是菜鸟");

				// 获取花名册
				Roster roster = connection.getRoster();
				Collection<RosterEntry> entries = roster.getEntries();
				for (RosterEntry entry : entries) {
					System.out.print(entry.getName() + " - " + entry.getUser()
							+ " - " + entry.getType() + " - "
							+ entry.getGroups().size());
					Presence presence = roster.getPresence(entry.getUser());
					System.out.println(" - " + presence.getStatus() + " - "
							+ presence.getFrom());
				}

				// 添加花名册监听器，监听好友状态的改变。
				roster.addRosterListener(new RosterListener() {

					@Override
					public void entriesAdded(Collection<String> addresses) {
						System.out.println("entriesAdded");
					}

					@Override
					public void entriesUpdated(Collection<String> addresses) {
						System.out.println("entriesUpdated");
					}

					@Override
					public void entriesDeleted(Collection<String> addresses) {
						System.out.println("entriesDeleted");
					}

					@Override
					public void presenceChanged(Presence presence) {
						System.out.println("presenceChanged - >"
								+ presence.getStatus());
					}

				});

				// 创建组
				// /RosterGroup group = roster.createGroup("大学");
				// for(RosterEntry entry : entries) {
				// group.addEntry(entry);
				// }
				for (RosterGroup g : roster.getGroups()) {
					for (RosterEntry entry : g.getEntries()) {
						System.out.println("Group " + g.getName() + " >> "
								+ entry.getName() + " - " + entry.getUser()
								+ " - " + entry.getType() + " - "
								+ entry.getGroups().size());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void setListAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.multi_line_list_item, messages);
		mList.setAdapter(adapter);
	}

 
//	/**
//	 * 直接调用setImageBitmap是无法正常显示的，经过查资料得知必须在一个独立的线程中才行
//	 * 
//	 * @author Lynx
//	 * 
//	 */
//	private class setMapThread implements Runnable {
//		public Bitmap mBitmap = null;
//
//		public void setB(Bitmap mBitmap) {
//			this.mBitmap = mBitmap;
//		}
//
//		// run方法会在UI线程中执行
//		public void run() {
//
//			mImageView.setImageBitmap(mBitmap);
//			if (D)
//				Log.d(TAG, "+++ Jpeg Sitted +++");
//		}
//	}

	/**
	 * 用于检测消息的钩子函数（这里用来检测并执行控制命令）
	 * 
	 * @param messageIn
	 *            将要被检测的消息
	 * @throws IOException 
	 */
	private void MessageHook(Message messageIn) throws IOException {
		boolean needReply = false;
		boolean isFound = false;
		String HookResult = "";

		if (D)
			Log.d(TAG, "+++ ON HOOK +++");

		// 调用正则表达式来判断并提取控制命令
		String input = messageIn.getBody();

		// #command action turnL 64 moveF 7 keep 200
		Pattern pattern = Pattern
				.compile("^#command\\saction\\sturn([LR])\\s(\\d+)\\smove([FB])\\s(\\d+)\\skeep\\s(\\d+)");
		Matcher matcher = pattern.matcher(input);
		String LR = "";
		String LRNum = "";
		String FB = "";
		String FBNum = "";
		String Keep = "";
		int LRInt = 0;
		int FBInt = 0;
		int KeepInt = 0;
		double LRDouble = 0;
		double FBDouble = 0;
		while (matcher.find()) { // Find each match in turn
			// Access a submatch group
			LR = matcher.group(1);
			LRNum = matcher.group(2);
			FB = matcher.group(3);
			FBNum = matcher.group(4);
			Keep = matcher.group(5);

			needReply = true;
			isFound = true;
			if (D)
				Log.d(TAG, "+++ Found Action +++");
		}
		if (isFound) {
			try // 尝试转换INT（之所以没有在正则表达式中判断越界是应为那样的代码过于难阅读）
			{
				LRInt = Integer.parseInt(LRNum);
				FBInt = Integer.parseInt(FBNum);
				KeepInt = Integer.parseInt(Keep);
			} catch (Exception e) {
				HookResult = "输入的数值不能转化为整型";
				if (D)
					Log.d(TAG, "+++ Not Integer +++");
				e.printStackTrace();
			}
			// 判断int数值是否越界
			if (LRInt < 0 || LRInt > 100 || FBInt < 0 || FBInt > 100
					|| KeepInt < 0 || KeepInt > 1000) {
				if (D)
					Log.d(TAG, "+++ Out of bond +++");
				HookResult = "输入的数值越界，请注意转向和速度值为0~100的整数，保持时间为0~1000的整数毫秒数";
			} else {
				if (LR.equals("L")) {
					if (FB.equals("F")) {
						if (D)
							Log.d(TAG, "+++ LF +++");
						LRDouble = LRInt * (-0.01);
						FBDouble = FBInt * (0.01);
						BroadContralCommand(LRDouble, FBDouble, KeepInt);
						HookResult = "收到命令：左转" + LRInt + "%，速度" + FBInt
								+ "%，前进" + KeepInt + "毫秒";
					} else {
						if (D)
							Log.d(TAG, "+++ LB +++");
						LRDouble = LRInt * (-0.01);
						FBDouble = FBInt * (-0.01);
						BroadContralCommand(LRDouble, FBDouble, KeepInt);
						HookResult = "收到命令：左转" + LRInt + "%，速度" + FBInt
								+ "%，后退" + KeepInt + "毫秒";
					}
				} else {
					if (FB.equals("F")) {
						if (D)
							Log.d(TAG, "+++ RF +++");
						LRDouble = LRInt * (0.01);
						FBDouble = FBInt * (0.01);
						BroadContralCommand(LRDouble, FBDouble, KeepInt);
						HookResult = "收到命令：右转" + LRInt + "%，速度" + FBInt
								+ "%，前进" + KeepInt + "毫秒";
					} else {
						if (D)
							Log.d(TAG, "+++ RB +++");
						LRDouble = LRInt * (0.01);
						FBDouble = FBInt * (-0.01);
						BroadContralCommand(LRDouble, FBDouble, KeepInt);
						HookResult = "收到命令：右转" + LRInt + "%，速度" + FBInt
								+ "%，后退" + KeepInt + "毫秒";
					}
				}

			}
		}
		isFound = false;  //加这句是为了防止下次的结果不分青红皂白覆盖前面的结果，删了试试就知道了

		// #command eye on every 2000
		pattern = Pattern
				.compile("^#command\\seye\\son\\severy\\s(\\d+)");
		matcher = pattern.matcher(input);
		String Cycle = "";
		int CycleInt = 0;
		while (matcher.find()) { // Find each match in turn
			// Access a submatch group
			Cycle = matcher.group(1);
			
			needReply = true;
			isFound = true;
			if (D)
				Log.d(TAG, "+++ Found Eye On +++");
		}
		if (isFound) {
			try // 尝试转换INT（之所以没有在正则表达式中判断越界是应为那样的代码过于难阅读）
			{
				CycleInt = Integer.parseInt(Cycle);
			} catch (Exception e) {
				HookResult = "输入的数值不能转化为整型";
				if (D)
					Log.d(TAG, "+++ Not Integer +++");
				e.printStackTrace();
			}
			// 判断int数值是否越界
			if (CycleInt < 0 || CycleInt > 10000) {
				if (D)
					Log.d(TAG, "+++ Out of bond +++");
				HookResult = "输入的数值越界，请注意监视周期值为0~10000（0~10秒）的整数。";
			} else {
				//远程眼打开
				cycleMiliseconds = CycleInt;
				ThreadCreate();
				HookResult = "远程眼已打开";
			}
		}
		isFound = false;  //加这句是为了防止下次的结果不分青红皂白覆盖前面的结果，删了试试就知道了

		// #command eye off
		pattern = Pattern.compile("^#command\\seye\\soff");
		matcher = pattern.matcher(input);
		while (matcher.find()) { // Find each match in turn
			// Access a submatch group
			needReply = true;
			isFound = true;
			if (D)
				Log.d(TAG, "+++ Found Eye Off +++");
		}
		if (isFound) {
			//远程眼关闭
			ThreadDestory();
			HookResult = "远程眼已关闭";
		}
		isFound = false;  //加这句是为了防止下次的结果不分青红皂白覆盖前面的结果，删了试试就知道了

		// #command eye photo
		pattern = Pattern.compile("^#command\\seye\\sphoto");
		matcher = pattern.matcher(input);
		while (matcher.find()) { // Find each match in turn
			// Access a submatch group
			needReply = true;
			isFound = true;
			if (D)
				Log.d(TAG, "+++ Found Eye Photo +++");
		}
		if (isFound) {
			//执行照相动作
			takeAPicture();
			HookResult = "远程眼拍照中...";
		}
		isFound = false;  //加这句是为了防止下次的结果不分青红皂白覆盖前面的结果，删了试试就知道了

		if (needReply) // 如果需要就发送回复
		{
			if (D)
				Log.d(TAG, "+++ To Find Chat +++");
			if (newChat != null) // 只有建立chat以后才进行回复
			{
				if (D)
					Log.d(TAG, "+++ Reply message +++");
				while (messages.size() > 50) // 防止消息过多
				{
					messages.remove(messages.size() - 1);
				}
				messages.add(0, HookResult);
				messages.add(0, "MessageHook" + ":");

				String to = mRecipient.getText().toString();
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(HookResult);

				try {
					newChat.sendMessage(msg);
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		}

	}
	/**
	 * 用于向小车控制程序发送控制广播
	 * 
	 * @param turn
	 *            转向的参数，0为正中，-1为左转最大角度，1为右转最大角度。
	 * @param move
	 *            移动的参数，0为静止，-1为后退最大速度，1为前进最大速度。
	 * @param keep
	 *            该动作持续的时间（0~1000毫秒）
	 */
	private void BroadContralCommand(double turn, double move, int keep) {
		Intent intent = new Intent(BroadcastName);
		intent.putExtra("Type", "Action");
		intent.putExtra("MilliSeconds", keep);
		intent.putExtra("TurnPosition", turn);
		intent.putExtra("MovePosition", move);
		this.sendBroadcast(intent);
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.main);
			initFindsAndListeners();
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			setContentView(R.layout.main);
			initFindsAndListeners();
		}
	}
	@Override
	protected void onDestroy() {
		ThreadDestory();  //先销毁守护进程
		connection.disconnect();
		System.exit(0);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// 被交换到后台时执行
		// camera.release();
		// camera = null;
		bPreviewing = false;
		super.onPause();
	}

	@Override
	public void onStop() {
		resetCamera();
		super.onStop();
	}

	@Override
	protected void onResume() {

		try {
			initCamera();
		} catch (IOException e) {
			Log.e(TAG, "initCamera() in Resume() erorr!");
		}
		super.onResume();
	}

	/**
	 * 创建用于定时发送位置状态的守护进程
	 */
	public void ThreadCreate()
	{
		if(Guarder != null)  //确保只有一个守护进程可以被创建
			return;
		Guarder = new Thread(this);
		ThreadFlag = true;
		Guarder.start();
	}
	
	
//	@Override
//	public void onPreviewFrame(byte[] data, Camera camera) {
//
//		frameData = data.clone();
//
//		// get the prew frame here,the data of default is YUV420_SP
//
//		// you should change YUV420_SP to YUV420_P
//
//	}

} 