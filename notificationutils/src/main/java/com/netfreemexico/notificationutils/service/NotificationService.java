/*
 * Created by Cristian Gonzalez on 02/12/23 17:58
 *  Copyright (c) NetFreeMexico 2023 . All rights reserved.
 */

package com.netfreemexico.notificationutils.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.netfreemexico.notificationutils.util.Utils;
import com.netfreemexico.notificationutils.constants.AppConstants;
import com.netfreemexico.notificationutils.model.Data;

public class NotificationService extends FirebaseMessagingService implements AppConstants {
    private final Gson gson = new Gson();
    private NotificationManager notificationManager;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        if (message.getData().size() == 0) {
            return;
        }

        try {
            String rawData = gson.toJson(message.getData());
            Data notificationData = gson.fromJson(rawData, Data.class);
            showNotification(notificationData);
        } catch (Exception e) {
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Error", e.getMessage()));
        }

    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        prefs().edit().putString(FIREBASE_TOKEN, token).apply();
    }

    private void showNotification(Data data) throws Exception {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "NetFreeMexico");
        notification.setContentTitle(data.getTitle());
        notification.setContentText(data.getMessage());
        notification.setSmallIcon(getNotificationIcon());
        notification.setPriority(Notification.PRIORITY_MAX);

        if (data.isPressed() && getOpenClass() != null) {
            Intent intent = new Intent(this, Class.forName(getOpenClass()));
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent reopen = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            notification.setContentIntent(reopen);
        }

        if (data.isShowActionOne()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data.getActionOneUrl()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(null, data.getActionOneName(), pendingIntent).build();
            notification.addAction(action);
        }

        if (data.isShowActionTwo()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data.getActionTwoUrl()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(null, data.getActionTwoName(), pendingIntent).build();
            notification.addAction(action);
        }

        if (!data.getImageMUrl().isEmpty()) {
            notification.setLargeIcon((Glide.with(this).asBitmap().load(data.getImageMUrl()).submit().get()));
        }

        if (data.getImageLUrl().isEmpty()) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.bigText(data.getMessage());
            notification.setStyle(style);
        } else {
            NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
            style.setBigContentTitle(data.getTitle());
            style.setSummaryText(data.getMessage());
            style.bigPicture(Glide.with(this).asBitmap().load(data.getImageLUrl()).submit().get());
            notification.setStyle(style);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel("NetFreeMexico", "NetFree", NotificationManager.IMPORTANCE_MAX);
            channel.setDescription("Notifications From App Sender By Cristian Gonzalez");
            getNotificationManager().createNotificationChannel(channel);
            notification.setChannelId("NetFreeMexico");
        }

        getNotificationManager().notify(Utils.randomId(), notification.build());
    }

    private NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    private String getOpenClass() {
        return prefs().getString(INTENT_CLASS, null);
    }

    private int getNotificationIcon() {
        return prefs().getInt(ICON_PREFS, 0);
    }
}
