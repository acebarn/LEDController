package de.acebarn.ledcontroller;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.ArrayList;

public class DeviceFinder {
    public static final String TAG = DeviceFinder.class.getSimpleName();
    private static DeviceFinder instance = null;
    NsdManager mNsdManager;
    public static final String REQUEST_TAG = "DeviceRequest";

    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    public String mServiceName = "NsdChat";
    private Activity activity;
    NsdServiceInfo mService;
    public ArrayList<Device> devices = new ArrayList<>();

    public static final String SERVICE_TYPE = "_http._tcp.";


    public static DeviceFinder getInstance(MainActivity mainActivity) {
        if (instance == null) {
            instance = new DeviceFinder(mainActivity);
        }
        return instance;
    }

    public static DeviceFinder getInstance() {
        return instance;
    }

    public DeviceFinder(MainActivity activity) {
        this.activity = activity;
        mNsdManager = (NsdManager) activity.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                Log.d(TAG, "onServiceResolved: insert device into array");
                Device tmpDevice = new Device(serviceInfo);
                setStatusOfDevice(tmpDevice);
                devices.add(tmpDevice);
                notifyDataSetChanged();
            }

        };
    }

    public void setStatusOfDevice(Device device) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, "http://" + device.getService().getHost().getHostAddress() + "/status", null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        device.setStatus(response);
                        notifyDataSetChanged();
                    }


                }, error -> {

                });
        jsonRequest.setTag(REQUEST_TAG);
        NetworkRequest.getInstance(activity).addToRequestQueue(jsonRequest);
    }

    void notifyDataSetChanged() {
        activity.runOnUiThread(() -> ((MainActivity) activity).refreshConnectionStatus());
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("ESP")) {
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void discoverServices() {
        devices.clear();
        notifyDataSetChanged();

        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    public void tearDown() {
        try {
            mNsdManager.unregisterService(mRegistrationListener);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

}
