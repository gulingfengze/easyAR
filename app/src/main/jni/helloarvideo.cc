
#include "ar.hpp"
#include "renderer.hpp"
#include <jni.h>
#include <cstring>
#include <string>
#include <GLES2/gl2.h>

using namespace std;

#define JNIFUNCTION_NATIVE(sig) Java_com_example_billgao_armagazine_MainActivity_##sig
#define maxn 6

extern "C" {
    JNIEXPORT jboolean JNICALL JNIFUNCTION_NATIVE(nativeInit(JNIEnv* env, jobject object));
    JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeDestory(JNIEnv* env, jobject object));
    JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeInitGL(JNIEnv* env, jobject object));
    JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeResizeGL(JNIEnv* env, jobject object, jint w, jint h));
    JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeRender(JNIEnv* env, jobject obj));
    JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeRotationChange(JNIEnv* env, jobject obj, jboolean portrait));
    JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeRestart(JNIEnv* env, jobject object));
    JNIEXPORT jint JNICALL JNIFUNCTION_NATIVE(nativeGetID(JNIEnv* env, jobject object));
    JNIEXPORT jstring JNICALL JNIFUNCTION_NATIVE(nativeGetBar(JNIEnv* env, jobject object));
};

namespace EasyAR {
namespace samples {

class HelloARVideo : public AR
{
public:
    HelloARVideo();
    ~HelloARVideo();
    virtual void initGL();
    virtual void resizeGL(int width, int height);
    virtual void render();
    virtual void restart();
    virtual int getID();
    virtual char * getBarUri();
    virtual bool clear();
private:
    Vec2I view_size;
    VideoRenderer* renderer[maxn];         // 3个ImageTarget
    int tracked_target;
    int active_target;
    int texid[maxn];
    ARVideo* video;
    VideoRenderer* video_renderer;
    string imageList[maxn] =  {"namecard","ld","wx","laopaoer","kouzhao","meijia"};
    string videoList[maxn] = {"video.mp4","ld.mp4","wx.mp4","laopaoer.mp4","kouzhao.mp4","meijia.mp4"};
    char * baruri;
};

HelloARVideo::HelloARVideo()
{
    view_size[0] = -1;
    tracked_target = 0;
    active_target = 0;
    for(int i = 0; i < maxn; ++i) {            // 为3个ImageTarget创建相应的变量
        texid[i] = 0;
        renderer[i] = new VideoRenderer;
    }
    video = NULL;
    video_renderer = NULL;
}

HelloARVideo::~HelloARVideo()
{
    for(int i = 0; i < maxn; ++i) {            //  释放每一个render的内存空间
        delete renderer[i];
    }
}

void HelloARVideo::initGL()
{
    augmenter_ = Augmenter();
    for(int i = 0; i < maxn; ++i) {            // 初始化
        renderer[i]->init();
        texid[i] = renderer[i]->texId();
    }
}

void HelloARVideo::resizeGL(int width, int height)
{
    view_size = Vec2I(width, height);           // 调整显示区域的长宽
}

void HelloARVideo::render()
{
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    Frame frame = augmenter_.newFrame(tracker_);
    if(view_size[0] > 0){
        AR::resizeGL(view_size[0], view_size[1]);
        if(camera_ && camera_.isOpened())               // 检查摄像头
            view_size[0] = -1;
    }
    augmenter_.drawVideoBackground();                   // 绘制视频的背景

    Frame barframe = augmenter_.newFrame(barcodescanner_);      // 二维码
    baruri = (char *)barframe.text();                                   // 二维码内的文字信息

    AugmentedTarget::Status status = frame.targets()[0].status();
    if(status == AugmentedTarget::kTargetStatusTracked){
        int id = frame.targets()[0].target().id();
        if(active_target && active_target != id) {
            video->onLost();                               // 丢失检测图片
            delete video;
            video = NULL;
            tracked_target = 0;
            active_target = 0;
        }
        if (!tracked_target) {                              // 丢失了目标图片
            if (video == NULL) {                           // 设置图片匹配的视频文件
                for (int k = 0; k < maxn; k++) {
                    if (frame.targets()[0].target().name() == imageList[k] && texid[k]) {
                        video = new ARVideo;
                        video->openVideoFile(videoList[k], texid[k]);
                        video_renderer = renderer[k];
                        break;
                    }
                }
         /*       else if(frame.targets()[0].target().name() == std::string("namecard") && texid[1]) {
                    video = new ARVideo;
                    video->openTransparentVideoFile("transparentvideo.mp4", texid[1]);
                    video_renderer = renderer[1];
                }
                else if(frame.targets()[0].target().name() == std::string("idback") && texid[2]) {
                    video = new ARVideo;
                    video->openStreamingVideo("http://7xl1ve.media1.z0.glb.clouddn.com/sdkvideo/EasyARSDKShow201520.mp4", texid[2]);
                    video_renderer = renderer[2];
                }*/
            }
            if (video) {
                video->onFound();           // 检测到图片
                tracked_target = id;
                active_target = id;
            }
        }
        Matrix44F projectionMatrix = getProjectionGL(camera_.cameraCalibration(), 0.2f, 500.f);
        Matrix44F cameraview = getPoseGL(frame.targets()[0].pose());                          // 获取Camera的视角，用于做投影变换
        ImageTarget target = frame.targets()[0].target().cast_dynamic<ImageTarget>();
        if(tracked_target) {
            video->update();                                                                // 更新Frame
            video_renderer->render(projectionMatrix, cameraview, target.size());            // 根据相机的视角对显示内容进行投影变换
        }
    }
    else {
        if (tracked_target) {
            video->onLost();
            tracked_target = 0;
        }
    }
}

void HelloARVideo::restart()        // 重播视频
{
    if(video){
        delete video;
        video = NULL;
        tracked_target = 0;
        active_target = 0;
    }
}

int HelloARVideo::getID()       // 获取 tracked_target;
{
    return tracked_target;
}

char * HelloARVideo::getBarUri()        // 获取二维码信息
{
    return baruri;
}

bool HelloARVideo::clear()          // 清除视频对象
{
    AR::clear();
    if(video){
        delete video;
        video = NULL;
        tracked_target = 0;
        active_target = 0;
    }
    return true;
}

}
}
EasyAR::samples::HelloARVideo ar;

JNIEXPORT jboolean JNICALL JNIFUNCTION_NATIVE(nativeInit(JNIEnv*, jobject))
{
    bool status = ar.initCamera();                  // 初始化摄像头
    ar.loadAllFromJsonFile("targets.json");         // 载入配置文件
  //  ar.loadFromImage("namecard.jpg");
    status &= ar.start();
    return status;
}

JNIEXPORT jint JNICALL JNIFUNCTION_NATIVE(nativeGetID(JNIEnv*, jobject))
{
    int ID = ar.getID();
    return ID;
}
JNIEXPORT jstring JNICALL JNIFUNCTION_NATIVE(nativeGetBar(JNIEnv* env, jobject object))
{
    char * pat  = ar.getBarUri();
    if (strlen(pat) == 0)
        return NULL;
    jstring rstStr = env->NewStringUTF(pat);
    return rstStr;
}
JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeDestory(JNIEnv*, jobject))
{
    ar.clear();
}

JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeRestart(JNIEnv*, jobject))
{
    ar.restart();
}

JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeInitGL(JNIEnv*, jobject))
{
    ar.initGL();
}

JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeResizeGL(JNIEnv*, jobject, jint w, jint h))
{
    ar.resizeGL(w, h);
}

JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeRender(JNIEnv*, jobject))
{
    ar.render();
}

JNIEXPORT void JNICALL JNIFUNCTION_NATIVE(nativeRotationChange(JNIEnv*, jobject, jboolean portrait))
{
    ar.setPortrait(portrait);
}
