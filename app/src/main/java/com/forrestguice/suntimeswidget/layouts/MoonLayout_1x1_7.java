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
import android.text.SpannableString;
import android.util.Log;
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
 * Moon Distance Widget
 */
public class MoonLayout_1x1_7 extends MoonLayout
{
    public MoonLayout_1x1_7()
    {
        super();
    }

    @Override
    public void initLayoutID()
    {
        this.layoutID = R.layout.layout_widget_moon_1x1_7;
    }

    @Override
    public void updateViews(Context context, int appWidgetId, RemoteViews views, SuntimesMoonData data)
    {
        super.updateViews(context, appWidgetId, views, data);

        SuntimesCalculator calculator = data.calculator();
        SuntimesCalculator.MoonPosition moonPosition = calculator.getMoonPosition(data.now());

        WidgetSettings.LengthUnit units = WidgetSettings.loadLengthUnitsPref(context, appWidgetId);
        SuntimesUtils.TimeDisplayText distanceDisplay = SuntimesUtils.formatAsDistance(context, moonPosition.distance, units, PositionLayout.DECIMAL_PLACES, true);

        String unitsSymbol = distanceDisplay.getUnits();
        String distanceString = distanceDisplay.toString();

        SpannableString distance = SuntimesUtils.createColorSpan(null, distanceString, distanceString, highlightColor, boldTime);
        distance = SuntimesUtils.createBoldColorSpan(distance, distanceString, unitsSymbol, suffixColor);
        distance = SuntimesUtils.createRelativeSpan(distance, distanceString, unitsSymbol, PositionLayout.SYMBOL_RELATIVE_SIZE);
        views.setTextViewText(R.id.info_moon_distance_current, distance);

        boolean showLabels = WidgetSettings.loadShowLabelsPref(context, appWidgetId);
        int visibility = (showLabels ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.info_moon_distance_current_label, visibility);
    }

    protected int suffixColor = Color.GRAY;
    protected int highlightColor = Color.WHITE;

    @Override
    public void themeViews(Context context, RemoteViews views, SuntimesTheme theme)
    {
        super.themeViews(context, views, theme);

        highlightColor = theme.getTimeColor();
        suffixColor = theme.getTimeSuffixColor();
        int textColor = theme.getTextColor();
        views.setTextColor(R.id.info_moon_distance_current_label, textColor);
        views.setTextColor(R.id.info_moon_distance_current, textColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            float textSize = theme.getTextSizeSp();
            views.setTextViewTextSize(R.id.info_moon_distance_current_label, TypedValue.COMPLEX_UNIT_DIP, textSize);

            float timeSize = theme.getTimeSizeSp();
            views.setTextViewTextSize(R.id.info_moon_distance_current, TypedValue.COMPLEX_UNIT_DIP, timeSize);
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
        long updateInterval = (5 * 60 * 1000);                 // update every 5 min
        long nextUpdate = Calendar.getInstance().getTimeInMillis() + updateInterval;
        WidgetSettings.saveNextSuggestedUpdate(context, appWidgetId, nextUpdate);
        Log.d("MoonLayout", "saveNextSuggestedUpdate: " + utils.calendarDateTimeDisplayString(context, nextUpdate).toString());
        return true;
    }
}
