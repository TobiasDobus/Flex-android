package se.dobus.flex;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import se.dobus.flex.adapters.FlexAdapter;
import se.dobus.flex.models.DatePickerFragment;

public class CalendarActivity extends AppCompatActivity
        implements DatePickerDialog.OnDateSetListener {

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;


    @BindView(R.id.calendar_error_text)
    TextView mOutputText;


    GoogleAccountCredential mCredential;
    @BindView(R.id.start_date)
    Button startDateButton;
    @BindView(R.id.end_date)
    Button endDateButton;
    @BindView(R.id.calendar_data_text)
    TextView calendarDataText;
    @BindView(R.id.get_calendar)
    Button getCalendar;
    @BindView(R.id.flex_in_recycler_view)
    RecyclerView inRecyclerView;
    @BindView(R.id.flex_out_recycler_view)
    RecyclerView outRecyclerView;

    private Date startDate, endDate;

    private FlexAdapter mInAdapter, mOutAdapter;
    private ArrayList<Event> eventArrayList;

    boolean isStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ButterKnife.bind(this);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(MainActivity.SCOPES))
                .setBackOff(new ExponentialBackOff());

        startDate = new Date(System.currentTimeMillis());
        endDate = new Date(System.currentTimeMillis());

        String accountName = getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                .getString(MainActivity.PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
        }
    }




    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                CalendarActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @OnClick({R.id.start_date, R.id.end_date, R.id.get_calendar})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.start_date:
                isStart = true;
                DialogFragment newStartFragment = new DatePickerFragment();
                Bundle bundleStart = new Bundle();
                bundleStart.putLong("current_date", startDate.getTime());
                newStartFragment.setArguments(bundleStart);
                newStartFragment.show(getFragmentManager(), "CalendarDatePicker");
                break;
            case R.id.end_date:
                isStart = false;
                DialogFragment newEndFragment = new DatePickerFragment();
                Bundle bundleEnd = new Bundle();
                bundleEnd.putLong("current_date", endDate.getTime());
                newEndFragment.setArguments(bundleEnd);
                newEndFragment.show(getFragmentManager(), "CalendarDatePicker");
                break;
            case R.id.get_calendar:
                new MakeRequestTask(mCredential).execute();
                break;
        }
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        if (isStart) {
            startDateButton.setText(year + "/" + month + "/" + day);
            startDate = new Date(year-1900, month, day);
        } else {
            endDateButton.setText(year + "/" + month + "/" + day);
            endDate = new Date(year-1900, month, day);
        }
    }

    public void setEvents(ArrayList<Event> events){
        ArrayList<Event> inEvents = new ArrayList<>();
        ArrayList<Event> outEvents = new ArrayList<>();

        for (Event event : events) {
            if (event.getSummary().toLowerCase().equals("flex in")){
                inEvents.add(event);
            }else if (event.getSummary().toLowerCase().equals("flex ut")){
                outEvents.add(event);
            }
        }
        mInAdapter = new FlexAdapter(this, inEvents);
        mOutAdapter = new FlexAdapter(this, outEvents);
        inRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        outRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inRecyclerView.setAdapter(mInAdapter);
        outRecyclerView.setAdapter(mOutAdapter);
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, Void> {
        private Calendar mService = null;
        private Exception mLastError = null;
        private ArrayList<Event> eventArrayList;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         *
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private Void getDataFromApi() throws IOException {
            DateTime startDateTime = new DateTime(startDate.getTime());
            DateTime endDateTime = new DateTime(endDate.getTime());
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list("primary")
                    .setMaxResults(100)
                    .setTimeMin(startDateTime)
                    .setTimeMax(endDateTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            eventArrayList = new ArrayList<>(events.getItems());
            /*long time = 0;

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                }
                if (event.getSummary().toLowerCase().equals("flex in") || event.getSummary().toLowerCase().equals("flex ut")) {
                    long eventDurationLong = getEventDuration(event);
                    String durationInMinutes = String.format(Locale.ENGLISH, "%d min",
                            TimeUnit.MILLISECONDS.toMinutes(getEventDuration(event)));
                    eventStrings.add(
                            String.format("%s (%s) %s", event.getSummary(), start, durationInMinutes));
                    if (event.getSummary().toLowerCase().equals("flex in")) {
                        time += eventDurationLong;
                    } else {
                        time -= eventDurationLong;
                    }
                }
            }
            eventStrings.add(
                    String.format("%s (%s)", "Tid: ", String.format(Locale.ENGLISH, "%d min",
                            TimeUnit.MILLISECONDS.toMinutes(time))));
            return eventStrings;*/
            return null;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setEvents(eventArrayList);
                }
            });
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}
