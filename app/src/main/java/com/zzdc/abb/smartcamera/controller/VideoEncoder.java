package com.zzdc.abb.smartcamera.controller;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.zzdc.abb.smartcamera.common.Constant;
import com.zzdc.abb.smartcamera.util.BufferPool;
import com.zzdc.abb.smartcamera.util.LogTool;
import com.zzdc.abb.smartcamera.util.NV21Convertor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoEncoder implements VideoGather.VideoRawDataListener {
    private static final String TAG = VideoEncoder.class.getSimpleName();
    private boolean DEBUG = false;

    public static MediaFormat VIDEO_FORMAT;
    private static final String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int I_FRAME_INTERVAL = 1;
    private int mWidth;
    private int mHeight;
    private int mFps;

    private MediaCodec mEncoder;
    private volatile boolean mEncoding = false;
    private long mStartTimeUs;
    private MediaFormat videoFormat;
    private byte[] mYuv420;
    private Thread mInputThread;
    public volatile boolean videoEncoderLoop = false;
    public byte[] mPPS;
    private static final int START_ENCODER = 100;

    public static ByteBuffer PPS_BUFFER;
    public static MediaCodec.BufferInfo PPS_BUFFER_INFO = new MediaCodec.BufferInfo();

    private LinkedBlockingQueue<VideoGather.VideoRawBuf> mViewRawDataQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Message> mInputIndexQueue = new LinkedBlockingQueue<>();
    //TODO remove
    private BufferPool<EncoderBuffer> mVideoEncodeBufPool = new BufferPool<>(EncoderBuffer.class, 3);

    private VideoEncoder() {}
    private static VideoEncoder mInstance = new VideoEncoder();
    public static VideoEncoder getInstance() {
        return mInstance;
    }

    private VideoGather mVideoGather = null;

    private Handler mStartEncoderHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_ENCODER:
                    LogTool.d(TAG,"Start mediaCodec fail,restart it in Handler!!!");
                    start();
                    break;
                default:
                    break;
            }
        }
    };

    private HandlerThread mEncodeThread = new HandlerThread("Video encode thread");
    {
        mEncodeThread.start();
    }
    private Handler mEncodeHandler = new Handler( mEncodeThread.getLooper() ){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final MediaCodecInfo codecInfo = selectCodec(VIDEO_MIME_TYPE);
            if (codecInfo == null) {
                Log.e(TAG, "Unable to find an appropriate codec for " + VIDEO_MIME_TYPE);
                return;
            }
            Log.d(TAG, "Found video codec: " + codecInfo.getName());
            try {
                mEncoder = MediaCodec.createByCodecName(codecInfo.getName());
            } catch (IOException e) {
                throw new RuntimeException("Create encoder failed!", e);
            }
        }
    };

    class InputThread extends Thread {


        public void run() {
            while (videoEncoderLoop && !Thread.interrupted()) {
                try {
                    int inputBufferIndex = mInputIndexQueue.take().arg1;
                    if (inputBufferIndex >= 0) {
                        VideoGather.VideoRawBuf buf = mViewRawDataQueue.take();
                        debug("Handle onInputBufferAvailable, buffer index = " + inputBufferIndex);
                        if (selectColorFormat(VIDEO_MIME_TYPE) == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            NV21Convertor.Nv21ToYuv420SP(buf.getData(), mYuv420, mWidth, mHeight);
                        }
                        buf.decreaseRef();

                        ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
                        inputBuffer.put(mYuv420);
                        long pts = System.currentTimeMillis() * 1000 - mStartTimeUs;
                        if (!mEncoding) {
                            Log.d(TAG, "Send Video Encoder with flag BUFFER_FLAG_END_OF_STREAM");
                            mEncoder.queueInputBuffer(inputBufferIndex, 0, mYuv420.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            //TODO Check flag
                            mEncoder.queueInputBuffer(inputBufferIndex, 0, mYuv420.length, pts, 0);
                        }
                    }
                } catch(InterruptedException e){
                    LogTool.w(TAG, "Take Video raw data from queue with exception. ", e);
                } catch (IllegalStateException e) {
                    LogTool.e(TAG,"VideoRawBuf onInputBufferAvailable error !!! ",e);
                }
            }
            unregisterVideoRawDataListener();
            if(mEncoder!=null){
                mEncoder.reset();
            }
            mInputIndexQueue.clear();
            mViewRawDataQueue.clear();
            LogTool.w(TAG, "Encoder input thread stop. InputIndex_Que size ="+mInputIndexQueue.size());
        }
    }


//    private HandlerThread mInputThread = new HandlerThread("Video encode input thread");
//    {
//        mInputThread.start();
//    }
//    private Handler mInputHandler = new Handler(mInputThread.getLooper()){
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            MediaCodec mediaCodec = (MediaCodec)msg.obj;
//            int i = msg.arg1;
//            try {
//                VideoGather.VideoRawBuf buf = mViewRawDataQueue.take();
//                debug("Handle onInputBufferAvailable, buffer index = " + i);
//                Log.d("keming","Handle onInputBufferAvailable, buffer index = " + i);
//                if (selectColorFormat(VIDEO_MIME_TYPE) == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
//                    NV21Convertor.Nv21ToYuv420SP(buf.getData(), mYuv420, mWidth, mHeight);
//                }
//                buf.decreaseRef();
//
//                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(i);
//                inputBuffer.put(mYuv420);
//                long pts = System.currentTimeMillis() * 1000 - mStartTimeUs;
//                if (!mEncoding) {
//                    Log.d(TAG, "Send Video Encoder with flag BUFFER_FLAG_END_OF_STREAM");
//                    mediaCodec.queueInputBuffer(i, 0, mYuv420.length, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                } else {
//                    //TODO Check flag
//                    mediaCodec.queueInputBuffer(i, 0, mYuv420.length, pts, 0);
//                }
//            } catch (InterruptedException e) {
//                LogTool.w(TAG, "Take Video raw data from queue with exception. ", e);
//            }
//        }
//    };
    private void unregisterVideoRawDataListener(){
        mVideoGather.unregisterVideoRawDataListener(this);
    }

    private MediaCodec.Callback mEncodeCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            debug("onInputBufferAvailable start, buffer index = " + i);
            Message msg = new Message();
            msg.obj = mediaCodec;
            msg.arg1 = i;
//            mInputHandler.sendMessage(msg);
            mInputIndexQueue.offer(msg);
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            debug("onOutputBufferAvailable start. buffer index = " + i);
            if( !videoEncoderLoop)
                return;

            if (bufferInfo.size != 0) {
                ByteBuffer buffer = mediaCodec.getOutputBuffer(i);
                int type = buffer.get(4) & 0x1F;
                if (type == 7 || type == 8) {
                    mPPS = new byte[bufferInfo.size];
                    buffer.get(mPPS);
                    Constant.PPS = mPPS;

                    PPS_BUFFER = ByteBuffer.wrap(mPPS);
                    PPS_BUFFER_INFO.set(0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                    buffer.position(bufferInfo.offset);
                    LogTool.d(TAG, "Find PPS size = " + bufferInfo.size);
                }

                EncoderBuffer buf = mVideoEncodeBufPool.getBuf(buffer.remaining());
                buf.put(buffer, bufferInfo);
                debug("VideoEncoderListener.size = " + mVideoEncoderListeners.size());
                if (mVideoEncoderListeners.size() > 0) {
                    for (VideoEncoderListener listener : mVideoEncoderListeners) {
                        listener.onVideoEncoded(buf);
                    }
                }
                buf.decreaseRef();
                //TODO check the render value
                mediaCodec.releaseOutputBuffer(i, false);
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            //TODO
            LogTool.w(TAG, "Video encode callback with onError", e);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            LogTool.d(TAG, "onOutputFormatChanged start");
            VIDEO_FORMAT = mediaFormat;
            if (mVideoEncoderListeners.size() > 0) {
                for (VideoEncoderListener listener : mVideoEncoderListeners) {
                    listener.onVideoFormatChanged(mediaFormat);
                }
            }
        }
    };

    public void init() {
        LogTool.d(TAG, "init");
        mVideoGather = VideoGather.getInstance();

        mWidth = mVideoGather.getPreWidth();
        mHeight = mVideoGather.getPreHeight();
        mFps = mVideoGather.getFrameRate();

        mVideoEncodeBufPool.setDebug(DEBUG);
        mYuv420 = new byte[getYuvBuffer(mWidth, mHeight)];

        videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, (int)(mWidth * mHeight * 0.4));
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, selectColorFormat(VIDEO_MIME_TYPE));
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        Log.d(TAG, "Video format: " + videoFormat.toString());

        mEncodeHandler.sendEmptyMessage(1);
    }

    public boolean start() {
        boolean mediaCodecStartSuccess = false;
        if (mEncoder == null) {
            for (int i=0; i<10; i++) {
                LogTool.w(TAG, "Sleep 1 second to wait the mEncoder created. i = " + i);
                try {
                    Thread.sleep(1000);
                    if (mEncoder != null) {
                        break;
                    }
                } catch (InterruptedException e) {
                    LogTool.w(TAG, "Exception when sleeping", e);
                }
            }
        }

        if (mEncoder == null) {
            LogTool.e(TAG, "start mEncoder is null");
            throw new RuntimeException("Encoder cann't start.");
        }

        mEncoder.reset();
        mEncoder.setCallback(mEncodeCallback);
        mEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        try {
            mediaCodecStartSuccess = true;
            mEncoder.start();
        } catch (Exception e) {
            mediaCodecStartSuccess = false;
            LogTool.e(TAG,"Start mediaCodec failed!!! : ",e);
        } finally {
            LogTool.e(TAG,"mediaCodecStartSuccess = "+mediaCodecStartSuccess);
            if(!mediaCodecStartSuccess) {
                mStartEncoderHandler.sendEmptyMessageDelayed(START_ENCODER,1000);
                return false;
            }
        }
        mVideoGather.registerVideoRawDataListener(this);
        mEncoding = true;
        mStartTimeUs = System.currentTimeMillis() * 1000;
        mInputThread = new InputThread();
        videoEncoderLoop = true;
        mInputThread.start();
        LogTool.w(TAG,"VideoEncoder start success.");
        return true;
    }

    public void stop() {
        videoEncoderLoop = false;
        mInputIndexQueue.clear();
        mEncoding = false;
    }

    public boolean isRunning() {
        return mEncoding;
    }

    private CopyOnWriteArrayList<VideoEncoderListener> mVideoEncoderListeners = new CopyOnWriteArrayList<>();
    public void registerEncoderListener(VideoEncoderListener listener) {
        if (!mVideoEncoderListeners.contains(listener)) {
            mVideoEncoderListeners.add(listener);
        }
    }

    public void unRegisterEncoderListener (VideoEncoderListener listener) {
        mVideoEncoderListeners.remove(listener);
    }

    public interface VideoEncoderListener {
        void onVideoEncoded(EncoderBuffer buf);
        void onVideoFormatChanged(MediaFormat format);
    }

    @Override
    public void onVideoRawDataReady(VideoGather.VideoRawBuf buf) {
        buf.increaseRef();
        mViewRawDataQueue.offer(buf);
    }

    private int selectColorFormat(String type) {
        return selectColorFormat(selectCodec(type), type);
    }

    private int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }

        Log.w(TAG, "Couldn't find color format for " + codecInfo.getName()
                + " / " + mimeType);
        return 0; // not reached
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private int getYuvBuffer(int width, int height) {
        int yStride = (int) Math.ceil(width / 16.0) * 16;
        int uvStride = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
        int ySize = yStride * height;
        int uvSize = uvStride * height / 2;
        return ySize + uvSize * 2;
    }

    private void debug(String msg) {
        if (DEBUG || MainActivity.VIDEO_ENCODE_DEBUG) LogTool.d(TAG, msg);
    }
}