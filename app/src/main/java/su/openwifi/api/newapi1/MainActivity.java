package su.openwifi.api.newapi1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.StreamUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    StringBuilder sb = new StringBuilder();
    HttpRequest h;
    Handler ha;
    public Double lat;
    public Double lon;
    IMapController mapController;
    /*
    class BssindsLocationRecord()
    {
        Double lat;
        Double lon;
        String path;
        Integer count_results;
    }
    */
    class  WifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            sb = new StringBuilder();
            wifiList = mainWifi.getScanResults();
            for(int i = 0; i < wifiList.size(); i++){
                String BSSID = (wifiList.get(i)).BSSID.replace(":","");
                sb.append( BSSID );
                if (i != wifiList.size() -1) { sb.append(","); }
            }
            Log.i("",sb.toString());
            //sb = new StringBuilder("F07D689A6922");
            Toast.makeText(getApplicationContext(), sb, Toast.LENGTH_LONG).show();
            MainActivity.this.getLocationFromNet(sb.toString());
        }
    }
    private void wifiScan() {
        mainWifi.startScan();
        Toast.makeText(getApplicationContext(), "Start scanning",
                Toast.LENGTH_LONG).show();
    }
    private void getLocationFromNet(final String s)
    {
        Thread th  = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpRequest h2  = null;
                String body = "";
                try {
                    String url_s = "http://openwifi.su/api/v1/bssids/"+s;
                    Log.i(" ",url_s);
                    h2 = HttpRequest.get(url_s);
                    Log.i("", ((Integer) h2.code()).toString());
                    body = h2.body("UTF-8");
                    Log.i("",body);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                try {
                    JSONObject jobj = new JSONObject(body);
                    Integer cr = jobj.getInt("count_results");
                    if (cr > 0) {
                        MainActivity.this.setLocation(jobj.getDouble("lat"), jobj.getDouble("lon"));
                        ha.sendEmptyMessage(0);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },"getfromnet");
        th.start();
    }
    public void setLocation(Double lat, Double lon) {
        MainActivity.this.lat = lat;
        MainActivity.this.lon = lon;
        return;
    }

    private GeoPoint getLocation() {
        return new GeoPoint(MainActivity.this.lat, MainActivity.this.lon);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(9);
        GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
        mapController.setCenter(startPoint);
        //-----------------------------------
        Button bt = (Button) findViewById(R.id.button);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiScan();
            }
        });
        //-------------------------------------
        Button bt2 = (Button) findViewById(R.id.button2);
        bt2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i("","Bt2 CLICK");
                MainActivity.this.getLocationFromNet("F07D689A6922");
            }
        });
        //-------------------------------------
        ha = new Handler() {
            public void handleMessage(android.os.Message msg) {
                mapController.setCenter(MainActivity.this.getLocation());
                mapController.setZoom(16);
            }
        };
        //-------------------------------------
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mainWifi.isWifiEnabled() == false)
        {
            // If wifi disabled then enable it
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",
                    Toast.LENGTH_LONG).show();

            mainWifi.setWifiEnabled(true);
        }
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiScan();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }
}
