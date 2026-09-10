package com.emre.gunumplanner

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SeriesEditActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val id=intent.getLongExtra("item_id",0L);val db=Db(this);FullRepository.ensureSchema(db)
        val item=db.getItem(id);if(item==null){finish();return}
        FullRepository.ensureSeries(db,id)
        setContent{V2Theme{SeriesEditor(this,db,item)}}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SeriesEditor(activity:Activity,db:Db,item:Db.Item){
    var title by remember{mutableStateOf(item.title)};var body by remember{mutableStateOf(item.body)};var duration by remember{mutableIntStateOf(if(item.durationMinutes>0)item.durationMinutes else 30)};var priority by remember{mutableIntStateOf(item.priority)};var recurrence by remember{mutableStateOf(item.recurrence)};var futureOnly by remember{mutableStateOf(true)}
    Scaffold(topBar={TopAppBar(title={Column{Text("Tekrar serisi",fontWeight=FontWeight.Bold);Text(item.title,style=MaterialTheme.typography.bodySmall)}},navigationIcon={IconButton({activity.finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}})}){p->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Repeat,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(10.dp));Text("Bu ekran serinin açık oluşumlarını birlikte yönetir. Tamamlanmış geçmiş kayıtlar değişmeden kalır.")}}
            OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Görev")},singleLine=true)
            OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Açıklama")},minLines=3)
            Text("Süre",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf(15,30,45,60,90,120)){m->FilterChip(duration==m,{duration=m},{Text("$m dk")})}}
            Text("Öncelik",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(0 to "Acil",1 to "Önemli",2 to "Normal").forEach{(x,n)->FilterChip(priority==x,{priority=x},{Text(n)})}}
            Text("Tekrar",fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("" to "Yok",NaturalLanguageParser.DAILY to "Her gün",NaturalLanguageParser.WEEKDAYS to "Hafta içi",NaturalLanguageParser.WEEKLY to "Her hafta",NaturalLanguageParser.MONTHLY to "Her ay")){(v,n)->FilterChip(recurrence==v,{recurrence=v},{Text(n)})}}
            Text("Kapsam",fontWeight=FontWeight.Bold)
            Surface(onClick={futureOnly=true},shape=RoundedCornerShape(18.dp),color=if(futureOnly)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.padding(14.dp)){Text("Bu ve sonraki açık görevler",fontWeight=FontWeight.SemiBold);Text("Geçmişe dokunmaz",style=MaterialTheme.typography.bodySmall)}}
            Surface(onClick={futureOnly=false},shape=RoundedCornerShape(18.dp),color=if(!futureOnly)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant){Column(Modifier.padding(14.dp)){Text("Serideki tüm açık görevler",fontWeight=FontWeight.SemiBold);Text("Tamamlanan kayıtlar yine korunur",style=MaterialTheme.typography.bodySmall)}}
            Button(onClick={FullRepository.updateSeriesOpen(db,item.id,title.trim(),body.trim(),recurrence,duration,priority,futureOnly);activity.finish()},enabled=title.isNotBlank(),modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Save,null);Spacer(Modifier.width(8.dp));Text("Seriyi güncelle")}
        }
    }
}
