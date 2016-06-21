package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by taipp on 5/20/2016.
 */
public class KeyCode extends Activity implements OnClickListener {
    SharedPreferences mSp;
    TextView mInfo;
    Button mBtOk;
    Button mBtFailed;
    String mKeycode = "";
    private GridView mGrid;
    private MySurfaceView joyStickView;
    private LinearLayout root;
    boolean isMeasured = false;
    final static int itemString[] = {
            //normal key
            R.string.keycode_back,
            R.string.keycode_vol_up,
            R.string.keycode_vol_down,
            //touch pad
            R.string.keycode_tp_singleclick,
            R.string.keycode_tp_up,
            R.string.keycode_tp_down,
            R.string.keycode_tp_left,
            R.string.keycode_tp_right,
            //joystick
            R.string.keycode_button_a,
            R.string.keycode_button_b,
            R.string.keycode_button_y,
    };

    private Handler handler = new Handler();
    private boolean mCheckDataSuccess;
    private byte okFlag = 0x00;
    private Timer mTimer;
    private TimerTask mTimerTask;

    private final String TAG = "KeyCode";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.keycode);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mInfo = (TextView) findViewById(R.id.keycode_info);
        mBtOk = (Button) findViewById(R.id.keycode_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.keycode_bt_failed);
        mBtFailed.setOnClickListener(this);
        ArrayList<String> mListData = new ArrayList<String>();
        HashMap<String, Integer> keyMap = new HashMap<String, Integer>();
        for (int item : itemString) {
            mListData.add(getString(item));
        }

        mGrid = (GridView) findViewById(R.id.keycode_grid);
        mGrid.setAdapter(new MyAdapter(this, mListData, keyMap));
        //
        //获取布局文件中LinearLayout容器
        root = (LinearLayout)findViewById(R.id.paint_root);
        joyStickView = new MySurfaceView(this, 0, 0, 0 * 5/ 14);
        root.addView(joyStickView);

        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!isMeasured) {
                    float circleX = root.getMeasuredWidth() / 2;
                    float circleY = root.getMeasuredHeight() / 2;
                    float circleR = (circleX > circleY ? circleY : circleX) * 5 / 7;
                    joyStickView.setMySurfaceView(circleX, circleY, circleR);
                    isMeasured = true;
                }
                return true;
            }
        });

        mCheckDataSuccess = false;
        mTimer = null;
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                SaveToReport();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown keyCode->" + String.valueOf(keyCode));
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp keyCode->" + String.valueOf(keyCode));
        return true; //super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyLongPress keyCode->" + String.valueOf(keyCode));
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent keyCode->" + event.getKeyCode());
        MyAdapter myAdapter = (MyAdapter) mGrid.getAdapter();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                //normal key
                case KeyEvent.KEYCODE_ESCAPE:
                    myAdapter.setKeyMap(getString(R.string.keycode_back), 1);
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    myAdapter.setKeyMap(getString(R.string.keycode_vol_up), 1);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    myAdapter.setKeyMap(getString(R.string.keycode_vol_down), 1);
                    break;
                //touch pad
                case KeyEvent.KEYCODE_ENTER: //single click
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_singleclick), 1);
                    break;
                case 0x25: //double click
                    break;
                case KeyEvent.KEYCODE_DPAD_UP: //up -> down
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_up), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN: //down -> up
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_down), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT: //right -> left
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_left), 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: // left -> right
                    myAdapter.setKeyMap(getString(R.string.keycode_tp_right), 1);
                    break;
                //joystick
                case KeyEvent.KEYCODE_BUTTON_A:
                    myAdapter.setKeyMap(getString(R.string.keycode_button_a), 1);
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    myAdapter.setKeyMap(getString(R.string.keycode_button_b), 1);
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    myAdapter.setKeyMap(getString(R.string.keycode_button_y), 1);
                    break;
                default:
                    break;
            }
            mGrid.setAdapter(myAdapter);
            if (myAdapter.getKeyMaSize() == myAdapter.getCount()) {
                okFlag |= 0x80;
            }
            checkDataSuccess();
        }
        return true; //super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if ((ev.getDevice().getSources() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            Log.d(TAG, String.format("dispatchGenericMotionEvent ev->(%f, %f)", ev.getX(), ev.getY()));

            if (ev.getX() > 0.5) {
                okFlag |= 0x01;
            }
            if (ev.getX() < -0.5) {
                okFlag |= 0x02;
            }
            if (ev.getY() > 0.5) {
                okFlag |= 0x04;
            }
            if (ev.getY() < -0.5) {
                okFlag |= 0x08;
            }

            checkDataSuccess();

            joyStickView.updateGenericMotionEvent(ev);
        }
        return true; //super.dispatchGenericMotionEvent(ev);
    }

    public class MyAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<String> mDataList;
        private HashMap<String, Integer> keyMap;

        public MyAdapter(Context context, ArrayList<String> mDataList, HashMap<String, Integer> keyMap) {
            this.context = context;
            this.mDataList = mDataList;
            this.keyMap = keyMap;
        }

        @Override
        public int getCount() {
            return (mDataList == null) ? 0 : mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return (mDataList == null) ? null : mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.keycode_grid,parent, false);
            }
            TextView textView = Utils.ViewHolder.get(convertView, R.id.factor_button);

            if (keyMap.containsKey(mDataList.get(position)) && keyMap.get(mDataList.get(position)) == 1) {
                textView.setBackgroundResource(R.drawable.btn_default_pressed);
            }
            textView.setText(mDataList.get(position));

            return convertView;
        }

        public void setKeyMap(String key, int value) {
            if (keyMap != null) {
                this.keyMap.put(key, value);
            }
        }

        public int getKeyMaSize() {
            if (keyMap == null) {
                return 0;
            }
            return keyMap.size();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.KeyCode_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        Utils.SetPreferences(this, mSp, R.string.KeyCode_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void checkDataSuccess() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("->okFlag = %#x", okFlag));

        if ((okFlag & 0x80) == 0x80 && (okFlag & 0x0F) == 0x0F) {
            if (mTimer == null) {
                mCheckDataSuccess = true;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBtFailed.setBackgroundColor(Color.GRAY);
                        mBtFailed.setClickable(false);
                        mBtOk.setBackgroundColor(Color.GREEN);
                    }
                });

                mTimer = new Timer();
                mTimer.schedule(mTimerTask, 3 * 1000);
            }
        }
    }

    //draw view for joystick
    public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
        private Thread th;
        private SurfaceHolder sfh;
        private Canvas canvas;
        private Paint paint;
        private boolean flag;
        //固定摇杆背景圆形的X,Y坐标以及半径
        private float RockerCircleX = 100;
        private float RockerCircleY = 100;
        private float RockerCircleR = 50;
        //摇杆的X,Y坐标以及摇杆的半径
        private float SmallRockerCircleX = 100;
        private float SmallRockerCircleY = 100;
        private float SmallRockerCircleR = 20;
        public MySurfaceView(Context context) {
            super(context);
            Log.v("Himi", "MySurfaceView");
            this.setKeepScreenOn(true);
            sfh = this.getHolder();
            sfh.addCallback(this);
            paint = new Paint();
            paint.setAntiAlias(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }
        public MySurfaceView(Context context, float circleX, float circleY, float circleR) {
            super(context);
            Log.v("Himi", "MySurfaceView");
            //this.setKeepScreenOn(true);
            this.RockerCircleX = circleX;
            this.RockerCircleY = circleY;
            this.RockerCircleR = circleR;
            this.SmallRockerCircleX = this.RockerCircleX;
            this.SmallRockerCircleY = this.RockerCircleY;
            this.SmallRockerCircleR = this.RockerCircleR  * 2 / 5;

            sfh = this.getHolder();
            sfh.addCallback(this);
            paint = new Paint();
            paint.setAntiAlias(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        public void setMySurfaceView(float circleX, float circleY, float circleR) {
            this.RockerCircleX = circleX;
            this.RockerCircleY = circleY;
            this.RockerCircleR = circleR;
            this.SmallRockerCircleX = this.RockerCircleX;
            this.SmallRockerCircleY = this.RockerCircleY;
            this.SmallRockerCircleR = this.RockerCircleR  * 2 / 5;

            this.draw();
        }

        public void surfaceCreated(SurfaceHolder holder) {
            th = new Thread(this);
            flag = true;
            th.start();
        }
        /***
         * 得到两点之间的弧度
         */
        public double getRad(float px1, float py1, float px2, float py2) {
            //得到两点X的距离
            float x = px2 - px1;
            //得到两点Y的距离
            float y = py1 - py2;
            //算出斜边长
            float xie = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            //得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
            float cosAngle = x / xie;
            //通过反余弦定理获取到其角度的弧度
            float rad = (float) Math.acos(cosAngle);
            //注意：当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
            if (py2 < py1) {
                rad = -rad;
            }
            return rad;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_MOVE) {
                // 当触屏区域不在活动范围内
                if (Math.sqrt(Math.pow((RockerCircleX - (int) event.getX()), 2)
                        + Math.pow((RockerCircleY - (int) event.getY()), 2)) >= RockerCircleR) {
                    //得到摇杆与触屏点所形成的角度
                    double tempRad = getRad(RockerCircleX, RockerCircleY, event.getX(), event.getY());
                    //保证内部小圆运动的长度限制
                    getXY(RockerCircleX, RockerCircleY, RockerCircleR, tempRad);
                } else {//如果小球中心点小于活动区域则随着用户触屏点移动即可
                    SmallRockerCircleX = (int) event.getX();
                    SmallRockerCircleY = (int) event.getY();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //当释放按键时摇杆要恢复摇杆的位置为初始位置
                SmallRockerCircleX = RockerCircleX;
                SmallRockerCircleY = RockerCircleY;
            }
            return true;
        }


        public boolean updateGenericMotionEvent(MotionEvent event) {

            float CosX = RockerCircleX + event.getX() * RockerCircleR;
            float CosY = RockerCircleY + event.getY() * RockerCircleR;

            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                // 当触屏区域不在活动范围内
                if (Math.sqrt(Math.pow((RockerCircleX - CosX), 2) + Math.pow((RockerCircleY - CosY), 2)) >= RockerCircleR) {
                    //得到摇杆与触屏点所形成的角度
                    double tempRad = getRad(RockerCircleX, RockerCircleY, CosX, CosY);
                    //保证内部小圆运动的长度限制
                    getXY(RockerCircleX, RockerCircleY, RockerCircleR, tempRad);
                } else {//如果小球中心点小于活动区域则随着用户触屏点移动即可
                    SmallRockerCircleX = CosX;
                    SmallRockerCircleY = CosY;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //当释放按键时摇杆要恢复摇杆的位置为初始位置
                SmallRockerCircleX = RockerCircleX;
                SmallRockerCircleY = RockerCircleY;
            }

            if (Math.abs(event.getX()) < 0.1 && Math.abs(event.getY()) < 0.1) {
                SmallRockerCircleX = RockerCircleX;
                SmallRockerCircleY = RockerCircleY;
            }
            this.draw();
            return true;//super.dispatchGenericMotionEvent(event);
        }

        /**
         *
         * @param R
         *            圆周运动的旋转点
         * @param centerX
         *            旋转点X
         * @param centerY
         *            旋转点Y
         * @param rad
         *            旋转的弧度
         */
        public void getXY(float centerX, float centerY, float R, double rad) {
            //获取圆周运动的X坐标
            SmallRockerCircleX = (float) (R * Math.cos(rad)) + centerX;
            //获取圆周运动的Y坐标
            SmallRockerCircleY = (float) (R * Math.sin(rad)) + centerY;
        }
        public void draw() {
            try {
                canvas = sfh.lockCanvas();
                canvas.drawColor(Color.WHITE);
                //设置透明度
                paint.setColor(0x70000000);
                //绘制摇杆背景
                canvas.drawCircle(RockerCircleX, RockerCircleY, RockerCircleR, paint);
                paint.setColor(0x70ff0000);
                //绘制摇杆
                canvas.drawCircle(SmallRockerCircleX, SmallRockerCircleY,
                        SmallRockerCircleR, paint);
            } catch (Exception e) {
                // TODO: handle exception
            } finally {
                try {
                    if (canvas != null)
                        sfh.unlockCanvasAndPost(canvas);
                } catch (Exception e2) {
                }
            }
        }
        public void run() {
            // TODO Auto-generated method stub
            while (flag) {
                draw();
                try {
                    Thread.sleep(50);
                } catch (Exception ex) {
                }
            }
        }
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.v("Himi", "surfaceChanged");
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            flag = false;
            Log.v("Himi", "surfaceDestroyed");
        }
    }
}
