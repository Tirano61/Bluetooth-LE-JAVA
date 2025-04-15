package com.example.ble_java;

import android.bluetooth.BluetoothGattCharacteristic;

import com.dothantech.lpapi.LPAPI;

public class ImpresoraBluetooth {

    LPAPI api;

    public ImpresoraBluetooth(LPAPI lapapi){
        api = lapapi;
    };

    public void printText(String content, String cabecera) {

            api.openPrinter("");
            /**
             * 	@param v =  width
             * 	@param v1 = height
             * 	@param i = orientation
             *
             */

            api.startJob(50, 150, 0);
            /**
             *	@param text = content,
             *	@param v = x,
             *	@param v1 = y,
             *	@param v2 = width,
             *	@param v3 = height,
             *	@param v4 = fontWeiht,
             * */
            api.drawText("Balanzas Hook sa", 9, 5, 40, 20, 4);
            api.drawText("~~~~~~~~~~~~~~~~~~~~~~~~~~", 5, 10, 40, 100, 2.2);
            api.drawText(cabecera, 5, 12, 40, 100, 2.5);
            api.drawText("~~~~~~~~~~~~~~~~~~~~~~~~~~", 5, 23, 40, 100, 2.2);
            api.drawText(content, 5, 26, 40, 100, 2.2);
            /**
             * 	@param s  = text
             * 	@param v  = x
             * 	@param v1 = y
             * 	@param v2 = width
             *
             */
            //api.draw2DQRCode(content,3,90,47);

            api.commitJob();





    }


}