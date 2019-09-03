/**
    Copyright (C) 2017-2019 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.forrestguice.suntimeswidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.calculator.SuntimesEquinoxSolsticeDataset;
import com.forrestguice.suntimeswidget.cards.CardAdapter;
import com.forrestguice.suntimeswidget.cards.CardLayoutManager;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

@SuppressWarnings("Convert2Diamond")
public class EquinoxView extends LinearLayout
{
    public static final String KEY_UI_USERSWAPPEDCARD = "userSwappedEquinoxCard";
    public static final String KEY_UI_CARDPOSITION = "equinoxCardPosition";
    public static final String KEY_UI_MINIMIZED = "equinoxIsMinimized";

    private static SuntimesUtils utils = new SuntimesUtils();
    private boolean userSwappedCard = false;

    private TextView empty;
    private RecyclerView card_view;
    private CardLayoutManager card_layout;
    private EquinoxViewAdapter card_adapter;
    private CardAdapter.CardViewScroller card_scroller;

    public EquinoxView(Context context)
    {
        super(context);
        init(context, null);
    }

    public EquinoxView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        applyAttributes(context, attrs);
        init(context, attrs);
    }

    private void applyAttributes(Context context, AttributeSet attrs)
    {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EquinoxView, 0, 0);
        try {
            setMinimized(a.getBoolean(R.styleable.EquinoxView_minimized, false));
        } finally {
            a.recycle();
        }
    }

    private void init(Context context, AttributeSet attrs)
    {
        initLocale(context);
        themeViews(context);
        LayoutInflater.from(context).inflate(R.layout.layout_view_equinox, this, true);

        if (attrs != null)
        {
            LinearLayout.LayoutParams lp = generateLayoutParams(attrs);
            options.centered = ((lp.gravity == Gravity.CENTER) || (lp.gravity == Gravity.CENTER_HORIZONTAL));
        }

        empty = (TextView)findViewById(R.id.txt_empty);

        card_view = (RecyclerView)findViewById(R.id.info_equinoxsolstice_flipper1);
        card_view.setHasFixedSize(true);
        card_view.setItemViewCacheSize(7);
        card_view.setLayoutManager(card_layout = new CardLayoutManager(context));
        //card_view.addItemDecoration(new CardAdapter.CardViewDecorator(context));

        card_adapter = new EquinoxViewAdapter(context, options);
        card_adapter.setEquinoxViewListener(cardListener);
        card_view.setAdapter(card_adapter);
        card_view.scrollToPosition(EquinoxViewAdapter.CENTER_POSITION);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(card_view);
        card_scroller = new CardAdapter.CardViewScroller(context);

        boolean minimized = isMinimized();
        if (!minimized) {
            card_view.setOnScrollListener(onCardScrollListener);
        }
        card_view.setLayoutFrozen(minimized);

        if (isInEditMode()) {
            updateViews(context);
        }
    }

    private EquinoxViewOptions options = new EquinoxViewOptions();

    @SuppressLint("ResourceType")
    private void themeViews(Context context) {
        options.init(context);
    }

    public void themeViews(Context context, SuntimesTheme theme)
    {
        if (theme != null)
        {
            options.init(theme);
            card_adapter.setThemeOverride(theme);
        }
    }

    public void initLocale(Context context)
    {
        SuntimesUtils.initDisplayStrings(context);
        options.isRtl = AppSettings.isLocaleRtl(context);
    }

    public void setTrackingMode(WidgetSettings.TrackingMode mode) {
        options.trackingMode = mode;
    }
    public WidgetSettings.TrackingMode getTrackingMode() {
        return options.trackingMode;
    }

    public void setMinimized( boolean value ) {
        options.minimized = value;
    }
    public boolean isMinimized() {
        return options.minimized;
    }

    private void showEmptyView( boolean show )
    {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
        card_view.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    protected void updateViews(Context context) {
        SuntimesEquinoxSolsticeDataset data = card_adapter.initData(context, EquinoxViewAdapter.CENTER_POSITION);
        showEmptyView(data == null || !data.isImplemented());

        int position = card_adapter.highlightNote(context);
        if (position != -1 && !userSwappedCard) {
            card_view.setLayoutFrozen(false);
            card_view.scrollToPosition(position);
            card_view.setLayoutFrozen(isMinimized());
        }
    }
    
    public boolean isImplemented(Context context)
    {
        SuntimesEquinoxSolsticeDataset data = card_adapter.initData(context, EquinoxViewAdapter.CENTER_POSITION);
        return (data != null && data.isImplemented());
    }

    public boolean saveState(Bundle bundle)
    {
        bundle.putInt(EquinoxView.KEY_UI_CARDPOSITION, ((card_layout.findFirstVisibleItemPosition() + card_layout.findLastVisibleItemPosition()) / 2));
        bundle.putBoolean(EquinoxView.KEY_UI_USERSWAPPEDCARD, userSwappedCard);
        bundle.putBoolean(EquinoxView.KEY_UI_MINIMIZED, options.minimized);
        return true;
    }

    public void loadState(Bundle bundle)
    {
        userSwappedCard = bundle.getBoolean(EquinoxView.KEY_UI_USERSWAPPEDCARD, false);
        options.minimized = bundle.getBoolean(EquinoxView.KEY_UI_MINIMIZED, options.minimized);

        int cardPosition = bundle.getInt(EquinoxView.KEY_UI_CARDPOSITION, EquinoxViewAdapter.CENTER_POSITION);
        if (cardPosition == RecyclerView.NO_POSITION) {
            cardPosition = EquinoxViewAdapter.CENTER_POSITION;
        }
        card_view.scrollToPosition(cardPosition);
        card_view.smoothScrollBy(1, 0);  // triggers a snap
    }

    public boolean showNextCard(int position)
    {
        int nextPosition = (position + 1);
        if (nextPosition < card_adapter.getItemCount()) {
            userSwappedCard = true;
            card_scroller.setTargetPosition(nextPosition);
            card_layout.startSmoothScroll(card_scroller);
        }
        return true;
    }

    public boolean showPreviousCard(int position)
    {
        int prevPosition = (position - 1);
        if (prevPosition >= 0) {
            userSwappedCard = true;
            card_scroller.setTargetPosition(prevPosition);
            card_layout.startSmoothScroll(card_scroller);
        }
        return true;
    }

    private View.OnClickListener onClickListener;
    public void setOnClickListener( View.OnClickListener listener ) {
        onClickListener = listener;
    }

    private View.OnLongClickListener onLongClickListener;
    public void setOnLongClickListener( View.OnLongClickListener listener) {
        onLongClickListener = listener;
    }

    private RecyclerView.OnScrollListener onCardScrollListener = new RecyclerView.OnScrollListener()
    {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState)
        {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState ==  RecyclerView.SCROLL_STATE_DRAGGING) {
                userSwappedCard = true;
            }
        }
    };

    private EquinoxViewListener cardListener = new EquinoxViewListener()
    {
        @Override
        public void onClick( int position ) {
            if (onClickListener != null) {
                onClickListener.onClick(EquinoxView.this);
            }
        }
        @Override
        public boolean onLongClick( int position ) {
            if (onLongClickListener != null) {
                return onLongClickListener.onLongClick(EquinoxView.this);
            } else return false;
        }
        @Override
        public void onTitleClick( int position ) {
            if (Math.abs(position - EquinoxViewAdapter.CENTER_POSITION) > SuntimesActivity.HIGHLIGHT_SCROLLING_ITEMS) {
                card_view.scrollToPosition(EquinoxViewAdapter.CENTER_POSITION);
            } else {
                card_scroller.setTargetPosition(EquinoxViewAdapter.CENTER_POSITION);
                card_layout.startSmoothScroll(card_scroller);
            }
        }
        @Override
        public void onNextClick( int position ) {
            userSwappedCard = showNextCard(position);
        }
        @Override
        public void onPrevClick( int position ) {
            userSwappedCard = showPreviousCard(position);
        }
    };

    public void adjustColumnWidth(int columnWidthPx)
    {
        options.columnWidthPx = columnWidthPx;
        card_adapter.notifyDataSetChanged();
    }

    public static EquinoxNote findClosestNote(Calendar now, boolean upcoming, ArrayList<EquinoxNote> notes)
    {
        if (notes == null || now == null) {
            return null;
        }

        EquinoxNote closest = null;
        long timeDeltaMin = Long.MAX_VALUE;
        for (EquinoxNote note : notes)
        {
            Calendar noteTime = note.getTime();
            if (noteTime != null)
            {
                if (upcoming && !noteTime.after(now))
                    continue;

                long timeDelta = Math.abs(noteTime.getTimeInMillis() - now.getTimeInMillis());
                if (timeDelta < timeDeltaMin)
                {
                    timeDeltaMin = timeDelta;
                    closest = note;
                }
            }
        }
        return closest;
    }
    public static int findClosestPage(Calendar now, boolean upcoming, ArrayList<Pair<Integer, Calendar>> notes)
    {
        if (notes == null || now == null) {
            return -1;
        }

        Integer closest = null;
        long timeDeltaMin = Long.MAX_VALUE;
        for (Pair<Integer, Calendar> note : notes)
        {
            Calendar noteTime = note.second;
            if (noteTime != null)
            {
                if (upcoming && !noteTime.after(now))
                    continue;

                long timeDelta = Math.abs(noteTime.getTimeInMillis() - now.getTimeInMillis());
                if (timeDelta < timeDeltaMin)
                {
                    timeDeltaMin = timeDelta;
                    closest = note.first;
                }
            }
        }
        return closest != null ? closest : -1;
    }

    /**
     * EquinoxNote
     */
    public static class EquinoxNote
    {
        protected TextView labelView, timeView, noteView;
        protected Calendar time;
        protected boolean highlighted;
        protected int pageIndex;
        private EquinoxViewOptions options;

        public EquinoxNote(TextView labelView, TextView timeView, TextView noteView, int pageIndex, EquinoxViewOptions options)
        {
            this.labelView = labelView;
            this.timeView = timeView;
            this.noteView = noteView;
            this.pageIndex = pageIndex;
            this.options = options;
        }

        public void adjustLabelWidth( int labelWidthPx )
        {
            ViewGroup.LayoutParams layoutParams = labelView.getLayoutParams();
            layoutParams.width = labelWidthPx;
            labelView.setLayoutParams(layoutParams);
        }

        public void themeViews(Integer labelColor, Integer timeColor, Integer textColor)
        {
            if (labelColor != null) {
                labelView.setTextColor(SuntimesUtils.colorStateList(labelColor, options.disabledColor));
            } else Log.e("EquinoxView", "themeViews: null color, ignoring...");

            if (timeColor != null) {
                timeView.setTextColor(SuntimesUtils.colorStateList(timeColor, options.disabledColor));
            } else Log.e("EquinoxView", "themeViews: null color, ignoring...");

            if (textColor != null) {
                noteView.setTextColor(SuntimesUtils.colorStateList(textColor, options.disabledColor));
            } else Log.e("EquinoxView", "themeViews: null color, ignoring...");
        }

        public void updateDate( Context context, Calendar time )
        {
            updateDate(context, time, true, false);
        }
        public void updateDate( Context context, Calendar time, boolean showTime, boolean showSeconds )
        {
            this.time = time;
            if (timeView != null)
            {
                SuntimesUtils.TimeDisplayText timeText = utils.calendarDateTimeDisplayString(context, time, showTime, showSeconds);
                timeView.setText(timeText.toString());
            }
        }

        public void updateNote( Context context, Calendar now, boolean showWeeks, boolean showHours )
        {
            if (noteView != null)
            {
                if (now != null && time != null)
                {
                    String noteText = utils.timeDeltaDisplayString(now.getTime(), time.getTime(), showWeeks, showHours).toString();

                    if (time.before(Calendar.getInstance()))
                    {
                        String noteString = context.getString(R.string.ago, noteText);
                        SpannableString noteSpan = (noteView.isEnabled() ? SuntimesUtils.createBoldColorSpan(null, noteString, noteText, (options.minimized || highlighted ? options.noteColor : options.disabledColor))
                                                                         : SuntimesUtils.createBoldSpan(null, noteString, noteText));
                        noteView.setText(noteSpan);

                    } else {
                        String noteString = context.getString(R.string.hence, noteText);
                        SpannableString noteSpan = (noteView.isEnabled() ? SuntimesUtils.createBoldColorSpan(null, noteString, noteText, options.noteColor)
                                                                         : SuntimesUtils.createBoldSpan(null, noteString, noteText));
                        noteView.setText(noteSpan);
                    }
                } else {
                    noteView.setText("");
                }
            }
        }

        public void setHighlighted( boolean highlighted )
        {
            this.highlighted = highlighted;
            //highlight(labelView, highlighted);
            highlight(timeView, highlighted);
            setEnabled(true);
            setVisible(true);
        }

        private void highlight( TextView view, boolean value )
        {
            if (view != null)
            {
                if (value)
                {
                    view.setTypeface(view.getTypeface(), Typeface.BOLD);
                    view.setPaintFlags(view.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                } else {
                    view.setTypeface(view.getTypeface(), Typeface.NORMAL);
                    view.setPaintFlags(view.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
                }
            }
        }

        public void setEnabled( boolean value)
        {
            labelView.setEnabled(value);
            timeView.setEnabled(value);
            noteView.setEnabled(value);
        }

        public void setEnabled()
        {
            if (time != null)
            {
                setEnabled(time.after(Calendar.getInstance()));

            } else {
                setEnabled(false);
            }
        }

        public void setVisible( boolean visible )
        {
            labelView.setVisibility( visible ? View.VISIBLE : View.GONE);
            timeView.setVisibility( visible ? View.VISIBLE : View.GONE);
            noteView.setVisibility( visible ? View.VISIBLE : View.GONE);
        }

        public Calendar getTime()
        {
            return time;
        }
    }

    /**
     * EquinoxViewAdapter
     */
    public static class EquinoxViewAdapter extends RecyclerView.Adapter<EquinoxViewHolder>
    {
        public static final int MAX_POSITIONS = 200;
        public static final int CENTER_POSITION = 100;
        private HashMap<Integer, SuntimesEquinoxSolsticeDataset> data = new HashMap<>();

        private WeakReference<Context> contextRef;
        private EquinoxViewOptions options;

        public EquinoxViewAdapter(Context context, EquinoxViewOptions options)
        {
            this.contextRef = new WeakReference<>(context);
            this.options = options;
        }

        @Override
        public EquinoxViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            LayoutInflater layout = LayoutInflater.from(parent.getContext());
            View view = layout.inflate(R.layout.info_time_solsticequinox1, parent, false);
            return new EquinoxViewHolder(view, options);
        }

        @Override
        public void onBindViewHolder(EquinoxViewHolder holder, int position)
        {
            Context context = (contextRef != null ? contextRef.get() : null);
            if (context == null) {
                Log.w("EquinoxViewAdapter", "onBindViewHolder: null context!");
                return;
            }
            if (holder == null) {
                Log.w("EquinoxViewAdapter", "onBindViewHolder: null view holder!");
                return;
            }
            SuntimesEquinoxSolsticeDataset dataset = initData(context, position);
            holder.bindDataToPosition(context, dataset, position, options);

            if (dataset.isCalculated() && dataset.isImplemented())
            {
                holder.enableNotes(!options.minimized);
                if (position == options.highlightPosition || options.minimized)
                {
                    EquinoxNote nextNote = findClosestNote(dataset.now(), options.trackingMode == WidgetSettings.TrackingMode.SOONEST, holder.notes);
                    if (nextNote != null) {
                        nextNote.setHighlighted(true);
                    }
                }
            }

            attachListeners(holder, position);
        }

        @Override
        public void onViewRecycled(EquinoxViewHolder holder)
        {
            detachListeners(holder);

            if (holder.position >= 0 && (holder.position < CENTER_POSITION - 1 || holder.position > CENTER_POSITION + 2))
            {
                data.remove(holder.position);
                //Log.d("DEBUG", "remove data " + holder.position);
            }
            holder.position = RecyclerView.NO_POSITION;
        }

        @Override
        public int getItemCount() {
            return MAX_POSITIONS;
        }

        public SuntimesEquinoxSolsticeDataset initData(Context context, int position)
        {
            SuntimesEquinoxSolsticeDataset retValue = data.get(position);
            if (retValue == null) {
                data.put(position, retValue = createData(context, position));   // data is removed in onViewRecycled
                //Log.d("DEBUG", "add data " + position);
            }
            return retValue;
        }

        protected SuntimesEquinoxSolsticeDataset createData(Context context, int position)
        {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.YEAR, position - CENTER_POSITION);

            SuntimesEquinoxSolsticeDataset retValue = new SuntimesEquinoxSolsticeDataset(context, 0);
            retValue.setTodayIs(date);
            retValue.calculateData();
            return retValue;
        }

        public int highlightNote(Context context)
        {
            ArrayList<Pair<Integer,Calendar>> pageInfo = new ArrayList<>();
            int position = CENTER_POSITION - 1;
            do {
                SuntimesEquinoxSolsticeDataset dataset1 = initData(context, position);
                pageInfo.add(new Pair<Integer,Calendar>(position, dataset1.dataEquinoxSpring.eventCalendarThisYear()));
                pageInfo.add(new Pair<Integer,Calendar>(position, dataset1.dataEquinoxAutumnal.eventCalendarThisYear()));
                pageInfo.add(new Pair<Integer,Calendar>(position, dataset1.dataSolsticeSummer.eventCalendarThisYear()));
                pageInfo.add(new Pair<Integer,Calendar>(position, dataset1.dataSolsticeWinter.eventCalendarThisYear()));
                position++;
            } while (position < CENTER_POSITION + 2);

            SuntimesEquinoxSolsticeDataset dataset = initData(context, CENTER_POSITION);
            options.highlightPosition = findClosestPage(dataset.now(), options.trackingMode == WidgetSettings.TrackingMode.SOONEST, pageInfo);

            notifyDataSetChanged();
            return options.highlightPosition;
        }

        public void setThemeOverride( SuntimesTheme theme ) {
            options.themeOverride = theme;
        }

        private EquinoxViewListener viewListener;
        public void setEquinoxViewListener( EquinoxViewListener listener ) {
            viewListener = listener;
        }

        private void attachListeners(EquinoxViewHolder holder, int position)
        {
            holder.title.setOnClickListener(onTitleClick(position));
            holder.btn_flipperNext.setOnClickListener(onNextClick(position));
            holder.btn_flipperPrev.setOnClickListener(onPrevClick(position));

            if (options.minimized) {
                holder.clickArea.setOnClickListener(onClick(position));
                holder.clickArea.setOnLongClickListener(onLongClick(position));
            }
        }

        private void detachListeners(EquinoxViewHolder holder)
        {
            holder.title.setOnClickListener(null);
            holder.btn_flipperNext.setOnClickListener(null);
            holder.btn_flipperPrev.setOnClickListener(null);
            holder.clickArea.setOnClickListener(null);
            holder.clickArea.setOnLongClickListener(null);
        }

        private View.OnClickListener onClick( final int position ) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewListener != null) {
                        viewListener.onClick(position);
                    }
                }
            };
        }
        private View.OnLongClickListener onLongClick( final int position ) {
            return new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (viewListener != null) {
                        return viewListener.onLongClick(position);
                    } else return false;
                }
            };
        }
        private View.OnClickListener onTitleClick( final int position ) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewListener != null) {
                        viewListener.onTitleClick(position);
                    }
                }
            };
        }
        private View.OnClickListener onNextClick( final int position ) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewListener != null) {
                        viewListener.onNextClick(position);
                    }
                }
            };
        }
        private View.OnClickListener onPrevClick( final int position ) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (viewListener != null) {
                        viewListener.onPrevClick(position);
                    }
                }
            };
        }
    }

    /**
     * EquinoxViewHolder
     */
    public static class EquinoxViewHolder extends RecyclerView.ViewHolder
    {
        public int position = RecyclerView.NO_POSITION;

        public View clickArea;
        public View container;
        public TextView title;
        public ImageButton btn_flipperNext, btn_flipperPrev;
        public EquinoxNote note_equinox_vernal, note_solstice_summer, note_equinox_autumnal, note_solstice_winter;
        public ArrayList<EquinoxNote> notes = new ArrayList<>();

        public EquinoxViewHolder(View view, EquinoxViewOptions options)
        {
            super(view);

            container = view.findViewById(R.id.card_content);
            clickArea = view.findViewById(R.id.clickArea);
            if (!options.minimized) {
                clickArea.setVisibility(View.GONE);
            }

            title = (TextView)view.findViewById(R.id.text_title);
            btn_flipperNext = (ImageButton)view.findViewById(R.id.info_time_nextbtn);
            btn_flipperPrev = (ImageButton)view.findViewById(R.id.info_time_prevbtn);

            TextView txt_equinox_vernal_label = (TextView)view.findViewById(R.id.text_date_equinox_vernal_label);
            TextView txt_equinox_vernal = (TextView)view.findViewById(R.id.text_date_equinox_vernal);
            TextView txt_equinox_vernal_note = (TextView)view.findViewById(R.id.text_date_equinox_vernal_note);
            note_equinox_vernal = addNote(txt_equinox_vernal_label, txt_equinox_vernal, txt_equinox_vernal_note, 0, options.seasonColors[0], options);

            TextView txt_solstice_summer_label = (TextView)view.findViewById(R.id.text_date_solstice_summer_label);
            TextView txt_solstice_summer = (TextView)view.findViewById(R.id.text_date_solstice_summer);
            TextView txt_solstice_summer_note = (TextView)view.findViewById(R.id.text_date_solstice_summer_note);
            note_solstice_summer = addNote(txt_solstice_summer_label, txt_solstice_summer, txt_solstice_summer_note, 0, options.seasonColors[1], options);

            TextView txt_equinox_autumnal_label = (TextView)view.findViewById(R.id.text_date_equinox_autumnal_label);
            TextView txt_equinox_autumnal = (TextView)view.findViewById(R.id.text_date_equinox_autumnal);
            TextView txt_equinox_autumnal_note = (TextView)view.findViewById(R.id.text_date_equinox_autumnal_note);
            note_equinox_autumnal = addNote(txt_equinox_autumnal_label, txt_equinox_autumnal, txt_equinox_autumnal_note, 0, options.seasonColors[2], options);

            TextView txt_solstice_winter_label = (TextView)view.findViewById(R.id.text_date_solstice_winter_label);
            TextView txt_solstice_winter = (TextView)view.findViewById(R.id.text_date_solstice_winter);
            TextView txt_solstice_winter_note = (TextView)view.findViewById(R.id.text_date_solstice_winter_note);
            note_solstice_winter = addNote(txt_solstice_winter_label, txt_solstice_winter, txt_solstice_winter_note, 0, options.seasonColors[3], options);

            if (options.columnWidthPx >= 0) {
                adjustColumnWidth(options.columnWidthPx);
            }

            /**if (options.centered)  // TODO
            {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.CENTER_HORIZONTAL;
                view.setLayoutParams(params);
            }*/
        }

        private EquinoxNote addNote(TextView labelView, TextView timeView, TextView noteView, int pageIndex, Integer timeColor, EquinoxViewOptions options)
        {
            EquinoxNote note = new EquinoxNote(labelView, timeView, noteView, pageIndex, options);
            if (timeColor != null) {
                note.themeViews(options.labelColor, timeColor, options.textColor);
            }
            notes.add(note);
            return note;
        }

        public void disableNotes(Context context, EquinoxViewOptions options)
        {
            for (EquinoxNote note : notes)
            {
                note.setHighlighted(false);
                note.setEnabled(false);
                note.updateDate(context, null);
                note.updateNote(context, null, false, false);

                if (options.minimized) {
                    note.setVisible(false);
                }
            }
        }
        public void enableNotes(boolean visible)
        {
            for (EquinoxNote note : notes)
            {
                note.setEnabled();
                note.setVisible(visible);
            }
        }

        public void bindDataToPosition(@NonNull Context context, SuntimesEquinoxSolsticeDataset data, int position, EquinoxViewOptions options)
        {
            this.position = position;
            for (EquinoxNote note : notes) {
                note.pageIndex = position;
            }

            if (options.themeOverride != null) {
                applyTheme(options.themeOverride, options);
            }
            themeViews(options);

            showTitle(!options.minimized);
            showNextPrevButtons(!options.minimized);

            if (data == null) {
                disableNotes(context, options);
                return;
            }

            if (data.isImplemented() && data.isCalculated())
            {
                SuntimesUtils.TimeDisplayText titleText = utils.calendarDateYearDisplayString(context, data.dataEquinoxSpring.eventCalendarThisYear());
                title.setText(titleText.toString());

                boolean showSeconds = WidgetSettings.loadShowSecondsPref(context, 0);
                boolean showTime = WidgetSettings.loadShowTimeDatePref(context, 0);

                note_equinox_vernal.updateDate(context, data.dataEquinoxSpring.eventCalendarThisYear(), showTime, showSeconds);
                note_equinox_autumnal.updateDate(context, data.dataEquinoxAutumnal.eventCalendarThisYear(), showTime, showSeconds);
                note_solstice_summer.updateDate(context, data.dataSolsticeSummer.eventCalendarThisYear(), showTime, showSeconds);
                note_solstice_winter.updateDate(context, data.dataSolsticeWinter.eventCalendarThisYear(), showTime, showSeconds);

                boolean showWeeks = WidgetSettings.loadShowWeeksPref(context, 0);
                boolean showHours = WidgetSettings.loadShowHoursPref(context, 0);
                for (EquinoxNote note : notes) {
                    note.setHighlighted(false);
                    note.updateNote(context, data.now(), showWeeks, showHours);
                }

            } else {
                disableNotes(context, options);
            }

            if (options.columnWidthPx >= 0) {
                adjustColumnWidth(options.columnWidthPx);
            }
        }

        public void showTitle( boolean show ) {
            title.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        public void showNextPrevButtons( boolean show )
        {
            if (show) {
                btn_flipperNext.setVisibility(View.VISIBLE);
                btn_flipperPrev.setVisibility(View.VISIBLE);
            } else {
                btn_flipperNext.setVisibility(View.GONE);
                btn_flipperPrev.setVisibility(View.GONE);
            }
        }

        public void adjustColumnWidth(int columnWidthPx)
        {
            for (EquinoxNote note : notes) {
                note.adjustLabelWidth(columnWidthPx);
            }
        }

        public void applyTheme(SuntimesTheme theme, EquinoxViewOptions options)
        {
            if (theme != null)
            {
                options.titleColor = theme.getTitleColor();
                options.textColor = theme.getTextColor();
                options.pressedColor = theme.getActionColor();
                options.seasonColors[0] = theme.getSpringColor();
                options.seasonColors[1] = theme.getSummerColor();
                options.seasonColors[2] = theme.getFallColor();
                options.seasonColors[3] = theme.getWinterColor();
            }
        }
        public void themeViews( EquinoxViewOptions options )
        {
            title.setTextColor(SuntimesUtils.colorStateList(options.titleColor, options.disabledColor, options.pressedColor));

            ImageViewCompat.setImageTintList(btn_flipperNext, SuntimesUtils.colorStateList(options.titleColor, options.disabledColor, options.pressedColor));
            ImageViewCompat.setImageTintList(btn_flipperPrev, SuntimesUtils.colorStateList(options.titleColor, options.disabledColor, options.pressedColor));

            note_equinox_vernal.themeViews(options.labelColor, options.seasonColors[0], options.textColor);
            note_solstice_summer.themeViews(options.labelColor, options.seasonColors[1], options.textColor);
            note_equinox_autumnal.themeViews(options.labelColor, options.seasonColors[2], options.textColor);
            note_solstice_winter.themeViews(options.labelColor, options.seasonColors[3], options.textColor);
        }
    }

    /**
     * EquinoxViewOptions
     */
    public static class EquinoxViewOptions
    {
        public boolean isRtl = false;
        public boolean minimized = false;
        public boolean centered = false;
        public int columnWidthPx = -1;
        public int highlightPosition = -1;

        public WidgetSettings.TrackingMode trackingMode = WidgetSettings.TrackingMode.SOONEST;

        public int titleColor, noteColor, disabledColor, pressedColor;
        public Integer[] seasonColors = new Integer[4];
        public Integer labelColor, textColor;
        public int resID_buttonPressColor;

        private SuntimesTheme themeOverride = null;

        @SuppressLint("ResourceType")
        public void init(Context context)
        {
            int[] colorAttrs = { android.R.attr.textColorPrimary, R.attr.text_disabledColor, R.attr.buttonPressColor };
            TypedArray typedArray = context.obtainStyledAttributes(colorAttrs);
            noteColor = ContextCompat.getColor(context, typedArray.getResourceId(0, R.color.transparent));
            titleColor = noteColor;
            disabledColor = ContextCompat.getColor(context, typedArray.getResourceId(1, R.color.text_disabled_dark));
            resID_buttonPressColor = typedArray.getResourceId(2, R.color.btn_tint_pressed_dark);
            pressedColor = ContextCompat.getColor(context, resID_buttonPressColor);
            labelColor = textColor = seasonColors[0] = seasonColors[1] = seasonColors[2] = seasonColors[3] = null;
            typedArray.recycle();
        }

        public void init(SuntimesTheme theme)
        {
            if (theme != null)
            {
                titleColor = theme.getTitleColor();
                noteColor = theme.getTimeColor();
                labelColor = theme.getTitleColor();
                textColor = theme.getTextColor();
                pressedColor = theme.getActionColor();
                seasonColors[0] = theme.getSpringColor();
                seasonColors[1] = theme.getSummerColor();
                seasonColors[2] = theme.getFallColor();
                seasonColors[3] = theme.getWinterColor();
            }
        }
    }

    /**
     * EquinoxViewClickListener
     */
    public static class EquinoxViewListener
    {
        public void onClick( int position ) {}
        public boolean onLongClick( int position ) { return false; }
        public void onTitleClick( int position ) {}
        public void onNextClick( int position ) {}
        public void onPrevClick( int position ) {}
    }

}
