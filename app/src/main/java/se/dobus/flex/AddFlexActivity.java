package se.dobus.flex;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import se.dobus.flex.models.DatePickerFragment;
import se.dobus.flex.models.TimePickerFragment;

public class AddFlexActivity extends AppCompatActivity
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener{

    GoogleAccountCredential mCredential;
    @BindView(R.id.date)
    Button dateButton;
    @BindView(R.id.start_time)
    Button startTimeButton;
    @BindView(R.id.duration_hours)
    Spinner durationHours;
    @BindView(R.id.duration_minutes)
    Spinner durationMinutes;
    @BindView(R.id.add)
    Button addButton;
    @BindView(R.id.in_or_out)
    Spinner inOrOut;

    private Date startDate;
    private int duration;
    private long startTime;
    private boolean dateSet = false;
    private boolean timeSet = false;

    private java.util.Calendar currentTimeSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_flex);
        ButterKnife.bind(this);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(MainActivity.SCOPES))
                .setBackOff(new ExponentialBackOff());
        String accountName = getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                .getString(MainActivity.PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
        }

        currentTimeSelected = java.util.Calendar.getInstance();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.in_or_out, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.hours, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this,
                R.array.minutes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inOrOut.setAdapter(adapter);
        durationHours.setAdapter(adapter2);
        durationMinutes.setAdapter(adapter3);
    }

    @OnClick({R.id.date, R.id.start_time, R.id.add})
    public void onViewClicked(View view) {
        Bundle bundle = new Bundle();
        bundle.putLong("current_date", currentTimeSelected.getTime().getTime());
        switch (view.getId()) {
            case R.id.date:
                DialogFragment newDateFragment = new DatePickerFragment();
                newDateFragment.setArguments(bundle);
                newDateFragment.show(getFragmentManager(), "AddFlexDatePicker");
                dateSet=true;
                break;
            case R.id.start_time:
                DialogFragment newTimeFragment = new TimePickerFragment();
                bundle.putString("type", "time");
                newTimeFragment.setArguments(bundle);
                newTimeFragment.show(getFragmentManager(), "timePicker");
                timeSet=true;
                break;
            case R.id.add:
                if (dateSet && timeSet){
                    new AddEvent().execute(inOrOut.getSelectedItem().toString(), durationHours.getSelectedItem().toString(), durationMinutes.getSelectedItem().toString());
                }
                else{
                    Toast.makeText(this, "Se till att allt är satt korrekt!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        currentTimeSelected.set(year,month,day);
        startDate = new Date(year-1900, month, day);
        dateButton.setText(year+"/"+ month+"/" +day);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        currentTimeSelected.set(java.util.Calendar.MINUTE, minute);
        currentTimeSelected.set(java.util.Calendar.HOUR, hourOfDay);

        startTimeButton.setText((hourOfDay > 9 ? hourOfDay: "0"+ hourOfDay)+":"+ (minute > 9 ? minute : "0" + minute));
        startTime = TimeUnit.HOURS.toMillis(hourOfDay);
        startTime += TimeUnit.MINUTES.toMillis(minute);
    }

    private class AddEvent extends AsyncTask<String,Void,Void>{
        Calendar mService = null;
        @Override
        protected Void doInBackground(String... params) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new Calendar.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
            Event event = new Event()
                    .setSummary(params[0])
                    .setLocation("Dobus AB");
            DateTime startDateTime = new DateTime(startDate.getTime() + startTime);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("Europe/Stockholm");
            event.setStart(start);

            int durationTime = (Integer.parseInt(params[1])*60) + Integer.parseInt(params[2]);
            DateTime endDateTime = new DateTime(startDate.getTime() + startTime + TimeUnit.MINUTES.toMillis(durationTime));
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("Europe/Stockholm");
            event.setEnd(end);

            String calendarId = "primary";

            try {
                event = mService.events().insert(calendarId, event).execute();
            } catch (UserRecoverableAuthIOException e){
                startActivityForResult(e.getIntent(), MainActivity.REQUEST_AUTHORIZATION);
            }catch (IOException e) {
                e.printStackTrace();
            }
            System.out.printf("Event created: %s\n", event.getHtmlLink());
            if (event.getHangoutLink()!=null) {
                Toast.makeText(AddFlexActivity.this, "Flex inlagd!", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(AddFlexActivity.this, "Flex inte inlagd...", Toast.LENGTH_LONG).show();
            }
            return null;
        }
    }
}
