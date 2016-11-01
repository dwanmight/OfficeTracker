package com.junior.dwan.gavrilovni_app.ui.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.junior.dwan.gavrilovni_app.R;
import com.junior.dwan.gavrilovni_app.utils.ConstantManager;
import com.junior.dwan.gavrilovni_app.utils.GPSTracker;
import com.junior.dwan.gavrilovni_app.utils.PaintActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.junior.dwan.gavrilovni_app.ui.activities.AuthActivity.convertStreamToString;

/**
 * Created by Might on 27.09.2016.
 */

public class MenuActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnGeo, btnSelfie, btnSite1, btnSite2;
    GPSTracker gps;
    private Timer mTimer;
    private MyTimerTask mMyTimerTask;

    private String encoded_string, image_name;
    private Bitmap bitmap;
    private File file;
    private Uri file_uri;
    Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_menu);
        btnGeo = (Button) findViewById(R.id.btn_geo);
        btnSelfie = (Button) findViewById(R.id.btn_selfie);
        btnSite1 = (Button) findViewById(R.id.btn_site1);
        btnSite2 = (Button) findViewById(R.id.btn_site2);
        btnGeo.setOnClickListener(this);
        btnSelfie.setOnClickListener(this);
        btnSite1.setOnClickListener(this);
        btnSite2.setOnClickListener(this);

            getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_FIO,"");
            getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_USER_ID,"");

        Toast.makeText(this, "Hello," + getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_FIO,""), Toast.LENGTH_SHORT).show();

        mHandler = new Handler();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_geo:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, ConstantManager.GEO_REQUEST_PERMISSION_CODE);
                    }

                } else {
                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                }
                // re-schedule timer here
                // otherwise, IllegalStateException of
                // "TimerTask is scheduled already"
                // will be thrown
                mTimer = new Timer();
                mMyTimerTask = new MyTimerTask();
                mTimer.schedule(mMyTimerTask, 0, 2 * 60 * 1000);
                btnGeo.setBackgroundResource(R.color.color_gps_enabled);
                Toast.makeText(this, R.string.toast_geo,Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_selfie:
                loadPhotoFromCamera();
                break;
            case R.id.btn_site1:
                openSite();
                break;
            case R.id.btn_site2:
                showDialog(ConstantManager.DIALOG_PODPIS);
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ConstantManager.DIALOG_PODPIS:
                String[] selectItems = {getString(R.string.dialog_ok), getString(R.string.dialog_cancel)};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_title);
                builder.setItems(selectItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int choiceItem) {
                        switch (choiceItem) {
                            case 0:
                                Intent i = new Intent(MenuActivity.this, PaintActivity.class);
                                startActivityForResult(i, ConstantManager.REQUEST_CODE_PODPIS);
                                break;
                            case 1:
                                break;
                        }
                    }
                });
                return builder.create();
        }
        return null;
    }

    private void openSite2() {
        Intent openSite1Intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example"));
        startActivity(openSite1Intent);

    }

    private void openSite() {
        Intent openSite1Intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://example"));
        startActivity(openSite1Intent);
    }

    private void loadPhotoFromCamera() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent takeCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            getFileUri();
            takeCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);
            startActivityForResult(takeCaptureIntent, ConstantManager.REQUEST_CAMERA);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, ConstantManager.CAMERA_REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ConstantManager.CAMERA_REQUEST_PERMISSION_CODE && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }

            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            }

        } else if (requestCode == ConstantManager.GEO_REQUEST_PERMISSION_CODE && grantResults.length == 2) {
        }
    }

    private void getFileUri() {
        getLocation();
        String timeStamp = new SimpleDateFormat("HH_mm_ss_yyyy_M_dd").format(new Date());
        image_name = timeStamp + "_" + getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_USER_ID,"") +"_lat_"+gps.getLatitude()+"_lon_"+gps.getLongitude()+ ".jpg";
        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + image_name);
        file_uri = Uri.fromFile(file);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ConstantManager.REQUEST_CAMERA && resultCode == RESULT_OK) {
            getLocation();
            postSelphiIntoDB();
            new EncodeImage().execute();
        } else if (requestCode == ConstantManager.REQUEST_CODE_PODPIS && resultCode == RESULT_OK) {
            getLocation();
            postpodpisiIntoDB();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openSite2();
                }
            }, 1500);
        }
    }

    private class EncodeImage extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            bitmap = BitmapFactory.decodeFile(file_uri.getPath());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            bitmap.recycle();
            byte[] array = stream.toByteArray();
            encoded_string = Base64.encodeToString(array, 0);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            makeRequest();
        }
    }

    private void makeRequest() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, "http://example",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MenuActivity.this, R.string.response_photo_ok,Toast.LENGTH_SHORT).show();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MenuActivity.this, R.string.response_photo_error,Toast.LENGTH_SHORT).show();

            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put(getString(R.string.server_request_encoding_string), encoded_string);
                map.put(getString(R.string.server_request_image_name), image_name);
                return map;
            }
        };
        requestQueue.add(request);
    }

    private void postpodpisiIntoDB() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd/HH:mm:ss").format(new Date());
                    String userid =getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_USER_ID,"");
                    String fio = getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_FIO,"");
                    String podpis_name=getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_USER_ID,"") + "_podpis_" + timeStamp + ".jpg";
                    URL url = new URL("http://example/podpistobd.php?action=save&userid="+ URLEncoder.encode(userid,"UTF-8")+"&fio="+ URLEncoder.encode(fio,"UTF-8")+ "&lat=" + gps.getLatitude() + "&lon=" + gps.getLongitude() + "&dat=" + URLEncoder.encode(timeStamp,"UTF-8")+"&img="+podpis_name);
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    String data = convertStreamToString(stream);
                    if (!data.equals("false")) {
                    }
                } catch (Exception e) {

                    // *** Если у нас есть какая-то ошибка, выводим её
                    Log.i("ERROR ctaciv>", e.getMessage());
                }
            }

        });
        thread.start();
    }

    private void postLocationIntoDB() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd/HH:mm:ss").format(new Date());
                    String userid = getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_USER_ID,"");
                    String fio = getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_FIO,"");
                    URL url = new URL("http://example/savegeo.php?action=save&userid="+ URLEncoder.encode(userid,"UTF-8")+"&fio="+ URLEncoder.encode(fio,"UTF-8")+ "&lat=" + gps.getLatitude() + "&lon=" + gps.getLongitude() + "&dat=" + URLEncoder.encode(timeStamp,"UTF-8"));
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    String data = convertStreamToString(stream);
                    if (!data.equals("false")) {
                    }
                } catch (Exception e) {
                    // *** Если у нас есть какая-то ошибка, выводим её
                    Log.i("ERROR ctaciv>", e.getMessage());
                }
            }

        });
        thread.start();
    }

    private void postSelphiIntoDB() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String timeStamp = new SimpleDateFormat("yyyy.MM.dd/HH:mm:ss").format(new Date());
                    String userid = getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_USER_ID,"");
                    String fio = getSharedPreferences("myPref",MODE_PRIVATE).getString(ConstantManager.EXTRA_FIO,"");
                    URL url = new URL("http://example/selphitobd.php?action=save&userid="+ URLEncoder.encode(userid,"UTF-8")+"&fio="+ URLEncoder.encode(fio,"UTF-8")+ "&lat=" + gps.getLatitude() + "&lon=" + gps.getLongitude() + "&dat=" + URLEncoder.encode(timeStamp,"UTF-8")+"&img="+image_name);
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    String data = convertStreamToString(stream);
                    if (!data.equals("false")) {
                    }
                } catch (Exception e) {
                    // *** Если у нас есть какая-то ошибка, выводим её
                    Log.i("ERROR ctaciv>", e.getMessage());
                }
            }

        });
        thread.start();
    }

    public void getLocation() {
        gps = new GPSTracker(MenuActivity.this);
        // check if GPS enabled
        if (gps.canGetLocation()) {
            Log.i("TAG", "lat=" + gps.getLatitude() + " lon=" + gps.getLongitude());
        } else {
            gps.showSettingsAlert();
        }

    }

    class MyTimerTask extends TimerTask {
        long s = System.currentTimeMillis();



        @Override
        public void run() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "dd:MMMM:yyyy HH:mm:ss a", Locale.getDefault());
            final String str = simpleDateFormat.format(new Date());

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (s + 60000 * 20 > System.currentTimeMillis()) {
                        getLocation();
                        postLocationIntoDB();
                    } else {
                        btnGeo.setBackgroundResource(android.R.drawable.btn_default);

                    }
                }
            });
        }
    }


}
