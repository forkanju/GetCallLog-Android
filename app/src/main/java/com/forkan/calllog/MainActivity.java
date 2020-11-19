package com.forkan.calllog;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_SEND_DATA = "Sending data to server";
    private ArrayList<CallLogs> callLogModelArrayList;
    private RecyclerView rv_call_logs;
    private CallLogAdapter callLogAdapter;

    public String str_number, str_contact_name, str_call_type, str_call_full_date,
            str_call_date, str_call_time, str_call_time_formatted, str_call_duration;

    // Request code. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_CODE = 3;

    String[] appPermissions = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
        if (CheckAndRequestPermission()) {
            FetchCallLogs();
        }
    }

    private void initUi() {

        rv_call_logs = findViewById(R.id.call_log_list);
        rv_call_logs.setHasFixedSize(true);
        rv_call_logs.setLayoutManager(new LinearLayoutManager(this));
        callLogModelArrayList = new ArrayList<>();
        callLogAdapter = new CallLogAdapter(this, callLogModelArrayList);
        rv_call_logs.setAdapter(callLogAdapter);
    }

    public void FetchCallLogs() {
        // reading all data in descending order according to DATE
        String sortOrder = android.provider.CallLog.Calls.DATE + " DESC";

        Cursor cursor = this.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                sortOrder);

        //clearing the arraylist
        callLogModelArrayList.clear();

        //looping through the cursor to add data into arraylist
        while (cursor.moveToNext()) {
            str_number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            str_contact_name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
            str_contact_name = str_contact_name == null || str_contact_name.equals("") ? "Unknown" : str_contact_name;
            str_call_type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            str_call_full_date = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
            str_call_duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));

            SimpleDateFormat dateFormatter = new SimpleDateFormat(
                    "dd MMM yyyy");
            str_call_date = dateFormatter.format(new Date(Long.parseLong(str_call_full_date)));

            SimpleDateFormat timeFormatter = new SimpleDateFormat(
                    "HH:mm:ss");
            str_call_time = timeFormatter.format(new Date(Long.parseLong(str_call_full_date)));

            str_call_time = getFormatedDateTime(str_call_time, "HH:mm:ss", "hh:mm ss");

            str_call_duration = DurationFormat(str_call_duration);

            switch (Integer.parseInt(str_call_type)) {
                case CallLog.Calls.INCOMING_TYPE:
                    str_call_type = "Incoming";
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    str_call_type = "Outgoing";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    str_call_type = "Missed";
                    break;
                case CallLog.Calls.VOICEMAIL_TYPE:
                    str_call_type = "Voicemail";
                    break;
                case CallLog.Calls.REJECTED_TYPE:
                    str_call_type = "Rejected";
                    break;
                case CallLog.Calls.BLOCKED_TYPE:
                    str_call_type = "Blocked";
                    break;
                case CallLog.Calls.ANSWERED_EXTERNALLY_TYPE:
                    str_call_type = "Externally Answered";
                    break;
                default:
                    str_call_type = "NA";
            }

            CallLogs callLogItem = new CallLogs(str_number, str_contact_name, str_call_type,
                    str_call_date, str_call_time, str_call_duration);

            callLogModelArrayList.add(callLogItem);

        }
        callLogAdapter.notifyDataSetChanged();
    }


    private String getFormatedDateTime(String dateStr, String strInputFormat, String strOutputFormat) {
        String formattedDate = dateStr;
        DateFormat inputFormat = new SimpleDateFormat(strInputFormat, Locale.getDefault());
        DateFormat outputFormat = new SimpleDateFormat(strOutputFormat, Locale.getDefault());
        Date date = null;
        try {
            date = inputFormat.parse(dateStr);
        } catch (ParseException e) {
        }

        if (date != null) {
            formattedDate = outputFormat.format(date);
        }
        return formattedDate;
    }


    private String DurationFormat(String duration) {
        String durationFormatted = null;
        if (Integer.parseInt(duration) < 60) {
            durationFormatted = duration + " sec";
        } else {
            int min = Integer.parseInt(duration) / 60;
            int sec = Integer.parseInt(duration) % 60;

            if (sec == 0)
                durationFormatted = min + " min";
            else
                durationFormatted = min + " min " + sec + " sec";

        }
        return durationFormatted;
    }


    public boolean CheckAndRequestPermission() {
        //checking which permissions are granted
        List<String> listPermissionNeeded = new ArrayList<>();
        for (String item : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED)
                listPermissionNeeded.add(item);
        }

        //Ask for non-granted permissions
        if (!listPermissionNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionNeeded.toArray(new String[listPermissionNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE);
            return false;
        }
        //App has all permissions. Proceed ahead
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PERMISSIONS_REQUEST_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                FetchCallLogs();

            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}