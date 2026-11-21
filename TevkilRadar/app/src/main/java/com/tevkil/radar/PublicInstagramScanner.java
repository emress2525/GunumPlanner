package com.tevkil.radar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Experimental public HTML monitoring without Meta API or login.
 * New posts are reported only when profile and post media are actually readable.
 */
public final class PublicInstagramScanner {
    public static final class Result {
        public final int checked, readable, newNotices, failures;
        public final String status;
        Result(int checked,int readable,int newNotices,int failures,String status) {
            this.checked=checked;this.readable=readable;this.newNotices=newNotices;
            this.failures=failures;this.status=status;
        }
    }
    private PublicInstagramScanner() {}

    /** Serializes scheduled, interactive, and foreground scans to avoid duplicated notices. */
    public static synchronized Result scan(Context context) {
        SharedPreferences prefs=Prefs.raw(context);
        List<String> accounts=InstagramSources.accounts(prefs.getString("instagram_sources",InstagramSources.DEFAULT));
        int checked=0, readable=0, newNotices=0, failures=0, baselined=0;
        StringBuilder details=new StringBuilder();
        if (accounts.isEmpty()) return finish(context,new Result(0,0,0,0,"Takip edilecek hesap eklenmedi"));
        try(NoticeStore store=new NoticeStore(context)) {
            for(String username: accounts) {
                if (Thread.currentThread().isInterrupted()) break;
                checked++;
                try {
                    List<PublicInstagramParser.Link> posts=PublicInstagramClient.fetchIndex(username);
                    readable++;
                    String key="public_seen_"+username;
                    String initKey="public_initialized_"+username;
                    Set<String> seen=new HashSet<>(prefs.getStringSet(key,java.util.Collections.emptySet()));
                    if(!prefs.getBoolean(initKey,false)) {
                        for(PublicInstagramParser.Link post:posts) seen.add(post.code);
                        prefs.edit().putStringSet(key,seen).putBoolean(initKey,true).apply();
                        baselined++;
                        continue; // No notifications for pre-existing posts on first successful scan.
                    }
                    // Process newly exposed post links. A missing/blocked post is not marked seen.
                    for(PublicInstagramParser.Link link:posts) {
                        if(seen.contains(link.code) || Thread.currentThread().isInterrupted()) continue;
                        try {
                            PublicInstagramClient.Post post=PublicInstagramClient.fetchPost(link);
                            StringBuilder content=new StringBuilder(post.caption);
                            if(!post.image.isEmpty()) {
                                try {
                                    String ocr=ImageTextReader.readInstagramImage(post.image);
                                    if(!ocr.trim().isEmpty()) content.append("\n[Görsel OCR: doğrulayın]\n").append(ocr);
                                } catch(Exception ignored) {
                                    // A caption can still be processed when a CDN URL has expired.
                                }
                            }
                            String body=content.toString().trim();
                            if(body.isEmpty()) {
                                failures++;
                                continue; // Retry if the image cannot be read and there is no caption.
                            }
                            String title="Instagram · @"+username;
                            if(FilterEngine.matches(title,body,Prefs.rules(context)) &&
                                    !TevkilDateGate.isExpired(body, LocalDate.now())) {
                                long id=store.addRemote("Instagram herkese açık (deneme)",title,body,link.url,
                                        "public:"+username+":"+link.code);
                                if(id>=0) {
                                    newNotices++;
                                    InstagramRemoteScanner.notifyMatch(context,id,username,body);
                                }
                            }
                            seen.add(link.code);
                        } catch(Exception ex) {
                            failures++;
                            if(details.length()<180) details.append("@").append(username)
                                    .append(" gönderi okunamadı. ");
                        }
                    }
                    prefs.edit().putStringSet(key,seen).apply();
                } catch(Exception ex) {
                    failures++;
                    if(details.length()<180) {
                        String msg=ex.getMessage()==null?"erişim yok":ex.getMessage();
                        details.append("@").append(username).append(": ").append(msg).append(". ");
                    }
                }
            }
        } catch(Exception ex) {
            failures++;
            details.append("Yerel kayıt hatası.");
        }
        String message;
        if(readable==0) message="Instagram girişsiz taramayı engelledi veya gönderi bağlantısı döndürmedi. "+details;
        else message="Girişsiz tarama: "+readable+"/"+checked+" hesapta bağlantı bulundu, "
                +baselined+" hesap başlangıca alındı, "+newNotices+" yeni uygun ilan"
                +(failures>0?", "+failures+" erişim/okuma hatası":"")
                +(details.length()>0?". "+details:"");
        return finish(context,new Result(checked,readable,newNotices,failures,message));
    }

    private static Result finish(Context context, Result result) {
        Prefs.raw(context).edit()
            .putLong("public_last_time", System.currentTimeMillis())
            .putString("public_last_status",result.status).apply();
        Intent update=new Intent(RadarListener.UPDATE).setPackage(context.getPackageName());
        context.sendBroadcast(update);
        return result;
    }
}
