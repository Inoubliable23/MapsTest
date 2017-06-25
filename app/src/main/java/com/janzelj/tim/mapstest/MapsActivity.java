package com.janzelj.tim.mapstest;

import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;


//TODO(): implement time(from servers) for markers (now set value)
//TODO(): after certian time(10min) old markers shold be delited

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;

    LocationManager locationManager;
    String provider;
    double lat;
    double lng;


    ArrayList<ParkingMarker> markersList;//stores ParkingMarkers for updating the oppacity of markers

    Handler UI_HANDLER;//For thread events (setting a clocl trigerted fucntion for updating the oppacity of markers)


    // Declare a variable for the cluster manager.
    private ClusterManager<MyItem> mClusterManager;//test

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        markersList = new ArrayList<>(); //sotres ParkingMarkers for updating the oppacity of markers


        //Code to get user location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if(location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }
        } catch (SecurityException e) {}


        //To trenutno dobi json iz linka in v Toast.show() izpiše podatke ki jih je vrnu server
        new JsonTask().execute("https://peaceful-taiga-88033.herokuapp.com/users");


        UI_HANDLER = new Handler(); //to be handle thread events
        UI_HANDLER.postDelayed(UI_UPDTAE_RUNNABLE, 1000);//This is like a clock triger event that runs on a UI(main) thread



    }


    //This is like a clock triger event that runs on a UI(main) thread
    //This is needed to update the oppacitiy of the markers, but they cannot be accest from another thread
    //It gets called every second
    Runnable UI_UPDTAE_RUNNABLE = new Runnable() {

        @Override
        public void run() {

            for(ParkingMarker mark : markersList){

                mark.updateAlpha();

            }

            UI_HANDLER.postDelayed(UI_UPDTAE_RUNNABLE, 1000);
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        drawMyMarker();
    }

    private void drawMyMarker() {
        Toast.makeText(this, "Loading Free Parking Spaces", Toast.LENGTH_LONG).show();

        mMap.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(lat, lng),
                        new LatLng(lat + 0.001, lng + 0.001),
                        new LatLng(lat + 0.0015, lng),
                        new LatLng(lat + 0.001, lng - 0.001),
                        new LatLng(lat, lng)
                )
                .strokeWidth(2)
                .fillColor(Color.GREEN));

        mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(lat + 0.00045, lng))
                .add(new LatLng(lat + 0.0011, lng))
                .add(new LatLng(lat + 0.0008, lng - 0.0003))
                .width(9)
        );

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
    }

    @Override
    public void onLocationChanged(Location location) {

        mMap.clear();

        lat = location.getLatitude();
        lng = location.getLongitude();

        drawMyMarker();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        } catch (SecurityException e) {

        }
    }


    //Class k iz podanega linka dobi JSON file iz serverja in v String zapiše podatke ki jih vrne server
    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();


        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Gson gson = new Gson();//a external libery object for reading HTTP JSON responses

            //Zbriše vse truntne markerje
            mMap.clear();

            try {
                JSONArray tempArray = new JSONArray(result);//Paharsa string Json v Json array
                Log.d("JSON arraqy : ",tempArray.toString());

                for(int i=0; i<tempArray.length();i+=1){

                    Log.d(i+"",tempArray.getString(i));

                    String innerArray = tempArray.getString(i);//ker je Json sestavljen iz arrayey morm dobit usak array posevi kot string

                    Type type = new TypeToken<Map<String, String>>(){}.getType(); //DA lagko pol v Map podam keksn tip je ker item
                    Map<String, String> myMap = gson.fromJson(innerArray, type); //Key-Value map k lagk pol vn uzamem lat, lng, time

                    Log.d("Map"+i,myMap.toString());

                    double tempLat = Double.parseDouble(myMap.get("lat")); // najdem lat in za tem uzamem stevki in jih spremenim v double
                    double tempLng = Double.parseDouble(myMap.get("lng")); // najdem lng in za tem uzamem stevki in jih spremenim v double
                    String tempName = myMap.get("id");


                    MarkerOptions myMarkerOptions = new MarkerOptions().position(new LatLng(tempLat, tempLng))
                                                        .title(tempName)
                                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round))
                                                        .anchor(0.5f,0.5f);

                    //v Maps dodam nov marker in ga shranim v marker list
                    markersList.add( new ParkingMarker( tempName, tempLat, tempLng,100f,10f,mMap.addMarker(myMarkerOptions)));

                    //TODO(): camera should be moved to user location not last marker
                    //Kamero pomaknem na ta marker
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(tempLat, tempLng), 3));

                    setUpClusterer();

                }

            } catch (JSONException e) {e.printStackTrace();}
        }


    }



    private void setUpClusterer() {
        // Position the map.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(46.058291, 14.507687), 1));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(this, mMap);




        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }

    private void addItems() {

        // Set some lat/lng coordinates to start with.
        double lat = 46.058291;
        double lng = 14.507687;

        // Add ten cluster items in close proximity, for purposes of this example.
        for (int i = 0; i < 10; i++) {
            double offset = i / 9000d;
            lat = lat + offset;
            lng = lng + offset;
            MyItem offsetItem = new MyItem(lat, lng);
            mClusterManager.addItem(offsetItem);
        }

        for(ParkingMarker marker : markersList){
            MyItem offsetItem = new MyItem(marker.lat, marker.lng);
            mClusterManager.addItem(offsetItem);

        }
    }





}
