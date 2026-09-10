package com.emre.gunumplanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
class FullSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { SettingsScreen(this, db) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(activity: Activity, db: Db) {
    val c = LocalContext.current
    var theme by remember { mutableStateOf(FullRepository.Prefs.theme(c)) }
    var duration by remember { mutableIntStateOf(FullRepository.Prefs.defaultDuration(c)) }
    var monday by remember { mutableStateOf(FullRepository.Prefs.weekStartsMonday(c)) }
    var approval by remember { mutableStateOf(FullRepository.Prefs.autoPlanApproval(c)) }
    val trashCount = remember { FullRepository.trash(db).size }

    Scaffold(topBar = { TopAppBar(title={ Text("Ayarlar ve araçlar", fontWeight=FontWeight.Bold) }, navigationIcon={ IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")} }) }) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom=32.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
            FullSectionCard("Görünüm", Icons.Rounded.Palette) {
                Text("Tema", fontWeight=FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("system" to "Sistem", "light" to "Açık", "dark" to "Koyu").forEachIndexed { i, pair ->
                        SegmentedButton(selected=theme==pair.first, onClick={ theme=pair.first; FullRepository.Prefs.setTheme(c,pair.first); activity.recreate() }, shape=SegmentedButtonDefaults.itemShape(i,3)) { Text(pair.second) }
                    }
                }
            }
            FullSectionCard("Planlama", Icons.Rounded.Tune) {
                Text("Varsayılan görev süresi", fontWeight=FontWeight.SemiBold)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(15,30,45,60,90,120).forEach { m -> FilterChip(selected=duration==m,onClick={duration=m;FullRepository.Prefs.setDefaultDuration(c,m)},label={Text("$m dk")}) }
                }
                Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text("Hafta pazartesi başlasın",fontWeight=FontWeight.SemiBold);Text("Haftalık görünüm için",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}; Switch(monday,{monday=it;FullRepository.Prefs.setWeekStartsMonday(c,it)}) }
                Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text("Akıllı plan önce onay istesin",fontWeight=FontWeight.SemiBold);Text("Görevleri kendiliğinden oynatmaz",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}; Switch(approval,{approval=it;FullRepository.Prefs.setAutoPlanApproval(c,it)}) }
            }
            FullSectionCard("Veri ve güvenlik", Icons.Rounded.Backup) {
                FullAction("Yedekle / geri yükle", "Görev, not, ekler ve konumlar", Icons.Rounded.Backup) { c.startActivity(Intent(c, BackupActivity::class.java)) }
                FullAction("Çöp kutusu", if(trashCount==0) "Silinen kayıt yok" else "$trashCount silinen kayıt", Icons.Rounded.DeleteOutline) { c.startActivity(Intent(c, TrashActivity::class.java)) }
            }
            FullSectionCard("Güçlü araçlar", Icons.Rounded.AutoAwesome) {
                FullAction("Gelişmiş arama", "Tür, durum, öncelik ve tarih filtreleri", Icons.Rounded.ManageSearch) { c.startActivity(Intent(c, AdvancedSearchActivity::class.java)) }
                FullAction("Kayıtlı konumlar", "Ev, iş, fabrika, market…", Icons.Rounded.Place) { c.startActivity(Intent(c, SavedPlacesActivity::class.java)) }
                FullAction("Tanıtımı yeniden göster", "İlk kullanım ekranlarını aç", Icons.Rounded.Slideshow) { c.startActivity(Intent(c, FullOnboardingActivity::class.java).putExtra("force",true)) }
            }
            Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { Text("Ana ekran widget'ı",fontWeight=FontWeight.Bold); Text("Telefonun ana ekranında boş alana basılı tut → Widget'lar → Günüm. Sıradaki görevi ve bugünkü açık iş sayısını gösterir.",color=MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }
    }
}

@Composable private fun FullSectionCard(title:String, icon: androidx.compose.ui.graphics.vector.ImageVector, content:@Composable ColumnScope.()->Unit) {
    Card(shape=RoundedCornerShape(26.dp)) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}; content() } }
}
@Composable private fun FullAction(title:String, sub:String, icon: androidx.compose.ui.graphics.vector.ImageVector, click:()->Unit) {
    Surface(onClick=click,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)) { Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(sub,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Rounded.ChevronRight,null)} }
}

class ChecklistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId=intent.getLongExtra("item_id",0)
        val db=Db(this); FullRepository.ensureSchema(db)
        if(itemId<=0||db.getItem(itemId)==null){finish();return}
        setContent { V2Theme { ChecklistScreen(this,db,itemId) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ChecklistScreen(activity:Activity, db:Db, itemId:Long) {
    val item=db.getItem(itemId)?:return
    var rev by remember{mutableIntStateOf(0)}
    var text by remember{mutableStateOf("")}
    val data=remember(rev){FullRepository.subtasks(db,itemId)}
    val done=data.count{it.done}
    Scaffold(topBar={TopAppBar(title={Column{Text("Checklist",fontWeight=FontWeight.Bold);Text(item.title,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)}},navigationIcon={IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}})}){p->
        LazyColumn(Modifier.padding(p).fillMaxSize(),contentPadding=PaddingValues(16.dp,8.dp,16.dp,40.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item { Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(18.dp)){Text("$done / ${data.size} tamamlandı",fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={if(data.isEmpty())0f else done.toFloat()/data.size},Modifier.fillMaxWidth())}} }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(text,{text=it},Modifier.weight(1f),label={Text("Alt görev")},singleLine=true);FilledIconButton(onClick={if(text.isNotBlank()){FullRepository.addSubtask(db,itemId,text);text="";rev++}}){Icon(Icons.Rounded.Add,"Ekle")}} }
            if(data.isEmpty()) item { Text("Henüz alt görev yok. Büyük işi küçük adımlara böl.",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(12.dp)) }
            items(data,key={it.id}){s-> Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(s.done,{FullRepository.toggleSubtask(db,s.id,it);rev++});Text(s.title,Modifier.weight(1f),fontWeight=FontWeight.Medium);IconButton({FullRepository.deleteSubtask(db,s.id);rev++}){Icon(Icons.Rounded.DeleteOutline,"Sil",tint=MaterialTheme.colorScheme.error)}}} }
        }
    }
}

class TrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val db=Db(this);FullRepository.ensureSchema(db);setContent{V2Theme{TrashScreen(this,db)}} }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TrashScreen(activity:Activity, db:Db) {
    var rev by remember{mutableIntStateOf(0)}; val data=remember(rev){FullRepository.trash(db)}
    Scaffold(topBar={TopAppBar(title={Text("Çöp kutusu",fontWeight=FontWeight.Bold)},navigationIcon={IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}})}){p->
        LazyColumn(Modifier.padding(p).fillMaxSize(),contentPadding=PaddingValues(16.dp,10.dp,16.dp,40.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            if(data.isEmpty()) item{Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surfaceVariant){Text("Çöp kutusu boş. Yanlışlıkla sildiğin görev ve notları buradan geri alabilirsin.",Modifier.padding(18.dp))}}
            items(data,key={it.trashId}){t->Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(t.title,fontWeight=FontWeight.Bold);Text(t.dayKey,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Row{Button({FullRepository.restoreTrash(db,t);rev++}){Icon(Icons.Rounded.Restore,null);Spacer(Modifier.width(6.dp));Text("Geri al")};Spacer(Modifier.width(8.dp));TextButton(onClick={FullRepository.deleteTrashForever(db,t);rev++},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("Kalıcı sil")}}}} }
        }
    }
}

class AdvancedSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);val db=Db(this);FullRepository.ensureSchema(db);setContent{V2Theme{AdvancedSearchScreen(this,db)}} }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AdvancedSearchScreen(activity:Activity,db:Db){
    var q by remember{mutableStateOf("")};var type by remember{mutableStateOf<String?>(null)};var status by remember{mutableStateOf<String?>(null)};var priority by remember{mutableStateOf<Int?>(null)}
    val rows=remember(q,type,status,priority){FullRepository.allItemsFiltered(db,q,type,status,priority,null,null)}
    Scaffold(topBar={TopAppBar(title={Text("Gelişmiş arama",fontWeight=FontWeight.Bold)},navigationIcon={IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}})}){p->
        LazyColumn(Modifier.padding(p).fillMaxSize(),contentPadding=PaddingValues(16.dp,8.dp,16.dp,40.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),leadingIcon={Icon(Icons.Rounded.Search,null)},label={Text("Görev veya not ara")},singleLine=true)}
            item{Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Filtreler",fontWeight=FontWeight.Bold);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(type==null,{type=null},{Text("Tümü")});FilterChip(type==Db.TYPE_TASK,{type=Db.TYPE_TASK},{Text("Görev")});FilterChip(type==Db.TYPE_NOTE,{type=Db.TYPE_NOTE},{Text("Not")});FilterChip(status==Db.STATUS_OPEN,{status=if(status==Db.STATUS_OPEN)null else Db.STATUS_OPEN},{Text("Açık")});FilterChip(status==Db.STATUS_DONE,{status=if(status==Db.STATUS_DONE)null else Db.STATUS_DONE},{Text("Tamam")});FilterChip(priority==0,{priority=if(priority==0)null else 0},{Text("Acil")});FilterChip(priority==1,{priority=if(priority==1)null else 1},{Text("Önemli")})}}}
            item{Text("${rows.size} sonuç",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
            items(rows,key={it.id}){i->Surface(onClick={activity.startActivity(Intent(activity,TaskExtrasActivity::class.java).putExtra("item_id",i.id))},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(i.type==Db.TYPE_NOTE)Icons.Rounded.Notes else if(i.status==Db.STATUS_DONE)Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(i.title,fontWeight=FontWeight.SemiBold);Text(i.dayKey+(if(i.body.isBlank())"" else " • ${i.body.take(70)}"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}}}}
        }
    }
}

class SavedPlacesActivity : ComponentActivity() {
    private lateinit var db:Db
    override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);db=Db(this);FullRepository.ensureSchema(db);setContent{V2Theme{SavedPlacesScreen(this,db)}}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SavedPlacesScreen(activity:Activity,db:Db){
    var rev by remember{mutableIntStateOf(0)};var pending by remember{mutableStateOf<Triple<Double,Double,String>?>(null)};var name by remember{mutableStateOf("")};var radius by remember{mutableFloatStateOf(200f)}
    val places=remember(rev){FullRepository.savedPlaces(db)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==Activity.RESULT_OK){val d=r.data;val la=d?.getDoubleExtra("lat",Double.NaN)?:Double.NaN;val lo=d?.getDoubleExtra("lng",Double.NaN)?:Double.NaN;val ad=d?.getStringExtra("address").orEmpty();if(!la.isNaN()&&!lo.isNaN()){pending=Triple(la,lo,ad);name=d?.getStringExtra("label").orEmpty()}}}
    Scaffold(topBar={TopAppBar(title={Text("Kayıtlı konumlar",fontWeight=FontWeight.Bold)},navigationIcon={IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}},actions={IconButton({picker.launch(Intent(activity,MapPickerActivity::class.java))}){Icon(Icons.Rounded.AddLocationAlt,"Konum ekle")}})}){p->
        LazyColumn(Modifier.padding(p).fillMaxSize(),contentPadding=PaddingValues(16.dp,8.dp,16.dp,40.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            pending?.let{loc->item{Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Konumu kaydet",fontWeight=FontWeight.Bold);OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Adı")},placeholder={Text("Ev, İş, Fabrika")});Text(loc.third,style=MaterialTheme.typography.bodySmall);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(100f,200f,500f,1000f).forEach{m->FilterChip(radius==m,{radius=m},{Text(if(m>=1000)"1 km" else "${m.toInt()} m")})}};Button({FullRepository.savePlace(db,name,loc.third,loc.first,loc.second,radius);pending=null;name="";rev++},Modifier.fillMaxWidth()){Icon(Icons.Rounded.Save,null);Spacer(Modifier.width(6.dp));Text("Kaydet")}}}}}
            if(places.isEmpty())item{Text("Ev, iş, fabrika veya sık kullandığın yerleri kaydedip görevlerde tek dokunuşla kullanabilirsin.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(places,key={it.id}){x->Card(shape=RoundedCornerShape(20.dp)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Place,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(x.name,fontWeight=FontWeight.Bold);Text(x.address,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis);Text(if(x.radius>=1000)"1 km" else "${x.radius.toInt()} m",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)};IconButton({FullRepository.deletePlace(db,x.id);rev++}){Icon(Icons.Rounded.DeleteOutline,"Sil",tint=MaterialTheme.colorScheme.error)}}}}
        }
    }
}

class BackupActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{V2Theme{BackupScreen(this)}}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun BackupScreen(activity:BackupActivity){
    var status by remember{mutableStateOf("")}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){status=if(GunumBackup.restore(activity,uri)){"Yedek geri yüklendi. Uygulama yeniden açılıyor…"}.also{activity.window.decorView.postDelayed({activity.startActivity(Intent(activity,PremiumV2Activity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));activity.finish()},700)} else "Yedek geri yüklenemedi"}}
    Scaffold(topBar={TopAppBar(title={Text("Yedekleme",fontWeight=FontWeight.Bold)},navigationIcon={IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}})}){p->Column(Modifier.padding(p).fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Card(shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(18.dp)){Text("Verilerin sende kalsın",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Görevler, notlar, konular, fotoğraflar, PDF'ler, konumlar, checklist ve ayarlar tek ZIP içinde yedeklenir.")}};Button({val f=GunumBackup.create(activity);if(f!=null){val u=FileProvider.getUriForFile(activity,activity.packageName+".files",f);activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Günüm yedeğini kaydet"));status="Yedek oluşturuldu"}else status="Yedek oluşturulamadı"},Modifier.fillMaxWidth()){Icon(Icons.Rounded.Upload,null);Spacer(Modifier.width(8.dp));Text("Yedek oluştur / paylaş")};OutlinedButton({importer.launch(arrayOf("application/zip","application/octet-stream"))},Modifier.fillMaxWidth()){Icon(Icons.Rounded.Download,null);Spacer(Modifier.width(8.dp));Text("Yedekten geri yükle")};if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)}}}
}

object GunumBackup {
    fun create(c:Context):File?=runCatching{
        val db=Db(c);FullRepository.ensureSchema(db);db.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)");db.close()
        val out=File(c.cacheDir,"Gunum_Yedek_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(out)).use{z->
            val database=c.getDatabasePath("gunum.db");if(database.exists())addFile(z,database,"database/gunum.db")
            val attach=File(c.filesDir,"attachments");if(attach.exists())attach.walkTopDown().filter{it.isFile}.forEach{addFile(z,it,"attachments/${it.name}")}
            val prefs=File(c.applicationInfo.dataDir,"shared_prefs/gunum_full_prefs.xml");if(prefs.exists())addFile(z,prefs,"prefs/gunum_full_prefs.xml")
        };out
    }.getOrNull()
    private fun addFile(z:ZipOutputStream,f:File,name:String){z.putNextEntry(ZipEntry(name));FileInputStream(f).use{it.copyTo(z)};z.closeEntry()}
    fun restore(c:Context,uri:Uri):Boolean=runCatching{
        val temp=File(c.cacheDir,"restore_${System.currentTimeMillis()}").apply{mkdirs()};var hasDb=false
        c.contentResolver.openInputStream(uri)!!.use{input->ZipInputStream(input).use{z->var e=z.nextEntry;while(e!=null){val n=e.name.replace('\\','/');if(!n.contains("..")&&!e.isDirectory){val target=when{n=="database/gunum.db"->{hasDb=true;File(temp,"gunum.db")};n.startsWith("attachments/")->File(temp,"attachments/${File(n).name}");n=="prefs/gunum_full_prefs.xml"->File(temp,"prefs.xml");else->null};target?.let{it.parentFile?.mkdirs();FileOutputStream(it).use{o->z.copyTo(o)}}};z.closeEntry();e=z.nextEntry}}}
        if(!hasDb)return false
        Db(c).close();val live=c.getDatabasePath("gunum.db");live.parentFile?.mkdirs();File(temp,"gunum.db").copyTo(live,true);File(live.absolutePath+"-wal").delete();File(live.absolutePath+"-shm").delete()
        val srcA=File(temp,"attachments");val dstA=File(c.filesDir,"attachments").apply{mkdirs()};if(srcA.exists())srcA.listFiles()?.forEach{it.copyTo(File(dstA,it.name),true)}
        val pref=File(temp,"prefs.xml");if(pref.exists()){val dst=File(c.applicationInfo.dataDir,"shared_prefs/gunum_full_prefs.xml");dst.parentFile?.mkdirs();pref.copyTo(dst,true)}
        temp.deleteRecursively();true
    }.getOrDefault(false)
}

class FullOnboardingActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{V2Theme{OnboardingScreen(this)}}}
}
@Composable private fun OnboardingScreen(activity:Activity){
    var page by remember{mutableIntStateOf(0)}
    val pages=listOf(
        Triple(Icons.Rounded.AutoAwesome,"Günün tek yerde","Görevlerini, notlarını, takvimini ve konum hatırlatmalarını aynı akışta yönet."),
        Triple(Icons.Rounded.Schedule,"Zamanını gerçekten gör","Gün–hafta–ay görünümü, sürükle-bırak planlama, çakışma ve kapasite uyarıları."),
        Triple(Icons.Rounded.PhotoLibrary,"Her şeyi göreve bağla","Fotoğraf, PDF, dosya, checklist ve konum ekle. Eve, işe veya fabrikaya gelince hatırlat."),
        Triple(Icons.Rounded.Psychology,"Daha az düşün, daha çok yap","Akıllı plan, sesle ekleme, odak modu ve konu hafızası günlük yükünü azaltır.")
    )
    val p=pages[page]
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){Column(Modifier.fillMaxSize().padding(28.dp).navigationBarsPadding(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Surface(shape=RoundedCornerShape(32.dp),color=MaterialTheme.colorScheme.primaryContainer){Icon(p.first,null,Modifier.padding(28.dp).size(64.dp),tint=MaterialTheme.colorScheme.primary)};Spacer(Modifier.height(28.dp));Text(p.second,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text(p.third,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(36.dp));LinearProgressIndicator(progress={(page+1)/pages.size.toFloat()},Modifier.fillMaxWidth());Spacer(Modifier.height(20.dp));Button(onClick={if(page<pages.lastIndex)page++ else {FullRepository.Prefs.setOnboardingSeen(activity,true);activity.finish()}},Modifier.fillMaxWidth()){Text(if(page<pages.lastIndex)"Devam" else "Günümü aç")};if(page<pages.lastIndex)TextButton({FullRepository.Prefs.setOnboardingSeen(activity,true);activity.finish()}){Text("Geç")}}}
}
