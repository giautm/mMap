package com.example.daobadat.androidmap;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.daobadat.androidmap.models.MyLocation;
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

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, RoutingListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {
    private final String LOG_TAG = "MapsActivity";

    protected GoogleMap mMap;
    protected LatLng mStartLatLng;
    protected LatLng mEndLatLng;

    @Bind(R.id.start)
    AutoCompleteTextView mEditTextStarting;

    @Bind(R.id.destination)
    AutoCompleteTextView mEditTextDestination;

    @Bind(R.id.btn_find_routes)
    ImageView send;

    @Bind(R.id.btn_show_locations)
    Button mButtonShowLocations;

    protected GoogleApiClient mGoogleApiClient;
    private PlaceAutoCompleteAdapter mAdapter;
    private ProgressDialog progressDialog;
    private ArrayList<Polyline> mPolylines;

    private int[] colors = new int[]{
            R.color.primary_dark,
            R.color.primary,
            R.color.primary_light,
            R.color.accent,
            R.color.primary_dark_material_light
    };

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

    private GoogleMap.OnMarkerClickListener mMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
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

            return false;
        }
    };

    /**
     * This activity loads a mMap and then displays the findRoute and pushpins on it.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        mRealm = Realm.getInstance(this);

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
    }

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

    public void findRoutes() {
        if (mStartLatLng == null) {
            mStartLatLng = mCurrentLatLng;
        }

        if (mStartLatLng == null || mEndLatLng == null) {
            if (mStartLatLng == null) {
                if (mEditTextStarting.getText().length() > 0) {
                    mEditTextStarting.setError("Chọn vị trí từ danh sách sổ.");
                } else {
                    Toast.makeText(this, "Please choose a starting point.", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            if (mStartLatLng != null && mEndLatLng == null && mStartLatLng != mCurrentLatLng) {
                location(mStartLatLng);
            }
        } else {
            progressDialog = ProgressDialog.show(this, "Please wait.",
                    "Fetching route information.", true);

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
        moveView(start);

        MarkerOptions options = new MarkerOptions();
        options.position(start);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));

        mMarkers.add(mMap.addMarker(options));
    }

    @Override
    public void onRoutingFailure() {
        // The Routing request failed
        progressDialog.dismiss();
        Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {
        // The Routing Request starts
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        progressDialog.dismiss();

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
