package com.ble.pojos;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class BTDev implements Parcelable {

    private BluetoothDevice bluetoothDevice;

    public BTDev(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    private BTDev(Parcel in) {
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    public static final Creator<BTDev> CREATOR = new Creator<BTDev>() {
        @Override
        public BTDev createFromParcel(Parcel in) {
            return new BTDev(in);
        }

        @Override
        public BTDev[] newArray(int size) {
            return new BTDev[size];
        }
    };

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(bluetoothDevice, flags);
    }
}
