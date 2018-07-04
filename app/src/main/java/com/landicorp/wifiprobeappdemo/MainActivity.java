package com.landicorp.wifiprobeappdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ums.wifiprobeapp.IProbeInfoCallback;
import com.ums.wifiprobeapp.IProbeService;
import com.ums.wifiprobeapp.IProbeStateCallback;

import java.text.SimpleDateFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    IProbeService probeService;
    boolean isConnected;
    private IBinder ibinder;
    TextView tvProbeState,tvBindState,tvMac;
    Button btnBind,btnUnbind,btnStart,btnStop;
    private int count;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvProbeState = (TextView) findViewById(R.id.tv_probestate);
        tvBindState = (TextView) findViewById(R.id.tv_servicestate);
        tvMac = (TextView) findViewById(R.id.tv_mac);
        btnBind = (Button) findViewById(R.id.btn_bind);
        btnUnbind = (Button) findViewById(R.id.btn_unbind);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnBind.setOnClickListener(this);
        btnUnbind.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    private void bindService(){
        Intent intent = new Intent("com.ums.wifiprobeapp.wifiprobeservice");
        Intent explicitIntent = getExplicitIntent(MainActivity.this, intent);
        if(explicitIntent==null){
            Toast.makeText(this,"未找到探针服务程序",Toast.LENGTH_LONG).show();
            return ;
        }
        bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            probeService = IProbeService.Stub.asInterface(iBinder);
            try {
                ibinder = iBinder;
                probeService.startGetProbeState(mProbeStateCallback);
                ibinder.linkToDeath(deathRecipient,0);
                isConnected = true;
                handler.sendEmptyMessage(0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            count=0;
            probeService = null;
            isConnected = false;
            handler.sendEmptyMessage(1);


        }
    };


    IProbeInfoCallback.Stub mProbeInfoCallback = new IProbeInfoCallback.Stub(){

        @Override
        public void getWiFiProbeInfo(String probeinfo, String rssi, long time) throws RemoteException {
            Log.e("TTTTT","mac="+probeinfo+" rssi="+rssi+" time="+time);
            ProbeInfoEntity entity = new ProbeInfoEntity(probeinfo,rssi,time);
            count++;
            Message msg = new Message();
            msg.obj = entity;
            msg.what=3;
            handler.sendMessage(msg);
        }
    };
    IProbeStateCallback.Stub mProbeStateCallback = new IProbeStateCallback.Stub(){

        @Override
        public void updateProbeState(int status) throws RemoteException {
            Log.e("TTTTT","state="+status);
            Message msg = new Message();
            msg.arg1 = status;
            msg.what=4;
            handler.sendMessage(msg);
        }
    };

    Binder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            probeService = null;
            isConnected = false;
            count=0;
            handler.sendEmptyMessage(2);
            bindService();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isConnected){
            if(probeService!=null){
                try {
                    probeService.stopGetProbeInfo(mProbeInfoCallback);
                    probeService.stopGetProbeState(mProbeStateCallback);
                    unbindService(connection);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    Toast.makeText(MainActivity.this,"服务绑定成功",Toast.LENGTH_LONG).show();
                    tvBindState.setText("wifi探针服务：已绑定");
                    break;
                case 1:
                    Toast.makeText(MainActivity.this,"服务已解绑",Toast.LENGTH_LONG).show();
                    tvBindState.setText("wifi探针服务：未绑定");
                    break;
                case 2:
                    Toast.makeText(MainActivity.this,"服务连接断开，正重新绑定",Toast.LENGTH_LONG).show();
                    break;
                case 3:
                    ProbeInfoEntity entity = (ProbeInfoEntity) msg.obj;
                    String macText = "第"+(count+1)+"条- MAC="+entity.getMac()+" RSSI="+entity.getRssi()+" TIME="+entity.getTime();
                    tvMac.setText(macText);
                    break;
                case 4:
                    String connectText = "wifi探针开启状态：";
                    int status = msg.arg1;
                    if(status==1){
                        connectText+="已开启";
                    }else if(status==0){
                        connectText+="已关闭";
                    }
                    tvProbeState.setText(connectText);
                    break;
            }
        }
    };


    public static Intent getExplicitIntent(Context context,
                                           Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent,
                0);
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
        // Set the component to be explicit
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_bind:
                if(isConnected){
                    Toast.makeText(MainActivity.this,"当前已绑定",Toast.LENGTH_LONG).show();
                }else{
                    bindService();
                }
                break;
            case R.id.btn_unbind:
                if(!isConnected){

                    Toast.makeText(MainActivity.this,"当前未绑定",Toast.LENGTH_LONG).show();
                }else{
                    try {
                        Log.e("TTTTT","start unbind service ");
                        probeService.stopGetProbeInfo(mProbeInfoCallback);
                        probeService.stopGetProbeState(mProbeStateCallback);
                        count=0;
                        probeService = null;
                        isConnected = false;
                        handler.sendEmptyMessage(1);

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    unbindService(connection);
                }
                break;
            case R.id.btn_start:
                if(isConnected){
                    if(probeService!=null){
                        try {
                            probeService.startGetProbeInfo(mProbeInfoCallback);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    Toast.makeText(MainActivity.this,"当前未绑定",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_stop:
                if(isConnected){
                    if(probeService!=null){
                        try {
                            probeService.stopGetProbeInfo(mProbeInfoCallback);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    Toast.makeText(MainActivity.this,"当前未绑定",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
    class ProbeInfoEntity{
        private String mac;
        private String rssi;
        long time;

        public ProbeInfoEntity(String mac, String rssi, long time) {
            this.mac = mac;
            this.rssi = rssi;
            this.time = time;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getRssi() {
            return rssi;
        }

        public void setRssi(String rssi) {
            this.rssi = rssi;
        }

        public String getTime() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
        }

        public void setTime(long time) {
            this.time = time;
        }
    }
}
