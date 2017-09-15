package com.example.cf_alarmclock;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author 蔡有飞 E-mail: caiyoufei@looip.cn
 * @version 创建时间：2014-2-17 下午3:12:31
 * 
 */
public class MainActivity extends Activity {
	// 闹钟包名
	private String clockPackageName = "";
	// 闹钟Activity名
	private String activityName = "";
	// 是否是第一次从该程序打开闹钟
	private boolean isFirst = true;
	// 线程操作标记
	private final static int SET = 0x01;
	// 调用系统闹钟按钮
	private Button callSystemClock;
	// 取得所有安装软件信息
	private List<PackageInfo> allPackageInfos;
	// 取得自己安装的软件信息
	private List<PackageInfo> userPackageInfos;
	// 取得系统安装的软件信息
	private List<PackageInfo> sysPackageInfos;
	// 系统软件显示
	private TextView cTextViewSys;
	// 安装软件显示
	private TextView cTextViewUser;
	// 系统软件信息
	String sysNameString = "系统软件：\n";
	// 安装软件信息
	String userNameString = "用户安装软件：\n";
	// 线程消息处理
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0x01:
				callSystemClock.setClickable(true);
				callSystemClock.setText("扫描完成,去设置闹铃");
				((ScrollView) findViewById(R.id.sv)).setClickable(true);
				Toast.makeText(MainActivity.this, "程序加载完成", Toast.LENGTH_SHORT)
						.show();
				break;
			case 0x02:
				cTextViewSys.setText(sysNameString);
				cTextViewUser.setText(userNameString);
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (getInfo().getBoolean("isfirst", true) == false) {
			clockPackageName = getInfo().getString("clockPackageName", "");
			activityName = getInfo().getString("activityName", "");
			isFirst = false;
		}
		// 启动线程加载安装程序
		new Thread(new SeachThread()).start();
		cTextViewSys = (TextView) findViewById(R.id.tv_sys);
		cTextViewUser = (TextView) findViewById(R.id.tv_user);
		callSystemClock = (Button) findViewById(R.id.btn_openAlarmClock);
		callSystemClock.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isFirst) {
					Toast.makeText(MainActivity.this, "第一次从这打开闹钟！",
							Toast.LENGTH_SHORT).show();
					// 跳转到系统闹钟
					call();
				} else {
					Toast.makeText(MainActivity.this, "不止一次从这打开闹钟！",
							Toast.LENGTH_SHORT).show();
					if (!(clockPackageName.equals("") || activityName
							.equals(""))) {
						try {
							Intent intent = new Intent();
							intent.setComponent(new ComponentName(
									clockPackageName, activityName));
							startActivity(intent);
						} catch (Exception e) {
							Toast.makeText(MainActivity.this,
									"请确保手机有闹铃并重新安装该程序！", Toast.LENGTH_SHORT)
									.show();
						}
					}
				}
			}
		});
	}

	/**
	 * @version 更新时间：2014-2-17 下午3:19:28
	 */
	private void call() {
		// activity名
		String activityName = "";
		// 得到程序包名
		String packageName = "";
		// 闹钟包名
		String clockPackageName = "";
		// 循环取出系统程序
		for (int i = 0; i < sysPackageInfos.size(); i++) {
			PackageInfo packageInfo = sysPackageInfos.get(i);
			packageName = packageInfo.packageName;
			if (packageName.indexOf("clock") != -1) {
				// 找到系统闹钟了
				if (!(packageName.indexOf("widget") != -1)) {
					// 取出activity信息
					ActivityInfo activityInfo = packageInfo.activities[0];
					// 取出activity名字
					activityName = activityInfo.name;
					clockPackageName = packageName;
					saveInfo(false, clockPackageName, activityName);
					break;
				}
			}
		}
		if ((activityName != "") && (clockPackageName != "")) {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(clockPackageName,
					activityName));
			startActivity(intent);
		} else {
			Toast.makeText(this, "启动闹钟失败！", Toast.LENGTH_SHORT).show();
		}
	}

	// ***************--------*异步线程加载加载安装程序*--------------*******************//
	private class SeachThread extends Thread {
		@Override
		public void run() {
			// 取得系统安装所有软件信息
			allPackageInfos = getPackageManager().getInstalledPackages(
					PackageManager.GET_UNINSTALLED_PACKAGES
							| PackageManager.GET_ACTIVITIES);
			// 定义用户安装软件信息包
			userPackageInfos = new ArrayList<PackageInfo>();
			// 定义系统安装软件信息包
			sysPackageInfos = new ArrayList<PackageInfo>();
			// 循环取出所有软件信息
			for (int i = 0; i < allPackageInfos.size(); i++) {
				// 得到每个软件信息
				PackageInfo temp = allPackageInfos.get(i);
				ApplicationInfo appInfo = temp.applicationInfo;
				if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
						|| (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
					// 系统软件
					sysPackageInfos.add(temp);
					// 应用程序名
					sysNameString += "\n"
							+ MainActivity.this.getPackageManager()
									.getApplicationLabel(temp.applicationInfo)
									.toString() + ":\t"
							+ temp.applicationInfo.packageName;
				} else {
					// 用户自己安装软件
					userPackageInfos.add(temp);
					// 应用程序名
					userNameString += "\n"
							+ MainActivity.this.getPackageManager()
									.getApplicationLabel(temp.applicationInfo)
									.toString() + ":\t"
							+ temp.applicationInfo.packageName;
				}
				mHandler.sendEmptyMessage(0x02);
			}
			// 查询完成，发送新消息
			Message msg = new Message();
			// 操作标记
			msg.what = SET;
			mHandler.sendMessage(msg);
		}
	};

	/**
	 * 保存配置信息
	 * 
	 * @version 更新时间：2014-2-17 下午4:41:26
	 * @param isfirst
	 *            是否是第一次从该程序打开闹钟
	 * @param clockPackageName
	 *            闹钟包名
	 * @param activityName
	 *            闹钟Activity名
	 */
	private void saveInfo(boolean isfirst, String clockPackageName,
			String activityName) {
		SharedPreferences sp = this.getSharedPreferences("AlramClock",
				MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putBoolean("isfirst", isfirst);
		editor.putString("clockPackageName", clockPackageName);
		editor.putString("activityName", activityName);
		editor.commit();
	}

	/**
	 * 读取配置信息
	 * 
	 * @version 更新时间：2014-2-17 下午4:41:19
	 * @return
	 */
	private SharedPreferences getInfo() {
		return this.getSharedPreferences("AlramClock", MODE_PRIVATE);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "关于").setIcon(

		android.R.drawable.ic_menu_info_details);

		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "帮助").setIcon(

		android.R.drawable.ic_menu_help);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			new AlertDialog.Builder(this)
					.setMessage(
							"作者:蔡有飞\n\n版权归上海持创信息技术有限公司所有\n\n任何人不得修改本程序后宣传本作品 ")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface dialoginterface, int i) {
									// 按钮事件
								}
							}).setIcon(android.R.drawable.ic_menu_info_details)
					.setTitle("作者").show();
			break;

		case Menu.FIRST + 2:

			new AlertDialog.Builder(this)
					.setMessage("使用过程中如有问题或建议\n请发邮件至caiyoufei@looip.cn")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface dialoginterface, int i) {
									// 按钮事件
								}
							}).setTitle("帮助")
					.setIcon(android.R.drawable.ic_menu_help).show();
			break;
		}
		return false;
	}
}