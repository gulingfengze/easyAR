package com.example.billgao.armagazine;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import cn.easyar.engine.EasyAR;

public class MainActivity extends Activity {
    
    static String key = "62c7dfaad566e5722cb375ed559dc1e85sVBrEhaxGGDl2s5XZrv05xMEuctogOHCkDwdtZ18Eghs1Fb2WQI02jTUFj48teulenK22IwKSk6mJyUikG3Biw5nwfbTLnPMNOlAU9lD0Fo9Kbt1tRLzSpSkd3wCNECEXI8V2dUdg5d9RMWiI2Z2T9zGtSq7pSJvH7NW8pQ";

    static {
        System.loadLibrary("EasyAR");
        System.loadLibrary("ARMagazine");
    }
    // uri列表
    static String uriList[] = {"http://www.baidu.com",
                                "https://h5.wps.cn/p/a36ecda6.html",
                                "https://h5.wps.cn/p/a36ecda6.html",
                                "http://baike.baidu.com/link?url=dubkZa8qna6BF_f6KNlrEkrOuqUQ0O4nlZ3w0aAlfeovuMTM8VFzuDNqdq-hdGCAOn7VIGWd3OUCFHTfwL-a0LBo-80uPOXyD8ax39eX_ZF3jk_O_6fOLkNonBHvEyEn",  // 老炮儿
                                "http://www.pm25mask.com/",     // 口罩
                                "http://www.imegabox.com/"};    // 美嘉
    


    public static native void nativeInitGL();
    public static native void nativeResizeGL(int w, int h);
    public static native void nativeRender();
    private native boolean nativeInit();        // 设置匹配图片namecard和目标配置文件target.json
    private native void nativeDestory();
    private native void nativeRestart();        // 重播视频
    private native void nativeRotationChange(boolean portrait);
    private native int nativeGetID();           // 获取当前检测图片的ID
    private native String nativeGetBar();       // 获取二维码信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        EasyAR.initialize(this, key);       // 使用key初始化EasyAR引擎
        nativeInit();                       // 设置匹配图片namecard和目标配置文件target.json

        GLView glView = new GLView(this);  // 继承至SurfaceView，它内嵌的surface专门负责OpenGL渲染。
        glView.setRenderer(new Render());
        glView.setZOrderMediaOverlay(true); // glView 置于视图的顶部（覆盖显示）

        // glView 覆盖在 preview 上显示
        ((ViewGroup) findViewById(R.id.preview)).addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        nativeRotationChange(getWindowManager().getDefaultDisplay().getRotation() == android.view.Surface.ROTATION_0);

        // 关联按钮
        ImageButton restartButton = (ImageButton) findViewById(R.id.restart_button);    // 重播视频
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "重播...", Toast.LENGTH_SHORT).show();
                nativeRestart();                    // 重播视频
            }
        });
        restartButton.setOnTouchListener(new View.OnTouchListener() {       // 按下按钮的效果
            @Override
            public boolean onTouch(View v, MotionEvent event) {             // 按下按钮的动画效果
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.replay1));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.replay));
                }
                return false;
            }
        });

        ImageButton browserButton = (ImageButton) findViewById(R.id.browser_button);       // 打开网页
        browserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ID = nativeGetID();
                if (ID != 0) {
                    final Uri uri = Uri.parse(uriList[ID - 1]);
                    final Intent it = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(it);
                }
            }
        });
        browserButton.setOnTouchListener(new View.OnTouchListener() {       // 按下按钮的效果
            @Override
            public boolean onTouch(View v, MotionEvent event) {             // 按下按钮的动画效果
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.browser1));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.browser));
                }
                return false;
            }
        });

        ImageButton barcodeButton = (ImageButton) findViewById(R.id.barcode_button);       // 打开二维码
        barcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/*                final String str = nativeGetBar();    // 获取二维码信息
                if (str != null) {
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                    AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();        // 显示提示框
                    alert.setIcon(R.drawable.ic_launcher);
                    alert.setTitle("发现二维码：");
                    alert.setMessage("是否打开链接：" + str + " ?");
                    alert.setButton(DialogInterface.BUTTON_POSITIVE, "打开", new DialogInterface.OnClickListener() {  // 打开按钮
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Uri uri = Uri.parse(str);
                            final Intent it = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(it);
                        }
                    });
                    alert.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { }
                    });
                    alert.show();
                *//*    final Uri uri = Uri.parse(str);
                    final Intent it = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(it);*//*
                }
                else {
                    Toast.makeText(MainActivity.this, "请扫描二维码", Toast.LENGTH_SHORT).show();
                }*/

               // Intent i=new Intent(MainActivity.this,PictureActivity.class);
               // startActivity(i);
               SharedAPPUtils.showShare(getApplicationContext());
            }
        });
     /*   barcodeButton.setOnTouchListener(new View.OnTouchListener() {       // 按下按钮的效果
            @Override
            public boolean onTouch(View v, MotionEvent event) {             // 按下按钮的动画效果
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.qrcode1));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.qrcode));
                }
                return false;
            }
        });*/
    }
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        nativeRotationChange(getWindowManager().getDefaultDisplay().getRotation() == android.view.Surface.ROTATION_0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nativeDestory();
    }
    @Override
    protected void onResume() {
        super.onResume();
        EasyAR.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        EasyAR.onPause();
    }
}
