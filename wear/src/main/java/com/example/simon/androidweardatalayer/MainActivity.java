package com.example.simon.androidweardatalayer;

import android.app.Activity;
import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

//Wearable Layout
public class MainActivity extends Activity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient googleClient;

    private LinearLayout mainContainer;
    private TextView apiMessage;
    private TextView apiDate;
    private TextView apiHeight;
    private RelativeLayout overlay;
    private Button apiButton;
    ListView lstTest;
    //Array Adapter that will hold our ArrayList and display the items on the ListView
    JSONAdapter jSONAdapter ;

    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(googleClient, this);
    }

    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        googleClient.connect();
        refresh();
    }

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    public void refresh() {
        //bring the loading overlay to the front
        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();

        //start API request to phone
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/apiurl");
        putDataMapRequest.getDataMap().putString("message", "This is a message from Android Wear, please connect to the API");
        putDataMapRequest.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        putDataRequest.setUrgent();

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleClient, putDataRequest);
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //set up our google play services client
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //find all of our UI element
        apiMessage = (TextView) findViewById(R.id.apiMessage);
        apiDate = (TextView) findViewById(R.id.apiDate);
        mainContainer = (LinearLayout) findViewById(R.id.mainContainer);
        overlay =  (RelativeLayout) findViewById(R.id.overlay);
        apiButton = (Button) findViewById(R.id.apiButton);
        refresh();
        //click action for button, connect to mobile
        apiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            refresh();
/*                //bring the loading overlay to the front
                overlay.setVisibility(View.VISIBLE);
                overlay.bringToFront();

                //start API request to phone
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/apiurl");
                putDataMapRequest.getDataMap().putString("message", "This is a message from Android Wear, please connect to the API");
                putDataMapRequest.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                putDataRequest.setUrgent();

                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleClient, putDataRequest);*/
            }
        });

        //click listenr for the overlay, touch to dismiss it in case the API fails or takes too long
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlay.setVisibility(View.INVISIBLE);
                mainContainer.bringToFront();
            }
        });
    }


    //function triggered every time there's a data change event
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);

                //RESPONSE back from mobile message
                if(item.getUri().getPath().equals("/responsemessage")){

                    //received a response back, turn overlay off and bring main content back to front
                    overlay.setVisibility(View.INVISIBLE);
                    mainContainer.bringToFront();

                    //collect all of our info
                    String error = dataMapItem.getDataMap().getString("error");
                    String unixTime = dataMapItem.getDataMap().getString("unixTime");
                    String height = dataMapItem.getDataMap().getString("height");
                    JSONArray jArray = new JSONArray();
                    try {
                        jArray = new JSONArray(dataMapItem.getDataMap().getString("jsonArray"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //success
                    if(error == null){
                        apiMessage.setText("Gare : "+height);
                        apiDate.setText("Time: " + unixTime);
                        displayList(jArray);
                    }
                    //error
                    else {
                        apiMessage.setText(error);
                    }

                }
            }
        }
    }

    public void displayList(JSONArray jArray) {
        //setContentView(R.layout.activity_main);
        //Initialize ListView
        lstTest= (ListView)findViewById(R.id.list);

        jSONAdapter = new JSONAdapter (MainActivity.this,jArray);//jArray is your json array

        //Set the above adapter as the adapter of choice for our list
        lstTest.setAdapter(jSONAdapter );
    }

    class JSONAdapter extends BaseAdapter implements ListAdapter {

        private final Activity activity;
        private final JSONArray jsonArray;
        private JSONAdapter (Activity activity, JSONArray jsonArray) {
            assert activity != null;
            assert jsonArray != null;

            this.jsonArray = jsonArray;
            this.activity = activity;
        }


        @Override public int getCount() {
            if(null==jsonArray)
                return 0;
            else
                return jsonArray.length();
        }

        @Override public JSONObject getItem(int position) {
            if(null==jsonArray) return null;
            else
                return jsonArray.optJSONObject(position);
        }

        @Override public long getItemId(int position) {
            JSONObject jsonObject = getItem(position);

            return jsonObject.optLong("id");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = activity.getLayoutInflater().inflate(R.layout.items, null);

            TextView direction =(TextView)convertView.findViewById(R.id.direction);
            TextView horaire =(TextView)convertView.findViewById(R.id.horaire);

            JSONObject json_data = getItem(position);
            if(null!=json_data ){
                String jj=json_data.optString("term");
                direction.setText(jj);
                horaire.setText(json_data.optJSONObject("date").optString("val").substring(11)+" ("+json_data.optString("miss")+")");
            }

            return convertView;
        }
    }

}
