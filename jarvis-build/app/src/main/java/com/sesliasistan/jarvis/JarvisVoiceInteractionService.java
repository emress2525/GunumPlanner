package com.sesliasistan.jarvis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.service.voice.VoiceInteractionService;

public final class JarvisVoiceInteractionService extends VoiceInteractionService {
    @Override
    public void onReady() {
        super.onReady();
        boolean requested = getSharedPreferences(JarvisListeningService.PREFS, MODE_PRIVATE)
                .getBoolean(JarvisListeningService.KEY_ACTIVE, false);
        if (!requested || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        try {
            startForegroundService(new Intent(this, JarvisListeningService.class).setAction(JarvisListeningService.ACTION_START));
        } catch (RuntimeException ignored) {
        }
    }
}
