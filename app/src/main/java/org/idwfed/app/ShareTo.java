package org.idwfed.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ShareTo extends Activity {

    public static final String PREFS_NAME = "IDWF";

    private static final int SELECT_PHOTO = 100;
    private static final int CAPTURE_PHOTO = 101;

    private SharedPreferences settings;

    EditText txtView;
    EditText titleTxt;
    EditText bodyTxt;
    ImageView imageView;
    Spinner countrySpin;

    List<String> countries = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_to);
        settings = getSharedPreferences(PREFS_NAME, 0);
        txtView = (EditText)findViewById(R.id.txt);
        titleTxt = (EditText)findViewById(R.id.titleTxt);
        bodyTxt = (EditText)findViewById(R.id.bodyTxt);
        imageView = (ImageView)findViewById(R.id.imageView);

        countries.add("Malaysia");
        countries.add("Indonesia");

        countrySpin = (Spinner) findViewById(R.id.country);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item,countries);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        countrySpin.setAdapter(adapter);

        Intent receivedIntent = getIntent();
        String receivedAction = receivedIntent.getAction();
        String receivedType = receivedIntent.getType();
        if(receivedAction!=null) {
            if (receivedAction.equals(Intent.ACTION_SEND)) {
                if (receivedType.startsWith("text/")) {
                    String receivedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
                    if (receivedText != null) {
                        txtView.setText(receivedText);
                    }
                    String receivedSubject = receivedIntent.getStringExtra(Intent.EXTRA_SUBJECT);
                    if (receivedSubject != null) {
                        titleTxt.setText(receivedSubject);
                    }

                    Bundle bundle = receivedIntent.getExtras();
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
                        Log.d("IDWF - intent", String.format("%s %s (%s)", key,
                                value.toString(), value.getClass().getName()));
                    }

                }
            } else if (receivedAction.equals(Intent.ACTION_MAIN)) {
                txtView.setText("Nothing has been shared");
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_send_now:
                doShare();
                return true;
            case R.id.action_pick_gallery:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                return true;
            case R.id.action_camera:
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, CAPTURE_PHOTO);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        imageView.setImageBitmap(decodeUri(selectedImage));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return;
            case CAPTURE_PHOTO:
                if(resultCode == RESULT_OK){
                    Bundle extras = imageReturnedIntent.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(imageBitmap);
                }
                return;
        }
    }

    public void doShare() {
        // Create a new HttpClient and Post Header
        class SendPostReqAsyncTask extends AsyncTask<String, Void, Boolean> {

            protected void onPostExecute(Boolean status){
                String message = "";
                if(status){
                    message = "Item successfully shared";
                }
                else{
                    message = "Failed to share item";
                }
                Toast toast;
                toast = Toast.makeText(getApplicationContext(),
                        message, Toast.LENGTH_SHORT);
                toast.show();
                if(status) {
                    finish();
                }
            }

            protected Boolean doInBackground(String... params) {

                HttpClient httpclient = new DefaultHttpClient();
                Boolean success = false;


                String urlqueryid = settings.getString("serverurl", "") + "/@@API/plone/api/1.0/folders?q=idwfshared";
                Log.d("IDWF - urlqueryid",urlqueryid);
                HttpGet queryidpost = new HttpGet(urlqueryid);
                String uid = "";
                try {
                    HttpResponse idresponse = httpclient.execute(queryidpost);
                    String idresponsetext = null;
                    try {
                        idresponsetext = EntityUtils.toString(idresponse.getEntity());
                        Log.d("IDWF - idresponse",idresponsetext);
                        JSONObject idjson = new JSONObject(idresponsetext);
                        JSONArray juid = idjson.getJSONArray("items");
                        uid = juid.getJSONObject(0).getString("uid");
                    }catch (ParseException e) {
                        e.printStackTrace();
                        Log.i("Parse Exception", e + "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


                String serverurl = settings.getString("serverurl", "") + "/@@API/plone/api/1.0/documents/create/" + uid;
                //String serverurl = settings.getString("serverurl", "");
                Log.d("IDWF", "Login:" + serverurl);
                HttpPost httppost = new HttpPost(serverurl);


                String credentials = settings.getString("username", "") + ":" + settings.getString("password", "");
                String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                httppost.addHeader("Authorization", "Basic " + base64EncodedCredentials);
                try {

                    JSONObject jsonobj = new JSONObject();

                    jsonobj.put("title", titleTxt.getText());
                    jsonobj.put("description", txtView.getText());
                    jsonobj.put("effective","2001-05-11");
                    jsonobj.put("expires","2015-12-11");
                    jsonobj.put("subjects","[]");
                    jsonobj.put("document_type","Letter");
                    jsonobj.put("document_owner","admin");
                    jsonobj.put("text",bodyTxt.getText());

                    StringEntity se = new StringEntity(jsonobj.toString());
                    se.setContentType("application/json;charset=UTF-8");
                    se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));

                    httppost.setEntity(se);

                    // Execute HTTP Post Request
                    HttpResponse response = httpclient.execute(httppost);

                    Log.d("IDWF - response",response.toString());

                    String responseText = null;
                    try {
                        responseText = EntityUtils.toString(response.getEntity());
                        JSONObject respjson = new JSONObject(responseText);
                        Integer count = respjson.getInt("count");
                        if(count>0){
                            success=true;
                        }
                        Log.d("IDWF - succcess",success.toString());

                    }catch (ParseException e) {
                        e.printStackTrace();
                        Log.i("Parse Exception", e + "");
                    }

                    Log.d("IDWF - response Text",responseText);


                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return success;
            }
        }
        SendPostReqAsyncTask postreq = new SendPostReqAsyncTask();
        postreq.execute();
    }
}
