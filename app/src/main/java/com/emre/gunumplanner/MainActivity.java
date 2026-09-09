package com.emre.gunumplanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private Db db;
    private TextView title, sub;
    private ListView list;
    private Button add;
    private String day;
    private Screen screen = Screen.TODAY;
    private long topicId = 0;
    private List<Db.Item> items = new ArrayList<>();
    private List<Db.Topic> topics = new ArrayList<>();

    private final DateTimeFormatter dayFmt = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter niceFmt = DateTimeFormatter.ofPattern("d MMMM EEEE", new Locale("tr", "TR"));
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    enum Screen { TODAY, HISTORY, TOPICS, TOPIC }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);
        day = LocalDate.now().format(dayFmt);
        buildUi();
        askNotificationPermission();
        showToday();
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(10));
        root.setBackgroundColor(Color.rgb(246,247,251));

        title = new TextView(this);
        title.setTextSize(28); title.setTextColor(Color.rgb(22,24,29)); title.setTypeface(null,1);
        root.addView(title);
        sub = new TextView(this);
        sub.setTextSize(14); sub.setTextColor(Color.DKGRAY); sub.setPadding(0,0,0,dp(10));
        root.addView(sub);

        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER);
        Button today = button("Bugün"); today.setOnClickListener(v -> showToday()); nav.addView(today, weight());
        Button hist = button("Geçmiş"); hist.setOnClickListener(v -> pickHistory()); nav.addView(hist, weight());
        Button top = button("Konular"); top.setOnClickListener(v -> showTopics()); nav.addView(top, weight());
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(48)));

        list = new ListView(this); list.setDividerHeight(dp(6));
        root.addView(list, new LinearLayout.LayoutParams(-1,0,1));

        add = button("＋ Görev / Not ekle"); add.setTextSize(16); add.setOnClickListener(v -> addChooser());
        root.addView(add, new LinearLayout.LayoutParams(-1,dp(54)));
        setContentView(root);
    }

    private void showToday() {
        screen = Screen.TODAY; topicId = 0; day = LocalDate.now().format(dayFmt);
        title.setText("Bugün"); sub.setText(LocalDate.now().format(niceFmt));
        add.setText("＋ Görev / Not ekle"); add.setOnClickListener(v -> addChooser()); add.setVisibility(View.VISIBLE);
        loadDay();
    }

    private void loadDay() {
        items = db.getItemsForDay(day);
        List<String> rows = new ArrayList<>();
        for (Db.Item i : items) {
            String mark = Db.STATUS_DONE.equals(i.status) ? "✓" : "○";
            String type = Db.TYPE_NOTE.equals(i.type) ? " 📝" : "";
            String tm = i.dueAt > 0 ? "  " + Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt) : "";
            String body = i.body == null || i.body.trim().isEmpty() ? "" : "\n   " + shortText(i.body);
            rows.add(mark + type + " " + i.title + tm + body);
        }
        if (rows.isEmpty()) rows.add("Bu gün için kayıt yok.");
        setRows(rows);
        list.setOnItemClickListener((p,v,pos,id) -> { if (pos < items.size()) details(items.get(pos)); });
        list.setOnItemLongClickListener((p,v,pos,id) -> { if (pos < items.size()) itemActions(items.get(pos)); return true; });
    }

    private void pickHistory() {
        LocalDate d;
        try { d = LocalDate.parse(day); } catch (Exception e) { d = LocalDate.now(); }
        new DatePickerDialog(this, (v,y,m,dd) -> showHistory(LocalDate.of(y,m+1,dd).format(dayFmt)),
                d.getYear(), d.getMonthValue()-1, d.getDayOfMonth()).show();
    }

    private void showHistory(String key) {
        screen = Screen.HISTORY; day = key; topicId = 0;
        title.setText("Geçmiş"); sub.setText(LocalDate.parse(key).format(niceFmt) + " • hareket günlüğü");
        add.setText("＋ Bu güne kayıt ekle"); add.setOnClickListener(v -> addChooser()); add.setVisibility(View.VISIBLE);
        List<String> rows = new ArrayList<>();
        for (Db.Event e : db.getEventsForDay(key)) {
            String tm = Instant.ofEpochMilli(e.createdAt).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt);
            rows.add(tm + "  " + eventName(e.eventType) + "\n" + e.titleSnapshot + (e.details == null || e.details.isEmpty() ? "" : "\n" + e.details));
        }
        if (rows.isEmpty()) rows.add("Bu tarihte kayıtlı hareket yok.");
        setRows(rows); list.setOnItemClickListener(null); list.setOnItemLongClickListener(null);
    }

    private void showTopics() {
        screen = Screen.TOPICS; topicId = 0;
        title.setText("Konular"); sub.setText("Benzer notlar otomatik aynı başlık altında toplanır");
        add.setText("＋ Yeni not ekle"); add.setOnClickListener(v -> noteDialog(null,0)); add.setVisibility(View.VISIBLE);
        topics = db.getTopics();
        List<String> rows = new ArrayList<>();
        for (Db.Topic t : topics) rows.add("📁 " + t.title + (t.locked ? " 🔒" : "") + "\n" + db.countNotesInTopic(t.id) + " not • otomatik " + (t.autoGroup ? "açık" : "kapalı"));
        if (rows.isEmpty()) rows.add("Henüz konu yok. Bir not ekle.");
        setRows(rows);
        list.setOnItemClickListener((p,v,pos,id) -> { if (pos < topics.size()) showTopic(topics.get(pos).id); });
        list.setOnItemLongClickListener((p,v,pos,id) -> { if (pos < topics.size()) topicActions(topics.get(pos)); return true; });
    }

    private void showTopic(long id) {
        Db.Topic t = db.getTopic(id); if (t == null) { showTopics(); return; }
        screen = Screen.TOPIC; topicId = id; title.setText(t.title); sub.setText("Bu konuya ait tüm notlar");
        add.setText("＋ Bu konuya not ekle"); add.setOnClickListener(v -> noteDialog(null,id));
        items = db.getNotesForTopic(id);
        List<String> rows = new ArrayList<>();
        for (Db.Item i : items) rows.add("📝 " + i.title + "\n" + i.dayKey + (i.body == null || i.body.isEmpty() ? "" : " • " + shortText(i.body)));
        if (rows.isEmpty()) rows.add("Bu konuda henüz not yok.");
        setRows(rows);
        list.setOnItemClickListener((p,v,pos,x) -> { if (pos < items.size()) details(items.get(pos)); });
        list.setOnItemLongClickListener((p,v,pos,x) -> { if (pos < items.size()) noteActions(items.get(pos)); return true; });
    }

    private void addChooser() {
        new AlertDialog.Builder(this).setTitle("Ne eklemek istiyorsun?")
                .setItems(new String[]{"Görev","Not"}, (d,w) -> { if (w==0) taskDialog(null); else noteDialog(null,0); }).show();
    }

    private void taskDialog(Db.Item old) {
        LinearLayout box = box();
        EditText name = edit("Görev", old == null ? "" : old.title); box.addView(name);
        EditText body = edit("Not / açıklama", old == null ? "" : old.body); box.addView(body);
        EditText date = edit("Tarih: YYYY-MM-DD", old == null ? day : old.dayKey); date.setInputType(InputType.TYPE_CLASS_DATETIME); box.addView(date);
        String oldTime = old != null && old.dueAt > 0 ? Instant.ofEpochMilli(old.dueAt).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt) : "";
        EditText time = edit("Saat: HH:mm (boş bırakılabilir)", oldTime); time.setInputType(InputType.TYPE_CLASS_DATETIME); box.addView(time);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(old == null ? "Görev ekle" : "Görevi düzenle").setView(box)
                .setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n=name.getText().toString().trim(), b=body.getText().toString().trim(), dk=date.getText().toString().trim(), ts=time.getText().toString().trim();
            if (n.isEmpty()) { toast("Görev adı yaz"); return; }
            long due=0;
            try { LocalDate d=LocalDate.parse(dk); if (!ts.isEmpty()) due=LocalDateTime.of(d, LocalTime.parse(ts,timeFmt)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
            catch(Exception e){ toast("Tarih veya saat hatalı"); return; }
            if (old == null) { long id=db.insertItem(Db.TYPE_TASK,n,b,dk,due,0); if (due>0) ReminderScheduler.schedule(this,id,due); }
            else { ReminderScheduler.cancel(this,old.id); db.updateItem(old.id,n,b,dk,due); if (due>0 && Db.STATUS_OPEN.equals(old.status)) ReminderScheduler.schedule(this,old.id,due); }
            dlg.dismiss(); refresh();
        })); dlg.show();
    }

    private void noteDialog(Db.Item old, long forcedTopic) {
        LinearLayout box=box();
        EditText name=edit("Not başlığı", old==null?"":old.title); box.addView(name);
        EditText body=edit("Not", old==null?"":old.body); box.addView(body);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(old==null?"Not ekle":"Notu düzenle").setView(box)
                .setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n=name.getText().toString().trim(), b=body.getText().toString().trim();
            if (n.isEmpty() && b.isEmpty()) { toast("Bir şey yaz"); return; }
            if (n.isEmpty()) n="Not";
            if (old==null) {
                long tid=forcedTopic>0?forcedTopic:TopicEngine.findOrCreateTopic(db,n,b);
                db.insertItem(Db.TYPE_NOTE,n,b,day,0,tid); TopicEngine.refreshAutoTitle(db,tid);
            } else { db.updateItem(old.id,n,b,old.dayKey,0); if(old.topicId>0) TopicEngine.refreshAutoTitle(db,old.topicId); }
            dlg.dismiss(); refresh();
        })); dlg.show();
    }

    private void details(Db.Item i) {
        String msg=(i.body==null||i.body.isEmpty()?"Açıklama yok":i.body)+"\n\nTarih: "+i.dayKey;
        if(i.topicId>0){Db.Topic t=db.getTopic(i.topicId); if(t!=null) msg+="\nKonu: "+t.title;}
        new AlertDialog.Builder(this).setTitle(i.title).setMessage(msg).setPositiveButton("Tamam",null).show();
    }

    private void itemActions(Db.Item i) {
        if (Db.TYPE_NOTE.equals(i.type)) { noteActions(i); return; }
        String first=Db.STATUS_DONE.equals(i.status)?"Tekrar aç":"Tamamla";
        String[] a={first,"Yarına ertele","Düzenle","Sil"};
        new AlertDialog.Builder(this).setTitle(i.title).setItems(a,(d,w)->{
            if(w==0){ if(Db.STATUS_DONE.equals(i.status)) db.reopenItem(i.id); else {db.completeItem(i.id); ReminderScheduler.cancel(this,i.id);} }
            else if(w==1){ LocalDate nd=LocalDate.parse(i.dayKey).plusDays(1); long due=0; if(i.dueAt>0){LocalTime tm=Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalTime(); due=LocalDateTime.of(nd,tm).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();} ReminderScheduler.cancel(this,i.id); db.postponeItem(i.id,nd.format(dayFmt),due); if(due>0) ReminderScheduler.schedule(this,i.id,due); }
            else if(w==2){ taskDialog(i); return; }
            else { ReminderScheduler.cancel(this,i.id); db.deleteItem(i.id); }
            refresh();
        }).show();
    }

    private void noteActions(Db.Item i) {
        String[] a={"Düzenle","Başka konuya taşı","Bu notu ayrı konu yap","Sil"};
        new AlertDialog.Builder(this).setTitle(i.title).setItems(a,(d,w)->{
            if(w==0){noteDialog(i,i.topicId);return;}
            if(w==1){moveNote(i);return;}
            if(w==2){long t=db.createTopic(TopicEngine.suggestTitle(i.title+" "+i.body)); db.moveNoteToTopic(i.id,t);}
            if(w==3)db.deleteItem(i.id); refresh();
        }).show();
    }

    private void moveNote(Db.Item i) {
        List<Db.Topic> all=db.getTopics(); if(all.isEmpty()){toast("Başka konu yok");return;}
        String[] names=new String[all.size()]; for(int x=0;x<all.size();x++)names[x]=all.get(x).title;
        new AlertDialog.Builder(this).setTitle("Konu seç").setItems(names,(d,w)->{db.moveNoteToTopic(i.id,all.get(w).id); TopicEngine.refreshAutoTitle(db,all.get(w).id); refresh();}).show();
    }

    private void topicActions(Db.Topic t) {
        String[] a={"Başlığı değiştir / kilitle","Otomatik gruplamayı "+(t.autoGroup?"kapat":"aç")};
        new AlertDialog.Builder(this).setTitle(t.title).setItems(a,(d,w)->{if(w==0)renameTopic(t);else{db.setTopicAutoGroup(t.id,!t.autoGroup);showTopics();}}).show();
    }

    private void renameTopic(Db.Topic t) {
        LinearLayout box=box(); EditText e=edit("Konu başlığı",t.title); box.addView(e);
        CheckBox lock=new CheckBox(this); lock.setText("Başlığı kilitle; otomatik değiştirme"); lock.setChecked(t.locked); box.addView(lock);
        new AlertDialog.Builder(this).setTitle("Konu ayarları").setView(box).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())db.renameTopic(t.id,n,lock.isChecked());showTopics();}).show();
    }

    private void refresh(){ if(screen==Screen.TODAY)showToday(); else if(screen==Screen.HISTORY)showHistory(day); else if(screen==Screen.TOPICS)showTopics(); else showTopic(topicId); }
    private String eventName(String e){ if("CREATED".equals(e))return"＋ Oluşturuldu"; if("NOTE_CREATED".equals(e))return"📝 Not alındı"; if("COMPLETED".equals(e))return"✓ Tamamlandı"; if("POSTPONED".equals(e))return"→ Ertelendi"; if("MOVED_IN".equals(e))return"← Taşındı"; if("EDITED".equals(e))return"✎ Düzenlendi"; if("DELETED".equals(e))return"⌫ Silindi"; return e; }
    private void setRows(List<String> rows){ ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,rows){@Override public View getView(int p,View c,ViewGroup g){TextView t=(TextView)super.getView(p,c,g);t.setTextSize(16);t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setBackgroundColor(Color.WHITE);return t;}};list.setAdapter(ad); }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-1,1);}
    private LinearLayout box(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(8),dp(18),dp(8));return b;}
    private EditText edit(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value==null?"":value);e.setTextSize(16);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);return e;}
    private String shortText(String s){String x=s.replace('\n',' ').trim();return x.length()>80?x.substring(0,79)+"…":x;}
    private int dp(int x){return(int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void askNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},901);}
}
