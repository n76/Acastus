package org.fitchfamily.android.spawnofacastus;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.Manifest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;


/**
 * Author: Daniel Barnett
 */

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity{
    private static final String TAG = "Acastus";
    /**
     * The Labels.
     */
    private ArrayList<String> labels = new ArrayList<>();
    /**
     * The Recents.
     */
    private ArrayList<String> recents = new ArrayList<>();
    /**
     * The constant lookupList.
     */
    public ArrayList<ResultNode> lookupList = new ArrayList<>();

    /**
     * The Cur lat.
     */
    public double curLat;
    /**
     * The Cur lon.
     */
    public double curLon;

    private Double[] geoCoordinates;
    /**
     * The Results.
     */
    private GetResults results = null;
    /**
     * The Map time.
     */
    private Boolean mapTime = false;
    /**
     * If there is a search query waiting
     */
    private boolean searching = false;
    private Intent intent;
    private String action;
    private String type;
    protected SharedPreferences prefs;
    /**
     * The Search text.
     */
    EditText searchText;
    /**
     * The Results list.
     */
    ListView resultsList;
    /**
     * The Toolbar.
     */
    Toolbar toolbar;

    /**
     * The Geocoder
     */
    protected Geocoder ourGeocoder;
    /**
     * The Make request.
     */
    protected MakeAPIRequest makeRequest;
    /**
     * The Geo location.
     */
    protected GeoLocation geoLocation;
    /**
     * The Use location.
     */
    public static Boolean useLocation;

    protected static Context context;

    LocationManager locationManager;

    ImageButton mViewMapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        useLocation = prefs.getBoolean("use_location", true);
        if (prefs.getBoolean("app_theme", false)){
            setTheme(R.style.DarkTheme_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        makeRequest = new MakeAPIRequest();
        context = getApplicationContext();
        ourGeocoder = new Geocoder(context);
        setupLocationUse();
        getInputs();
        setupMapButton();
        updateRecentsList();
        startTimer();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    useLocation = false;
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            searchText.setText("");
            return true;
        }
        if (id == R.id.action_share_location) {
            shareLocation();
            return true;
        }
        if (id == R.id.clear_recents) {
            clearRecents();
            return true;
        }
        if (id == R.id.donate) {
            Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://danielbarnett714.github.io/Acastus/"));
            startActivity(donateIntent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupMapButton(){
        mViewMapButton = (ImageButton) findViewById(R.id.imageButton);

        mViewMapButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                geoCoordinates = geoLocation.getLocation();
                if (geoCoordinates != null) {
                    curLat = geoCoordinates[0];
                    curLon = geoCoordinates[1];
                    Intent intent = new Intent(MainActivity.this, ViewMapActivity.class);

                    intent.putExtra("latitude", curLat);
                    intent.putExtra("longitude", curLon);


                    JSONArray jsonArray = new JSONArray();

                    try{
                        for (int i = 0; i < lookupList.size(); i++){
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("name", lookupList.get(i).name);
                            jsonObject.put("lat", lookupList.get(i).lat);
                            jsonObject.put("lon", lookupList.get(i).lon);
                            jsonArray.put(jsonObject);
                        }

                        intent.putExtra("lookup_list", jsonArray.toString());

                    }catch (JSONException e){

                    }
                    startActivity(intent);
                }


            }
        });
    }

    /**
     * Get inputs.
     */
    void setupLocationUse(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        geoLocation = new GeoLocation(locationManager, getApplicationContext());
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        if (useLocation && isLocationEnabled()) {
            geoCoordinates = geoLocation.getLocation();
            if (geoCoordinates != null) {
                curLat = geoCoordinates[0];
                curLon = geoCoordinates[1];
            }
        }
    }


    private void getInputs() {
        searchText = (EditText) findViewById(R.id.searchText);
        handleIntent();
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                startSearch();
            }
        });




    }

    /**
     * Clear recents.
     */
    private void clearRecents() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("recents");
        editor.apply();
        recents.clear();
        String[] data = recents.toArray(new String[recents.size()]);  // terms is a List<String>
        updateList(data);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Object o = resultsList.getItemAtPosition(position);
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                searchQuery.setText(o.toString());
                if (lookupList.isEmpty()) {
                    return;
                }
                ResultNode tempNode = lookupList.get(position);
                setRecents(tempNode.name);
                searchText.setText("");

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gpsString(tempNode.lat, tempNode.lon)));
                try {
                    startActivity(browserIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.server_url_default),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    /**
     * Share location.
     */
    protected void shareLocation() {
        Double[] coordinates = null;
        geoLocation.updateLocation();
        try {
            coordinates = geoLocation.getLocation();
        } catch (NullPointerException e) {
        }
        if (coordinates != null) {
            double lat = coordinates[0];
            double lon = coordinates[1];

            String uri = gpsString(lat, lon);
            String shareBody = getResources().getString(R.string.my_current_location) + ":\n" + uri;
            Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
            sharingLocation.setType("text/plain");
            sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.my_current_location));
            sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingLocation, getResources().getString(R.string.share_my_location)));
        }
    }

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
     * Handle intent.
     */
    private void handleIntent() {
        intent = getIntent();
        action = intent.getAction();
        type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {

            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            handleActionView(intent);
        }
    }

    /**
     * Reset time.
     */
    private void resetTime() {
        mapTime = true;
        EditText searchQuery = (EditText) findViewById(R.id.searchText);
        String urlString = searchQuery.getText().toString();
        results = null;
        results = new GetResults();
        results.execute(urlString);
    }

    /**
     * Start timer.
     */
    private void startTimer() {
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                mapTime = false;
                if (searching == true) {
                    resetTime();
                    searching = false;
                }
            }
        }, 0, 3334);
    }

    /**
     * Start search.
     */
    private void startSearch() {
        if (searchText.getText().toString().isEmpty()) {
            updateRecentsList();
            return;
        }
        if (mapTime == false) {
            resetTime();
        } else {
            searching = true;
        }
    }

    /**
     * Handle send text.
     *
     * @param intent the intent
     */
    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        sharedText = sharedText.replace("+", " ");
        sharedText.replace("," , " ");
        if (sharedText != null) {
            EditText searchQuery = (EditText) findViewById(R.id.searchText);
            searchQuery.setText(sharedText);
            startSearch();
        }
    }

    /**
     * Handle action view.
     *
     * @param intent the intent
     */
    void handleActionView(Intent intent) {
        try {
            URI uri = new URI(intent.getData().toString());
            String q = uri.getQuery();
            if (q != null) {
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                String addr = "";
                addr = addr.replace("&", " ");
                int indexLoc = q.indexOf("loc");
                if (indexLoc >= 3){
                    addr = q.substring(q.indexOf("=") + 1, indexLoc).replace("\n", " ");
                }else{
                    addr = q.substring(q.indexOf("=") + 1).replace("\n", " ");
                }
                addr = addr.replace("+", " ");
                searchQuery.setText(addr);
                startSearch();
            }
        } catch (URISyntaxException e) {
            // Probably ought to put something here
        }
    }

    void sharePlace(String shareBody) {
        Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
        sharingLocation.setType("text/plain");
        sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.shared_location));
        sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingLocation, getResources().getString(R.string.share_this_location)));
    }

    void copyToClipboard(String copyBody){
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(copyBody);
        Toast.makeText(MainActivity.this, getResources().getString(R.string.copied_to_clipboard),
                Toast.LENGTH_LONG).show();
    }

    void openInNavApp(String geoCoords){
        try {
            Intent openInMaps = new Intent(Intent.ACTION_VIEW, Uri.parse(geoCoords));
            startActivity(openInMaps);
            searchText.setText("");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.need_nav_app),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Set recents.
     *
     * @param name the name
     */
    public void setRecents(String name) {
        SharedPreferences.Editor editor = prefs.edit();
        if (prefs.getBoolean("store_recents", true) == false) {
            return;
        }
        if (recents.contains(name)) {
            recents.remove(name);
        }
        recents.add(0, name);
        JSONArray mJSONArray = new JSONArray(recents);
        editor.remove("recents");
        editor.apply();
        editor.putString("recents", mJSONArray.toString());
        editor.apply();
    }

    /**
     * Update list.
     *
     * @param data the data
     */
    private void updateList(String[] data) {
        ArrayAdapter<?> adapter = new ArrayAdapter<Object>(this, android.R.layout.simple_selectable_list_item, data);
        resultsList = (ListView) findViewById(R.id.resultsList);
        resultsList.setAdapter(adapter);
        resultsList.setClickable(true);
    }

    /**
     * Update results list.
     */
    private void updateResultsList() {
        if (searchText.getText().toString().isEmpty()) {
            updateRecentsList();
            return;
        }
        String[] data;
        data = labels.toArray(new String[labels.size()]);  // terms is a List<String>
        updateList(data);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (lookupList.isEmpty()) {
                    return;
                }
                ResultNode tempNode = lookupList.get(position);
                setRecents(tempNode.name);
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                searchQuery.setText(tempNode.name);
                String geoCoords = addressString(tempNode.lat, tempNode.lon, tempNode.name);
                geoCoords = geoCoords.replace(' ', '+');
                openInNavApp(geoCoords);
            }
        });

        resultsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long id) {
                // TODO Auto-generated method stub
                if (lookupList.isEmpty()) {
                    return true;
                }

                CharSequence list_options[] = new CharSequence[] {getResources().getString(R.string.navigate), getResources().getString(R.string.share_this_location), getResources().getString(R.string.copy_address_place), getResources().getString(R.string.copy_gps)};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Choose option");
                builder.setItems(list_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ResultNode tempNode = lookupList.get(position);
                        setRecents(tempNode.name);
                        if (which == 0){
                            String geoCoords = addressString(tempNode.lat, tempNode.lon, tempNode.name);
                            geoCoords = geoCoords.replace(' ', '+');
                            openInNavApp(geoCoords);
                        }

                        if (which == 1){
                            String shareBody = tempNode.name + "\n" + addressString(tempNode.lat, tempNode.lon, tempNode.name);
                            sharePlace(shareBody);
                        }

                        if (which == 2){
                            String copyBody = tempNode.name;
                            copyToClipboard(copyBody);
                        }

                        if (which == 3){
                            String copyBody = gpsString(tempNode.lat, tempNode.lon);
                            copyToClipboard(copyBody);
                        }
                    }
                });
                builder.show();

                return true;
            }
        });
    }

    /**
     * Update recents list.
     */
    private void updateRecentsList() {
        String recentsStore = prefs.getString("recents", null);
        JSONArray mJSONArray = null;
        resultsList = (ListView) findViewById(R.id.resultsList);
        if (recentsStore != null) {
            try {
                mJSONArray = new JSONArray(recentsStore);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            recents = null;
            recents = new ArrayList<>();
            if (mJSONArray != null) {
                for (int i = 0; i < mJSONArray.length(); i++) {
                    try {
                        recents.add(mJSONArray.get(i).toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            String[] data;
            data = recents.toArray(new String[recents.size()]);  // terms is a List<String>
            updateList(data);
            resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Object result = resultsList.getItemAtPosition(position);
                    EditText searchQuery = (EditText) findViewById(R.id.searchText);
                    searchQuery.setText(result.toString());
                }
            });

            resultsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long id) {
                    // TODO Auto-generated method stub

                    CharSequence list_options[] = new CharSequence[] {getResources().getString(R.string.search_address_place), getResources().getString(R.string.share_address_place), getResources().getString(R.string.copy_address_place)};

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Choose option");
                    builder.setItems(list_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String name = recents.get(position);
                            setRecents(name);
                            EditText searchQuery = (EditText) findViewById(R.id.searchText);

                            if (which == 0){
                                searchQuery.setText(name);
                            }

                            if (which == 1){
                                String shareBody = name;
                                sharePlace(shareBody);
                            }

                            if (which == 2){
                                String copyBody = name;
                                copyToClipboard(copyBody);
                            }

                        }
                    });
                    builder.show();

                    return true;
                }
            });
        } else {
            resultsList = (ListView) findViewById(R.id.resultsList);
            resultsList.clearChoices();
        }
    }

    /**
     * Fetch search results.
     *
     * @param searchQuery the search query
     * @throws IOException   the io exception
     */
    private void fetchSearchResults(String searchQuery) throws IOException {
        if (ourGeocoder.isPresent()) {
            List<Address> addresses = ourGeocoder.getFromLocationName(searchQuery, 20);

            lookupList.clear();
            labels.clear();

            for (Address addr : addresses) {
                if (addr.hasLatitude() && addr.hasLatitude()) {
                    ResultNode tempNode = new ResultNode();
                    tempNode.lat = addr.getLatitude();
                    tempNode.lon = addr.getLongitude();
                    tempNode.name = addr.getFeatureName();
                    tempNode.distance = 0.0;
                    if (tempNode.name == null) {
                        tempNode.name = addr.getAddressLine(0);
                        for (Integer i=1; i<addr.getMaxAddressLineIndex(); i++)
                            tempNode.name += ", " + addr.getAddressLine(i);
                    }
                    if (useLocation) {
                        Boolean kilometers = prefs.getBoolean("unit_length", false);
                        Double distance = geoLocation.distance(curLat, tempNode.lat, curLon, tempNode.lon, kilometers);
                        tempNode.distance = distance;
                    }
                    lookupList.add(tempNode);
                }
            }
            Collections.sort(lookupList);

            for (ResultNode node : lookupList) {
                if (useLocation) {
                    Boolean kilometers = prefs.getBoolean("unit_length", false);
                    if (kilometers) {
                        labels.add(node.name + " : " + node.distance + " km");
                    } else {
                        labels.add(node.name + " : " + node.distance + " mi");
                    }
                } else {
                    labels.add(node.name);
                }
            }
        } else {
            Log.d(TAG,"fetchSearchResults(): No geocoder present?");
        }
    }

    private class GetResults extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateResultsList();
        }
        @Override
        protected String doInBackground(String... strings) {
            String searchQuery = strings[0];
            if (searchQuery != null){
                try {
                    fetchSearchResults(searchQuery);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }
}
