package com.example.ble_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
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
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.dothantech.lpapi.LPAPI;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final UUID SERVICE_UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"); // Ejemplo: Servicio Heart Rate
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3"); // Característica notificable
    private String impresoraDevice = "G5-41180127";

    private ListView serviceListView;
    private ListView characteristicListView;
    private ArrayAdapter<String> serviceAdapter;
    private ArrayAdapter<String> characteristicAdapter;
    private List<BluetoothGattService> gattServices;

    private Button button2;
    private LPAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = LPAPI.Factory.createInstance();

        textView = findViewById(R.id.data_text);
        ListView listView = findViewById(R.id.list_device);
        button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                View dialogView = inflater.inflate(R.layout.dialog, null); // Use your actual layout name
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

                // If you have a button inside the custom layout
                Button okButton = dialogView.findViewById(R.id.button);
                if (okButton != null) {
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // do something
                            onPrintButtonClicked(view, "Hola Mundo");
                            dialog.dismiss();
                        }
                    });
                }
                dialog.show();
            }

        });

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



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // For Android 12 (S) and above, request BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 3);
            } else {
                startScan();
            }
        } else {
            // For Android versions below 12, request ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                startScan();
            }
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
                    StringBuilder characteristicInfo = new StringBuilder();
                    characteristicInfo.append("UUID: ").append(characteristic.getUuid().toString()).append("\n");
                    characteristicInfo.append("Properties: ");
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                        characteristicInfo.append("Read ");
                    }
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                        characteristicInfo.append("Write ");
                    }
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        characteristicInfo.append("Notify ");
                    }
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        characteristicInfo.append("Indicate ");
                    }
                    characteristicAdapter.add(characteristicInfo.toString());
                    Log.d(TAG, "Characteristic Info: " + characteristicInfo.toString());
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.DEVICE_TYPE_CLASSIC);  // `false` para no auto-conectar
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Estado de conexión: conectado, status: " + status);

                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        //gatt.discoverServices();
                        new Handler().postDelayed(() -> gatt.discoverServices(), 2000);
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        Log.i(TAG, "Desconectado");
                    } else {
                        Log.d(TAG, "Cambio de estado de conexión, newState: " + newState + ", status: " + status); // Añade esto para otros estados
                    }
                }
            });

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered status: " + status);
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
                        Log.d(TAG, "Service: " + service.getUuid().toString());
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
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Data write confirmed successfully (callback): " + new String(characteristic.getValue())); // Or log any relevant status or data
                // If you are sending multiple chunks, send the next chunk here.
            } else {
                Log.e(TAG, "Data write failed (callback), status: " + status);
                // Handle the error: retry, inform the user, etc.
            }
        }
    };

    public void onPrintButtonClicked(View view, String dataToPrint) {
        //sendDataToPrinter();
        List<String> cabecera = Arrays.asList("Cabecera 1", "Cabecera 2");
        List<String> datos = Arrays.asList("Dato 1", "Dato 2", "Dato 3");
        imprimirTexto(datos, cabecera);
        Log.d(TAG, "Data written successfully: " + datos);

    }
    public void imprimirTexto(List<String> datos, List<String> listaCabecera){
        ImpresoraBluetooth impresoraBluetooth = new ImpresoraBluetooth(api);
        String impresion = "";
        String cabecera = "";
        for (int i = 0; i < listaCabecera.size(); i++){
            cabecera = cabecera + listaCabecera.get(0)+"\n";
        }
        for (int i = 0; i < datos.size(); i++){
            impresion = impresion + datos.get(i)+"\n";
        }

        impresoraBluetooth.printText(impresion, cabecera);
    }
}
