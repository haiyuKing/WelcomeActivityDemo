package com.why.project.welcomeactivitydemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @CreateUser HaiyuKing
 * @Used 欢迎界面
 * Android Handler leak 分析及解决办法：http://www.cnblogs.com/0616--ataozhijia/p/3672021.html
 */
public class WelcomActivity extends AppCompatActivity {
	
	/**倒计时文本*/
	private TextView mCountdownTextView;

	private static final int MSG_COUNT_WHAT = 99;
	private static final int NUM = 3;
	private int countdownNum;//倒计时的秒数
	private static Timer timer;//计时器
	private MyHandler countdownHandle;//用于控制倒计时子线程
	private Runnable runnable;//倒计时子线程
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*==========设置全屏======必须在setContentView前面=======*/
		/*set it to be no title*/
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*set it to be full screen*/
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_welcom);
		
		//初始化控件
		initView();

		//初始化Handler和Runnable
		initThread();
	}
	
	
	/**
	 * 初始化控件
	 * */
	private void initView(){
		mCountdownTextView = (TextView) findViewById(R.id.id_countdownTextView);
	}
	/**
	 * 初始化Handler和Runnable
	 * */
	private void initThread(){
		//倒计时变量
		initCountdownNum();
		//handler对象
		countdownHandle = new MyHandler(this);
		//runnable
		runnable = new Runnable() {
			
			@Override
			public void run() {
				//执行倒计时代码
				timer = new Timer();
				TimerTask task = new TimerTask() {
					public void run() {
						countdownNum --;
						
						Message msg = countdownHandle.obtainMessage();
	                    msg.what = MSG_COUNT_WHAT;//message的what值
	                    msg.arg1 = countdownNum;//倒计时的秒数
	                    
	                    countdownHandle.sendMessage(msg);
					}
				};
				timer.schedule(task,0,1000);
			}
		};
	}
	
	/**必须使用静态类：解决问题：This Handler class should be static or leaks might occur Android
	 * http://www.cnblogs.com/jevan/p/3168828.html*/
	private static class MyHandler extends Handler {
        // WeakReference to the outer class's instance.
        private WeakReference<WelcomActivity> mOuter;
        
        public MyHandler(WelcomActivity activity) {
            mOuter = new WeakReference<WelcomActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	
        	WelcomActivity theActivity = mOuter.get();
        	
            if (theActivity != null) {
            	
            	switch (msg.what) {
					case MSG_COUNT_WHAT:
						if(msg.arg1 == 0){//表示倒计时完成
							
							//在这里执行的话，不会出现-1S的情况
							if(timer != null){
								timer.cancel();//销毁计时器
							}
							
							//跳转到登录界面并销毁当前界面
							Intent intent = new Intent(theActivity, MainActivity.class);
							theActivity.startActivity(intent);
							
							theActivity.finish();
							
						}else{
							theActivity.mCountdownTextView.setText(msg.arg1 + "S");
						}
						break;
	
					default:
						break;
				}
            }
        }
    }
	
	/*
	 * Activity有三个状态：
	 * 运行——当它在屏幕前台时（位于当前任务堆栈的顶部）。它是激活或运行状态。它就是相应用户操作的Activity
	 * 暂停——当它失去焦点但仍然对用户可见时，它处于暂停状态
	 * 停止——完全被另一个Activity覆盖时则处于停止状态。它仍然保留所有的状态和成员信息。然而对用户是不可见的，所以它的窗口将被隐藏，如果其他地方需要内存，则系统经常会杀死这个Activity。
	 * 
	 * 运行：OnCreate——>OnStart——>OnResume
	 * 暂停：OnResume——>OnPause  再次重新运行：——>OnResume
	 * 停止：
	 * （1）切换到其他界面或者按home键回到桌面：OnPause——>OnStop   重新执行：——>OnRestart——>OnStart——>OnResume
	 * （2）退出整个应用或者finish()：OnPause——>OnStop——>OnDestroy   重新执行：——>OnCreate——>OnStart——>OnResume
	 * 
	 * */
	
	//1、正常状态下，运行——倒计时——跳转到登录界面，finish欢迎界面
	//2、用户在打开应用时，按home键返回到了桌面，过了一段时间再次打开了应用
	//3、在欢迎界面，手机出现了一个其他应用的提示对话框，此时实现的是继续倒计时，所以暂未处理
	
	@Override
	protected void onResume() {
		//开启线程
		countdownHandle.post(runnable);
		super.onResume();
		
	}
	
	@Override
	protected void onStop() {
		
		initCountdownNum();//初始化倒计时的秒数，这样按home键后再次进去欢迎界面，则会重新倒计时
		
		//在这里执行的话，用户点击home键后，不会继续倒计时进入登录界面
		if(timer != null){
			timer.cancel();//销毁计时器
		}
		
		//将线程销毁掉 
		countdownHandle.removeCallbacks(runnable);
		
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	/*初始化倒计时的秒数*/
	private void initCountdownNum(){
		countdownNum = NUM;
	}
}
