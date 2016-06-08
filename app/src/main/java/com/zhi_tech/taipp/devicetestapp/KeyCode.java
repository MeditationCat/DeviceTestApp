package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
    private List<Integer> mListData;
    final int imgString[] = {
            R.drawable.home, R.drawable.menu, R.drawable.vldown, R.drawable.vlup, R.drawable.back,
            R.drawable.search, R.drawable.camera, R.drawable.power,  R.drawable.unknown
    };
    private final String TAG = "KeyCode";

/*    private DeviceTestAppService dtaService = null;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dtaService = ((DeviceTestAppService.DtaBinder)service).getService();
            dtaService.setOnDataChangedListener(new OnDataChangedListener() {
                @Override
                public void sensorDataChanged(SensorPackageObject object) {
                    //to get the data from the object.
                    postUpdateHandlerMsg(object);
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dtaService = null;
        }
    };

    private Handler handler = new Handler();

    private void postUpdateHandlerMsg(final SensorPackageObject object) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                byte[] keyCode = object.getKeyCode();
                byte keyCodeFlag = keyCode[0];
                byte keyCodeValue, keyCodeValueX, keyCodeValueY;
                if (keyCodeFlag > 0x04) {
                    return;
                }
                Log.d(TAG, String.format("keycode:%d,%d,%d", keyCode[0], keyCode[1], keyCode[2]));
                switch (keyCodeFlag) {
                    case 0x01:
                        //board key value
                        keyCodeValue = keyCode[1];
                        switch (keyCodeValue) {
                            case 0x01:
                                break;
                        }
                        break;
                    case 0x02:
                        //board touch pad key value
                        keyCodeValue = keyCode[1];
                        break;
                    case 0x03:
                        //bluetooth key value
                        keyCodeValue = keyCode[1];
                        break;
                    case 0x04:
                        //bluetooth joystick
                        keyCodeValueX = keyCode[1];
                        keyCodeValueY = keyCode[2];
                        break;
                    default:
                        break;
                }
                //textView.setText(String.valueOf(object.getHeader()) + ":" + object.getTimestamp());
            }
        });
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.keycode);
/*
        Intent intent = new Intent(KeyCode.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
*/
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mInfo = (TextView) findViewById(R.id.keycode_info);
        mBtOk = (Button) findViewById(R.id.keycode_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.keycode_bt_failed);
        mBtFailed.setOnClickListener(this);
        mListData = new ArrayList<Integer>();
        mGrid = (GridView) findViewById(R.id.keycode_grid);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown keyCode->" + String.valueOf(keyCode));
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                if (mKeycode.indexOf("CAMERA") >= 0) {
                    return false;
                }
                mKeycode += "CAMERA\n";
                mListData.add(imgString[6]);
                break;
            case KeyEvent.KEYCODE_HOME:
                if (mKeycode.indexOf("HOME") >= 0) {
                    return false;
                }
                mKeycode += "HOME\n";
                mListData.add(imgString[0]);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mKeycode.indexOf("VLDOWN") >= 0) {
                    return false;
                }
                mKeycode += "VLDOWN\n";
                mListData.add(imgString[2]);
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mKeycode.indexOf("VLUP") >= 0) {
                    return false;
                }
                mKeycode += "VLUP\n";
                mListData.add(imgString[3]);
                break;
            case KeyEvent.KEYCODE_BACK:
                if (mKeycode.indexOf("BACK") >= 0) {
                    return false;
                }
                mKeycode += "BACK\n";
                mListData.add(imgString[4]);
                break;
            case KeyEvent.KEYCODE_SEARCH:
                if (mKeycode.indexOf("SEARCH") >= 0) {
                    return false;
                }
                mKeycode += "SEARCH\n";
                mListData.add(imgString[5]);
                break;
            case KeyEvent.KEYCODE_MENU:
                if (mKeycode.indexOf("MENU") >= 0) {
                    return false;
                }
                mKeycode += "MENU\n";
                mListData.add(imgString[1]);
                break;
            case KeyEvent.KEYCODE_POWER:
                if (mKeycode.indexOf("POWER") >= 0) {
                    return false;
                }
                mKeycode += "POWER\n";
                mListData.add(imgString[7]);
                break;

            default:
                mListData.add(imgString[8]);
                break;

        }
        mGrid.setAdapter(new MyAdapter(this));
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp keyCode->" + String.valueOf(keyCode));
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyLongPress keyCode->" + String.valueOf(keyCode));
        return super.onKeyLongPress(keyCode, event);
    }

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public MyAdapter(DeviceTestApp deviceTestApp, int deviceButton) {
        }

        public int getCount() {
            if (mListData == null) {
                return 0;
            }
            return mListData.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.keycode_grid, null);
            ImageView imgview = (ImageView) convertView.findViewById(R.id.imgview);
            imgview.setBackgroundResource(mListData.get(position));
            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        //unbindService(conn);
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.KeyCode_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
