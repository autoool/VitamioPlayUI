package com.stx.vitamiodemo;


import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

/**
 * Created by sam.zhang on 2017/7/13.
 */

public class ThreetiMediaController extends MediaController {

    private static final int HIDEFRAM = 0;//控制提示窗口的显示

    private GestureDetector gestureDetector;
    private ImageButton imageButtonBack;
    private TextView textViewFileName;
    private VideoView videoView;
    private Activity activity;
    private Context context;
    private String videoName;
    private int controllerWith = 0;

    private View volumeBrightnessLayout;
    private ImageView operationBg;
    private ImageButton imageScale;
    private ImageButton imagePause;
    private TextView operationTv;
    private AudioManager audioManager;
    private SeekBar seekBar;
    private boolean dragging;
    private MediaPlayerControl player;

    private int maxVolume;
    private int volume = -1;
    private float brightness = -1f;

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                activity.finish();
            }
        }
    };

    private View.OnClickListener scaleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activity != null) {
                switch (activity.getResources().getConfiguration().orientation) {
                    case Configuration.ORIENTATION_LANDSCAPE:
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        if (imageScale != null) {
                            imageScale.setImageResource(R.mipmap.zoom_in_32);
                        }
                        break;
                    case Configuration.ORIENTATION_PORTRAIT:
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        if (imageScale != null) {
                            imageScale.setImageResource(R.mipmap.zoom_out_32);
                        }
                        break;
                }
            }
        }

    };

    private View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playOrPause();
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long pos;
            switch (msg.what) {
                case HIDEFRAM:
                    volumeBrightnessLayout.setVisibility(View.GONE);
                    operationTv.setVisibility(View.GONE);
                    break;
            }
        }
    };

    public ThreetiMediaController(Context context, VideoView videoView, Activity activity) {
        super(context);
        this.context = context;
        this.videoView = videoView;
        this.activity = activity;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        controllerWith = wm.getDefaultDisplay().getWidth();
        gestureDetector = new GestureDetector(context, new ThreeTiGestureListener());
    }

    @Override
    protected View makeControllerView() {
        View v = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(getResources().getIdentifier("threeti_mediacontroller", "layout",
                        getContext().getPackageName()), this);
        v.setMinimumHeight(controllerWith);

        imageButtonBack = (ImageButton) v.findViewById(getResources().getIdentifier("mediacontroller_top_back", "id", context.getPackageName()));
        textViewFileName = (TextView) v.findViewById(getResources().getIdentifier("mediacontroller_filename", "id", context.getPackageName()));
        imageScale = (ImageButton) v.findViewById(getResources().getIdentifier("mediacontroller_scale", "id", context.getPackageName()));
        imagePause = (ImageButton) v.findViewById(getResources().getIdentifier("mediacontroller_pause", "id", context.getPackageName()));
        if (textViewFileName != null) {
            textViewFileName.setText(videoName);
        }
        volumeBrightnessLayout = (RelativeLayout) v.findViewById(R.id.operation_volume_brightness);
        operationBg = (ImageView) v.findViewById(R.id.operation_bg);
        operationTv = (TextView) v.findViewById(R.id.operation_tv);
        operationTv.setVisibility(GONE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        imageButtonBack.setOnClickListener(backListener);
        imageScale.setOnClickListener(scaleListener);
        imagePause.setOnClickListener(pauseListener);
        return v;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) return true;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                endGesture();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void endGesture() {
        volume = -1;
        brightness = -1f;
        handler.removeMessages(HIDEFRAM);
        handler.sendEmptyMessageDelayed(HIDEFRAM, 1);
    }

    private class ThreeTiGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            toggleMediaControlsVisiblity();
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float oldX = e1.getX(), oldY = e1.getY();
            int x = (int) e2.getRawX();
            int y = (int) e2.getRawY();
            Display display = activity.getWindowManager().getDefaultDisplay();
            int windowsWidth = display.getWidth();
            int windowsHeight = display.getHeight();
            if (oldX > windowsWidth * 3.0 / 4.0) {
                onVolumeSlide((oldY - y) / windowsHeight);
            } else if (oldX < windowsWidth * 1.0 / 4.0) {
                onBrightnessSlide((oldY - y) / windowsHeight);
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            playOrPause();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    private void onVolumeSlide(float percent) {
        if (volume < 0) {
            volume = 0;
            volumeBrightnessLayout.setVisibility(View.VISIBLE);
            operationTv.setVisibility(VISIBLE);
        }
        int index = (int) (percent * maxVolume) + volume;
        if (index > maxVolume) {
            index = maxVolume;
        } else if (index < 0) {
            index = 0;
        }
        if (index >= 10) {
            operationBg.setImageResource(R.mipmap.volume_2);
        } else if (index >= 5 && index < 10) {
            operationBg.setImageResource(R.mipmap.volume_1);
        } else if (index > 0 && index < 5) {
            operationBg.setImageResource(R.mipmap.volume);
        } else {
            operationBg.setImageResource(R.mipmap.volume_x);
        }

        operationTv.setText((int) (((double) index / maxVolume) * 100) + "%");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
    }

    private void onBrightnessSlide(float percent) {
        if (brightness < 0) {
            brightness = activity.getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f) {
                brightness = 0.50f;
            }
            if (brightness < 0.01f) {
                brightness = 0.01f;
            }
            volumeBrightnessLayout.setVisibility(VISIBLE);
            operationTv.setVisibility(VISIBLE);
        }
        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        activity.getWindow().setAttributes(lpa);

        operationTv.setText((int) (lpa.screenBrightness * 100) + "%");
        if (lpa.screenBrightness * 100 >= 90) {
            operationBg.setImageResource(R.drawable.light_100);
        } else if (lpa.screenBrightness * 100 >= 80 && lpa.screenBrightness * 100 < 90) {
            operationBg.setImageResource(R.drawable.light_90);
        } else if (lpa.screenBrightness * 100 >= 70 && lpa.screenBrightness * 100 < 80) {
            operationBg.setImageResource(R.drawable.light_80);
        } else if (lpa.screenBrightness * 100 >= 60 && lpa.screenBrightness * 100 < 70) {
            operationBg.setImageResource(R.drawable.light_70);
        } else if (lpa.screenBrightness * 100 >= 50 && lpa.screenBrightness * 100 < 60) {
            operationBg.setImageResource(R.drawable.light_60);
        } else if (lpa.screenBrightness * 100 >= 40 && lpa.screenBrightness * 100 < 50) {
            operationBg.setImageResource(R.drawable.light_50);
        } else if (lpa.screenBrightness * 100 >= 30 && lpa.screenBrightness * 100 < 40) {
            operationBg.setImageResource(R.drawable.light_40);
        } else if (lpa.screenBrightness * 100 >= 20 && lpa.screenBrightness * 100 < 20) {
            operationBg.setImageResource(R.drawable.light_30);
        } else if (lpa.screenBrightness * 100 >= 10 && lpa.screenBrightness * 100 < 20) {
            operationBg.setImageResource(R.drawable.light_20);
        }

    }

    public void setVideoName(String name) {
        videoName = name;
        if (textViewFileName != null) {
            textViewFileName.setText(videoName);
        }
    }


    private void toggleMediaControlsVisiblity() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

    private void playOrPause() {
        if (videoView != null) {
            if (videoView.isPlaying()) {
                videoView.pause();
                if (imagePause != null) {
                    imagePause.setImageResource(R.mipmap.media_start);
                }
            } else {
                videoView.start();
                if (imagePause != null) {
                    imagePause.setImageResource(R.mipmap.media_pause);
                }
            }
        }
    }
}
