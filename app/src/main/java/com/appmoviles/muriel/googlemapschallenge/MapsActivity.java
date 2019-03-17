package com.appmoviles.muriel.googlemapschallenge;


import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DialogoMarker.ComunicacionDialogo {

    private static final int REQUEST_CODE = 11;

    private GoogleMap mMap;

    private LatLng ubicacionNueva;

    private LocationManager manager;

    private List<MarkerOptions> markerList;

    private Marker myMarker;

    private static final double RATIO_EARTH = 6371;

    private TextView tv_lugar_cercano;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        markerList = new ArrayList<MarkerOptions>();

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_CODE);

        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if (myMarker != null) {

                    myMarker.remove();

                }

                MarkerOptions markerOptions = new MarkerOptions();

                markerOptions.title("Usted se encuentra aquí");

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                markerOptions.position(latLng);

                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("current_position_2", 130, 130)));

                myMarker = mMap.addMarker(markerOptions);

                Toast.makeText(MapsActivity.this, "Updated...", Toast.LENGTH_LONG).show();

                setNearSite();
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
        });

        LatLng Yopal = new LatLng(5.309766, -72.425281);
        MarkerOptions marcadorInicial = new MarkerOptions();
        marcadorInicial.title("Yopal");
        marcadorInicial.position(Yopal);
        mMap.addMarker(marcadorInicial);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Yopal));

        //Se añade a la lista de marcadores
        markerList.add(marcadorInicial);


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                showDialog();

                ubicacionNueva = latLng;

            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                //Si hago click en mi mismo marcador
                if (marker.getTitle().equals(myMarker.getTitle())) {
                    myMarker.setSnippet("You are here:  " + getDirection());

                } else {
                    double distancia = distanceBeetSites(myMarker.getPosition().latitude, myMarker.getPosition().longitude, marker.getPosition().latitude, marker.getPosition().longitude);
                    marker.setSnippet("Its a " + distancia + " km from you");

                }


                return false;
            }
        });


    }


    public void setNearSite() {

        double lati1 = myMarker.getPosition().latitude;
        double longi1 = myMarker.getPosition().longitude;

        MarkerOptions nearMarker = new MarkerOptions();
        double mostNearDistance = Double.MAX_VALUE;

        for (int i = 0; i < markerList.size(); i++) {


            double lati = markerList.get(i).getPosition().latitude;
            double longi = markerList.get(i).getPosition().longitude;

            double r = distanceBeetSites(lati1, longi1, lati, longi);

            if (r < mostNearDistance) {
                mostNearDistance = r;
                nearMarker = markerList.get(i);
            }

        }

        tv_lugar_cercano = findViewById(R.id.tv_lugar_cercano);

        if (mostNearDistance <= 0.100) {
            DecimalFormat formateador = new DecimalFormat("####.##");
            String dis = formateador.format(mostNearDistance);

            tv_lugar_cercano.setText("You are in: " + nearMarker.getTitle() + ", with a distance of " + dis + " km");
        } else {

            tv_lugar_cercano.setText("Most near site about you is: " + nearMarker.getTitle() + " with a distance of " + mostNearDistance + " km");
            tv_lugar_cercano.setHeight(105);

        }


    }

    private String getDirection() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String direction = "";
        List<Address> addresses = null;
        try {
            // My position
            LatLng position = myMarker.getPosition();
            addresses = geocoder.getFromLocation(position.latitude, position.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Address address = addresses.get(0);
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            direction += address.getAddressLine(i) + "\n";
        }
        return direction;
    }


    public double distanceBeetSites(double lat1, double lng1, double lat2, double lng2) {
        double Lat = Math.toRadians(lat2 - lat1);
        double Lng = Math.toRadians(lng2 - lng1);
        double sinLat = Math.sin(Lat / 2);
        double sinLng = Math.sin(Lng / 2);
        double value1 = Math.pow(sinLat, 2) + Math.pow(sinLng, 2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double value2 = 2 * Math.atan2(Math.sqrt(value1), Math.sqrt(1 - value1));
        double distance = RATIO_EARTH * value2;

        return Math.round(distance * 1000d) / 1000d;
    }

    public Bitmap resizeMapIcons(String iconName, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }


    public void showDialog() {

        DialogFragment newFragment = new DialogoMarker();
        newFragment.show(getSupportFragmentManager(), "missiles");

    }

    @Override
    public void createMarker(String nombre) {

        MarkerOptions newMarker = new MarkerOptions();
        newMarker.title(nombre);
        double d = distanceBeetSites(myMarker.getPosition().latitude, myMarker.getPosition().longitude, ubicacionNueva.latitude, ubicacionNueva.longitude);
        newMarker.snippet("Is a distance from" + d + " km the site");
        newMarker.position(ubicacionNueva);

        mMap.addMarker(newMarker);

        markerList.add(newMarker);

        setNearSite();

    }
}
