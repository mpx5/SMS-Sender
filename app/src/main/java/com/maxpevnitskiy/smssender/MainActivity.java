package com.maxpevnitskiy.smssender;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";
    private static final String JOB_TAG = "SMS-sender";
    public static final String COUNT_TAG = "count";
    private static final int PICK_CONTACT_REQUEST = 1;
    private Context context;
    private FirebaseJobDispatcher mDispatcher;
    private EditText mPhoneEditText;
    private EditText mCountEditText;
    private TextView mCounter;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        mCounter = (TextView) findViewById(R.id.tv_counter);
        mCounter.setText(String.valueOf(mSharedPreferences.getInt(COUNT_TAG, 0)));

        mDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        mCountEditText = (EditText) findViewById(R.id.et_count);
        mPhoneEditText = (EditText) findViewById(R.id.et_phone);

        mPhoneEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhoneEditText.getText().toString().length() == 0) {
                    Intent intent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
                    intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                    startActivityForResult(intent, PICK_CONTACT_REQUEST);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri contactUri = data.getData();
                Cursor cursor = getContentResolver().query(
                        contactUri,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        null,
                        null,
                        null);
                cursor.moveToFirst();
                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(column);
                mPhoneEditText.setText(number);
            }
        }
    }

    public void startService(View view) {

        String countString = mCountEditText.getText().toString();

        if (countString.length() != 0) {
            int count = Integer.parseInt(countString);


            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putInt(COUNT_TAG, count);
            editor.commit();

            mCounter.setText(String.valueOf(count));
        }


        Job job = mDispatcher.newJobBuilder()
                .setService(SMSJobService.class)
                .setTag(JOB_TAG)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(0, 1))
                .setReplaceCurrent(true)
//                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
//                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                .build();


        mDispatcher.mustSchedule(job);
    }

    public void stopService(View view) {
        mDispatcher.cancel(JOB_TAG);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(COUNT_TAG)) {
            int count = sharedPreferences.getInt(COUNT_TAG, 0);
            mCounter.setText(String.valueOf(count));
            if (count == 0) {
                mDispatcher.cancel(JOB_TAG);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
    }
}
