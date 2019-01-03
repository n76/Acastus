package org.fitchfamily.android.spawnofacastus;

// See: https://developers.google.com/maps/documentation/android-sdk/map-with-marker
// See: https://androidclarified.com/android-example-display-current-location-on-google-map-with-fusedlocationproviderapi/

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The type View map activity.
 */
public class ViewMapActivity extends AppCompatActivity
        implements OnMarkerClickListener, OnMapReadyCallback {

    private static final String TAG = "Acastus";
    private static final int DEFAULT_ZOOM = 15;

    /**
     * The Prefs.
     */
    protected SharedPreferences prefs;

    /**
     * The current location
     */
    LatLng curLoc;

    /**
     * The Lookup list.
     */
    public JSONArray lookupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_view_map);

        Intent intent = getIntent();
        double curLat = intent.getExtras().getDouble("latitude");
        double curLon = intent.getExtras().getDouble("longitude");
        curLoc = new LatLng(curLat, curLon);

        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(curLoc));
        LatLngBounds bounds = null;
        LatLng center = curLoc;

        try {
            lookupList = new JSONArray(getIntent().getStringExtra("lookup_list"));

            for (int i = 0; i < lookupList.length(); i++){
                JSONObject jsonObject = lookupList.getJSONObject(i);
                LatLng mLatLon = new LatLng(jsonObject.getDouble("lat"), jsonObject.getDouble("lon"));
                if (bounds == null) {
                    bounds = new LatLngBounds(mLatLon, mLatLon);
                    center = mLatLon;
                } else
                    bounds = bounds.including(mLatLon);
                googleMap.addMarker(new MarkerOptions().position(mLatLon)
                                        .title(jsonObject.getString("name"))
                                        .snippet("")
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker)));
            }

            // Zoom to results
            if (lookupList.length() == 1) {
                Log.d(TAG, "onMapReady(): Zooming to only result");
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM));
            } else if (bounds != null) {
                Log.d(TAG,"onMapReady(): Setting camera bounds");
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            } else {
                Log.d(TAG, "onMapReady(): No results?");
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLoc, DEFAULT_ZOOM));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        googleMap.setOnMarkerClickListener(this);
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        LatLng markerPosition = marker.getPosition();
        String geoCoords = addressString(markerPosition.latitude, markerPosition.longitude, marker.getTitle());
        geoCoords = geoCoords.replace(' ', '+');
        openInNavApp(geoCoords);

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return true;
    }
    /**
     * Add points.
     */
    public void addPoints(GoogleMap googleMap){
        LatLng sydney = new LatLng(-33.852, 151.211);
        googleMap.addMarker(new MarkerOptions().position(sydney)
                .title("Marker in Sydney"));
        /*
        try {
            lookupList = new JSONArray(getIntent().getStringExtra("lookup_list"));


            System.out.println(lookupList.toString());

            ArrayList<Marker> markers = new ArrayList<>();

            for (int i = 0; i < lookupList.length(); i++){
                Marker marker = map.addMarker();
                markers.add(marker);
            }

            for (int i = 0; i < lookupList.length(); i++){

                Marker marker = markers.get(i);
                marker.setStylingFromString(pointStyle);
                LngLat lngLat = new LngLat();
                JSONObject jsonObject = lookupList.getJSONObject(i);
                lngLat.set(jsonObject.getDouble("lon"), jsonObject.getDouble("lat"));
                marker.setPoint(lngLat);
                marker.setDrawable(getResources().getDrawable(R.mipmap.ic_marker));

            }
            JSONObject jsonObject = new JSONObject();
            String name = getResources().getString(R.string.my_current_location) + ", " + curLat + ", " + curLon;
            jsonObject.put("name", name);
            jsonObject.put("lat", curLat);
            jsonObject.put("lon", curLon);
            lookupList.put(jsonObject);
            Marker marker = map.addMarker();
            LngLat lngLat = new LngLat();
            lngLat.set(curLon, curLat);
            marker.setPoint(lngLat);
            marker.setStylingFromString(pointStyle);
            marker.setDrawable(getResources().getDrawable(R.mipmap.ic_my_position));
            map.requestRender();
            map.setPosition(lngLat);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        map.setZoom(14);

        map.setTapResponder(new TouchInput.TapResponder() {
            @Override
            public boolean onSingleTapUp(float x, float y) {
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(float x, float y) {

                final LngLat tap = map.screenPositionToLngLat(new PointF(x, y));
                String minName = "";
                double min = 100000000;
                for (int i = 0; i < lookupList.length(); i++) {

                    try {
                        JSONObject jsonObject = lookupList.getJSONObject(i);
                        double distance = GeoLocation.distance(tap.latitude, jsonObject.getDouble("lat"), tap.longitude, jsonObject.getDouble("lon"), true);

                        if (distance < .1 && distance < min) {
                            TextView mLocationTitle = (TextView) findViewById(R.id.locationTitle);
                            mLocationTitle.setText(jsonObject.getString("name").substring(0, lookupList.getJSONObject(i).getString("name").indexOf(',')));
                            TextView mLocationDesc = (TextView) findViewById(R.id.locationDesc);
                            mLocationDesc.setText(jsonObject.getString("name").substring(lookupList.getJSONObject(i).getString("name").indexOf(',')+2, jsonObject.getString("name").length()));
                            min = distance;
                            minName = jsonObject.getString("name");
                            ImageButton imageButton = (ImageButton) findViewById(R.id.mapActions);
                            final String[] geoCoords = {addressString(tap.latitude, tap.longitude, minName)};
                            imageButton.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    geoCoords[0] = geoCoords[0].replace(' ', '+');
                                    openInNavApp(geoCoords[0]);

                                }
                            });

                            imageButton.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    CharSequence list_options[] = new CharSequence[]{getResources().getString(R.string.navigate), getResources().getString(R.string.share_this_location), getResources().getString(R.string.copy_address_place), getResources().getString(R.string.copy_gps)};
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ViewMapActivity.this);
                                    builder.setTitle("Choose option");

                                    builder.setItems(list_options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == 0) {
                                                geoCoords[0] = geoCoords[0].replace(' ', '+');
                                                openInNavApp(geoCoords[0]);
                                            }

                                            if (which == 1) {
                                                String shareBody = "home" + "\n" + geoCoords[0];
                                                sharePlace(shareBody);
                                            }

                                            if (which == 2) {
                                                String copyBody = "Home";
                                                copyToClipboard(copyBody);
                                            }

                                            if (which == 3) {
                                                String copyBody = gpsString(tap.latitude, tap.longitude);
                                                copyToClipboard(copyBody);
                                            }
                                            return;
                                        }
                                    });
                                    builder.show();

                                    return true;
                                }

                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                System.out.println(minName);
                return false;
            }
        });
*/
    }

    /**
     * Address string string.
     *
     * @param lat   the lat
     * @param lon   the lon
     * @param label the label
     * @return the string
     */
    protected String addressString(double lat, double lon, String label){
        String location;
        label = label.replace(" " , "+");
        label = label.replace("," , "+");
        label = label.replace("++" , "+");
        if (prefs.getBoolean("use_google", false) == true) {
            location = "http://maps.google.com/maps?q=+" + lat + "+" + lon;
            return location;
        }else {
            location = "geo:" + lat + "," + lon + "?q="+ lat + "+" + lon + "("+label+")";
            return location;
        }
    }

    /**
     * Gps string string.
     *
     * @param lat the lat
     * @param lon the lon
     * @return the string
     */
    protected String gpsString(double lat, double lon){
        String location;
        if (prefs.getBoolean("use_google", false) == true) {
            location = "http://maps.google.com/maps?q=" + lat + "+" + lon;
            return location;
        }else {
            location = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon;
            return location;
        }
    }

    /**
     * Open in nav app.
     *
     * @param geoCoords the geo coords
     */
    void openInNavApp(String geoCoords){
        try {
            Intent openInMaps = new Intent(Intent.ACTION_VIEW, Uri.parse(geoCoords));
            startActivity(openInMaps);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ViewMapActivity.this, getResources().getString(R.string.need_nav_app),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Share place.
     *
     * @param shareBody the share body
     */
    void sharePlace(String shareBody) {
        Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
        sharingLocation.setType("text/plain");
        sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.shared_location));
        sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingLocation, getResources().getString(R.string.share_this_location)));
    }

    /**
     * Copy to clipboard.
     *
     * @param copyBody the copy body
     */
    void copyToClipboard(String copyBody){
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(copyBody);
        Toast.makeText(ViewMapActivity.this, getResources().getString(R.string.copied_to_clipboard),
                Toast.LENGTH_LONG).show();
    }
}
