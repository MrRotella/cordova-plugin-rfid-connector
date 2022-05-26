package cordova.plugin.rfidconnector;

import org.apache.cordova.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.lang.ref.WeakReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.commands.BarcodeCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.BatteryStatusCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.VersionInformationCommand;
import com.uk.tsl.rfid.asciiprotocol.enumerations.Databank;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QueryTarget;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SelectAction;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SelectTarget;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.device.IAsciiTransport;
import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.asciiprotocol.responders.IAsciiCommandResponder;
import com.uk.tsl.rfid.asciiprotocol.responders.IBarcodeReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ITransponderReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.TransponderData;
import com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import com.uk.tsl.rfid.asciiprotocol.parameters.AntennaParameters;
import com.uk.tsl.utils.Observable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class TSLScannerDevice implements ScannerDevice {

    private static final String ERROR_LABEL = "Error: ";
    private static final String DEVICE_IS_ALREADY_CONNECTED = "Device is already connected.";
    private static final String SCAN_POWER = "dScanPower";
    private static final String ANTENNA_MAX = "antennaMax";
    private static final String ANTENNA_MIN = "antennaMin";
    private static final String SERIAL_NUMBER = "serialNumber";
    private static final String MANUFACTURER = "manufacturer";
    private static final String FIRMWARE_VERSION = "firmwareVersion";
    private static final String HARDWARE_VERSION = "hardwareVersion";
    private static final String BATTERY_STATUS = "batteryStatus";
    private static final String BATTERY_LEVEL = "batteryLevel";
    private static final String DEVICE_NAME = "deviceName";
    private static final String DEVICE_IS_NOT_CONNECTED = "Device is not connected.";
    private static AsciiCommander commander;
    private CordovaPlugin rfidConnector;
    // The Reader currently in use
    private Reader mReader = null;
    private ObservableReaderList mReaders;

    // The current setting of the power level
    private int mPowerLevel = AntennaParameters.MaximumCarrierPower;

    private boolean isInventoryRunning = false;
    private boolean isTriggeredInventory = false;

    final Context context;
    private static CallbackContext dataAvailableCallback;
    private static InventoryCommand mInventoryCommand;
    private static InventoryCommand inventoryResponder;
    private static BarcodeCommand barcodeResponder;

    private static InventoryCommand inventorySearchResponder;
    private static CallbackContext searchCallback;
    private static CallbackContext connectCallback;
    private static CallbackContext disconnectCallback;

    private GenericHandler mGenericModelHandler;

    private CallbackContext mConnectCallbackContext = null;
    private CallbackContext mScanCallbackContext = null;
    private CallbackContext mStatusCallbackContext = null;
    private CallbackContext mWriteCallbackContext = null;


    private Collector collector;
    
    public TSLScannerDevice(final CordovaPlugin rfidConnector) {
        this.rfidConnector = rfidConnector;



        this.context = rfidConnector.cordova.getActivity().getBaseContext();
        // Ensure the shared instance of AsciiCommander exists
        //this.commander = AsciiCommander.createSharedInstance(rfidConnector.cordova.getActivity().getApplicationContext());


        //this.commander = getCommander();
        //this.commander = getCommander();

        // Add responder to enable the synchronous commands
        //commander.addSynchronousResponder();

        // Configure the ReaderManager when necessary
        //ReaderManager.create(rfidConnector.cordova.getContext());



        //ReaderManager.sharedInstance().getReaderList().list();
//        try {
//            ReaderManager.sharedInstance().initialiseList();
//            mReaders = ReaderManager.sharedInstance().getReaderList();
//            //mReaders = ReaderManager.sharedInstance().getReaderList().list();
//            //if(mReaders != null && !mReaders.isEmpty()){
//            //    mReader = mReaders.get(0);
//            //}
//        } catch (Exception e) {
//            //TODO: handle exception
//        }
//        // Add observers for changes
//        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
//        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
//        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);
//
//        mInventoryCommand = getInventoryInstance();
        // See if there is a reader currently in use
        //Intent intent = rfidConnector.cordova.getActivity().getIntent();
        //int startIndex = intent.getIntExtra("tsl_device_index", -1);
//        int startIndex = 0;
//        if( startIndex >= 0 )
//        {
//            mReader = ReaderManager.sharedInstance().getReaderList().list().get(startIndex);
//            if( mReader != null ) {
//                try {
//                    Log.d("", "TSLScannerDevice: " + mReader.getDisplayName() + "\n");
//                } catch (Exception e) {
//                    //TODO: handle exception
//                }
//
//            }
//        }



    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        // https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html#plugin-initialization-and-lifetime
        Log.i("CordovaLog", "initialize() was called");
        super.initialize(cordova, webView);

        onCreate();
    }

    //@Override
    public void onCreate() {

        //super.onCreate();
        Log.i("CordovaLog", "onCreate() was called");
        //super.onCreate(savedInstanceState);

        mGenericModelHandler = new GenericHandler(this.cordova.getActivity());

        // Ensure the shared instance of AsciiCommander exists
        AsciiCommander.createSharedInstance(getApplicationContext());
        //AsciiCommander.createSharedInstance(rfidConnector.cordova.getContext());
        final AsciiCommander commander = getCommander();

        // Ensure that all existing responders are removed
        commander.clearResponders();

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is ADDED FIRST so that no other responder can consume received lines before they are logged.
        commander.addResponder(new LoggerResponder());

        //
        // Add a simple Responder that sends the Reader output to the App message list
        //
        // Note - This is not the recommended way of receiving Reader input - it is just a convenient
        // way to show that the Reader is connected and communicating - see the other Sample Projects
        // for how to Inventory, Read, Write etc....
        //
        /*
        commander.addResponder(new IAsciiCommandResponder() {
            @Override
            public boolean isResponseFinished() { return false; }

            @Override
            public void clearLastResponse() {}

            @Override
            public boolean processReceivedLine(String fullLine, boolean moreLinesAvailable)
            {
                logMessage(">>> " + fullLine);
                // don't consume the line - allow others to receive it
                return false;
            }
        });
        */

        // Add responder to enable the synchronous commands
        commander.addSynchronousResponder();

        // Configure the ReaderManager when necessary
        ReaderManager.create(getApplicationContext());

        // Add observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

        //setModels();

        ReaderManager.sharedInstance().updateList();

        autoSelectReader(true);

        //onCreateForEDA51();
    }

    private void onResumeTask(){
        // Register to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(this.cordova.getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));


        // Remember if the pause/resume was caused by ReaderManager - this will be cleared when ReaderManager.onResume() is called
        boolean readerManagerDidCauseOnPause = ReaderManager.sharedInstance().didCauseOnPause();

        // The ReaderManager needs to know about Activity lifecycle changes
        ReaderManager.sharedInstance().onResume();

        // The Activity may start with a reader already connected (perhaps by another App)
        // Update the ReaderList which will add any unknown reader, firing events appropriately
        ReaderManager.sharedInstance().updateList();

        // Reconnect to the Reader in use (locate a Reader to use when necessary)
        autoSelectReader(!readerManagerDidCauseOnPause);

        //mIsSelectingReader = false;
    }

    @Override
    public void onPause(boolean multitasking) {


        logMessage("Pausing...");

        // Register to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(this.cordova.getActivity()).unregisterReceiver(mMessageReceiver);

        // Disconnect from the reader to allow other Apps to use it
        // unless pausing when USB device attached or using the DeviceListActivity to select a Reader
        if(!ReaderManager.sharedInstance().didCauseOnPause() && mReader != null )
        {
            mReader.disconnect();
            onDisconnect();
        }

        ReaderManager.sharedInstance().onPause();

        if (this.collector != null){
            this.collector.onPause();
        }

    }

    @Override
    public void onDestroy()
    {
        logMessage("onDestroy");
        //super.onDestroy();

        // Remove observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);

        if (this.collector != null){
            this.collector.onDestroy();
            this.collector = null;
        }

    }

    private Context getApplicationContext(){
        return this.cordova.getActivity().getApplicationContext();
    }

    private void handleException(Exception ex, String caller, CallbackContext callbackContext) {
        Log.e("CordovaLog", "Exception: ", ex);
        callbackContext.error("ERROR " + caller + ". Exception: " + ex.getMessage());

    }
    //
    // Select the Reader to use and reconnect to it as needed
    //
    private void AutoSelectReader(boolean attemptReconnect)
    {
        ObservableReaderList readerList = ReaderManager.sharedInstance().getReaderList();
        Reader usbReader = null;
        if( readerList.list().size() >= 1)
        {
            // Currently only support a single USB connected device so we can safely take the
            // first CONNECTED reader if there is one
            for (Reader reader : readerList.list())
            {
                if (reader.hasTransportOfType(TransportType.USB))
                {
                    usbReader = reader;
                    break;
                }
            }
        }

        if( mReader == null )
        {
            if( usbReader != null )
            {
                // Use the Reader found, if any
                mReader = usbReader;
                getCommander().setReader(mReader);
            }
        }
        else
        {
            // If already connected to a Reader by anything other than USB then
            // switch to the USB Reader
            IAsciiTransport activeTransport = mReader.getActiveTransport();
            if ( activeTransport != null && activeTransport.type() != TransportType.USB && usbReader != null)
            {
                Log.d("", "Disconnecting from: " + mReader.getDisplayName() + "\n");
                if( mReader != null )
                {
                    mReader.disconnect();
                    // Explicitly clear the Reader as we are finished with it
                    mReader = null;
                }


                mReader = usbReader;

                // Use the Reader found, if any
                getCommander().setReader(mReader);
            }
        }

        // Reconnect to the chosen Reader
        if( mReader != null
                && !mReader.isConnecting()
                && (mReader.getActiveTransport()== null || mReader.getActiveTransport().connectionStatus().value() == ConnectionState.DISCONNECTED))
        {
            // Attempt to reconnect on the last used transport unless the ReaderManager is cause of OnPause (USB device connecting)
            if( attemptReconnect )
            {
                if( mReader.allowMultipleTransports() || mReader.getLastTransportType() == null )
                {
                    // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
                    if( mReader.connect() )
                    {
                        Log.d("", "Connecting to: " + mReader.getDisplayName() +"\n");
                    }
                }
                else
                {
                    // Reader supports only a single active transport so connect to it over the transport that was last in use
                    if( mReader.connect(mReader.getLastTransportType()) )
                    {
                        Log.d("", "Connecting (over last transport) to: " + mReader.getDisplayName() +"\n");
                    }
                }
            }
        }
    }

    // ReaderList Observers
    Observable.Observer<Reader> mAddedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // See if this newly added Reader should be used
            AutoSelectReader(true);
        }
    };

    Observable.Observer<Reader> mUpdatedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
//            // Is this a change to the last actively disconnected reader
//            if( reader == mLastUserDisconnectedReader )
//            {
//                // Things have changed since it was actively disconnected so
//                // treat it as new
//                mLastUserDisconnectedReader = null;
//            }

            // Was the current Reader disconnected i.e. the connected transport went away or disconnected
            if( reader == mReader && !reader.isConnected() )
            {
                // No longer using this reader
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
            else
            {
                // See if this updated Reader should be used
                // e.g. the Reader's USB transport connected
                AutoSelectReader(true);
            }
        }
    };

    Observable.Observer<Reader> mRemovedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // Was the current Reader removed
            if( reader == mReader)
            {
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
        }
    };

    public AsciiCommander getCommander() {
        return AsciiCommander.sharedInstance();

//        if (commander == null) {
//            commander = new AsciiCommander(context);
//        }
//        return commander;
    }

    //
    // Handle the messages broadcast from the AsciiCommander
    //
    private BroadcastReceiver mCommanderMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(getClass().getName(), "AsciiCommander state changed - isConnected: " + getCommander().isConnected());

            String connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY);
            Log.d(getClass().getName(), "AsciiCommander state changed - connectionStateMsg : " + connectionStateMsg);

//            if( getCommander().isConnected() )
//            {
//                // Update for any change in power limits
//                setPowerBarLimits();
//                // This may have changed the current power level setting if the new range is smaller than the old range
//                // so update the model's inventory command for the new power value
//                mModel.getCommand().setOutputPower(mPowerLevel);
//
//                mModel.resetDevice();
//                mModel.updateConfiguration();
//            }

            switch (commander.getConnectionState()) {
                case CONNECTED:
                    if (connectCallback != null) {
                        removeAsyncAndAddSyncResponder();
                        if (commander.isConnected()) {
                            VersionInformationCommand versionInfoCommand = VersionInformationCommand.synchronousCommand();
                            commander.executeCommand(versionInfoCommand);
                            if (versionInfoCommand.getManufacturer() == null || !(versionInfoCommand.getManufacturer()
                                    .toString()
                                    .contains("TSL")
                                    || versionInfoCommand.getManufacturer()
                                    .toString()
                                    .contains("Technology Solutions"))) {
                                //commander.disconnect();
                                //if( mReader != null ) {
                                //    mReader.disconnect();
                                    // Explicitly clear the Reader as we are finished with it
                                //    mReader = null;
                               // }
                                connectCallback.error("Not a recognised device!");
                            }else{
                                InventoryCommand inventoryCommand = getInventoryInstance();
                                inventoryCommand.setTakeNoAction(TriState.YES);
                                commander.executeCommand(inventoryCommand);
                                removeSyncAndAddAsyncResponder();
                                connectCallback.success("true");
                                //connectCallback = null;
                            }

                        }else{
                            connectCallback.error("Commander is not connected!");
                        }
                    }
                    break;
                case DISCONNECTED:
                    if (connectCallback != null) {
                        connectCallback.error(commander.getConnectionState().name());
                        //connectCallback = null;
                    }
                    if (disconnectCallback != null) {
                        disconnectCallback.success("true");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void connect(final String deviceID, final CallbackContext callbackContext) {
        connectCallback = callbackContext;
        // Register to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(context).registerReceiver(mCommanderMessageReceiver,
                new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));
        // printResponders(callbackContext, "Before connect");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (deviceID != null && deviceID.length() > 0) {
                    Log.d("", "deviceID: " + deviceID + "\n");
                    if (commander.isConnected()) {
                        Log.d("", "DEVICE_IS_ALREADY_CONNECTED \n");
                        callbackContext.error(DEVICE_IS_ALREADY_CONNECTED);
                    } else {
                        try {
                            //ReaderManager.create(getApplicationContext());
                            //ReaderManager.sharedInstance().getReaderList().list()
                            ObservableReaderList mReadersCurrent = ReaderManager.sharedInstance().getReaderList();
                            // ArrayList<Reader> mReadersCurrent = ReaderManager.sharedInstance().getReaderList().list();
                            Log.d("", "ReaderManager \n");
                            if(mReadersCurrent.list().size() >= 1){
                                Log.d("", "mReaders \n");
                                for (Reader listReader : mReadersCurrent.list()) {
                                    if(!listReader.isConnected())
                                    {
                                        Log.d("", "Serial Number: " + listReader.getSerialNumber() + "\n");
                                        Log.d("", "Display Name: " + listReader.getDisplayName() + "\n");
                                        //if(listReader.getSerialNumber().equals(deviceID))
                                        //{
                                        mReader = listReader;
                                        getCommander().setReader(mReader);
                                        commander.setReader(mReader);
                                        if( mReader.allowMultipleTransports() || mReader.getLastTransportType() == null )
                                        {
                                            // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
                                            mReader.connect();
                                            Log.d("", "Connected: " + mReader.getDisplayName() + "\n");
                                        }
                                        else
                                        {
                                            // Reader supports only a single active transport so connect to it over the transport that was last in use
                                            mReader.connect(mReader.getLastTransportType());
                                            Log.d("", "Connected: " + mReader.getDisplayName() + "\n");
                                        }
                                        callbackContext.success("true");
                                        return;
                                        //}
                                    }

                                }

                            }
                        } catch (Exception e) {
                            Log.d("", "Error: " + e.getMessage() + "\n");
                        }

//                        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
//                        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
//
//                        Boolean status = bluetoothAdapter.startDiscovery();
//                        Set<BluetoothDevice> listOfBondedDevices = bluetoothAdapter.getBondedDevices();

//                        for (BluetoothDevice device : listOfBondedDevices) {
//                            if (deviceID.equals(device.getAddress()) || deviceID.equals(device.getName())) {
//                                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());
//                                //commander.connect(bluetoothDevice);
//                                try {
//                                    ArrayList<Reader> mReaders = ReaderManager.sharedInstance().getReaderList().list();
//                                    if(mReaders != null && !mReaders.isEmpty()){
//                                        for (Reader listReader : mReaders) {
//                                           if(listReader.getSerialNumber().equals(deviceID))
//                                           {
//                                                mReader = listReader;
//                                                getCommander().setReader(mReader);
//                                                if( mReader.allowMultipleTransports() || mReader.getLastTransportType() == null )
//                                                {
//                                                    // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
//                                                    mReader.connect();
//                                                }
//                                                else
//                                                {
//                                                    // Reader supports only a single active transport so connect to it over the transport that was last in use
//                                                    mReader.connect(mReader.getLastTransportType());
//                                                }
//                                                break;
//                                           }
//                                        }
//
//                                    }
//                                } catch (Exception e) {
//                                    //TODO: handle exception
//                                }
//                                // printResponders(callbackContext, "After connect");
//
//                                // PluginResult pluginResult = new
//                                // PluginResult(PluginResult.Status.OK,
//                                // "Trying to connect " + deviceID + "(" + bluetoothDevice.getName()
//                                // + ")");
//                                // pluginResult.setKeepCallback(true);
//                                // callbackContext.sendPluginResult(pluginResult);
//                                return;
//                            }
//                        }
                        callbackContext.error("Device not found " + deviceID);
                    }
                } else {
                    callbackContext.error("Expected one non-empty string argument for device ID.");
                }
            }
        });
    }

    @Override
    public void isConnected(final CallbackContext callbackContext) {

        removeAsyncAndAddSyncResponder();
        if (commander.isConnected()) {
            VersionInformationCommand versionInfoCommand = VersionInformationCommand.synchronousCommand();
            commander.executeCommand(versionInfoCommand);
            if(versionInfoCommand.getManufacturer() == null || !versionInfoCommand.getManufacturer().toString().contains("TSL")){
                callbackContext.error("This not a recognised device!");
            }else{
                callbackContext.success("true");
            }
            removeSyncAndAddAsyncResponder();
        }else{
            callbackContext.error("Commander is not connected!");
        }

    }

    @Override
    public void disconnect(final CallbackContext callbackContext) {
        disconnectCallback = callbackContext;
        if (commander.isConnected()) {
            removeAsyncResponders();

            inventorySearchResponder = null;
            searchCallback = null;

            inventoryResponder = null;
            barcodeResponder = null;
            dataAvailableCallback = null;
            // commander.disconnect();
            if( mReader != null ) {
                mReader.disconnect();
                // Explicitly clear the Reader as we are finished with it
                mReader = null;
            }
        }

        // PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "Trying to
        // disconnect");
        // pluginResult.setKeepCallback(true);
        // callbackContext.sendPluginResult(pluginResult);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mCommanderMessageReceiver);

    }

    @Override
    public void getDeviceInfo(final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before getDeviceInfo");
        try {
            removeAsyncAndAddSyncResponder();
            if (commander.isConnected()) {
                JSONObject deviceInfo = new JSONObject();
                BatteryStatusCommand status = BatteryStatusCommand.synchronousCommand();
                commander.executeCommand(status);

                deviceInfo.put(DEVICE_NAME, commander.getConnectedDeviceName());
                deviceInfo.put(BATTERY_LEVEL, status.getBatteryLevel());
                deviceInfo.put(BATTERY_STATUS, status.getChargeStatus() == null ? " " : status.getChargeStatus().getDescription());

                VersionInformationCommand versionInfoCommand = VersionInformationCommand.synchronousCommand();
                commander.executeCommand(versionInfoCommand);

                deviceInfo.put(HARDWARE_VERSION, "N.A.");
                deviceInfo.put(FIRMWARE_VERSION, versionInfoCommand.getFirmwareVersion() == null ? " " : versionInfoCommand.getFirmwareVersion());
                deviceInfo.put(MANUFACTURER, versionInfoCommand.getManufacturer() == null ? " " : versionInfoCommand.getManufacturer());
                deviceInfo.put(SERIAL_NUMBER, versionInfoCommand.getSerialNumber() == null ? " " : versionInfoCommand.getSerialNumber());
                deviceInfo.put(ANTENNA_MIN, commander.getDeviceProperties().getMinimumCarrierPower());
                deviceInfo.put(ANTENNA_MAX, commander.getDeviceProperties().getMaximumCarrierPower());
                deviceInfo.put(SCAN_POWER, getInventoryInstance().getOutputPower());

                callbackContext.success(JSONUtil.createJSONObjectSuccessResponse(deviceInfo));
            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }

        } catch (JSONException ex) {
            callbackContext.error(ex.getMessage());
        } finally {
            removeSyncAndAddAsyncResponder();
        }
        // printResponders(callbackContext, "After getDeviceInfo");
    }

    @Override
    public void scanRFIDs(final boolean useAscii, final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before scanRFIDs");
        try {
            removeAsyncAndAddSyncResponder();
            if(!getCommander().isConnected()) {
                // The Activity may start with a reader already connected (perhaps by another App)
                // Update the ReaderList which will add any unknown reader, firing events appropriately
                ReaderManager.sharedInstance().updateList();
                if(mReader != null && !mReader.isConnected())
                {
                    mReader.connect();
                    commander.setReader(mReader);
                }
                if(mReader == null && ReaderManager.sharedInstance().getReaderList().list().size() >= 1){
                    mReader = ReaderManager.sharedInstance().getReaderList().list().get(0);
                    mReader.connect();
                    commander.setReader(mReader);
                }

            }
            if (commander.isConnected()) {
                final JSONArray data = new JSONArray();

                InventoryCommand inventoryCommand = getInventoryInstance();
                inventoryCommand.setTakeNoAction(TriState.NO);

                inventoryCommand.setTransponderReceivedDelegate(new ITransponderReceivedDelegate() {

                    @Override
                    public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
                        // PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,
                        // "TTTTTT scanRFIDs transponderReceived");
                        // pluginResult.setKeepCallback(true);
                        // callbackContext.sendPluginResult(pluginResult);
                        try {
                            String epc = transponder.getEpc();

//                            if (useAscii) {
//                                epc = ConversionUtil.hexToAscii(epc);
//                            }

                            data.put(JSONUtil.createRFIDJSONObject(epc, transponder.getRssi()));
                        } catch (JSONException ex) {
                            // Handle tag failure response
                        }
                    }
                });

                commander.executeCommand(inventoryCommand);

                callbackContext.success(JSONUtil.createJSONObjectSuccessResponse(data));

            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        } finally {
            removeSyncAndAddAsyncResponder();
        }
        // printResponders(callbackContext, "After scanRFIDs");
    }

    @Override
    public void search(final String tagID, final boolean useAscii, final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before search");
        try {
            removeAsyncAndAddSyncResponder();
            if (commander.isConnected() && tagID != null) {
                final JSONArray data = new JSONArray();

                InventoryCommand inventoryCommand = getInventoryInstance();
                inventoryCommand.setTakeNoAction(TriState.NO);

                inventoryCommand.setInventoryOnly(TriState.YES);

                // inventoryCommand.setQueryTarget(QueryTarget.TARGET_B);
                // inventoryCommand.setQuerySession(QuerySession.SESSION_0);
                // inventoryCommand.setSelectAction(SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A);
                // inventoryCommand.setSelectTarget(SelectTarget.SESSION_0);

                // inventoryCommand.setSelectBank(Databank.ELECTRONIC_PRODUCT_CODE);

                String tagIDTemp = tagID;
                if (useAscii) {
                    tagIDTemp = ConversionUtil.asciiToHex(tagID);
                }
                // inventoryCommand.setSelectData(tagIDTemp);
                // inventoryCommand.setSelectLength(40);
                // inventoryCommand.setSelectOffset(0020);
                inventoryCommand.setCaptureNonLibraryResponses(true);

                // Toast.makeText(context, "adding search tag- " + tagID, Toast.LENGTH_SHORT);

                inventoryCommand.setTransponderReceivedDelegate(new ITransponderReceivedDelegate() {

                    @Override
                    public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
                        // PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,
                        // "TTTTTT search transponderReceived");
                        // pluginResult.setKeepCallback(true);
                        // callbackContext.sendPluginResult(pluginResult);
                        String epc = transponder.getEpc();
                        if (useAscii) {
                            epc = ConversionUtil.hexToAscii(epc);
                        }
                        if (!epc.equals(tagID)) {
                            return;
                        }

                        // if (tagID.equals(epc)) {
                        try {
                            data.put(JSONUtil.createRFIDJSONObject(epc, transponder.getRssi()));
                        } catch (JSONException ex) {

                        }
                        // }
                    }
                });

                commander.executeCommand(inventoryCommand);
                callbackContext.success(JSONUtil.createJSONObjectSuccessResponse(data));

            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse("Device is not connected/ No tag is given for searching."));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        } finally {
            removeSyncAndAddAsyncResponder();
        }
        // printResponders(callbackContext, "After search");
    }

    @Override
    public void startSearch(final String tagID, final boolean useAscii, final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before startSearch");
        try {
            if (commander.isConnected() && searchCallback == null) {
                searchCallback = callbackContext;
                removeAsyncResponders();
                // Inventory responder
                if (inventorySearchResponder == null) {
                    final List<JSONObject> dataList = new ArrayList<JSONObject>();
                    inventorySearchResponder = new InventoryCommand();
                    inventorySearchResponder.setTakeNoAction(TriState.NO);

                    inventorySearchResponder.setInventoryOnly(TriState.YES);

                    inventorySearchResponder.setQueryTarget(QueryTarget.TARGET_B);
                    inventorySearchResponder.setQuerySession(QuerySession.SESSION_0);
                    inventorySearchResponder.setSelectAction(SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A);
                    inventorySearchResponder.setSelectTarget(SelectTarget.SESSION_0);

                    inventorySearchResponder.setSelectBank(Databank.ELECTRONIC_PRODUCT_CODE);

                    String tagIDTemp = tagID;
                    if (useAscii) {
                        tagIDTemp = ConversionUtil.asciiToHex(tagID);
                    }
                    inventorySearchResponder.setSelectData(tagIDTemp);
                    inventorySearchResponder.setSelectLength(40);
                    inventorySearchResponder.setSelectOffset(0020);
                    inventorySearchResponder.setCaptureNonLibraryResponses(true);

                    // PluginResult pluginResult1 = new PluginResult(PluginResult.Status.OK, "TTTTTT
                    // " + inventorySearchResponder.getCommandLine());
                    // pluginResult1.setKeepCallback(true);
                    // callbackContext.sendPluginResult(pluginResult1);

                    inventorySearchResponder.setTransponderReceivedDelegate(new ITransponderReceivedDelegate() {

                        @Override
                        public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
                            String epc = transponder.getEpc();
                            if (useAscii) {
                                epc = ConversionUtil.hexToAscii(epc);
                            }
                            if (!epc.equals(tagID)) {
                                return;
                            }

                            try {
                                dataList.add(JSONUtil.createRFIDJSONObject(epc, transponder.getRssi()));
                                final JSONArray data = new JSONArray();
                                for (JSONObject rfidObject : dataList) {
                                    data.put(rfidObject);
                                }
                                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, JSONUtil.createJSONObjectSuccessResponse(data));
                                pluginResult.setKeepCallback(true);
                                searchCallback.sendPluginResult(pluginResult);
                                dataList.clear();
                            } catch (JSONException ex) {
                                // Handle tag failure response
                            }
                        }
                    });

                    commander.addResponder(inventorySearchResponder);
                    // commander.executeCommand(inventorySearchResponder);
                }

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "SEARCH ACTIVATED");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } else if (commander.isConnected()) {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse("SEARCH IS ALREADY ACTIVATED"));
            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        }
        // printResponders(callbackContext, "After startSearch");
    }

    @Override
    public void stopSearch(final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before stopSearch");
        try {
            if (commander.isConnected()) {
                if (inventorySearchResponder != null) {
                    commander.removeResponder(inventorySearchResponder);
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "REMOVING SEARCH RESPONDER");
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
                inventorySearchResponder = null;
                searchCallback = null;
                addAsyncResponders();

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "SEARCH DEACTIVATED");
                // pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        }
        // printResponders(callbackContext, "After stopSearch");
    }

    @Override
    public void setOutputPower(final int powerValue, final CallbackContext callbackContext) {
        try {
            removeAsyncAndAddSyncResponder();
            if (commander.isConnected()) {
                int minPower = commander.getDeviceProperties().getMinimumCarrierPower();
                int maxPower = commander.getDeviceProperties().getMaximumCarrierPower();

                if (powerValue >= minPower && powerValue <= maxPower) {
                    InventoryCommand mInventoryCommand = getInventoryInstance();
                    int oldPower = mInventoryCommand.getOutputPower();

                    // mInventoryCommand.setResetParameters(TriState.YES);
                    // Configure the type of inventory
                    mInventoryCommand.setIncludeTransponderRssi(TriState.YES);
                    // mInventoryCommand.setIncludeChecksum(TriState.YES);
                    // mInventoryCommand.setIncludePC(TriState.YES);
                    // mInventoryCommand.setIncludeDateTime(TriState.YES);
                    mInventoryCommand.setTakeNoAction(TriState.YES);
                    mInventoryCommand.setOutputPower(powerValue);

                    commander.executeCommand(mInventoryCommand);

                    callbackContext.success("Scan power set from " + oldPower + " to " + powerValue);
                } else {
                    callbackContext.error("Scan power " + powerValue + " is not in device range(" + minPower + " to " + maxPower + ")");
                }
            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        } finally {
            removeSyncAndAddAsyncResponder();
        }
    }

    @Override
    public void subscribeScanner(final boolean useASCII, final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before subscribeScanner");
        try {
            if (commander.isConnected() && dataAvailableCallback == null) {
                dataAvailableCallback = callbackContext;

                // Inventory responder
                if (inventoryResponder == null) {
                    final List<JSONObject> dataList = new ArrayList<JSONObject>();
                    inventoryResponder = new InventoryCommand();
                    inventoryResponder.setTakeNoAction(TriState.NO);
                    inventoryResponder.setIncludeTransponderRssi(TriState.YES);
                    inventoryResponder.setCaptureNonLibraryResponses(true);
                    inventoryResponder.setTransponderReceivedDelegate(new ITransponderReceivedDelegate() {
                        @Override
                        public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
                            String epc = transponder.getEpc();
                            if (useASCII) {
                                epc = ConversionUtil.hexToAscii(epc);
                            }
                            try {
                                dataList.add(JSONUtil.createRFIDJSONObject(epc, transponder.getRssi()));
                                if (!moreAvailable) {
                                    final JSONArray data = new JSONArray();
                                    for (JSONObject rfidObject : dataList) {
                                        data.put(rfidObject);
                                    }
                                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,
                                            JSONUtil.createJSONObjectSuccessResponse(data));
                                    pluginResult.setKeepCallback(true);
                                    dataAvailableCallback.sendPluginResult(pluginResult);
                                    dataList.clear();
                                }
                            } catch (JSONException ex) {
                                // Handle tag failure response
                            }
                        }
                    });

                    commander.addResponder(inventoryResponder);
                }

                if (barcodeResponder == null) {
                    barcodeResponder = new BarcodeCommand();
                    barcodeResponder.setCaptureNonLibraryResponses(true);
                    barcodeResponder.setUseEscapeCharacter(TriState.YES);
                    barcodeResponder.setBarcodeReceivedDelegate(new IBarcodeReceivedDelegate() {
                        @Override
                        public void barcodeReceived(String barCode) {
                            try {
                                JSONArray data = new JSONArray();
                                data.put(JSONUtil.createBarcodeJSONObject(barCode));

                                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, JSONUtil.createJSONObjectSuccessResponse(data));
                                pluginResult.setKeepCallback(true);
                                dataAvailableCallback.sendPluginResult(pluginResult);
                            } catch (JSONException ex) {
                                // Handle tag failure response
                            }
                        };
                    });
                    commander.addResponder(barcodeResponder);
                }
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "SUBSCRIBED TO SCANNER.");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } else if (commander.isConnected()) {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse("DEVICE IS ALREADY SUBSCRIBED."));
            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        }
        // printResponders(callbackContext, "After subscribeScanner");
    }

    @Override
    public void unsubscribeScanner(final CallbackContext callbackContext) {
        // printResponders(callbackContext, "Before unsubscribeScanner");
        try {
            if (commander.isConnected()) {
                removeAsyncResponders();
                inventoryResponder = null;
                barcodeResponder = null;
                dataAvailableCallback = null;
                callbackContext.success("RESPONDERS REMOVED.");
            } else {
                callbackContext.error(JSONUtil.createJSONObjectErrorResponse(DEVICE_IS_NOT_CONNECTED));
            }
        } catch (JSONException ex) {
            callbackContext.error(ERROR_LABEL + ex.getMessage());
        }
        // printResponders(callbackContext, "After unsubscribeScanner");
    }

    private InventoryCommand getInventoryInstance() {

        // This is the command that will be used to perform configuration changes and inventories
        if (mInventoryCommand == null) {
            mInventoryCommand = InventoryCommand.synchronousCommand();
        }
        // mInventoryCommand.setResetParameters(TriState.YES);
        // Configure the type of inventory
        mInventoryCommand.setIncludeTransponderRssi(TriState.YES);
        mInventoryCommand.setIncludeChecksum(TriState.YES);
        mInventoryCommand.setIncludePC(TriState.YES);
        mInventoryCommand.setIncludeDateTime(TriState.YES);

        return mInventoryCommand;
    }

    private void removeAsyncAndAddSyncResponder() {
        removeAsyncResponders();
        commander.addSynchronousResponder();
    }

    private void removeSyncAndAddAsyncResponder() {
        commander.removeSynchronousResponder();
        addAsyncResponders();
    }

    private void removeAsyncResponders() {
        if (dataAvailableCallback != null) {
            if (inventoryResponder != null) {
                commander.removeResponder(inventoryResponder);
            }
            if (barcodeResponder != null) {
                commander.removeResponder(barcodeResponder);
            }
        }
    }

    private void addAsyncResponders() {
        if (dataAvailableCallback != null) {
            if (inventoryResponder != null) {
                commander.addResponder(inventoryResponder);
            }
            if (barcodeResponder != null) {
                commander.addResponder(barcodeResponder);
            }
        }
    }

    private void printResponders(final CallbackContext callbackContext, final String message) {
        for (IAsciiCommandResponder responder : commander.getResponderChain()) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message + " ***RRRR*** " + responder.toString());
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    }

    //----------------------------------------------------------------------------------------------
    // Model notifications
    //----------------------------------------------------------------------------------------------

    private class GenericHandler extends WeakHandler<Activity>
    {
        public GenericHandler(Activity t)
        {
            super(t);
        }

        @Override
        public void handleMessage(Message msg, Activity t)
        {
            Log.d("CordovaLog", "handling message @GenericHandler");
            try {
                switch (msg.what) {
                    case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
                        //TODO: process change in model busy state
                        break;

                    case ModelBase.MESSAGE_NOTIFICATION:
                        // Examine the message for prefix
                        String message = (String)msg.obj;

                        if( message.startsWith("ER:")) {
                            Log.d("CordovaLog","message.obj.substring(3) : "+message.substring(3));
                        }
                        else if( message.startsWith("BC:")) {
                            Log.d("CordovaLog","message.obj: "+message);

                        } else {
                            Log.d("CordovaLog","message.obj: "+message);
                            if (isInventoryRunning){
                                //notify cordova after first scan
                                if (isTriggeredInventory && message.indexOf("TagCount")>=0){
                                    sendOkToCallbackAndKeep(dataAvailableCallback);
                                }else{
                                    processReadRFID(message);
                                }
                            }else if (mWriteCallbackContext != null){
                                if (message.contains("WriteTransponderCommand failed")){ //TODO replace it and use a Error code (see API)
                                    mWriteCallbackContext.error(message);
                                }
                                if (message.contains("Words Written: 6 of 6")){ //TODO replace it and use a OK code (see API)
                                    logMessage("writeTag without Exception :-)");
                                    mWriteCallbackContext.success("Ok");
                                }
                            }
                        }

                        break;

                    default:
                        Log.d("CordovaLog", "message: "+msg);
                        break;
                }
            } catch (Exception e) {
            }

        }
    };

    private void sendMessageAndKeepCallback(String message, CallbackContext _callbackContext){
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", message);
            PluginResult presult = new PluginResult(PluginResult.Status.OK, jsonObject);
            presult.setKeepCallback(true);
            if (_callbackContext != null){
                _callbackContext.sendPluginResult(presult);
            }
        }catch(Exception ex) {
            Log.i("CordovaLog", "Exception: ", ex);
            handleException(ex, "sendMessageAndKeepCallback", _callbackContext);
        }
    }

    private void onCreateForEDA51(){
        this.collector = new Collector(this.cordova.getActivity());
        this.collector.onCreate();
    }
}
