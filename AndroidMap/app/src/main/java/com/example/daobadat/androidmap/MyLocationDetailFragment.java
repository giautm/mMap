package com.example.daobadat.androidmap;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.daobadat.androidmap.models.MyLocation;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * A fragment representing a single MyLocation detail screen.
 * This fragment is either contained in a {@link MyLocationListActivity}
 * in two-pane mode (on tablets) or a {@link MyLocationDetailActivity}
 * on handsets.
 */
public class MyLocationDetailFragment extends Fragment implements OnMapReadyCallback {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private MyLocation mMyLocation;

    protected GoogleMap mMap;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MyLocationDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            FragmentActivity activity = this.getActivity();

            Realm realm = Realm.getInstance(activity);

            String locationId = getArguments().getString(ARG_ITEM_ID);
            RealmResults<MyLocation> result = realm.where(MyLocation.class)
                    .equalTo("locationId", Integer.parseInt(locationId))
                    .findAll();

            if (result.size() > 0) {
                mMyLocation = result.get(0);

                Toolbar toolbar = (Toolbar) activity.findViewById(R.id.detail_toolbar);
                if (toolbar != null) {
                    toolbar.setTitle(mMyLocation.getName());
                }
            }

            SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance();
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();

                mapFragment.getMapAsync(this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.mylocation_detail, container, false);

//        // Show the dummy content as text in a TextView.
//        if (mMyLocation != null) {
//
//            ((TextView) rootView.findViewById(R.id.mylocation_detail)).setText(mMyLocation.details);
//        }
//
        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        if (this.mMyLocation != null) {
            LatLng latLng = new LatLng(mMyLocation.getLat(), mMyLocation.getLng());
            moveView(latLng);

            MarkerOptions markerOptions = new MarkerOptions()
                    .title(mMyLocation.getName())
                    .position(latLng);

            mMap.addMarker(markerOptions);
        }
    }

    private void moveView(LatLng point) {
        if (mMap != null) {
            CameraUpdate center = CameraUpdateFactory.newLatLng(point);
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
            mMap.moveCamera(center);
            mMap.animateCamera(zoom);
        }
    }

}
