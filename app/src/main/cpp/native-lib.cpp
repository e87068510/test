#include <jni.h>

#include <cstdio>
#include <cstdlib>
#include <linux/usbdevice_fs.h>
#include <sys/ioctl.h>
#include <string>
// We have 32 URBs
#define NUM_URBS       1
#define BUFFER_SIZE    8192
#define FRAME_SIZE     8192

char *getURBs(int fd, int ep)
{
    struct usbdevfs_urb urbs[NUM_URBS];
    struct usbdevfs_bulktransfer bt;
    int len = 8192;
    int sizeCount = len;
    unsigned int urb_num = 0;

    // Allocate buffer for image
    char *buf = (char *)malloc(len * sizeof(char));

    //Send out initial URBs
    memset(urbs, 0, sizeof urbs);
    for (unsigned int i = 0; i < NUM_URBS; i++) {
        urbs[i].type = USBDEVFS_URB_TYPE_BULK;
        urbs[i].endpoint = ep;
        urbs[i].buffer = buf + (i * BUFFER_SIZE);
        urbs[i].buffer_length = (sizeCount < BUFFER_SIZE) ? sizeCount : BUFFER_SIZE;
        urbs[i].actual_length = (sizeCount < BUFFER_SIZE) ? sizeCount : BUFFER_SIZE;

        if (sizeCount > BUFFER_SIZE)
            sizeCount -= BUFFER_SIZE;

        if (ioctl(fd, USBDEVFS_SUBMITURB, &urbs[i]) < 0) {
            free(buf);
            return NULL;
        }
    }

    //Wait for completions
    while(urb_num < NUM_URBS) {

        struct usbdevfs_urb *urb;

        if (ioctl(fd, USBDEVFS_REAPURB, &urb) < 0) {
            free(buf);
            return NULL;
        }

        // Completed early
        if (urb->actual_length < BUFFER_SIZE)
            break;

        urb_num++;
    }

    return buf;
}
/*
char *pixelArrange(char * frame){
    int FrmaeSize = 524288;
    char *buf = (char *)malloc(FrmaeSize * sizeof(char));
    //buf = frame;
    char FrameData[FrmaeSize];
    int ChNum =128;
    int DepthSmp = 4096;
    for(int i= 0; i< ChNum; i++){
        for(int j = 0; j<DepthSmp; j++){
            *buf =
        }
    }
    return buf;
}
*/


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_itri_uschart_Activity_USChartActivity_ByteArrayFromJNI(JNIEnv *env, jobject thiz, jint fd,
                                                               jint ep) {
    // TODO: implement ByteArrayFromJNI()
    int fileDescrption = fd;
    int Endpoint = ep;
    char* frame =  getURBs(fileDescrption, Endpoint);
    jbyteArray OutputArray = env->NewByteArray(FRAME_SIZE);
    env ->SetByteArrayRegion(OutputArray, 0, FRAME_SIZE, reinterpret_cast<jbyte*>(frame));
    free(frame);
    return OutputArray;
}