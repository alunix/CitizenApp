package edu.elfak.mosis.phoneguardian;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import java.lang.Math;

public class PGMapActivity extends FragmentActivity implements OnMarkerClickListener
{
    CheckableMenuItem fireMenuItem;
    CheckableMenuItem emergencyMenuItem;
    CheckableMenuItem policeMenuItem;
	public GoogleMap mapa;
    private SeekBar seekBar;
    TextView tvSeekBarProgress;
	MarkerOptions markerOptions;
	Marker markers[];
	int finishedTask = 0;
	Spinner s;

    double d=1; // radius around our location where we want to find events, in km
	double lat;
    double lng;

	int refresh= 1;

	JSONParser jParser = new JSONParser();
	
    Tags t = new Tags();

    JSONArray markers_response = null;

    private float previousZoomLevel = -1.0f;

	
	@Override
	protected void onCreate(Bundle savedInstanceBundle) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceBundle);
		setContentView(R.layout.pgmap_activity);
		

	    mapa = ((SupportMapFragment)(getSupportFragmentManager().findFragmentById(R.id.mapf))).getMap();
		mapa.setOnMarkerClickListener(this);
			
		finishedTask=0;
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tvSeekBarProgress = (TextView) findViewById(R.id.tv_seekbar_progress);

        GetCurrentLocation();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 1;

            @Override

            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                tvSeekBarProgress.setText(progresValue + "km");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                d = seekBar.getProgress();
                new GetMarkersByCategory().execute();
                /*if(d>10)
                getMapa().animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );*/
            }
        });


	}

		@Override
		public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
			
			Intent i = new Intent(PGMapActivity.this,MarkerActivity.class);
			Marker m = new Marker();
			
			String[] niz = new String[7];
			String snip = marker.getSnippet();
			niz = snip.split("&");
			
			m.id = niz[6];
			m.setType_of_event(marker.getTitle());
			m.setUser_phone(niz[0]);
			m.setAddress(niz[1]);
			m.setEvent_time(niz[2]);
            m.setAnonymous(Integer.parseInt(niz[3]));
            m.setLocation_acc(Float.parseFloat(niz[4]));
			m.setDescription(niz[5]);
			m.setLat(marker.getPosition().latitude);
			m.setLng(marker.getPosition().longitude);

			i.putExtra("marker", m);
			startActivity(i);
			return true;// vraca true da bi se ukinulo defaultno ponasanje markera... da se ne bi prikazao info prozor markera
		}
	
	@Override 
	protected void onRestart()
	{
		super.onRestart();
		if(refresh==1)
		{
			new GetMarkersByCategory().execute();
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pgmap_menu, menu);

        fireMenuItem = new CheckableMenuItem(menu.findItem(R.id.fire_events_toggle), menu.findItem(R.id.fire_events_toggle).isChecked(),
                R.drawable.flame_blue, R.drawable.flame_gray);
        emergencyMenuItem = new CheckableMenuItem(menu.findItem(R.id.emergency_events_toggle), menu.findItem(R.id.emergency_events_toggle).isChecked(),
                R.drawable.ambulance_blue, R.drawable.ambulance_gray);
        policeMenuItem = new CheckableMenuItem(menu.findItem(R.id.police_events_toggle), menu.findItem(R.id.police_events_toggle).isChecked(),
                R.drawable.police_badge_blue, R.drawable.police_badge_gray);

        new GetMarkersByCategory().execute();

        return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch(item.getItemId())
		{
			case R.id.refresh_events:
				refresh = 1;
                fireMenuItem.setChecked(true);
                policeMenuItem.setChecked(true);
                emergencyMenuItem.setChecked(true);
                onRestart();
				break;
			case R.id.filter_events:
				Intent i = new Intent(PGMapActivity.this,FilterActivity.class);
				startActivityForResult(i, 1);
				break;
            case R.id.fire_events_toggle:
                refresh = 1;
                fireMenuItem.toggle();
                onRestart();
                break;
            case R.id.emergency_events_toggle:
                refresh = 1;
                emergencyMenuItem.toggle();
                onRestart();
            break;
            case R.id.police_events_toggle:
                refresh = 1;
                policeMenuItem.toggle();
                onRestart();
            break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==RESULT_OK)
		{
			refresh=0;
			DataWrapper dw = (DataWrapper) data.getSerializableExtra("markers");
	        markers = dw.getMarkers();
            DrawMarkers();

		}
	}
	
	
	
	
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();

	}



	private void DrawMarkers() {

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapf); 
		setMapa(mapFragment.getMap());
		if (getMapa()== null)
	    {
	            Toast.makeText(PGMapActivity.this,"Google Maps not Available",
	                    Toast.LENGTH_LONG).show();
	    }
		
		mapa.clear();
		if(markers!=null)
		{
			for(int i=0;i<markers.length;i++)
			{
				String title = markers[i].type_of_event;
				String snippet = markers[i].user_phone+"&"
						+markers[i].address+"&"
						+markers[i].event_time+"&"
                        +markers[i].anonymous+"&"
                        +markers[i].location_acc+"&"
						+markers[i].description+"&"+markers[i].id;

				if(markers[i].type_of_event.equals("F"))
				{
					com.google.android.gms.maps.model.Marker m = mapa
			        .addMarker(new MarkerOptions()
			                .position(new LatLng(markers[i].lat,markers[i].lng))
			                .title(title)
			                .snippet(snippet)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.flame_pin)));
					m.hideInfoWindow();
				}
				else
					if(markers[i].type_of_event.equals("E"))
					{
						com.google.android.gms.maps.model.Marker m = mapa
				        .addMarker(new MarkerOptions()
				                .position(new LatLng(markers[i].lat,markers[i].lng))
				                .title(title)
				                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.emergency_pin)));
						m.hideInfoWindow();
					}
					else
					{
						com.google.android.gms.maps.model.Marker m = mapa
				        .addMarker(new MarkerOptions()
				                .position(new LatLng(markers[i].lat,markers[i].lng))
				                .title(title)
				                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.police_pin)));
						m.hideInfoWindow();
					}
				

			}
		}

		 
	    
	}

	public GoogleMap getMapa() {
		return mapa;
	}

	public void setMapa(GoogleMap mapa) {
		this.mapa = mapa;
	}

    private double  toRad(double val) {
        /** Converts numeric degrees to radians */
        return val * Math.PI / 180;
    }

    private double  toDeg(double val) {
        /** Converts numeric degrees to radians */
        return val * 180 / Math.PI;
    }

	class GetMarkersByCategory extends AsyncTask<Void, Void, Integer>
	{

		@Override
		protected Integer doInBackground(Void... paramss) {
			// TODO Auto-generated method stub

            String URL = "http://nemanjastolic.co.nf/guardian/get_all_events.php";
			refresh = 0;

            double R = 6371; //in km
            double r= d/R; //d has to be in km

            double lat_rad = toRad(lat);
            double lng_rad = toRad(lng);

            double lat_min_rad = lat_rad - r;
            double lat_max_rad = lat_rad + r;

            double delta_lot = Math.asin(Math.sin(r)/Math.cos(r));
            double lng_min_rad = lng_rad - delta_lot;
            double lng_max_rad = lng_rad + delta_lot;

            double lat_min = toDeg(lat_min_rad);
            double lat_max = toDeg(lat_max_rad);
            double lng_min = toDeg(lng_min_rad);
            double lng_max = toDeg(lng_max_rad);

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			
			params.add(new BasicNameValuePair("category_fire", fireMenuItem.isChecked()? "1" : "0"));
	        params.add(new BasicNameValuePair("category_police", policeMenuItem.isChecked()? "1" : "0"));
	        params.add(new BasicNameValuePair("category_emergency", emergencyMenuItem.isChecked()? "1" : "0"));


            params.add(new BasicNameValuePair("lng_min",Double.toString(lng_min)));
            params.add(new BasicNameValuePair("lng_max",Double.toString(lng_max)));
            params.add(new BasicNameValuePair("lat_min",Double.toString(lat_min)));
            params.add(new BasicNameValuePair("lat_max",Double.toString(lat_max)));
			
        	JSONObject json = jParser.makeHttpRequest(URL, "GET", params);
 
            try {
                // Checking for SUCCESS TAG
	                int success = json.getInt(t.TAG_SUCCESS);
	 
	                if (success == 1)
	                {
	                    // products found
	                    // Getting Array of Products
	                	markers_response = json.getJSONArray(t.TAG_EVENTS);
	                	if(markers_response==null)
	                		Toast.makeText(PGMapActivity.this, "No markers found!", Toast.LENGTH_LONG).show();
	                	else
	                	{
	                		markers = null;
	                		markers = new Marker[markers_response.length()];

                            for (int i = 0; i < markers_response.length(); i++)
                            {
                                JSONObject c = markers_response.getJSONObject(i);

                                markers[i] = new Marker();
                                markers[i].id = c.getString(t.TAG_EVENT_ID);
                                markers[i].setAddress(c.getString(t.TAG_ADDRESS));
                                markers[i].setUser_phone(c.getString(t.TAG_USER_PHONE));
                                markers[i].setType_of_event( c.getString(t.TAG_TYPE_OF_EVENT));
                                markers[i].setDescription(c.getString(t.TAG_DESC));
                                markers[i].setEvent_time(c.getString(t.TAG_EVENT_TIME));
                                markers[i].setLng(c.getDouble(t.TAG_LNG));
                                markers[i].setLat(c.getDouble(t.TAG_LAT));
                                markers[i].setAnonymous(c.getInt(t.TAG_ANONYMOUS));
                                markers[i].setLocation_acc(c.getLong(t.TAG_LOCATION_ACC));


                            }
	                	}
	                }
	                else
	                {
	                	markers = null;
	                }
	               
            	}
            	catch (JSONException e)
            	{
            		
            	}
            
            return 0;
		}
		@Override
		protected void onPostExecute(Integer result)
		{

            DrawMarkers();

        }
	}



	private void GetCurrentLocation()
	{

	    double[] a = getLocation();
	    lat = a[0];
	    lng = a[1];

        LatLng ll = new LatLng(lat,lng);

	    mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
	    mapa.setMyLocationEnabled(true);


	}

	public double[] getLocation()
	{
	    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    List<String> providers = lm.getProviders(true);

	    Location l = null;
	    for (int i = 0; i < providers.size(); i++) {
	        l = lm.getLastKnownLocation(providers.get(i));
	        if (l != null)
	            break;
	    }
	    double[] gps = new double[2];

	    if (l != null) {
	        gps[0] = l.getLatitude();
	        gps[1] = l.getLongitude();
	    }
	    return gps;
	}

	





	

}
