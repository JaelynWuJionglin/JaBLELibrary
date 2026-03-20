package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.log.LOGUtils;
import com.linkiing.ble.utils.BLEConstant;
import com.linkiing.ble.utils.ByteUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

class BLECommandPolicy implements BLEWriteCallback {
    private static final String TAG = "BLECommandPolicy";
    public static final int hanPostOn = 1001;
    public static final int hanRemove = 1002;
    public static final int hanTimeOut = 1003;
    private final BLECommandPolicyHandler bleCommandPolicyHandler;
    private final CopyOnWriteArrayList<CommandFormat> commandFormatList = new CopyOnWriteArrayList<>();
    private BluetoothGatt bluetoothGatt;

    public BLECommandPolicy() {
        bleCommandPolicyHandler = new BLECommandPolicyHandler(Looper.getMainLooper(), BLEManager.getInstance().getContext());
    }

    @Override
    public boolean sendCommandFormat(CommandFormat commandFormat, BluetoothGatt bluetoothGatt, int mtuSize) {
        this.bluetoothGatt = bluetoothGatt;

        if (commandFormat == null) {
            LOGUtils.e(TAG + " sendCommandFormat Error! commandFormat == null");
            return false;
        }
        byte[] data = commandFormat.getBytes();
        if (data == null) {
            LOGUtils.e(TAG + " sendCommandFormat Error! data == null");
            return false;
        }
        int dataLen = data.length;
        if (dataLen == 0 && commandFormat.getCommandType().equals(BLEConstant.Command_Type_write)) {
            LOGUtils.e(TAG + " sendCommandFormat Error! Command_Type_write data.length == 0");
            return false;
        }

        LOGUtils.d(TAG + " sendCommandFormat sus! dataLen:" + dataLen + " mtuSize:" + mtuSize);

        /*根据mtu长度，对数据分包，*/
        if (dataLen <= mtuSize) {
            addData(commandFormat);
        } else {
            for (int size = 0; size < dataLen; size += mtuSize) {
                int len = mtuSize;
                if (size + mtuSize >= dataLen) {
                    len = dataLen - size;
                }
                byte[] sizeData = new byte[len];
                System.arraycopy(data, size, sizeData, 0, sizeData.length);
                CommandFormat cf = new CommandFormat(commandFormat);
                cf.setBytes(sizeData);
                addData(cf);
            }
        }
        return true;
    }

    @Override
    public void stopAndClearCommandFormat() {
        commandFormatList.clear();
    }

    private class BLECommandPolicyHandler extends Handler {
        WeakReference<Context> wContext;

        public BLECommandPolicyHandler(@NonNull Looper looper, Context context) {
            super(looper);
            if (context != null) {
                wContext = new WeakReference<>(context);
            }
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (wContext == null) {
                return;
            }

            Context context = wContext.get();
            if (context == null) {
                //context销毁了
                return;
            }

            if (!commandFormatList.isEmpty()) {
                CommandFormat commandFormat = commandFormatList.get(0);
                switch (msg.what) {
                    case hanPostOn:
                        //执行命令
                        if (commandFormat != null) {
                            int sendCmdNumber = commandFormat.getSendCmdNumber();
//                            LOGUtils.d(TAG + " onHanPostCmd"
//                                    + " sendCmdNumber:" + sendCmdNumber
//                                    + " size:" + commandFormatList.size()
//                                    + " data:" + ByteUtils.toHexString(commandFormat.getBytes()));
                            if (commandFormat.getCommandType().equals(BLEConstant.Command_Type_write)
                                    && commandFormat.getWRITE_TYPE() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                                //不带回应的写，不回触发onCharacteristicWrite，无重发
                                if (commandExecute(commandFormat)) {
                                    //发送成功等待写回应
                                    postMessage(hanRemove, 0);
                                } else {
                                    //命令发送错误，下一条
                                    postMessage(hanRemove, 500);
                                }
                            } else {
                                if (sendCmdNumber > 0) {
                                    //重发次数大于0，命令失败重新执行
                                    commandFormat.reduceCmdNumber();
                                    if (commandExecute(commandFormat)) {
                                        //发送成功等待写回应
                                        postMessage(hanPostOn, 1000);
                                    } else {
                                        //命令发送错误，下一条
                                        postMessage(hanRemove, 500);
                                    }
                                } else {
                                    //命令发送错误，下一条
                                    postMessage(hanRemove, 0);
                                }
                            }
                        } else {
                            postMessage(hanRemove, 0);
                        }
                        break;
                    case hanRemove:
                        //指令移除
                        commandFormatList.remove(0);
                        postMessage(hanPostOn, 0);
                        break;
                    case hanTimeOut:
                        //指令超时
                        if (commandFormat != null) {
                            //超时重发
                            postMessage(hanPostOn, 0);
                        } else {
                            postMessage(hanRemove, 0);
                        }
                        break;
                }

            }
        }
    }

    private void addData(CommandFormat commandFormat) {
        commandFormatList.add(commandFormat);
        if (!bleCommandPolicyHandler.hasMessages(hanPostOn)
                && !bleCommandPolicyHandler.hasMessages(hanRemove)
                && !bleCommandPolicyHandler.hasMessages(hanTimeOut)) {
            LOGUtils.d(TAG + " addData start hanPostOn!");
            postMessage(hanPostOn, 0);
        }
    }

    private void postMessage(int what, long delayed) {
        //LOGUtils.e(TAG + " postMessage() ==> what:" + what + "  delayed:" + delayed);
        if (bleCommandPolicyHandler != null) {
            bleCommandPolicyHandler.removeMessages(what);
            bleCommandPolicyHandler.sendEmptyMessageDelayed(what, delayed);
        } else {
            LOGUtils.e(TAG + " Error! bleCommandPolicyHandler == null");
        }
    }

    /**
     * 执行 CommandFormat
     */
    private boolean commandExecute(CommandFormat commandHolder) {
        if (commandHolder == null) {
            LOGUtils.e(TAG + " Error! commandHolder == null");
            return false;
        }
        String servicesUUID = commandHolder.getServicesUUID();
        String characteristicUUID = commandHolder.getCharacteristicUUID();
        if (servicesUUID.isEmpty() || characteristicUUID.isEmpty()) {
            LOGUtils.e(TAG + " Error! ServicesUUID.equals() || CharacteristicUUID.equals()");
            return false;
        }
        if (bluetoothGatt == null) {
            LOGUtils.e(TAG + " Error! bluetoothGatt == null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = BLEUtils.getGattCharacteristic(bluetoothGatt, servicesUUID, characteristicUUID);
        if (characteristic == null) {
            LOGUtils.e(TAG + " Error! characteristic == null");
            return false;
        }
        switch (commandHolder.getCommandType()) {
            case BLEConstant.Command_Type_write:
                boolean isWrite = writeCharacteristic(characteristic, commandHolder.getBytes(), commandHolder.getWRITE_TYPE());
                String bytesStr = ByteUtils.toHexString(commandHolder.getBytes());
                LOGUtils.d(TAG + " commandExecute ==> isWrite:" + isWrite + " size:" + commandHolder.getBytes().length + " bytesStr:" + bytesStr);
                return isWrite;
            case BLEConstant.Command_Type_read:
                boolean isRead = readCharacteristic(characteristic);
                LOGUtils.d(TAG + " commandExecute ==> isRead:" + isRead);
                return isRead;
            case BLEConstant.Command_Type_enNotify:
                boolean isEnNotify = enableNotification(servicesUUID, characteristicUUID);
                LOGUtils.d(TAG + " commandExecute ==> isEnNotify:" + isEnNotify);
                return isEnNotify;
            case BLEConstant.Command_Type_disNotify:
                boolean disEnNotify = disableNotification(servicesUUID, characteristicUUID);
                LOGUtils.d(TAG + " commandExecute ==> disEnNotify:" + disEnNotify);
                return disEnNotify;
            default:
                LOGUtils.e(TAG + " Error! CommandType:" + commandHolder.getCommandType());
                return false;
        }
    }

    //写
    @SuppressLint("MissingPermission")
    private boolean writeCharacteristic(@NonNull BluetoothGattCharacteristic characteristic, byte[] bytes, int writeType) {
        if (bytes == null || bytes.length == 0) {
            LOGUtils.e(TAG + " Error! bytes == null || bytes.length == 0");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bluetoothGatt.writeCharacteristic(characteristic, bytes, writeType) == BluetoothStatusCodes.SUCCESS;
        } else {
            if (!characteristic.setValue(bytes)) {
                LOGUtils.e(TAG + " Error! characteristic.setValue(bytes) == false");
                return false;
            }
            characteristic.setWriteType(writeType);
            return bluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
        return bluetoothGatt.readCharacteristic(characteristic);
    }

    //打开通知
    private boolean enableNotification(String servicesUUID, String characteristicUUID) {
        return BLEUtils.enableCharacteristicNotification(bluetoothGatt, servicesUUID, characteristicUUID);
    }

    //关闭通知
    private boolean disableNotification(String servicesUUID, String characteristicUUID) {
        return BLEUtils.disableCharacteristicNotification(bluetoothGatt, servicesUUID, characteristicUUID);
    }

    @Override
    public void onCharacteristicWriteCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        bleCommandPolicyHandler.removeMessages(hanPostOn);
        postMessage(hanRemove, 0);
    }

    @Override
    public void onCharacteristicReadCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        bleCommandPolicyHandler.removeMessages(hanPostOn);
        postMessage(hanRemove, 0);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        bleCommandPolicyHandler.removeMessages(hanPostOn);
        postMessage(hanRemove, 0);
    }
}
