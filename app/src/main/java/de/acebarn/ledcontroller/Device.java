package de.acebarn.ledcontroller;

import android.net.nsd.NsdServiceInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class Device extends Observable implements Parcelable {
    public static final String TAG = Device.class.getSimpleName();
    public static final int BOOLEAN_TYPE = 1;
    public static final int STRING_TYPE = 2;
    public static final int INT_TYPE = 3;
    public static final int DOUBLE_TYPE = 4;
    public static final int FLOAT_TYPE = 5;
    private JSONObject config = new JSONObject();
    private JSONObject status = new JSONObject();
    private NsdServiceInfo service;
    private int port = 80;

    public List<String> getConfigKeys() {

        List<String> configKeys = new ArrayList<>();
        if (hasConfig()) {
            Iterator<?> keys = config.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if(getConfigType(key)!=0){
                    configKeys.add(key);
                }
            }
        }
        Log.d(TAG, "getConfigKeys() returned "+ configKeys.size()+"  keys");
        return configKeys;
    }

    public int getConfigType(String key){
        try {

            if (config.get(key) instanceof Boolean) {
                return BOOLEAN_TYPE;
            } else if (config.get(key) instanceof Integer) {
                return INT_TYPE;
            } else if (config.get(key) instanceof Float) {
                return FLOAT_TYPE;
            } else if (config.get(key) instanceof Double) {
                return DOUBLE_TYPE;
            } else if (config.get(key) instanceof String) {
                return STRING_TYPE;
            }else{
                Log.d(TAG, "getConfigType: "+key + " : "+config.get(key) +" cannot be displayed");

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }


    protected Device(Parcel in) {
        try {
            config = new JSONObject(in.readString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            status = new JSONObject(in.readString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        service = in.readParcelable(NsdServiceInfo.class.getClassLoader());
        port = in.readInt();
    }

    public void notifyConfigChange() {
        setChanged();
        notifyObservers();
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
        Log.d(TAG, "addObserver() called with: " + "o = [" + o + "]");
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public boolean hasStatus() {
        return status != null;
    }

    public boolean hasConfig() {
        return config != null;
    }


    public Device(JSONObject config, JSONObject status, NsdServiceInfo service, int port) {
        this.config = config;
        this.status = status;
        this.service = service;
        this.port = port;
    }

    public String getStatusString(String key) {
        String value = "";
        try {
            value = status.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    public int getStatusInt(String key) {
        int value;
        try {
            value = status.getInt(key);
            return value;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public NsdServiceInfo getService() {
        return service;
    }


    public Device(NsdServiceInfo service) {
        this.service = service;
    }


    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(JSONObject config) {
        this.config = config;
        Log.d(TAG, "setConfig() called with: " + "config = [" + config + "]");
        setChanged();
        notifyObservers();
    }

    public JSONObject getStatus() {
        return status;
    }

    public void setStatus(JSONObject status) {
        this.status = status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(config.toString());
        parcel.writeString(status.toString());
        parcel.writeParcelable(service, 0);
        parcel.writeInt(port);
    }

}
