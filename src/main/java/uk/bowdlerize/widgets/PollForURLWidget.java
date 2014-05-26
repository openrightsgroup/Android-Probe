/*
* Copyright (C) 2014 - Gareth Llewellyn
*
* This file is part of Bowdlerize - https://bowdlerize.co.uk
*
* This program is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>
*/
package uk.bowdlerize.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import uk.bowdlerize.API;
import uk.bowdlerize.R;
import uk.bowdlerize.service.CensorCensusService;

public class PollForURLWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++)
        {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.poll_for_urlwidget);
            int appWidgetId = appWidgetIds[i];

            Intent offIntent = new Intent(context, PollForURLWidget.class);
            offIntent.putExtra("action", "getURL" );
            PendingIntent pendingOffIntent = PendingIntent.getBroadcast(context, 0, offIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.refreshButton, pendingOffIntent);
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.poll_for_urlwidget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        Intent getURLIntent = new Intent(context, CensorCensusService.class);
        Bundle extras = new Bundle();
        extras.putBoolean(API.EXTRA_POLL, true);
        getURLIntent.putExtras(extras);
        context.startService(getURLIntent);
    }

}


