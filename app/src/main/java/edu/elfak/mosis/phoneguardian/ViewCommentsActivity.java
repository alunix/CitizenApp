package edu.elfak.mosis.phoneguardian;


import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ViewCommentsActivity extends ListActivity {

	final String TAG_SUCCESS = "success";
	final String TAG_EVENT_ID = "id_event";
	final String TAG_ID = "id";
	final String TAG_USERNAME = "username";
	final String TAG_COMMENT_DATE = "comment_date";
	final String TAG_COMMENT_TEXT = "comment_text";
	final String TAG_COMMENTS = "comments";

	final JSONParser jParser = new JSONParser();
	String URL = "http://nemanjastolic.co.nf/guardian/get_all_comments_for_event.php";

	Context ctx;
	Comment comments[];
	Marker m;

	JSONArray comments_response = null;
	
	TextView t;
	String id_event = "";
	
	int success;
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewcomment_activity);
		ctx = getApplicationContext();

		m = (Marker) getIntent().getSerializableExtra("marker");
		id_event = m.id;
		
		t= (TextView) findViewById(R.id.tv_empty);
	    t.setVisibility(View.GONE);
	   
	    new GetAllComments().execute();

	}

	class GetAllComments extends AsyncTask<String, String, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }
 

        protected String doInBackground(String... argss) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();

	        params.add(new BasicNameValuePair("id_event", id_event));

            JSONObject json = jParser.makeHttpRequest(URL, "GET", params);

            try {
                // Checking for SUCCESS TAG
	                success = json.getInt(TAG_SUCCESS);
	 
	                if (success == 1)
	                {

	                	comments_response = json.getJSONArray(TAG_COMMENTS);
	                	if(comments_response==null)
	                		Toast.makeText(ViewCommentsActivity.this, "No comments found!", Toast.LENGTH_LONG).show();
	                	else
	                	{
	                		comments = new Comment[comments_response.length()];

                            for (int i = 0; i < comments_response.length(); i++)
                            {
                                JSONObject c = comments_response.getJSONObject(i);

                                comments[i] = new Comment();
                                comments[i].id = c.getString(TAG_ID);
                                comments[i].id_event = c.getString(TAG_EVENT_ID);
                                comments[i].username = c.getString(TAG_USERNAME);
                                comments[i].comment_date = c.getString(TAG_COMMENT_DATE);
                                comments[i].comment_text = c.getString(TAG_COMMENT_TEXT);

                            }
	                	}
	                }

            	}
            	catch (JSONException e)
            	{

            	}
 
 
            return null;
        }
 
        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
        	
     	if( success == 0 )
                t.setVisibility(View.VISIBLE);
        	else
                setListAdapter(new CommentArrayAdapter(ctx, R.layout.view_comments_list_item, comments));
        	
        }
 
    }

}
