package com.example.billgao.armagazine;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by BillGao on 2016/5/19.
 */
public class Render implements GLSurfaceView.Renderer {

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        MainActivity.nativeInitGL();
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        MainActivity.nativeResizeGL(w, h);
    }

    public void onDrawFrame(GL10 gl) {
        MainActivity.nativeRender();
    }

}
