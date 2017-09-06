package se.dobus.flex.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;

import se.dobus.flex.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by jtobi on 2017-09-06.
 */

public class FlexAdapter extends RecyclerView.Adapter<FlexAdapter.MyViewHolder> {

    private ArrayList<Event> eventList;
    private Activity activity;
    private Context context;

    private String[] months = {"Jan", "Feb", "Mar", "Apr", "Maj", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec"};


    public FlexAdapter(Activity activity, ArrayList<Event> eventList) {
        this.eventList = eventList;
        this.activity = activity;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView text;

        public MyViewHolder(View view) {
            super(view);
            text = view.findViewById(R.id.event_text);

        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        context = parent.getContext();

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        Event event = eventList.get(position);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(event.getStart().getDateTime().getValue()));
        holder.text.setText(String.format("%s\n(%s %s, %s)\n%s",
                event.getSummary(),
                c.get(Calendar.DAY_OF_MONTH),
                months[c.get(Calendar.MONTH)],
                c.get(Calendar.YEAR),
                TimeUnit.MILLISECONDS.toMinutes(getEventDuration(event)) + " min"));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    private long getEventDuration(Event event) {
        DateTime startTime = event.getStart().getDateTime();
        DateTime endTime = event.getEnd().getDateTime();
        return endTime.getValue() - startTime.getValue();
    }
}