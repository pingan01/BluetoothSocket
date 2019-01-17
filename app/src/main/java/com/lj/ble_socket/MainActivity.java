package com.lj.ble_socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    // 获取到蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    //用来保存搜索到的设备信息
    private List<String> bluetoothDevices = new ArrayList<>();
    //ListView组件
    private ListView lvDevices;
    //ListView的字符串数组适配器
    private ArrayAdapter<String> arrayAdapter;
    //UUID ,蓝牙建立连接需要的---类似socket通信中的端口号
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //为其链接创建一个名称
    private final String NAME = "Bluetooth";
    //选中发送数据的蓝牙设备，全局变量，否则连接在方法执行完就结束了
    private BluetoothDevice selectDevice;
    // 获取到选中设备的客户端串口，全局变量，否则连接在方法执行完就结束了
    private BluetoothSocket socket;
    //服务端利用线程不断接受客户端信息
    private AcceptThread mAcceptThread;

    private ConnectThread mConnectThread;

    private ConnectedThread mConnectedThread;


    private TextView tv1;
    private TextView tv2;
    private TextView tv3;
    private TextView tv4;


    private String str1;
    private String str2;
    private String str3;
    private String str4;

    private int count;
    private int n;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1 = (TextView) findViewById(R.id.tvNum1);
        tv2 = (TextView) findViewById(R.id.tvNum2);
        tv3 = (TextView) findViewById(R.id.tvNum3);
        tv4 = (TextView) findViewById(R.id.tvNum4);

        //获取到蓝牙默认的适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (!mBluetoothAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(intent, 0);
            // 不做提示，强行打开
            mBluetoothAdapter.enable();
        }
        //获取到ListView组件
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        //为listview设置字符换数组适配器
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, bluetoothDevices);
        //为listView绑定适配器
        lvDevices.setAdapter(arrayAdapter);
        //为listView设置item点击事件侦听
        lvDevices.setOnItemClickListener(this);

        // 用Set集合保持已绑定的设备 
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : devices) {
                //保存到arrayList集合中
                bluetoothDevices.add(bluetoothDevice.getName() + ":" + bluetoothDevice.getAddress() + "\n");
            }
        }
        // 因为蓝牙搜索到设备和完成搜索都是通过广播来告诉其他应用的  
        // 这里注册找到设备和完成搜索广播

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

    }

    public void onClick_Search(View view) {
        setTitle("正在扫描...");
        //点击扫描周边设备，如果正在搜索，则暂停搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    //注册广播接收者
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取到广播的action
            String action = intent.getAction();
            //判断广播是搜索设备还是搜索完成
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                //找到设备后获取其设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //判断这个设备是否是之前已经绑定过了，如果是则不需要添加，在程序初始化的时候就添加了
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //设备没有绑定过，则将其保存到arrayList集合中
                    bluetoothDevices.add(device.getName() + ":" + device.getAddress() + "\n");
                    //更新字符串数组适配器，将内容显示在listView中 
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                setTitle("扫描完成");
            }
        }
    };

    //点击listView中的设备，传送数据 
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //获取到这个设备的信息
        String s = arrayAdapter.getItem(position);
        //对其进行分割，获取这个设备的的地址
        String address = s.substring(s.indexOf(":") + 1).trim();
        //判断当前是否还是在搜索周边设备，如果是则暂停搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        //如果选择设备为空代表还没选择设备
        if (selectDevice == null) {
            //通过设备地址获取到该设备   // TODO: 2017/12/18 通过不同的mac地址区分不同的蓝牙设备
            selectDevice = mBluetoothAdapter.getRemoteDevice(address);

            try {
                socket = selectDevice.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();

                Toast.makeText(MainActivity.this, "客户端信息发送成功", Toast.LENGTH_SHORT).show();
                mConnectedThread = new ConnectedThread();
                mConnectedThread.start();


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    //创建handler，因为我们接收是采用线程来接收的，在线程中无法操作UI，所以需要handler 
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            float[] arrayOfFloat = msg.getData().getFloatArray("Data");

            switch (msg.what) {

                case 1:

                    Object[] arrayOfObject1 = new Object[1];
                    arrayOfObject1[0] = Float.valueOf(arrayOfFloat[6]);
                    str1 = String.format("%10.2f°", arrayOfObject1).trim();
                    Log.e("TAG", "str1==" + str1.trim());
                    tv1.setText(str1);

                    Object[] arrayOfObject2 = new Object[1];
                    arrayOfObject2[0] = Float.valueOf(arrayOfFloat[7]);
                    str2 = String.format("%10.2f°", arrayOfObject2).trim();
                    Log.e("TAG", "str2==" + str2.trim());
                    tv2.setText(str2);

                    Object[] arrayOfObject3 = new Object[1];
                    arrayOfObject3[0] = Float.valueOf(arrayOfFloat[8]);
                    str3 = String.format("%10.2f°", arrayOfObject3).trim();
                    Log.e("TAG", "str3==" + str3.trim());
                    tv3.setText(str3);

                    Object[] arrayOfObject4 = new Object[1];
                    arrayOfObject4[0] = Float.valueOf(arrayOfFloat[16]);
                    str4 = String.format("%10.2f℃", arrayOfObject4).trim();
                    Log.e("TAG", "str4==" + str4.trim());
                    tv4.setText(str4);

                    return;
            }
            super.handleMessage(msg);
        }
    };

    // 服务端接收信息线程
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;// 服务端接口
        private BluetoothSocket socket;//获取到客户端的接口

        public AcceptThread() {
            try {
                // 通过UUID监听请求，然后获取到对应的服务端接口 
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);

                Log.e("TAG", "服务端接口");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                //接收客户端的接口
                socket = serverSocket.accept();//测试
                Log.e("TAG", "服务端接收数据-----2");
                if (socket == null) return;
                //connected(socket);
                OutputStream outputStream = socket.getOutputStream();
                //输出流给蓝牙设备发送信息
                byte[] msg = new byte[1024];
                outputStream.write(msg);
                //输入流 手机接收信息
                InputStream inputStream = socket.getInputStream();
                while (true) {
                    byte[] buffer = new byte[1024];
                    inputStream.read(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //客户端接收数据的线程---
    private class ConnectedThread extends Thread {
        private InputStream inputStream;
        private OutputStream outputStream;
        private float[] fData = new float[32];
        private byte[] packBuffer = new byte[11];
        private Queue<Byte> queueBuffer = new LinkedList();
        private BluetoothSocket connectedSocket;


        public ConnectedThread() {
            Log.e("TAG", "客户端线程");

            try {
                inputStream = socket.getInputStream();

                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void run() {

            byte[] arrayOfByte = new byte[1024];
            long l1 = System.currentTimeMillis();
            Log.e("TAG", "l1==" + l1);
            long l2 = 0;
            while (true) {
                try {

                    int i = inputStream.read(arrayOfByte);
                    Log.e("TAG", "i==" + i);


                    for (int j = 0; j < i; j++) {

                        queueBuffer.add(Byte.valueOf(arrayOfByte[j]));
                    }

                    /**
                     *  if (queueBuffer.size() < 11) {
                     queueBuffer.add(Byte.valueOf(arrayOfByte[queueBuffer.size()]));
                     Log.e("TAG", "queueBuffer=="+queueBuffer+"\n"+"长度=="+queueBuffer.size());
                     }
                     */

                    Log.e("TAG", "queueBuffer队列=="+queueBuffer);

                    if (queueBuffer.poll().byteValue() == 0x55) {

                        //获取并移除此队列的头---包头
                        byte b = queueBuffer.poll().byteValue();
                        Log.e("TAG","b=="+b);
                        for (int k = 0; k < 9; k++) {
                            packBuffer[k] = queueBuffer.poll().byteValue();
                        }

                        if (b == 0x53) {
                            //标识这个包是角度包
                            //角度
                            fData[6] = (180.0F * (((short) packBuffer[1] << 8 | 0xFF & (short) packBuffer[0]) / 32768.0F));
                            Log.e("TAG", "X轴角度==" + fData[6]);

                            fData[7] = (180.0F * (((short) packBuffer[3] << 8 | 0xFF & (short) packBuffer[2]) / 32768.0F));
                            Log.e("TAG", "Y轴角度==" + fData[7]);

                            fData[8] = (180.0F * (((short) packBuffer[5] << 8 | 0xFF & (short) packBuffer[4]) / 32768.0F));
                            Log.e("TAG", "Z轴角度==" + fData[8]);

                            fData[16] = (((short) packBuffer[7] << 8 | 0xFF & (short) packBuffer[6]) / 100.0F);
                            Log.e("TAG", "温度==" + fData[16]);


                            Message message = handler.obtainMessage(1);
                            Bundle bundle = new Bundle();
                            bundle.putFloatArray("Data", fData);
                            message.setData(bundle);
                            handler.sendMessage(message);

                            queueBuffer.clear();
                        }

                    } else {
                        queueBuffer.clear();
                    }
                    /**
                     * for (int j = 0; j < i; j++) {
                     queueBuffer.add(Byte.valueOf(arrayOfByte[j]));
                     //   Byte.valueOf(arrayOfByte[j])====返回表示指定 byte 值的一个 Byte 实例

                     }
                     Log.e("TAG", "queueBuffer==" + queueBuffer);

                     if (queueBuffer.size() < 11) {
                     // TODO: 2017/12/27 待实现
                     }

                     if (queueBuffer.poll().byteValue() != 85) {

                     continue;
                     }
                     for (k = 0; k < 11; k++) {
                     packBuffer[k] = queueBuffer.poll().byteValue();
                     Log.e("TAG", "第" + k + "个字节" + packBuffer[k]);
                     }

                     b = queueBuffer.poll().byteValue();
                     Log.e("TAG", "b==" + b);
                     */


                    /**
                     * //加速度
                     fData[0] = (16.0F * (((short) packBuffer[1] << 8 | 0xFF & (short) packBuffer[0]) / 32768.0F));
                     Log.e("TAG", "X轴加速度==" + fData[0]);
                     fData[1] = (16.0F * (((short) packBuffer[3] << 8 | 0xFF & (short) packBuffer[2]) / 32768.0F));
                     Log.e("TAG", "Y轴加速度==" + fData[1]);
                     fData[2] = (16.0F * (((short) packBuffer[5] << 8 | 0xFF & (short) packBuffer[4]) / 32768.0F));
                     Log.e("TAG", "Z轴加速度==" + fData[2]);
                     fData[16] = (((short) packBuffer[7] << 8 | 0xFF & (short) packBuffer[6]) / 100.0F);
                     Log.e("TAG", "温度==" + fData[16]);


                     //角速度
                     fData[3] = (2000.0F * (((short) packBuffer[1] << 8 | 0xFF & (short) packBuffer[0]) / 32768.0F));
                     fData[4] = (2000.0F * (((short) packBuffer[3] << 8 | 0xFF & (short) packBuffer[2]) / 32768.0F));
                     fData[5] = (2000.0F * (((short) packBuffer[5] << 8 | 0xFF & (short) packBuffer[4]) / 32768.0F));
                     fData[16] = (((short) packBuffer[7] << 8 | 0xFF & (short) packBuffer[6]) / 100.0F);

                     //角度
                     fData[6] = (180.0F * (((short) packBuffer[1] << 8 | 0xFF & (short) packBuffer[0]) / 32768.0F));
                     fData[7] = (180.0F * (((short) packBuffer[3] << 8 | 0xFF & (short) packBuffer[2]) / 32768.0F));
                     fData[8] = (180.0F * (((short) packBuffer[5] << 8 | 0xFF & (short) packBuffer[4]) / 32768.0F));
                     fData[16] = (((short) packBuffer[7] << 8 | 0xFF & (short) packBuffer[6]) / 100.0F);


                     Object[] arrayOfObject1 = new Object[1];
                     arrayOfObject1[0] = Float.valueOf(fData[0]);
                     str1 = String.format("%10.2fg", arrayOfObject1).trim();

                     Log.e("TAG", "str1==" + str1.trim());
                     runOnUiThread(new Runnable() {
                    @Override public void run() {
                    tv1.setText(str1);
                    }
                    });

                     Object[] arrayOfObject2 = new Object[1];
                     arrayOfObject2[0] = Float.valueOf(fData[1]);
                     str2 = String.format("%10.2fg", arrayOfObject2).trim();
                     Log.e("TAG", "str2==" + str2.trim());
                     runOnUiThread(new Runnable() {
                    @Override public void run() {
                    tv2.setText(str2);
                    }
                    });

                     Object[] arrayOfObject3 = new Object[1];
                     arrayOfObject3[0] = Float.valueOf(fData[2]);
                     str3 = String.format("%10.2fg", arrayOfObject3).trim();
                     Log.e("TAG", "str3==" + str3.trim());
                     runOnUiThread(new Runnable() {
                    @Override public void run() {
                    tv3.setText(str3);
                    }
                    });

                     Object[] arrayOfObject4 = new Object[1];
                     arrayOfObject4[0] = Float.valueOf(fData[16]);
                     str4 = String.format("%10.2f℃", arrayOfObject4).trim();
                     Log.e("TAG", "str4==" + str4.trim());
                     runOnUiThread(new Runnable() {
                    @Override public void run() {
                    tv4.setText(str4);
                    }
                    });
                     */

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    //从蓝牙接收信息的线程  
    private class ConnectThread extends Thread {
        private BluetoothSocket connectSocket;

        public ConnectThread(BluetoothDevice device) {
            try {
                //判断客户端接口是否为空
                if (connectSocket == null) {
                    mBluetoothAdapter.cancelDiscovery();
                    connectSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                }
                /**
                 *  //判断是否拿到输出流
                 if (outputStream != null) {
                 //需要发送的信息
                 String text = "服务端成功收到信息";//发送给服务端的信息---服务端接收
                 outputStream.write(text.getBytes("UTF-8"));
                 }
                 */
                //提示用户发送成功 
                Toast.makeText(MainActivity.this, "客户端信息发送成功", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", "e==" + e.toString());
                Toast.makeText(MainActivity.this, "发送信息失败", Toast.LENGTH_SHORT).show();
            }
        }

        public void cancel() {
            try {
                connectSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                //向服务端发送连接
                connectSocket.connect();
                Log.e("TAG", "客户端连接服务器");

                connected();

                connectSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connected() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        //连接上后监听数据传输
        mConnectedThread = new ConnectedThread();

        mConnectedThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        //stop();
    }

    private void start() {
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mAcceptThread == null) {
            this.mAcceptThread = new AcceptThread();
            this.mAcceptThread.start();
        }
    }

    private void stop() {
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }
    }

    //字节流转换为字节数组
    private byte[] InputStreamToByte(InputStream is) throws IOException {

        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();//创建一个新的byte数组输出流

        byte[] buffer = new byte[1024];
        int ch;//从输入流中读取一定数量字节并将其存储在缓冲区数组buffer中----返回 0 到 255 范围内的 int 字节值---已经到达流末尾而没有可用的字节，则返回值 -1
        while ((ch = is.read(buffer)) != -1) {
            Log.e("TAG", "下一个字节长度" + ch);
            bytestream.write(buffer, 0, ch);
            Log.e("TAG", "字节数组流长度==" + bytestream.size());

        }
        byte[] data = bytestream.toByteArray();//创建一个新分配的 byte 数组。
        Log.e("TAG", "data==" + data.length);
        bytestream.close();
        return data;
    }

    //字节流转换为字节数组
    private byte[] InputStreamToByte1(InputStream is) throws IOException {
        byte[] data = null;
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();//创建一个新的byte数组输出流

        byte[] buffer = new byte[1024];
        int ch;//从输入流中读取一定数量字节并将其存储在缓冲区数组buffer中----返回 0 到 255 范围内的 int 字节值---已经到达流末尾而没有可用的字节，则返回值 -1
        while ((ch = is.read(buffer)) != -1) {
            Log.e("TAG", "下一个字节长度" + ch);
            bytestream.write(buffer, 0, ch);
            Log.e("TAG", "字节数组流长度==" + bytestream.size());
            data = bytestream.toByteArray();//创建一个新分配的 byte 数组。
            Log.e("TAG", "data==" + data.length);
            getFloat(data);

            if (bytestream.size() > 2000) {
                bytestream.close();
                break;
            }

        }

        return data;
    }

    //字节数组转换为float
    public float getFloat(byte[] b) {
        int accum = 0;
        for (int i = 0; i < 4; i++) {
            accum |= (b[i] & 0xff) << i * 8;
            Log.e("TAG", "float 4个字节==" + Float.intBitsToFloat(accum));
        }
        return Float.intBitsToFloat(accum);
    }

    //字符串转16进制数组
    private byte[] getHexBytes(String message) {
        int len = message.length() / 2;
        char[] chars = message.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
    }

    //字节数组转化为16进制字符串
    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        Log.e("TAG", "result==" + result);
        return result;
    }

}
