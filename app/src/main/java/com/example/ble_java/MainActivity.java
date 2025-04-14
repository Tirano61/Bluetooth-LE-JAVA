package com.example.ble_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private BluetoothGatt bluetoothGatt;
    private TextView textView;

    private static final long SCAN_PERIOD = 10000; // 10 segundos
    private static final String TAG = "BLEApp";
    private static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"); // Ejemplo: Servicio Heart Rate
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"); // Característica notificable
    private String impresoraDevice = "G5-41180127";

    private ListView serviceListView;
    private ListView characteristicListView;
    private ArrayAdapter<String> serviceAdapter;
    private ArrayAdapter<String> characteristicAdapter;
    private List<BluetoothGattService> gattServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.data_text);
        ListView listView = findViewById(R.id.list_device);

        serviceListView = (ListView) findViewById( R.id.list_services);
        characteristicListView = (ListView) findViewById(R.id.list_characteristics);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        handler = new Handler();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> connectToDevice(deviceList.get(position)));

        serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        serviceListView.setAdapter(serviceAdapter);

        characteristicAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        characteristicListView.setAdapter(characteristicAdapter);

        serviceListView.setOnItemClickListener((parent, view, position, id) -> displayCharacteristics(gattServices.get(position)));



        /*1if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 3);
        }*/
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            attemptConnectionToPairedPrinter();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1|| requestCode == 3) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
               Log.d("BLUETOOTH", "*****************  Permisos denegados ********************");
            }
        }
    }

    private void displayCharacteristics(BluetoothGattService service){
        runOnUiThread(() ->{
            characteristicAdapter.clear();
            if (service != null){
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics){
                    characteristicAdapter.add(characteristic.getUuid().toString());
                }
            }
        });
    }

    public void startScan() {
        if (!scanning) {
            deviceList.clear();
            adapter.clear();
            handler.postDelayed(() -> {
                scanning = false;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            if (!deviceList.contains(device) && device.getName() != null) { // Verificar si ya está en la lista
                                deviceList.add(device);
                                adapter.add("(Enlazado) " + device.getName() + " - " + device.getAddress()); // Indicar que está enlazado
                            }
                        }
                        attemptConnectionToPairedPrinter();
                    }
                    return;
                }
                bluetoothLeScanner.stopScan(scanCallback);
            }, SCAN_PERIOD);
            scanning = true;
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            if (!deviceList.contains(device) && device.getName() != null) {
                deviceList.add(device);
                adapter.add(device.getName() + " - " + device.getAddress());
                adapter.notifyDataSetChanged();
                Log.d("BLUETOOTH", deviceList.toString());

            }

        }

    };

    public void attemptConnectionToPairedPrinter() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null && device.getName().equals(impresoraDevice)) {
                    Log.i(TAG, "Impresora enlazada encontrada: " + device.getName());
                    connectToDevice(device); // Intentar conectar directamente
                    return; // Salir después de intentar la conexión
                }
            }
            // Si no se encontró la impresora enlazada, iniciar el escaneo
            Log.i(TAG, "Impresora no enlazada. Iniciando escaneo...");
            startScan(); // Llama a tu método startScan() original (sin modificaciones)
        } else {
            Log.e(TAG, "Permiso BLUETOOTH_CONNECT no concedido. No se puede buscar dispositivos enlazados.");
            // Manejar el error de permiso...
        }
    }

    // Modifica tu método connectToDevice para reflejar que ya conocemos el dispositivo
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permiso BLUETOOTH_CONNECT no concedido en connectToDevice.");
            return;
        }
        Log.d(TAG, "Intentando conectar a (directo): " + device.getName() + " - " + device.getAddress());
        bluetoothGatt = device.connectGatt(this, false, gattCallback);  // `false` para no auto-conectar
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "Desconectado");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gattServices = gatt.getServices();
                displayServices(gattServices);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        private void displayServices(List<BluetoothGattService> services) {
            runOnUiThread(() -> {
                serviceAdapter.clear();
                if (services != null && !services.isEmpty()) {
                    for (BluetoothGattService service : services) {
                        serviceAdapter.add("Service: " + service.getUuid().toString());
                    }
                    serviceAdapter.notifyDataSetChanged();
                } else {
                    serviceAdapter.add("No services discovered");
                    serviceAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                byte[] data = characteristic.getValue();
                runOnUiThread(() -> textView.setText("Dato recibido: " + new String(data)));

        }
    };
}