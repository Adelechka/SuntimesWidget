/**
   Copyright (C) 2019 Forrest Guice
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

package com.forrestguice.suntimeswidget.layouts;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.util.Calendar;

/**
 * Moon Apogee / Perigee Widget (next apogee / perigee)
 */
public class MoonLayout_1x1_8 extends MoonLayout
{
    public MoonLayout_1x1_8()
    {
        super();
    }

    @Override
    public void initLayoutID()
    {
        this.layoutID = R.layout.layout_widget_moon_1x1_8;
    }

    @Override
    public void updateViews(Context context, int appWidgetId, RemoteViews views, SuntimesMoonData data)
    {
        super.updateViews(context, appWidgetId, views, data);

        if (data != null && data.isCalculated())
        {
            Pair<Calendar, SuntimesCalculator.MoonPosition> apogee = data.getMoonApogee();
            Pair<Calendar, SuntimesCalculator.MoonPosition> perigee = data.getMoonPerigee();
            updateApogeePerigee(context, appWidgetId, views, data.now(), apogee, perigee);
            hideDistant(views, apogee, perigee);

        } else {
            views.setViewVisibility(R.id.moonapsis_apogee_layout, View.GONE);
            views.setViewVisibility(R.id.moonapsis_perigee_layout, View.GONE);
        }
    }

    protected void hideDistant(RemoteViews views,
                               Pair<Calendar, SuntimesCalculator.MoonPosition> apogee, Pair<Calendar, SuntimesCalculator.MoonPosition> perigee)
    {
        if (apogee != null && apogee.first != null && perigee != null)
        {
            if (apogee.first.before(perigee.first)) {
                views.setViewVisibility(R.id.moonapsis_apogee_layout, View.VISIBLE);
                views.setViewVisibility(R.id.moonapsis_perigee_layout, View.GONE);
            } else {
                views.setViewVisibility(R.id.moonapsis_apogee_layout, View.GONE);
                views.setViewVisibility(R.id.moonapsis_perigee_layout, View.VISIBLE);
            }
        } else {
            views.setViewVisibility(R.id.moonapsis_apogee_layout, View.GONE);
            views.setViewVisibility(R.id.moonapsis_perigee_layout, View.VISIBLE);
        }
    }

    protected void updateApogeePerigee(Context context, int appWidgetId, RemoteViews views, Calendar now,
                                       Pair<Calendar, SuntimesCalculator.MoonPosition> apogee, Pair<Calendar, SuntimesCalculator.MoonPosition> perigee)
    {
        //boolean showWeeks = WidgetSettings.loadShowWeeksPref(context, appWidgetId);
        //boolean showHours = WidgetSettings.loadShowHoursPref(context, appWidgetId);
        boolean showSeconds = WidgetSettings.loadShowSecondsPref(context, appWidgetId);
        boolean showTimeDate = WidgetSettings.loadShowTimeDatePref(context, appWidgetId);
        WidgetSettings.LengthUnit units = WidgetSettings.loadLengthUnitsPref(context, appWidgetId);

        if (apogee != null)
        {
            SuntimesUtils.TimeDisplayText apogeeString = utils.calendarDateTimeDisplayString(context, apogee.first, showTimeDate, showSeconds);
            views.setTextViewText(R.id.moonapsis_apogee_date, apogeeString.getValue());
            //views.setTextViewText(R.id.moonapsis_apogee_note, noteSpan(context, now, apogee.first, showWeeks, showHours));
            if (apogee.second != null) {
                views.setTextViewText(R.id.moonapsis_apogee_distance, distanceSpan(context, apogee.second.distance, units, settingColor));
            }
        } else {
            views.setViewVisibility(R.id.moonapsis_apogee_layout, View.GONE);
        }

        if (perigee != null)
        {
            SuntimesUtils.TimeDisplayText perigeeString = utils.calendarDateTimeDisplayString(context, perigee.first, showTimeDate, showSeconds);
            views.setTextViewText(R.id.moonapsis_perigee_date, perigeeString.getValue());
            //views.setTextViewText(R.id.moonapsis_perigee_note, noteSpan(context, now, perigee.first, showWeeks, showHours));
            if (perigee.second != null) {
                views.setTextViewText(R.id.moonapsis_perigee_distance, distanceSpan(context, perigee.second.distance, units, risingColor));
            }
        } else {
            views.setViewVisibility(R.id.moonapsis_perigee_layout, View.GONE);
        }

        boolean showLabels = WidgetSettings.loadShowLabelsPref(context, appWidgetId);
        int visibility = (showLabels ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.moonapsis_apogee_label, visibility);
        views.setViewVisibility(R.id.moonapsis_perigee_label, visibility);
    }

    protected SpannableString distanceSpan(Context context, double distance, WidgetSettings.LengthUnit units, int color)
    {
        SuntimesUtils.TimeDisplayText distanceDisplay = SuntimesUtils.formatAsDistance(context, distance, units, PositionLayout.DECIMAL_PLACES, true);
        String unitsSymbol = distanceDisplay.getUnits();
        String distanceString = distanceDisplay.toString();
        SpannableString distanceSpan = SuntimesUtils.createColorSpan(null, distanceString, distanceString, color, boldTime);
        distanceSpan = SuntimesUtils.createBoldColorSpan(distanceSpan, distanceString, unitsSymbol, suffixColor);
        distanceSpan = SuntimesUtils.createRelativeSpan(distanceSpan, distanceString, unitsSymbol, PositionLayout.SYMBOL_RELATIVE_SIZE);
        return distanceSpan;
    }

    /**protected SpannableString noteSpan(Context context, @NonNull Calendar now, @NonNull Calendar event, boolean showWeeks, boolean showHours)
    {
        String noteTime = utils.timeDeltaDisplayString(now.getTime(), event.getTime(), showWeeks, showHours).toString();
        String noteString = context.getString((event.before(now) ? R.string.ago : R.string.hence), noteTime);
        return (boldTime ? SuntimesUtils.createBoldColorSpan(null, noteString, noteTime, timeColor) : SuntimesUtils.createColorSpan(null, noteString, noteTime, timeColor));
    }*/

    protected int suffixColor = Color.GRAY;
    protected int timeColor = Color.WHITE;
    protected int risingColor = Color.WHITE;
    protected int settingColor = Color.GRAY;

    @Override
    public void themeViews(Context context, RemoteViews views, SuntimesTheme theme)
    {
        super.themeViews(context, views, theme);

        timeColor = theme.getTimeColor();
        suffixColor = theme.getTimeSuffixColor();
        int textColor = theme.getTextColor();
        risingColor = theme.getMoonriseTextColor();
        settingColor = theme.getMoonsetTextColor();

        views.setTextColor(R.id.moonapsis_apogee_label, textColor);
        views.setTextColor(R.id.moonapsis_apogee_date, timeColor);
        views.setTextColor(R.id.moonapsis_apogee_note, textColor);
        views.setTextColor(R.id.moonapsis_apogee_distance, textColor);

        views.setTextColor(R.id.moonapsis_perigee_label, textColor);
        views.setTextColor(R.id.moonapsis_perigee_date, timeColor);
        views.setTextColor(R.id.moonapsis_perigee_note, textColor);
        views.setTextColor(R.id.moonapsis_perigee_distance, textColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            float textSize = theme.getTextSizeSp();
            views.setTextViewTextSize(R.id.moonapsis_apogee_label, TypedValue.COMPLEX_UNIT_DIP, textSize);
            views.setTextViewTextSize(R.id.moonapsis_apogee_note, TypedValue.COMPLEX_UNIT_DIP, textSize);
            views.setTextViewTextSize(R.id.moonapsis_apogee_distance, TypedValue.COMPLEX_UNIT_DIP, textSize);

            views.setTextViewTextSize(R.id.moonapsis_perigee_label, TypedValue.COMPLEX_UNIT_DIP, textSize);
            views.setTextViewTextSize(R.id.moonapsis_perigee_note, TypedValue.COMPLEX_UNIT_DIP, textSize);
            views.setTextViewTextSize(R.id.moonapsis_perigee_distance, TypedValue.COMPLEX_UNIT_DIP, textSize);

            float timeSize = theme.getTimeSizeSp();
            views.setTextViewTextSize(R.id.moonapsis_apogee_date, TypedValue.COMPLEX_UNIT_DIP, timeSize);
            views.setTextViewTextSize(R.id.moonapsis_perigee_date, TypedValue.COMPLEX_UNIT_DIP, timeSize);
        }
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void prepareForUpdate(Context context, int appWidgetId, SuntimesMoonData data)
    {
        // EMPTY
    }

    @Override
    public boolean saveNextSuggestedUpdate(Context context, int appWidgetId)
    {
        long updateInterval = (5 * 60 * 1000);                 // update every 5 min  // TODO
        long nextUpdate = Calendar.getInstance().getTimeInMillis() + updateInterval;
        WidgetSettings.saveNextSuggestedUpdate(context, appWidgetId, nextUpdate);
        Log.d("MoonLayout", "saveNextSuggestedUpdate: " + utils.calendarDateTimeDisplayString(context, nextUpdate).toString());
        return true;
    }
}
