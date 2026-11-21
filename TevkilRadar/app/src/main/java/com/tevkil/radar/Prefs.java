package com.tevkil.radar;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String P = "radar_config_v1";
    public static final String DEFAULT_REPLY = "Merhaba meslektaşım, paylaştığınız tevkil işi için müsaitim. Uygunsa ayrıntıları görüşebiliriz.";

    public static final String DEFAULT_COMMENT = "Sayın meslektaşım, ilanınızdaki tevkil işi için uygunum. Benimle iletişime geçebilir misiniz?";

    private Prefs() {}
    public static SharedPreferences raw(Context context) {
        return context.getSharedPreferences(P, Context.MODE_PRIVATE);
    }
    public static FilterEngine.Rules rules(Context context) {
        SharedPreferences p = raw(context);
        FilterEngine.Rules r = new FilterEngine.Rules();
        r.ankaraOnly = p.getBoolean("ankara", true);
        r.include = p.getString("include", r.include);
        r.exclude = p.getString("exclude", "");
        r.districts = p.getString("districts", "");
        r.minFee = p.getLong("minFee", 0);
        return r;
    }
    public static String comment(Context context) {
        return raw(context).getString("comment", DEFAULT_COMMENT);
    }
    public static String reply(Context context) {
        return raw(context).getString("reply", DEFAULT_REPLY);
    }
}
