package com.exampledv.mymp15;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.lib.android.bluetooth.BluetoothService;
import com.lib.android.usbserial.driver.UsbSerialDriver;
import com.lib.android.usbserial.driver.UsbSerialProber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.gertec.exception.PPCGeneralException;
import br.com.gertec.pinpad.GPINpad;
import br.com.gertec.pinpad.GPPError;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private BluetoothDevice mmDevice;

    public GPINpad ppcCommand;
    private static final String TAG = "Bluetooth";
    private static final boolean D = true;
    private Button btnConectar;
    private Button btnConectarUsb;
    private Button btnDesconectar;
    private Button btnReceber;
    private Button btnSend;
    private Button btnClear;
    private Button btnInfoComplete;
    private EditText editTextSerial;
    private EditText editTextDisplay;
    private TextView InfoCompleteTxt;

    public byte bStatusBT = 0;
    public byte bStatusUsb = 0;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private String mConnectedDeviceName = null;
    private TextView textViewStatus;

    public static AtomicInteger aiStatus = new AtomicInteger();
    private ProgressDialog pDialog;
    public static int contador = 60;
    public static boolean controleThreadTeclado = false;

    public static String erroDeleteFiles = "Error delete files";

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static final int MESSAGE_DEVICE_NAME = 4;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final int MESSAGE_STATE_CHANGE = 1;

    private int iFrequency, iDuration = 1000;

    private void findComponents() {
        btnConectar = (Button) findViewById(R.id.btnConectar);
        btnConectarUsb = (Button) findViewById(R.id.btnConnectUsb);
        btnDesconectar = (Button) findViewById(R.id.btnDesconectar);
        btnReceber = (Button) findViewById(R.id.btnReceber);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnClear = (Button) findViewById(R.id.btnClear);
        editTextSerial = (EditText) findViewById(R.id.editTextSerial);
        editTextDisplay = (EditText) findViewById(R.id.editTextDisplay);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        //Campos novos
        InfoCompleteTxt = (TextView) findViewById(R.id.InfoCompleteTxt);
        btnInfoComplete = (Button) findViewById(R.id.btnInfoComplete);
    }

    private void onClickCallBack() {
        //Button to make a bluetooth connection to the mobi pin
        btnConectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Checks if there is already a pairing done, if yes, already connects directly
                if (bStatusBT == (byte) 1) {
                    byte bInterface = 2;
                    try {
                        //Opens bluetooth interface
                        Log.i("MainActivity", " chamada a ppc_open");
                        ppcCommand.PPC_Open(bInterface);
//                        ppcCommand.
                        //Changes the status to connected
                        textViewStatus.setText("Status: Conectado");
                        textViewStatus.setTextColor(Color.parseColor("#009900"));
                        //Writes in the mobi pin display
                        ppcCommand.PPC_EMVDisplayString("MP15 - APP Test");
                        Toast.makeText(getApplicationContext(), "Conectado!", Toast.LENGTH_LONG).show();
                        System.out.println("Open OK");
                    } catch (PPCGeneralException e) {
                        Log.i("MainActivity", "btnOpen Error: " + e.getErrorCode());
                        System.out.println("btnOpen Error: " + GPPError.PPC_Error_Message_Debug(e.getErrorCode()));
                        e.printStackTrace();
                    }
                    System.out.println("Bluetooth already connected");
                    return;
                }
                //If no pairing exists, opens the list of paired devices to make the connection
                Intent serverIntent = null;
                serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

        /** NOVO: Botão para conexão USB*/
        btnConectarUsb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bStatusBT == (byte) 0) {
                    byte bInterface = 1; // USB = 1
                    manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    driver = UsbSerialProber.acquire(manager);

                    if (bStatusUsb == (byte) 0) {
                        try {
                            ppcCommand.Set_USB(driver, manager);
                            ppcCommand.PPC_Open(bInterface);
                            ppcCommand.PPC_Sound(iFrequency, iDuration);
                            ppcCommand.PPC_EMVDisplayString("MP15 - PIN PAD");
                            Toast.makeText(getApplicationContext(), "Conectado!", Toast.LENGTH_LONG).show();
                            Log.i("TAG", "Open USB OK");
                            btnConectarUsb.setText("DISCONNECT USB");
//
                            textViewStatus.setText("Status: Conectado");
                            textViewStatus.setTextColor(Color.parseColor("#009900"));
                            bStatusUsb = 1;
                        } catch (PPCGeneralException e) {
                            e.printStackTrace();
                        }
                    } else {
                        byte bAction = 0;
                        try {
                            ppcCommand.PPC_PowerManagement(bAction);
                            ppcCommand.PPC_Close();
                            btnConectarUsb.setText("CONNECT USB");
                            editTextSerial.setText("");
                            textViewStatus.setText("Status: Não conectado");
                            textViewStatus.setTextColor(Color.parseColor("#FF0000"));
                            bStatusUsb = 0;
                        } catch (PPCGeneralException e) {
                            e.printStackTrace();
                            btnConectarUsb.setText("CONNECT USB");
                            editTextSerial.setText("");
                            textViewStatus.setText("Status: Não conectado");
                            textViewStatus.setTextColor(Color.parseColor("#FF0000"));
                            bStatusUsb = 0;
                        }
                    }
                }
            }
        });

        btnDesconectar.setOnClickListener(new View.OnClickListener() {
            //Button to close the bluetooth connection to the mobi pin
            public void onClick(View v) {
                try {
                    //Verifies that really is connected
                    if (bStatusBT == (byte) 1) {
                        //Closes the communication
                        ppcCommand.PPC_Close();
                        System.out.println("Close OK");
                        //Changes the status to disconnected
                        textViewStatus.setText("Status: Não conectado");
                        textViewStatus.setTextColor(Color.parseColor("#FF0000"));
                    } else {
                        System.out.println("Bluetooth not connected");
                    }
                } catch (PPCGeneralException e) {
                    System.out.println("btnClose Error: " + e.getErrorCode());
                    e.printStackTrace();
                }
            }
        });

        btnReceber.setOnClickListener(new View.OnClickListener() {
            //Button to get serial number mobi pin
            public void onClick(View v) {
                try {
                    editTextSerial.setText("");
                    //Verifies that really is connected
                    if (bStatusBT == (byte) 1 || bStatusUsb == (byte) 1) {
                        StringBuffer sbSerNum = new StringBuffer();
                        //Command to receive the serial number
                        ppcCommand.PPC_GetSerialNumber(sbSerNum);
                        //Formats the serial number
                        String sSerNum = TrimZeros(sbSerNum.toString());
                        //Shows the serial number on the application
                        editTextSerial.setText(sSerNum);
                        System.out.println("Serial Number: " + sSerNum);
                    } else {
                        System.out.println("Bluetooth not connected");
                    }
                } catch (PPCGeneralException e) {
                    System.out.println("btnGetSerNum Error: " + e.getErrorCode());

                    e.printStackTrace();
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            //Button to get serial number mobi pin
            public void onClick(View v) {
                try {
                    //Verifies that really is connected
                    if (bStatusBT == (byte) 1 || bStatusUsb == (byte) 1) {
                        String txt = editTextDisplay.getText().toString();
                        //Command to receive the serial number
                        ppcCommand.PPC_DisplayString((byte) 1, txt);
                    } else {
                        System.out.println("Bluetooth not connected");
                    }
                } catch (PPCGeneralException e) {
                    System.out.println("btnDisplay Error: " + e.getErrorCode());

                    e.printStackTrace();
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            //Button to get serial number mobi pin
            public void onClick(View v) {
                try {
                    //Verifies that really is connected
                    if (bStatusBT == (byte) 1 || bStatusUsb == (byte) 1) {
                        //Command to clear display
                        ppcCommand.PPC_LCD_Clear();
                    } else {
                        System.out.println("Bluetooth not connected");
                    }
                } catch (PPCGeneralException e) {
                    System.out.println("btnClearDisplay Error: " + e.getErrorCode());

                    e.printStackTrace();
                }
            }
        });

        btnInfoComplete.setOnClickListener(new View.OnClickListener() {
            // Botão para obter o número de série do dispositivo Mobi Pin
            public void onClick(View v) {
                try {
                    InfoCompleteTxt.setText("");
                    // Verifica se está realmente conectado
                    if (bStatusBT == (byte) 1 || bStatusUsb == (byte) 1) {
                        StringBuffer sbSerNum = new StringBuffer();
                        // Comando para receber o número de série
                        ppcCommand.PPC_GetSerialNumber(sbSerNum);

                        // Verifica se o número de série tem o tamanho correto (16 dígitos)
                        if (sbSerNum.length() != 16) {
                            InfoCompleteTxt.setText("Número de série inválido");
                            return;
                        }

                        // 1º e 3º dígito - Família do Produto
                        String familiaProduto = sbSerNum.substring(0, 1) + "." + sbSerNum.substring(2, 3);

                        // 4 dígitos - PN do Produto
                        String pnProduto = sbSerNum.substring(3, 7);

                        // 2 últimos dígitos do ano de fabricação
                        String anoFabricacao = "20" + sbSerNum.substring(7, 9);

                        // 1 dígito - Mês de fabricação
                        int fabCode = Integer.parseInt(sbSerNum.substring(9, 10));
                        String mesFabricacao = getMesFabricacao(fabCode);
                        String nomeMes = getNomeMes(fabCode); // Obter nome do mês

                        // 5 dígitos sequenciais de produção
                        String sequencialProducao = sbSerNum.substring(10, 15);

                        // Dígito Verificador
                        String digitoVerificador = sbSerNum.substring(15, 16);

                        // Formata o número de série (removendo zeros se necessário)
                        String sSerNum = TrimZeros(sbSerNum.toString());

                        // Montar e exibir as informações detalhadas
                        String serialInfo = "Número de Série: " + sSerNum + "\n" +
                                "Família do Produto: " + familiaProduto + "\n" +
                                "PN do Produto: " + pnProduto + "\n" +
                                "Ano de Fabricação: " + anoFabricacao + "\n" +
                                "Local de Fabricação: " + mesFabricacao + "\n" + // Adicionado local de fabricação
                                "Mês de Fabricação: " + nomeMes + "\n" + // Adicionado nome do mês
                                "Sequencial de Produção: " + sequencialProducao + "\n" +
                                "Dígito Verificador: " + digitoVerificador;

                        InfoCompleteTxt.setText(serialInfo);

                    } else {
                        InfoCompleteTxt.setText("Bluetooth não conectado");
                    }
                } catch (PPCGeneralException e) {
                    System.out.println("Erro ao obter número de série: " + e.getErrorCode());
                    e.printStackTrace();
                }
            }

            private String getMesFabricacao(int fabCode) {
                // Mapeia o código de fabricação para o local correspondente
                if (fabCode >= 1 && fabCode <= 12) {
                    return "Ilhéus";
                } else if (fabCode >= 21 && fabCode <= 32) {
                    return "Diadema";
                } else if (fabCode >= 41 && fabCode <= 52) {
                    return "Manaus";
                } else if (fabCode >= 61 && fabCode <= 71) {
                    return "ODM";
                } else {
                    return "Código de mês desconhecido";
                }
            }

            private String getNomeMes(int mesCode) {
                // Retorna o nome do mês com base no código
                switch (mesCode) {
                    case 1: return "Janeiro";
                    case 2: return "Fevereiro";
                    case 3: return "Março";
                    case 4: return "Abril";
                    case 5: return "Maio";
                    case 6: return "Junho";
                    case 7: return "Julho";
                    case 8: return "Agosto";
                    case 9: return "Setembro";
                    case 10: return "Outubro";
                    case 11: return "Novembro";
                    case 12: return "Dezembro";
                    default: return "Código de mês inválido";
                }
            }
        });


    }

    public static byte[] StringToByteArray2(String hex) {
        return hex.getBytes();
    }

    //Method for formatting the serial number
    private static String TrimZeros(String s) {
        if (s.indexOf('\0') < 0) {
            return s;
        } else {
            return s.substring(0, s.indexOf(0));
        }
    }

    private List<Integer> readFile(InputStream inputStream, List<String> nameFiles) {
        List<Integer> listFiles = new ArrayList<Integer>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            int contador = 0;
            while (line != null) {
                nameFiles.add(line.toLowerCase());
                listFiles.add(getApplicationContext().getResources().getIdentifier(line.toLowerCase().substring(0, line.length() - 4), "raw", getApplicationContext().getPackageName()));
                line = reader.readLine();
                contador = contador + 1;
            }
            return listFiles;
        } catch (Exception e) {
            e.printStackTrace();
            return listFiles;
        }

    }

    private void readFile(InputStream inputStream, List<Integer> capFiles, List<String> nameFiles) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            int contador = 0;
            while (line != null) {
                nameFiles.add(line.toLowerCase());
                capFiles.add(getApplicationContext().getResources().getIdentifier(line.toLowerCase().substring(0, line.length() - 4), "raw", getApplicationContext().getPackageName()));
                line = reader.readLine();
                contador = contador + 1;
            }
        } catch (IOException e) {
        }
    }

    public void resultadoTeclas() {
        // Check if the result was equal to 0, if yes change the icon of the button to "check"
        if (aiStatus.toString().equals("48")) {
            /*btnTeclado.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.check, 0);
            itResult.putExtra("KEYBOARD", "OK");*/
        } else {
            //btnTeclado.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.close, 0);
        }
        Log.i("LOG CONTROLE", "btnTeclado OK - PPC_KBD");
        System.out.println("PPC_KBD OK");
    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                try {
                    ppcCommand.PPC_Close();
                } catch (PPCGeneralException e) {
                }
            }
        }
    };

    //Validação de permissão
    // Verifica se todas as permissões foram concedidas
    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
    }

    // Solicita permissões ao usuário
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES
        }, REQUEST_CODE_PERMISSIONS);
    }

    // Verifica o resultado da solicitação de permissões
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissões concedidas
                // Aqui você pode continuar a busca por dispositivos próximos
            } else {
                // Permissões negadas
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        try {
            //Initializes the library mobi pin
            ppcCommand = new GPINpad(this);
        } catch (IOException e) {
            e.printStackTrace();
            if (D) {
                Log.e(TAG, "Load Library 'libppc900.so' error.", e);
            }
            System.exit(0);
        }
        findComponents();
        onClickCallBack();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
            } else {

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        onNewIntent(getIntent());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Bluetooth not supported
        if (mBluetoothAdapter == null) {
            if (D) {
                Log.e(TAG, "Bluetooth is not available");
            }
            finish();
            return;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                //When DeviceListActivity returns with a device connected
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        ppcCommand.PPC_Close();
                    } catch (PPCGeneralException e) {
                        e.printStackTrace();
                    }

                    //Cancels other connections
                    mBluetoothService.stop();

                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a service session
                    setupBluetooth();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // Attempt to connect to the device
        mBluetoothService.connect(device, secure);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String action = intent.getAction();

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);

            driver = UsbSerialProber.acquire(manager);
            if (driver != null) {
                try {
                    ppcCommand.Set_USB(driver, manager);
                } catch (PPCGeneralException e) {
                    if (D) {
                        Log.e(TAG, "Set_USB@Error");
                    }
                    e.printStackTrace();
                }

            } else {
                if (D) {
                    Log.e(TAG, "onCreate@Driver_Create_Error");
                }
            }
        }
    }

    ;

    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) {
                setupBluetooth();
            }
        }

        // Verifique as permissões necessárias
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 ou superior
            if (!hasPermissions()) {
                requestPermissions();
            }
        }
    }

    private void setupBluetooth() {
        mBluetoothService = new BluetoothService(this, mHandler);
        try {
            ppcCommand.Set_Bluetooth(mBluetoothService, mHandler);
        } catch (PPCGeneralException e) {
            if (D) {
                Log.e(TAG, "Set_Bluetooth@Error");
            }
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        switch (keyCode) {

            case KeyEvent.KEYCODE_MENU:
                return false;
            case KeyEvent.KEYCODE_HOME:
                return false;
            case KeyEvent.KEYCODE_BACK:
                //Cancels the connections
                try {
                    ppcCommand.PPC_Close();
                } catch (PPCGeneralException e) {
                    e.printStackTrace();
                }
                MainActivity.this.finish();
                android.os.Process.killProcess(android.os.Process.myPid());

                return false;
            case KeyEvent.KEYCODE_SEARCH:
                return false;

            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //ImageView img_statusBluetooth = (ImageView) findViewById(R.id.imgStatusBluetooth);

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) {
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    }
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            if (D) {
                                Log.e(TAG, "Bluetooth Connect to " + mConnectedDeviceName);
                            }

                            textViewStatus.setText("Status: Conectado");
                            textViewStatus.setTextColor(Color.parseColor("#009900"));

                            try {
                                Thread.sleep(100);
                                byte bInterface = 2; //1 = USB, 2 = Bluetooth
                                ppcCommand.PPC_Open(bInterface);

                                StringBuffer sbManufacturer = new StringBuffer();
                                StringBuffer sbModel = new StringBuffer();
                                StringBuffer sbFWVersion = new StringBuffer();
                                StringBuffer sbSpecVersion = new StringBuffer();
                                StringBuffer sbAppVersion = new StringBuffer();
                                StringBuffer sbSN = new StringBuffer();
                                byte bNetWork = 0;

                                ppcCommand.PPC_GetInfo(bNetWork, sbManufacturer, sbModel, sbFWVersion, sbSpecVersion, sbAppVersion, sbSN);
                                Toast.makeText(getApplicationContext(), sbManufacturer.toString() + "\n" +
                                                sbModel.toString() + "\n" +
                                                sbFWVersion.toString() + "\n" +
                                                sbSpecVersion.toString() + "\n" +
                                                sbAppVersion.toString() + "\n" +
                                                sbSN.toString()
                                        , Toast.LENGTH_LONG).show();
                                //ppcCommand.PPC_EMVDisplayString("MOBI PIN -   APP Test");

                                bStatusBT = 1;
                            } catch (PPCGeneralException e) {
                                System.out.println("PPC_Open Error: " + e.getErrorCode());
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            bStatusBT = 0;
                            textViewStatus.setText("Status: Não conectando");
                            textViewStatus.setTextColor(Color.parseColor("#FF0000"));
                            break;
                        case BluetoothService.STATE_NONE:
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                            if (D) {
                                Log.i(TAG, "No Bluetooth Connect..");
                            }
                            bStatusBT = 0;
                            textViewStatus.setText("Status: Não conectando");
                            textViewStatus.setTextColor(Color.parseColor("#FF0000"));
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // Save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    if (D) {
                        Log.d(TAG, "Connected to " + mConnectedDeviceName);
                    }
                    break;
                case MESSAGE_TOAST:
                    bStatusBT = 0;
                    if (D) {
                        Log.d(TAG, "Desconnected " + MESSAGE_TOAST);
                    }
                    break;
            }
        }
    };

    class Teclado extends AsyncTask<Integer, Integer, Integer> {
        /**
         * Before starting background thread Show Progress Dialog
         */
        String teclas [][] = {{"Numbers: ","Cancel: ","Clear: ","Enter: ","*: ","#: "},{"Waiting","Waiting","Waiting","Waiting","Waiting","Waiting"}};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Method that will be called if the test is canceled
            pDialog = ProgressDialog.show(
                    MainActivity.this,
                    "",
                    "Test keyboard" + "\n\n" +
                            teclas[0][0] + teclas[1][0] + "\n" +
                            teclas[0][1] + teclas[1][1] + "\n" +
                            teclas[0][2] + teclas[1][2] + "\n" +
                            teclas[0][3] + teclas[1][3] + "\n" +
                            teclas[0][4] + teclas[1][4] + "\n" +
                            teclas[0][5] + teclas[1][5],
                    true,
                    true,
                    new DialogInterface.OnCancelListener(){

                        public void onCancel(DialogInterface dialog) {
                            contador = 0;
                        }
                    }
            );
        }

        protected Integer doInBackground(Integer... args) {
            try {
                // Starts the keyboard test thread
                final RetornoTeclado retorno = new RetornoTeclado(teclas);
                Thread threadRetorno = new Thread(retorno);
                threadRetorno.start();
                contador = 60;
                // Start the 1 minute counter
                while(contador> 0){
                    Log.i("AsyncTask Teclado", "PUBLISH");
                    // Updates the counter
                    publishProgress(contador);
                    contador = contador - 1;
                    Thread.sleep(1000);
                }

                // Wait until the keyboard thread stops spinning to be able to call the stop
                while(controleThreadTeclado == true){

                }
                controleThreadTeclado = false;

                Log.i("AsyncTask Teclado", "STOP");

                // Function to cancel keyboard test
                ppcCommand.PPC_KBD_FunctionalTest_Stop();

            } catch (Exception e) {
                Log.i("AsyncTask Teclado", "EXECPTION QUALQUER" + e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         **/
        protected void onPostExecute(Integer file_url) {
            // Closes the progress bar
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    // Calls the method to show the test result
                    resultadoTeclas();
                }
            });

        }

        protected void onProgressUpdate(Integer... params){
            Log.i("AsyncTask Teclado", "onProgressUpdate");
            // Updates the counter
            pDialog.setMessage("Test keyboard" + "\n\n" +
                    teclas[0][0] + teclas[1][0] + "\n" +
                    teclas[0][1] + teclas[1][1] + "\n" +
                    teclas[0][2] + teclas[1][2] + "\n" +
                    teclas[0][3] + teclas[1][3] + "\n" +
                    teclas[0][4] + teclas[1][4] + "\n" +
                    teclas[0][5] + teclas[1][5] + "\n\n" +
                    String.valueOf(params[0]) + " seconds");
        }
    }

    public class RetornoTeclado implements Runnable {
        String[][] teclas;
        RetornoTeclado(String[][] teclas){
            this.teclas = teclas;
        }


        public void run () {
            try {
                // Choose the test with all the keys at once
                byte bTestMask = (byte)(1 + 2 + 4);

                // Function to start keyboard test
                ppcCommand.PPC_KBD_FunctionalTest_Start(bTestMask);
                controleThreadTeclado = true;

                // While it does not return success or timeout it gets calling the get
                controleTeclado: while(!aiStatus.toString().equals("48") && !aiStatus.toString().equals("49") && contador> 0){
                    Log.i("RetornoTeclado", "PUBLISH");
                    try {
                        aiStatus.set(0);
                        Log.i("RetornoTeclado", "GET");
                        // Function that returns keyboard test result
                        ppcCommand.PPC_KBD_FunctionalTest_Get(aiStatus);
                        Log.i("RetornoTeclado", "RETORNO GET SEM EXCEPTION");
                        Log.i("RetornoTeclado", aiStatus.toString());

                    }
                    catch (PPCGeneralException e){
                        //117 Means that no key was pressed at this time then back to the "while"
                        //11000 Means that it has a key
                        if(e.getErrorCode() == 11000){
                            Log.i("RetornoTeclado", "RETORNO GET COM EXCEPTION 11000");
                            Log.i("RetornoTeclado", aiStatus.toString());
                            if(aiStatus.toString().equals("78")){
                                teclas[1][0] = "OK";
                            }
                            else if(aiStatus.toString().equals("75")){
                                teclas[1][1] = "OK";
                            }
                            else if(aiStatus.toString().equals("76")){
                                teclas[1][2] = "OK";
                            }
                            else if(aiStatus.toString().equals("77")){
                                teclas[1][3] = "OK";
                            }
                            else if(aiStatus.toString().equals("85")){
                                teclas[1][4] = "OK";
                            }
                            else if(aiStatus.toString().equals("68")){
                                teclas[1][5] = "OK";
                            }
                            continue controleTeclado;
                        }
                        Log.i("RetornoTeclado", "RETORNO GET COM EXCEPTION = " + e.getErrorCode());
                    }
                }
                Log.i("RetornoTeclado", "STOP");
                // Warns that the thread has stopped
                controleThreadTeclado = false;
                contador = 0;

            } catch (Exception e) {
                contador = 0;
                Log.i("RetornoTeclado", "EXECPTION QUALQUER" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void resultadoDeleteFiles(){
        Toast.makeText(getApplicationContext(), erroDeleteFiles, Toast.LENGTH_LONG).show();
    }

}