package de.acebarn.ledcontroller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private Paint colorPicker;
    private DeviceFinder mDeviceFinder;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String REQUEST_TAG = "DeviceRequest";

    private boolean isLedOn;
    private Device mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDeviceFinder = DeviceFinder.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                mDeviceFinder.initializeNsd();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.INTERNET)) {
                    Toast.makeText(MainActivity.this, "Internet permission is required to scan for devices", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Internet permission is required to scan for devices", Toast.LENGTH_LONG).show();
                }

                requestPermissions(new String[]{Manifest.permission.INTERNET}, Constants.REQUEST_INTERNET);
            }
        } else {
            mDeviceFinder.initializeNsd();
        }

        initializeUIComponentFunctions();

        ArrayList<Device> devices = mDeviceFinder.devices;
        if (devices.size()>0) {
            mDevice = devices.get(0);
            TextView txtStatus = (TextView) findViewById(R.id.txtConnected);
            txtStatus.setText("Connected");
        }
    }

    private void initializeUIComponentFunctions() {
        Switch switchOnOff = (Switch) findViewById(R.id.switchLedOn);
        switchOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mDevice!=null) {
                    Log.i("Cool", "LED toggled " + !isLedOn);
                    toggleLED();
                }
                else
                {
                    Snackbar.make(findViewById(android.R.id.content),"No device to control found...",Snackbar.LENGTH_LONG).show();
                }
            }
        });

        Button btnRefreshDevices = (Button) findViewById(R.id.btnRefreshDevices);
        btnRefreshDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Device> devices = mDeviceFinder.devices;
                if (devices.size()>0) {
                    mDevice = devices.get(0);
                    TextView txtStatus = (TextView) findViewById(R.id.txtConnected);
                    txtStatus.setText("Connected");
                }
                else
                {
                    TextView txtStatus = (TextView) findViewById(R.id.txtConnected);
                    txtStatus.setText("Not Connected");
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        mDeviceFinder.tearDown();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (mDeviceFinder != null) {
            mDeviceFinder.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDeviceFinder != null) {
            mDeviceFinder.discoverServices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQUEST_INTERNET:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mDeviceFinder.initializeNsd();
                } else {

                    Toast.makeText(MainActivity.this, "Internet permission has not been granted", Toast.LENGTH_SHORT).show();
                }
                break;

        }


    }




    public void refreshConnectionStatus() {
        Log.i("Damn", "Connection status changed");
    }


    public void toggleLED() {

        JSONObject jsonObject = new JSONObject();
        try {
            if (isLedOn) {
                jsonObject.put("state", 0);
            }
            else
            {
                jsonObject.put("state",1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("Problem","Unable to create LED Status JSON Object");
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.POST, "http://" + mDevice.getService().getHost().getHostAddress() + "/led", jsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "LEDs toggled");
                        if (isLedOn)
                        {
                            isLedOn = false;
                        }
                        else{
                            isLedOn = true;
                        }
                    }
                }, error -> {
                    Snackbar.make(findViewById(android.R.id.content), "Error: " + (error.networkResponse != null ? error.networkResponse.statusCode + " " + error.getMessage() : error.getMessage()), Snackbar.LENGTH_LONG).show();
                });
        jsonRequest.setTag(REQUEST_TAG);
        NetworkRequest.getInstance(this).addToRequestQueue(jsonRequest);
    }

}
