package com.tt.jattkaim.helprr;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Rating;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class Homepage extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener , com.google.android.gms.location.LocationListener{
    // varriables for map, and map marker
    private GoogleMap mMap;
    private Marker marker;
    private Marker markerTwo;
    private Boolean follow=true;

    ArrayList<Marker> markerArray=new ArrayList<Marker>();


    Firebase photoFirebase;
    Firebase postRef;
    double lng=151.2093;
    double lat=-33.8688;

    // setting up variables  for getting user location
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    LocationManager locationManager;
    // camera variable for changing zoom
    CameraUpdate camera;
    int zoom=0;
    Button followButton;

    static ArrayList<Coordinates> coList = new ArrayList<Coordinates>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        // iniatlize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initGoogleAPI();

        Firebase.setAndroidContext(this);
        photoFirebase = new Firebase("https://tuterr.firebaseio.com/");
        postRef = photoFirebase.child("Coordinates");
        // initialize location requests in oncreate, determines how often the gps tries to locate your position
        createLocationRequest();

        checkGPSSettings();




    }



    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                    Toast.makeText(Homepage.this,"this will start new activity",Toast.LENGTH_SHORT).show();
                    //TODO start new activity, and send title of clicked item, then on next activity check against the list of coordinates

                Intent faultIntent = new Intent(Homepage.this,FaultActivity.class);
                faultIntent.putExtra("id",marker.getTitle());
                startActivity(faultIntent);
            }
        });

        mMap.setInfoWindowAdapter(new MyCustomWindow());
        // set default marker at the moment, that loads when the application first loads.
        LatLng sydney = new LatLng(lat, lng);
        marker=mMap.addMarker(new MarkerOptions().position(sydney));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,18));
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.head));
        updateLocations();






//TODO change markers depending on zoom level, figure this out
/*
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if(cameraPosition.zoom>18){

                        mMap.clear();
                    markPhotos(coList);
                }else{
                        mMap.clear();
                    markMarkers(coList);
                }
            }
        });
*/
    }

    // TODO gotta fix these make permissions and set this up properly
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {

            case 10:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //getLocation();
                    return;
                } else {
                    Toast.makeText(this, "Permission Denied by user, app will not function", Toast.LENGTH_SHORT);
                    return;
                }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        // connection made on resume, so there is always connection even if incoming call disturbs program
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            // disconnects and removes locations when paused to save power
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        if (connectionResult.hasResolution()) {

                // TODO Start an Activity that tries to resolve the error

        } else {
            //Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i("", "Location services connected.");
        // gets last known location from users phone
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // if first time application is running request a location
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        }
        else {
            // if new location is request and recieved succesfully start this method
           handleNewLocation(location);

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }





    // this is an override for the LOCATIONLISTENER, every time location is changed, it runs method to udpate marker
    @Override
    public void onLocationChanged(Location location) {
      //  Toast.makeText(this,"does it even go here",Toast.LENGTH_SHORT).show();
        handleNewLocation(location);
    }

    // initialize location requests in oncreate, determines how often the gps tries to locate your position
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1 * 1000) ;       // 1 second
        mLocationRequest.setFastestInterval(1 * 10); // 10 milliseconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    protected void initGoogleAPI(){
        // initialize google api client in onCreate
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void checkGPSSettings(){

        //request the settings that are current only the users phone location wise
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        // check if they match what you require
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                // TODO have to make pop ups to enable HIGH ACCURACY if it is not enabled, this checks whether it is available or not
            }
        });
    }



    // method handles a new location, sets variables and updates the marker on the google map API

    // TODO maybe allow marker to move around, but not keep in middle of screen, this way people can keep scrolling around to search
    private void handleNewLocation(Location location){
        lat=location.getLatitude();
        lng=location.getLongitude();

        LatLng yourLoc = new LatLng(lat, lng);
        marker.setPosition(yourLoc);
        marker.setTitle("Your Location");
        //marker=mMap.addMarker(new MarkerOptions().position(yourLoc).title("Your Location"));
        //set camera view to your location
        camera = CameraUpdateFactory.newLatLng(yourLoc);
        // animate camera to your location
        if(follow=true){
            mMap.animateCamera(camera);
        }


    }


    private void updateLocations() {

        photoFirebase.child("Coordinates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // TODO brah do this properly, figure out storage, instead of using string conversions
                Toast.makeText(getApplicationContext(), "no of : " + dataSnapshot.getChildrenCount(), Toast.LENGTH_SHORT).show();
                //calls a background thread to download all things off firebase including image
                new LoadFromFirebase().execute(dataSnapshot);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

/*
        postRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                new LoadFromFirebase().execute(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });*/
    }

/*unused method at the moment*/
    private void markPhotos(List<Coordinates> list){
        int i;
        markerArray.clear();
        for(i=0;i<list.size();i++){
            //showing nearby places from coordinateslist
            LatLng nextDoor=new LatLng(coList.get(i).getLat(),coList.get(i).getLng()) ;
            markerTwo=mMap.addMarker(new MarkerOptions().position(nextDoor).title(coList.get(i).getDetails().getBuilding()));
            //Toast.makeText(getApplicationContext(), "loaded"+coList.get(i).getName(), Toast.LENGTH_SHORT).show();

            //TODO code to add images after placing markers, maybe have a try catch so doesnt crash application if it all fucks up?
            byte [] encodeByte= Base64.decode(list.get(i).getPhoto(),Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0,encodeByte.length);
            Bitmap fnlB=makeCanvas(bitmap);
            markerTwo.setIcon(BitmapDescriptorFactory.fromBitmap(fnlB));
            markerTwo.setAnchor(0.5f, 1);

            markerArray.add(markerTwo);
        }

    }

    private void markMarkers(List<Coordinates> list){
        int i;

        for(i=0;i<list.size();i++){
            //showing nearby places from coordinateslist

            LatLng nextDoor=new LatLng(coList.get(i).getLat(),coList.get(i).getLng()) ;
            markerTwo=mMap.addMarker(new MarkerOptions().position(nextDoor).title(coList.get(i).getId()).snippet(coList.get(i).getDetails().getDesc()).icon(BitmapDescriptorFactory.fromResource(R.drawable.blu)));
            //Toast.makeText(getApplicationContext(), "loaded"+coList.get(i).getName(), Toast.LENGTH_SHORT).show();
          //  markerTwo.showInfoWindow();
            markerArray.add(markerTwo);


        }

    }




    private class LoadFromFirebase extends AsyncTask<DataSnapshot, Void, List<Coordinates>> {
        private ProgressDialog pDialog;
        Coordinates coordinates;
        @Override
        //before executing the task eg show loading, tell user loading
        protected void onPreExecute() {
            super.onPreExecute();
            //TODO show loading bar here till coList is not null, meaning it has loaded information into it
            pDialog = new ProgressDialog(Homepage.this);
            pDialog.setMessage("Hang tight...");
            pDialog.setCancelable(true);
            pDialog.setMax(100);
//            pDialog.show();
        }
        @Override
        protected List<Coordinates> doInBackground(DataSnapshot... dataSnapshots) {
            //gets datasnapshot from updatelocations method
            DataSnapshot snapshot=dataSnapshots[0];
            for (DataSnapshot postShot : snapshot.getChildren()) {
                // get values from the online datasnapshots
                coordinates = postShot.getValue(Coordinates.class);


                if (!coList.contains(coordinates)) {
                    // each time a coordiantes is loaded, add to the coordinates master List
                    coList.add(coordinates);

                }

            }
            return coList;
        }

        @Override
        protected void onPostExecute(List<Coordinates> coordinates) {
            super.onPostExecute(coordinates);
            //once coList has been updated, add the markers to the map
            pDialog.dismiss();
            //if(coordinates!=null)

                markMarkers(coordinates);
        }
    }




    // creating nice looking canvas to view on map with image.
    public Bitmap makeCanvas(Bitmap bmp){
        //make mutable bitmap to be able to edit on canvas
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap mutableBitmap = Bitmap.createBitmap(400, 400, conf);
        Canvas canvas1 = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        Bitmap editBitmap=Bitmap.createScaledBitmap(bmp,260,260,false);
        editBitmap=rotateBitmap(editBitmap);
        Rect rectangle = new Rect(0,0,300,300);
        Paint shadow = new Paint();
        shadow.setColor(Color.BLUE);
        shadow.setShadowLayer(10.0f, 4f, 0.0f, 0xFF000000);

        canvas1.drawRect(rectangle,shadow);
        canvas1.drawBitmap(editBitmap,20,20,null);
        //drawing a triangle for bottom bit of canvas
        Path triangle=new Path();
        Point a=new Point(50,0); //starts at 0x and 0y
        Point b=new Point(0, 0); // starts at 0x and moves down by 100
        Point c=new Point(25, 50); // moves across by 87 pixels, moves back up by 50

        triangle.setFillType(Path.FillType.EVEN_ODD);
        triangle.lineTo(b.x, b.y);
        triangle.lineTo(c.x, c.y);
        triangle.lineTo(a.x, a.y);
        triangle.close();
        triangle.offset(150,300);
        canvas1.drawPath(triangle,paint);



        //canvas1.drawBitmap(null, 0.0f, 0.0f, shadow);



        // modify canvas
      //  canvas1.drawBitmap(mutableBitmap, 0,0, color);
      //  canvas1.drawText("User Name!", 30, 40, color);

        return mutableBitmap;
    }
    public Bitmap makeWindowImage(Bitmap bmp){
        //make mutable bitmap to be able to edit on canvas
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap mutableBitmap = Bitmap.createBitmap(400, 400, conf);
        Canvas canvas1 = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        Bitmap editBitmap=Bitmap.createScaledBitmap(bmp,390,390,false);
        editBitmap=rotateBitmap(editBitmap);
        canvas1.drawBitmap(editBitmap,5,5,null);



        return mutableBitmap;
    }


    public static Bitmap rotateBitmap(Bitmap bmp)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
    }




    class MyCustomWindow implements GoogleMap.InfoWindowAdapter {
        private final View mymarkerview;

        MyCustomWindow() {
            mymarkerview = getLayoutInflater()
                    .inflate(R.layout.custominfowindow, null);
        }

        public View getInfoWindow(Marker marker) {
            render(marker, mymarkerview);
            return mymarkerview;
        }

        public View getInfoContents(Marker marker) {
            return null;
        }

        private void render(Marker marker, View view) {

            TextView desc= (TextView) view.findViewById(R.id.infowindowDesc);
            ImageView image= (ImageView) view.findViewById(R.id.infowindowImage);
            TextView title = (TextView) view.findViewById(R.id.infowindowTitle);
            RatingBar severity =(RatingBar) view.findViewById(R.id.infowindowRating);
            for(int i=0;i<coList.size();i++){
                if(marker.getSnippet().equals(coList.get(i).getDetails().getDesc())){
                    byte [] encodeByte= Base64.decode(coList.get(i).getPhoto(),Base64.DEFAULT);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0,encodeByte.length);
                    Bitmap fnlB=makeWindowImage(bitmap);
                    image.setImageBitmap(fnlB);
                    title.setText("Building: "+coList.get(i).getDetails().getBuilding());
                    severity.setRating(coList.get(i).getDetails().getSev());
                }
            }
            desc.setText(marker.getSnippet());
            // Add the code to set the required values
            // for each element in your custominfowindow layout file
        }


    }
}




