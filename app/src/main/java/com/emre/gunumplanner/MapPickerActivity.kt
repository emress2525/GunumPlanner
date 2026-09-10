package com.emre.gunumplanner

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

class MapPickerActivity : ComponentActivity() {
    data class SearchHit(val label:String,val address:String,val lat:Double,val lng:Double)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db=Db(this);FullRepository.ensureSchema(db)
        val startQuery = intent.getStringExtra("query").orEmpty()
        val hasCoord = intent.getBooleanExtra("has_coord", false)
        val startLat = if (hasCoord) intent.getDoubleExtra("lat", 39.0) else 39.0
        val startLng = if (hasCoord) intent.getDoubleExtra("lng", 35.0) else 35.0
        val startRadius = intent.getFloatExtra("radius",200f)

        setContent {
            V2Theme {
                var query by remember { mutableStateOf(startQuery) }
                var selectedLat by remember { mutableStateOf<Double?>(if (hasCoord) startLat else null) }
                var selectedLng by remember { mutableStateOf<Double?>(if (hasCoord) startLng else null) }
                var selectedAddress by remember { mutableStateOf(startQuery) }
                var selectedLabel by remember { mutableStateOf(startQuery.substringBefore(',').trim()) }
                var radius by remember { mutableFloatStateOf(startRadius) }
                var status by remember { mutableStateOf(if (hasCoord) "Konum hazır • pini sürükleyebilir veya haritada başka yere dokunabilirsin" else "Adres ara veya haritada bir noktaya dokun") }
                var searching by remember { mutableStateOf(false) }
                var searchHits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
                var webView by remember { mutableStateOf<WebView?>(null) }
                var mapReady by remember { mutableStateOf(false) }
                val savedPlaces=remember{FullRepository.savedPlaces(db)}

                fun redraw() {
                    val la=selectedLat;val lo=selectedLng
                    if(mapReady&&la!=null&&lo!=null) webView?.evaluateJavascript("window.setSelection($la,$lo,$radius);",null)
                }
                fun moveMap(lat:Double,lng:Double,address:String="",label:String="") {
                    selectedLat=lat;selectedLng=lng
                    if(address.isNotBlank()){selectedAddress=address;query=address}
                    if(label.isNotBlank())selectedLabel=label
                    redraw()
                }
                fun reverseGeocode(lat:Double,lng:Double) {
                    Thread {
                        val resolved=runCatching {@Suppress("DEPRECATION") Geocoder(this@MapPickerActivity,Locale("tr","TR")).getFromLocation(lat,lng,1)?.firstOrNull()?.getAddressLine(0).orEmpty()}.getOrDefault("")
                        runOnUiThread { if(resolved.isNotBlank()){selectedAddress=resolved;selectedLabel=resolved.substringBefore(',').trim();query=resolved};status="Konum seçildi • pini sürükleyerek ince ayar yapabilirsin" }
                    }.start()
                }
                fun choose(hit:SearchHit){moveMap(hit.lat,hit.lng,hit.address,hit.label);searchHits=emptyList();status="Konum seçildi • yarıçapı kontrol et"}
                fun searchAddress(){
                    val text=query.trim();if(text.isBlank()){status="Önce adres veya yer adı yaz";return}
                    searching=true;status="Adres aranıyor…"
                    Thread{
                        val list=runCatching{@Suppress("DEPRECATION") Geocoder(this@MapPickerActivity,Locale("tr","TR")).getFromLocationName(text,5).orEmpty()}.getOrDefault(emptyList())
                        val hits=list.map{a:Address->val ad=a.getAddressLine(0).orEmpty().ifBlank{text};SearchHit(a.featureName?.takeIf{it.isNotBlank()}?:text.substringBefore(','),ad,a.latitude,a.longitude)}
                        runOnUiThread{searching=false;searchHits=hits;if(hits.isEmpty())status="Adres bulunamadı. Daha açık yaz veya haritadan seç." else if(hits.size==1)choose(hits.first()) else status="${hits.size} sonuç bulundu • doğru olanı seç"}
                    }.start()
                }

                Scaffold(
                    topBar={TopAppBar(title={Column{Text("Haritada konum seç",fontWeight=FontWeight.Bold);Text("Ara, pini taşı, yarıçapı gör",style=MaterialTheme.typography.bodySmall)}},navigationIcon={IconButton({finish()}){Icon(Icons.Rounded.ArrowBack,"Geri")}})},
                    bottomBar={Surface(tonalElevation=4.dp){Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf(100f,200f,500f,1000f)){m->FilterChip(selected=radius==m,onClick={radius=m;redraw()},label={Text(if(m>=1000)"1 km" else "${m.toInt()} m")})}}
                        Text(status,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick={val la=selectedLat?:return@Button;val lo=selectedLng?:return@Button;setResult(Activity.RESULT_OK,Intent().putExtra("lat",la).putExtra("lng",lo).putExtra("address",selectedAddress).putExtra("label",selectedLabel).putExtra("radius",radius));finish()},enabled=selectedLat!=null&&selectedLng!=null,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Icon(Icons.Rounded.MyLocation,null);Spacer(Modifier.width(8.dp));Text("Bu konumu kullan")}
                    }}}
                ){padding->
                    Column(Modifier.fillMaxSize().padding(padding),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(query,{query=it},Modifier.weight(1f),label={Text("Adres / yer adı")},placeholder={Text("Örn. Anıtkabir, Ankara")},singleLine=true);FilledIconButton({searchAddress()},enabled=!searching,modifier=Modifier.size(56.dp)){Icon(Icons.Rounded.Search,"Ara")}}
                        if(savedPlaces.isNotEmpty()) LazyRow(Modifier.padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(savedPlaces,key={it.id}){p->AssistChip(onClick={moveMap(p.lat,p.lng,p.address,p.name);radius=p.radius;redraw();status="${p.name} seçildi"},label={Text(p.name,maxLines=1,overflow=TextOverflow.Ellipsis)},leadingIcon={Icon(Icons.Rounded.Place,null,Modifier.size(18.dp))})}}
                        if(searchHits.isNotEmpty()) LazyRow(Modifier.padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(searchHits){h->Surface(onClick={choose(h)},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.widthIn(max=280.dp)){Column(Modifier.padding(12.dp)){Text(h.label,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(h.address,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)}}}}
                        AndroidView(modifier=Modifier.fillMaxWidth().weight(1f),factory={context->WebView(context).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.setSupportZoom(true);settings.builtInZoomControls=true;settings.displayZoomControls=false;webViewClient=object:WebViewClient(){};addJavascriptInterface(object{
                            @JavascriptInterface fun onMapTapped(lat:Double,lng:Double){runOnUiThread{selectedLat=lat;selectedLng=lng;reverseGeocode(lat,lng)}}
                            @JavascriptInterface fun onMapReady(){runOnUiThread{mapReady=true;if(hasCoord)redraw()}}
                        },"Android");webView=this;loadDataWithBaseURL("https://www.openstreetmap.org/",mapHtml(startLat,startLng,hasCoord,startRadius),"text/html","UTF-8",null)}})
                    }
                }
            }
        }
    }

    private fun mapHtml(lat:Double,lng:Double,hasCoord:Boolean,radius:Float):String{
        val zoom=if(hasCoord)16 else 6
        return """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>html,body,#map{width:100%;height:100%;margin:0;padding:0;background:#eef1f7}.leaflet-control-attribution{font-size:10px}</style></head><body><div id="map"></div><script>
const map=L.map('map',{zoomControl:true}).setView([$lat,$lng],$zoom);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);
let marker=null;let circle=null;
function setMarker(lat,lng,r){if(marker===null){marker=L.marker([lat,lng],{draggable:true}).addTo(map);marker.on('drag',function(e){const p=e.target.getLatLng();if(circle)circle.setLatLng(p)});marker.on('dragend',function(e){const p=e.target.getLatLng();Android.onMapTapped(p.lat,p.lng)})}else marker.setLatLng([lat,lng]);if(circle===null)circle=L.circle([lat,lng],{radius:r,color:'#5B5FEF',weight:2,fillColor:'#5B5FEF',fillOpacity:.12}).addTo(map);else{circle.setLatLng([lat,lng]);circle.setRadius(r)}}
window.setSelection=function(lat,lng,r){setMarker(lat,lng,r);map.setView([lat,lng],Math.max(map.getZoom(),16),{animate:true})};
map.on('click',function(e){setMarker(e.latlng.lat,e.latlng.lng,$radius);Android.onMapTapped(e.latlng.lat,e.latlng.lng)});
${if(hasCoord)"setMarker($lat,$lng,$radius);" else ""}
setTimeout(function(){Android.onMapReady()},250);
</script></body></html>
""".trimIndent()
    }
}
