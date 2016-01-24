package com.example.daobadat.androidmap;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.daobadat.androidmap.models.MyLocation;
import com.example.daobadat.androidmap.models.PlaceJSONParser;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener,
                                                                GoogleApiClient.OnConnectionFailedListener,
                                                                    GoogleApiClient.ConnectionCallbacks {
    //Tiêu đề của log (không quan trọng)
    private final String LOG_TAG = "MapsActivity";

    // Khởi tạo map
    protected GoogleMap mMap;

    //Khởi tạo các biến liên quan tới tìm đường (route)
    protected LatLng mStartLatLng;
    protected LatLng mEndLatLng;

    //Khởi tạo các biến liên quan tìm địa điểm gần nhất
    Spinner mSprPlaceType;
    String[] mPlaceType=null;
    String[] mPlaceTypeName=null;
    double mLatitude=0;
    double mLongitude=0;

    //Khởi tạo các biến kết nối tới view
    @Bind(R.id.start)
    AutoCompleteTextView mEditTextStarting;

    @Bind(R.id.destination)
    AutoCompleteTextView mEditTextDestination;

    @Bind(R.id.btn_find_routes)
    ImageView send;

    @Bind(R.id.btn_show_locations)
    Button mButtonShowLocations;

    //Không biết giải thích
    protected GoogleApiClient mGoogleApiClient;

    //Khởi tạo biến liên quan tới việc tự động đưa ra các gợi ý về địa điễm mỗi khi nhập chữ vào
    private PlaceAutoCompleteAdapter mAdapter;

    //Khởi tạo biến show progressDialog (dùng khi nhấn tìm kiếm đường giữa 2 điểm)
    private ProgressDialog progressDialog;

    //Các đường vẽ (dùng để tô màu đường đi khi tìm đường giữa 2 điểm)
    private ArrayList<Polyline> mPolylines;

    //Màu sắc
    private int[] colors = new int[]{
            R.color.primary_dark,
            R.color.primary,
            R.color.primary_light,
            R.color.accent,
            R.color.primary_dark_material_light
    };

    //Phần bên lưu địa điểm
    private static final LatLngBounds BOUNDS_HO_CHI_MINH = new LatLngBounds(
            new LatLng(-57.965341647205726, 144.9987719580531),
            new LatLng(72.77492067739843, -9.998857788741589));
    private Marker mCurrentLocationMarker = null;
    private LatLng mCurrentLatLng = null;
    private LocationRequest mLocationRequest;
    private LocationListener mCurrentLocationListener;
    private boolean mRequestingLocationUpdate = false;
    private ArrayList<Marker> mMarkers = new ArrayList<>();
    private Realm mRealm;

    //Khi click vào marker sẽ show ra InfoWindow và gán vị trí vào 2 biến mLatitude,mLongitude để có thể sử dụng tìm các ... gần nhất
    private GoogleMap.OnMarkerClickListener mMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
                //Show InfoWindow
            marker.showInfoWindow();
            mLatitude = marker.getPosition().latitude;
            mLongitude = marker.getPosition().longitude;
            return false;
        }
    };

    //Hàm bắt đầu
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        //****************** HIỆN MAP ******************

        //không biết
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        //KHông biết
        mCurrentLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());


                if (mCurrentLocationMarker == null) {
                    if (mMap != null) {
                        // Tạo mới marker cho vị trí hiện tại.
                        mCurrentLocationMarker = mMap.addMarker(new MarkerOptions()
                                .title("Vị trí hiện tại")
                                .position(mCurrentLatLng));
                    }
                } else {
                    // Cập nhật vị trí market
                    mCurrentLocationMarker.setPosition(mCurrentLatLng);
                }
            }
        };

        MapsInitializer.initialize(this);

        //Liên kết map với map để show lên
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        mMap= mapFragment.getMap();

        //****************** HIỆN MAP ******************

        //****************** TÌM KIẾM CÁC NƠI GẦN NHẤT ******************

        mPlaceType = getResources().getStringArray(R.array.place_type);
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);

        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);

        Button btnFind;

        // Getting reference to Find Button
        btnFind = ( Button ) findViewById(R.id.btn_find);


        btnFind.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                String type = mPlaceType[selectedPosition];

                if(mLatitude == 0 && mLongitude == 0)
                {
                    Toast.makeText(MapsActivity.this, "Bạn hãy chọn địa điểm cố định", Toast.LENGTH_SHORT).show();
                    return;
                }
                StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                sb.append("location="+mLatitude+","+mLongitude);
                sb.append("&radius=5000");
                sb.append("&types="+type);
                sb.append("&sensor=true");
                sb.append("&key=AIzaSyBGwoqztspvIVmCNmXwv2eP2wFlwNUpupQ");

                // Creating a new non-ui thread task to download json data
                PlacesTask placesTask = new PlacesTask();

                // Invokes the "doInBackground()" method of the class PlaceTask
                placesTask.execute(sb.toString());

            }
        });

        //****************** TÌM KIẾM CÁC NƠI GẦN NHẤT ******************

        //****************** CLICK MARKER HIỆN INFOWINDOW AND LƯU ĐỊA ĐIỂM ******************

        //set adapter for infowindow
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {

                // Getting view from the layout file info_window_layout
                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                // Getting the position from the marker
                LatLng latLng = arg0.getPosition();

                // Getting reference to the TextView to set title
                TextView title = (TextView) v.findViewById(R.id.title);

                // Getting reference to the TextView to set latitude
                TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);

                // Getting reference to the TextView to set longitude
                TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);

                // Getting reference to the TextView to set longitude
                TextView addLocation = (TextView) v.findViewById(R.id.add_location);

                // Setting title
                title.setText(arg0.getTitle());

                // Setting the latitude
                tvLat.setText("Latitude:" + latLng.latitude);

                // Setting the longitude
                tvLng.setText("Longitude:" + latLng.longitude);

                // Returning the view containing InfoWindow contents

                return v;

            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String name = marker.getTitle();
                LatLng latLng = marker.getPosition();

                if (name == null) {
                    name = "?Không biết?";
                }

                Log.d(LOG_TAG, "Clicked Marker: " + name + ", [" + latLng.latitude + ", " + latLng.longitude + ']');


                mRealm.beginTransaction();

                MyLocation location = mRealm.createObject(MyLocation.class);
                location.setLocationId(generateMyLocationId());
                location.setName(name);
                location.setLat(latLng.latitude);
                location.setLng(latLng.longitude);

                mRealm.commitTransaction();

                Toast.makeText(MapsActivity.this, "Đã thêm địa điểm: " + name, Toast.LENGTH_SHORT).show();
            }
        });

        mRealm = Realm.getInstance(this);

        //****************** CLICK MARKER HIỆN INFOWINDOW AND LƯU ĐỊA ĐIỂM ******************

        //****************** TÌM ĐƯỜNG ******************

        mAdapter = new PlaceAutoCompleteAdapter(this, android.R.layout.simple_list_item_1,
                mGoogleApiClient, BOUNDS_HO_CHI_MINH, null);

        /*
         * Adds auto complete adapter to both auto complete
         * text views.
         */
        mEditTextStarting.setAdapter(mAdapter);
        mEditTextDestination.setAdapter(mAdapter);


        /*
        * Sets the start and destination points based on the values selected
        * from the autocomplete text views.
        * */

        mEditTextStarting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);

                Log.i(LOG_TAG, "Autocomplete item selected: " + item.description);

                /* Issue a request to the Places Geo Data API to retrieve a Place object with additional
                 * details about the place.
                 */
                PendingResult<PlaceBuffer> placeResult =
                        Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);

                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        mStartLatLng = place.getLatLng();
                    }
                });

            }
        });

        mEditTextDestination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(LOG_TAG, "Autocomplete item selected: " + item.description);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        mEndLatLng = place.getLatLng();
                    }
                });

            }
        });

        /*
        These text watchers set the mStartLatLng and mEndLatLng points to null because once there's
        * a change after a value has been selected from the dropdown
        * then the value has to reselected from dropdown to get
        * the correct location.
        * */
        mEditTextStarting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int startNum, int before, int count) {
                if (mStartLatLng != null) {
                    mStartLatLng = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mEditTextDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mEndLatLng != null) {
                    mEndLatLng = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //****************** TÌM ĐƯỜNG ******************
    }

    //****************** HÀM LIÊN QUAN TỚI TÌM CÁC NƠI... GẦN NHẤT ******************


    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }

    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String,String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String,String>> list){

            // Clears all the existing markers
            mMap.clear();
            onConnected(Bundle.EMPTY);
            mLatitude  = 0;
            mLongitude = 0;

            for(int i=0;i<list.size();i++){

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);

                // Getting latitude of the place
                double lat = Double.parseDouble(hmPlace.get("lat"));

                // Getting longitude of the place
                double lng = Double.parseDouble(hmPlace.get("lng"));

                // Getting name
                String name = hmPlace.get("place_name");

                // Getting vicinity
                String vicinity = hmPlace.get("vicinity");

                LatLng latLng = new LatLng(lat, lng);

                // Setting the position for the marker
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                // Setting the title for the marker.
                //This will be displayed on taping the marker
                markerOptions.title(name + " : " + vicinity);

                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);
            }
        }
    }

    //****************** HÀM LIÊN QUAN TỚI TÌM CÁC NƠI... GẦN NHẤT ******************

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.daobadat.androidmap/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.daobadat.androidmap/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
    }

    @OnClick(R.id.btn_find_routes)
    public void onFindRoutesClick() {
        if (Util.Operations.isOnline(this)) {
            findRoutes();
        } else {
            Toast.makeText(this, "No internet connectivity", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_show_locations)
    public void onShowLocationClick() {
        Intent intent = new Intent(this, MyLocationListActivity.class);
        startActivity(intent);
    }

    private boolean checkMyLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (checkMyLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        } else {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // return;
        }

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);

        /*
        * Updates the bounds being used by the auto complete adapter based on the position of the
        * mMap.
        * */
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                mAdapter.setBounds(bounds);
            }
        });

        moveView(new LatLng(18.013610, -77.498803));

        mMap.setOnMarkerClickListener(mMarkerClickListener);
    }

    //****************** HÀM LIÊN QUAN TỚI TÌM ĐƯỜNG ******************

    public void findRoutes() {
        if (mStartLatLng == null) {
            mStartLatLng = mCurrentLatLng;
        }

        if (mStartLatLng == null || mEndLatLng == null) {
            if (mStartLatLng == null) {
                if (mEditTextStarting.getText().length() > 0) {
                    Toast.makeText(this, "Chọn 1 trong những gợi ý được đưa ra", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this, "Nhập địa điểm cần tìm", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            if (mStartLatLng != null && mEndLatLng == null && mStartLatLng != mCurrentLatLng) {
                location(mStartLatLng);
            }
        } else {
            progressDialog = ProgressDialog.show(this, "Đang tìm đường",
                    "Xin vui lòng đợi", true);

            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(mStartLatLng, mEndLatLng)
                    .build();

            routing.execute();
        }
    }

    /*
     *  Di chuyển 'khung nhìn' đến địa điểm trên bản đồ.
     */
    private void moveView(LatLng point) {
        if (mMap != null) {
            CameraUpdate center = CameraUpdateFactory.newLatLng(point);
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
            mMap.moveCamera(center);
            mMap.animateCamera(zoom);
        }
    }


    private void location(LatLng start) {

        //xóa cái marker và poliline
        mMap.clear();
        mLatitude  = 0;
        mLongitude = 0;

        moveView(start);

        MarkerOptions options = new MarkerOptions();
        options.position(start);
        options.title(mEditTextStarting.getText().toString());

        mMap.addMarker(options);
    }

    @Override
    public void onRoutingFailure() {
        // The Routing request failed
        progressDialog.dismiss();
        Toast.makeText(this, "Không tìm thấy đường", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {
        // The Routing Request starts
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        progressDialog.dismiss();

        mMap.clear();
        mLatitude  = 0;
        mLongitude = 0;


        moveView(mStartLatLng);
        if (mPolylines != null && mPolylines.size() > 0) {
            for (Polyline poly : mPolylines) {
                poly.remove();
            }
            mPolylines = null;
        }

        mPolylines = new ArrayList<Polyline>();
        //add findRoutes(s) to the mMap.
        for (int i = 0; i < route.size(); i++) {
            //In case of more than 5 alternative routes
            int colorIndex = i % colors.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(colors[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());

            if (mMap != null) {
                Polyline polyline = mMap.addPolyline(polyOptions);
                mPolylines.add(polyline);
            } else {
                Log.d(LOG_TAG, "mMap is null");
            }

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }

        // Start marker
        String startName = mEditTextStarting.getText().toString();
        MarkerOptions options = new MarkerOptions();
        options.position(mStartLatLng);
        options.title(startName);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));

        mMap.addMarker(options);

        // End marker
        options = new MarkerOptions();
        String endName = mEditTextDestination.getText().toString();
        options.position(mEndLatLng);
        options.title(endName);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));

        mMap.addMarker(options);
    }

    @Override
    public void onRoutingCancelled() {
        Log.i(LOG_TAG, "Routing was cancelled.");
    }

    //****************** HÀM LIÊN QUAN TỚI TÌM ĐƯỜNG ******************

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(LOG_TAG, connectionResult.toString());
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (checkMyLocationPermission()) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (lastLocation != null) {
                mStartLatLng = mCurrentLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                if (mMap != null) {
                    mCurrentLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(mCurrentLatLng)
                            .title("Vị trí của bạn"));

                    moveView(mCurrentLatLng);
                }
            }
        }

        startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public int generateMyLocationId() {
        return mRealm.where(MyLocation.class).max("locationId").intValue() + 1;
    }

    private void startLocationUpdate() {
        if (checkMyLocationPermission()) {
            if (mLocationRequest == null) {
                mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(10000);
                mLocationRequest.setFastestInterval(5000);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, mCurrentLocationListener);

            mRequestingLocationUpdate = true;
        }
    }

    private void stopLocationUpdates() {
        if (mRequestingLocationUpdate) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, mCurrentLocationListener);

            mRequestingLocationUpdate = false;
        }
    }
}
