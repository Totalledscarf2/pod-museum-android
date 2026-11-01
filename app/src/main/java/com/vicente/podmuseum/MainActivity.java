package com.vicente.podmuseum;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity implements DeviceView.Listener {
    private static final int BG=0xff101a20,WHITE=0xffedf3f4,MUTED=0xff9bacb4,MINT=0xff75dfb4;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private final ExecutorService images=Executors.newSingleThreadExecutor();
    private SpotifyClient spotify;
    private android.content.SharedPreferences prefs;
    private DeviceView device;
    private TextView modelLabel,status,footer,screenTitle;
    private LinearLayout root,screenContent,menuList;
    private ScrollView menuScroll;
    private AlertDialog shuffleDialog;
    private ImageView albumView;
    private SeekBar seek;
    private TextView timer;
    private Model model;
    private String color,deviceId="",deviceName="",mode="home",pageTitle="iPod",query="",playlistUri="",playlistId="";
    private final List<Entry> entries=new ArrayList<>();
    private final Deque<Page> history=new ArrayDeque<>();
    private final List<TextView> rowViews=new ArrayList<>();
    private int selection=0,nextOffset=-1,generation=0,iconPage=0;
    private boolean playing=false,active=false,pollPending=false,requestPending=false;
    private long progress=0,duration=0,progressAt=0;
    private JSONObject currentTrack;
    private Bitmap artwork;
    private String artUrl="",title="Tu música, otra época.",artist="Conecta Spotify en Ajustes";
    private final Runnable poll=new Runnable(){@Override public void run(){if(!active)return;sync(false);ui.postDelayed(this,10000);}};
    private final Runnable clock=new Runnable(){@Override public void run(){if(!active)return;updatePosition();ui.postDelayed(this,1000);}};
    interface Job {void run() throws Exception;}
    static final class Entry {
        final String title,subtitle,action;final JSONObject data;
        Entry(String title,String subtitle,String action,JSONObject data){this.title=title;this.subtitle=subtitle;this.action=action;this.data=data;}
    }
    static final class Page {
        String mode,title,query,playlistUri,playlistId;int selected,next;List<Entry> entries;
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        spotify=new SpotifyClient(this,prefs.getString("client",""));
        model=Model.find(prefs.getString("model","classic6"));color=prefs.getString("color",model.colors[0]);
        if(!Arrays.asList(model.colors).contains(color))color=model.colors[0];
        deviceId=prefs.getString("deviceId","");deviceName=prefs.getString("deviceName","");
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        createLayout();home();handleIntent(getIntent());
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String label,Runnable click){Button b=new Button(this);b.setText(label);b.setTextColor(WHITE);b.setTextSize(12);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(44));b.setPadding(dp(8),0,dp(8),0);b.setBackground(rounded(0xff22343f,12));b.setOnClickListener(v->click.run());return b;}
    private void createLayout(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(16),0,dp(16),dp(10));setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(dp(16),insets.getSystemWindowInsetTop(),dp(16),Math.max(dp(10),insets.getSystemWindowInsetBottom()));return insets.consumeSystemWindowInsets();});
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top,new LinearLayout.LayoutParams(-1,dp(62)));
        TextView brand=text("Pod Museum",21,WHITE);brand.setTypeface(null,Typeface.BOLD);top.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
        top.addView(button("Modelos",this::models),new LinearLayout.LayoutParams(dp(78),dp(42)));
        Button settings=button("⚙",this::settings);settings.setContentDescription("Ajustes de Spotify y apariencia");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(46),dp(42));sp.leftMargin=dp(8);top.addView(settings,sp);
        status=text("ANDROID · SPOTIFY CONTROLLER",10,MINT);status.setLetterSpacing(.12f);root.addView(status,new LinearLayout.LayoutParams(-1,dp(20)));
        device=new DeviceView(this);device.setListener(this);device.setModel(model,color);root.addView(device,new LinearLayout.LayoutParams(-1,0,1));
        modelLabel=text(model.name+"  ·  "+model.year,12,WHITE);modelLabel.setGravity(Gravity.CENTER);modelLabel.setMaxLines(2);root.addView(modelLabel,new LinearLayout.LayoutParams(-1,dp(38)));
        modelLabel.setOnClickListener(v->appearance());modelLabel.setContentDescription("Cambiar color o ver reverso");
        footer=text(title,12,MUTED);footer.setSingleLine(true);footer.setEllipsize(android.text.TextUtils.TruncateAt.END);footer.setGravity(Gravity.CENTER);root.addView(footer,new LinearLayout.LayoutParams(-1,dp(32)));footer.setOnClickListener(v->now());
        LinearLayout transport=new LinearLayout(this);transport.setGravity(Gravity.CENTER);root.addView(transport,new LinearLayout.LayoutParams(-1,dp(48)));
        String[] labels={"Menú","◀◀","▶Ⅱ","▶▶","●"};String[] actions={"back","previous","toggle","next","select"};
        String[] descriptions={"Volver al menú","Canción anterior","Reproducir o pausar","Canción siguiente","Seleccionar"};
        for(int i=0;i<labels.length;i++){final String a=actions[i];Button b=button(labels[i],()->action(a));b.setContentDescription(descriptions[i]);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(3),0,dp(3),0);transport.addView(b,p);}
    }
    private void background(Job job){network.execute(()->{try{job.run();}catch(Exception e){post(()->message(e.getMessage()));}});}
    private void post(Runnable runnable){ui.post(()->{if(!isDestroyed()&&!isFinishing())runnable.run();});}
    private void message(String value){new AlertDialog.Builder(this).setTitle("Pod Museum").setMessage(value==null?"No se pudo completar la operación.":value).setPositiveButton("Entendido",null).show();}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();active=true;ui.removeCallbacks(poll);ui.removeCallbacks(clock);ui.post(poll);ui.post(clock);}
    @Override protected void onPause(){active=false;ui.removeCallbacks(poll);ui.removeCallbacks(clock);super.onPause();}
    @Override protected void onDestroy(){super.onDestroy();network.shutdownNow();images.shutdownNow();ui.removeCallbacksAndMessages(null);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    private void handleIntent(Intent intent){Uri uri=intent.getData();if(uri==null)return;intent.setData(null);background(()->{spotify.callback(uri);JSONObject me=spotify.api("/me","GET",null);post(()->{status.setText("SPOTIFY · "+me.optString("display_name","Conectado"));toast("Spotify conectado. Abre Spotify y selecciona un dispositivo.");devices();});});}
    private void login(){background(()->{Uri auth=spotify.authorize();post(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,auth));}catch(ActivityNotFoundException e){message("Instala un navegador para iniciar sesión.");}});});}
    private void settings(){
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(6),dp(22),dp(12));
        TextView help=text("Conecta tu aplicación de Spotify Developer. No se necesita Client Secret.",13,WHITE);help.setPadding(0,0,0,dp(12));content.addView(help);
        EditText client=new EditText(this);client.setText(prefs.getString("client",""));client.setHint("Spotify Client ID");client.setTextSize(13);client.setSingleLine(true);client.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);content.addView(client);
        TextView redirect=text("Redirect URI exacta:\n"+SpotifyClient.REDIRECT,12,MINT);redirect.setTextIsSelectable(true);redirect.setPadding(0,dp(12),0,dp(12));content.addView(redirect);
        TextView note=text("Premium para controlar la reproducción mediante Web API. El sonido lo reproduce la app Spotify o un dispositivo Connect. Esta app no descarga música ni transmite audio por su cuenta.",12,MUTED);content.addView(note);
        content.addView(button("Seleccionar dispositivo de Spotify",this::devices));
        content.addView(button("Abrir Spotify",this::openSpotify));
        content.addView(button("Color y reverso",this::appearance));
        content.addView(button("Cerrar sesión",()->{background(()->{spotify.logout();post(()->{currentTrack=null;playing=false;title="Tu música, otra época.";artist="Conecta Spotify en Ajustes";status.setText("SIN CONECTAR");footer.setText(title);home();toast("Sesión local cerrada");});});}));
        new AlertDialog.Builder(this).setTitle("Spotify y apariencia").setView(content).setNegativeButton("Cerrar",null)
            .setPositiveButton("Guardar y conectar",(d,w)->{
                String value=client.getText().toString().trim();
                if(!value.matches("(?i)[a-f0-9]{32}")){message("El Client ID debe tener 32 caracteres hexadecimales.");return;}
                boolean changed=!value.equals(prefs.getString("client",""));prefs.edit().putString("client",value).apply();
                background(()->{if(changed)spotify.logout();spotify.setClientId(value);post(this::login);});
            }).show();
    }
    private void openSpotify(){
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if(launch!=null)startActivity(launch);
        else{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://open.spotify.com")));}catch(ActivityNotFoundException e){message("Instala Spotify y un navegador.");}}
    }
    private void models(){
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        EditText filter=new EditText(this);filter.setHint("Filtrar: nano, touch, 2007…");filter.setSingleLine(true);layout.addView(filter);
        ListView list=new ListView(this);layout.addView(list,new LinearLayout.LayoutParams(-1,dp(390)));
        ArrayList<Model> shown=new ArrayList<>(Model.ALL);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);list.setAdapter(adapter);
        Runnable update=()->{shown.clear();adapter.clear();String q=filter.getText().toString().toLowerCase(Locale.ROOT);for(Model m:Model.ALL)if((m.name+" "+m.year).toLowerCase(Locale.ROOT).contains(q)){shown.add(m);adapter.add(m.name+"\n"+m.year);}adapter.notifyDataSetChanged();};update.run();
        filter.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){update.run();}public void afterTextChanged(android.text.Editable e){}});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Colección · "+Model.ALL.size()+" variantes").setView(layout).setNegativeButton("Cerrar",null).create();
        list.setOnItemClickListener((p,v,pos,id)->{model=shown.get(pos);color=model.colors[0];persistModel();dialog.dismiss();home();});dialog.show();
    }
    private void persistModel(){prefs.edit().putString("model",model.id).putString("color",color).apply();device.setModel(model,color);modelLabel.setText(model.name+" · "+model.year);render();}
    private void appearance(){
        new AlertDialog.Builder(this).setTitle(model.name).setMessage(model.year+"\n\n"+model.detail+"\n\nRecreación gráfica; los grabados especiales se representan con texto.")
            .setPositiveButton("Cambiar color",(d,w)->new AlertDialog.Builder(this).setTitle("Acabado").setItems(model.colors,(dd,which)->{color=model.colors[which];persistModel();}).show())
            .setNeutralButton("Girar",(d,w)->device.flip()).setNegativeButton("Cerrar",null).show();
    }
    private void devices(){background(()->{
        JSONArray devices=spotify.api("/me/player/devices","GET",null).optJSONArray("devices");
        post(()->{
            if(devices==null||devices.length()==0){message("No hay dispositivos de Spotify disponibles. Abre Spotify, reproduce algo y vuelve a este selector.");return;}
            String[] names=new String[devices.length()];for(int i=0;i<names.length;i++){JSONObject d=devices.optJSONObject(i);names[i]=d.optString("name")+(d.optBoolean("is_active")?" · activo":"");}
            new AlertDialog.Builder(this).setTitle("Reproducir en…").setItems(names,(dialog,pos)->{
                JSONObject d=devices.optJSONObject(pos);String id=d.optString("id");
                if(id.isEmpty()||id.equals("null")||d.optBoolean("is_restricted")){message("Este dispositivo no permite control desde la API.");return;}
                background(()->{spotify.api("/me/player","PUT",new JSONObject().put("device_ids",new JSONArray().put(id)).put("play",false));post(()->{deviceId=id;deviceName=d.optString("name");prefs.edit().putString("deviceId",deviceId).putString("deviceName",deviceName).apply();toast("Dispositivo: "+deviceName);sync(false);});});
            }).setNegativeButton("Cerrar",null).show();
        });
    });}
    private String target(){return deviceId.isEmpty()?"":"?device_id="+Uri.encode(deviceId);}
    private void transport(String command){background(()->{
        if(command.equals("toggle")){
            JSONObject state=spotify.api("/me/player","GET",null);
            boolean isPlaying=state.optBoolean("is_playing");
            spotify.api("/me/player/"+(isPlaying?"pause":"play")+target(),"PUT",null);
        }else spotify.api("/me/player/"+command+target(),"POST",null);
        post(()->{ui.postDelayed(()->sync(true),500);});
    });}
    private void play(JSONObject body){background(()->{spotify.api("/me/player/play"+target(),"PUT",body);post(()->{now();ui.postDelayed(()->sync(true),600);});});}
    @Override public void action(String action){
        switch(action){
            case "back":if(model.shuffle&&mode.equals("home"))shuffleMenu();else back();break;case "home":home();break;case "select":if(model.shuffle&&mode.equals("home"))shuffleMenu();else if(!entries.isEmpty())select(selection);else if(mode.equals("now"))transport("toggle");break;
            case "previous":case "next":case "toggle":transport(action);break;
            case "volumeUp":case "volumeDown":volume(action.equals("volumeUp")?1:-1);break;
        }
    }
    private void volume(int direction){
        // Android's media volume controls the Spotify app on this phone.
        if(deviceName.isEmpty()||deviceId.isEmpty()){
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,direction>0?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);return;
        }
        background(()->{
            JSONObject state=spotify.api("/me/player","GET",null),d=state.optJSONObject("device");
            if(d==null)throw new Exception("Abre Spotify para activar un dispositivo.");
            if(!deviceId.equals(d.optString("id")))throw new Exception("El dispositivo elegido no está activo. Selecciónalo otra vez en Ajustes.");
            if(!d.optBoolean("supports_volume",true))throw new Exception("Ajusta el volumen con los botones físicos del dispositivo de reproducción.");
            int value=Math.max(0,Math.min(100,d.optInt("volume_percent",50)+direction*5));
            spotify.api("/me/player/volume?volume_percent="+value+"&device_id="+Uri.encode(deviceId),"PUT",null);
        });
    }
    @Override public void step(int direction){
        if(entries.isEmpty())return;selection=(selection+direction+entries.size())%entries.size();highlight();
    }
    private void highlight(){
        for(int i=0;i<rowViews.size();i++){TextView row=rowViews.get(i);row.setBackgroundColor(i==selection?(model.mono?0xff24382c:0xff287dc2):Color.TRANSPARENT);row.setTextColor(i==selection?Color.WHITE:0xff152630);}
        if(selection<rowViews.size()&&menuScroll!=null){View row=rowViews.get(selection);menuScroll.smoothScrollTo(0,Math.max(0,row.getTop()-menuScroll.getHeight()/2));}
    }
    private void savePage(){Page p=new Page();p.mode=mode;p.title=pageTitle;p.query=query;p.playlistUri=playlistUri;p.playlistId=playlistId;p.selected=selection;p.next=nextOffset;p.entries=new ArrayList<>(entries);history.push(p);}
    private void back(){if(history.isEmpty()){home();return;}Page p=history.pop();mode=p.mode;pageTitle=p.title;query=p.query;playlistUri=p.playlistUri;playlistId=p.playlistId;selection=p.selected;nextOffset=p.next;entries.clear();entries.addAll(p.entries);generation++;render();}
    private void home(){iconPage=0;history.clear();mode="home";pageTitle="iPod";selection=0;entries.clear();generation++;
        entries.add(new Entry("Buscar Canciones","Canciones y artistas","search",null));
        entries.add(new Entry("Mis Playlists","Tu biblioteca de Spotify","playlists",null));
        entries.add(new Entry("Reproduciendo","Carátula y controles","now",null));
        entries.add(new Entry("Cambiar Modelo","Todas las generaciones","models",null));
        entries.add(new Entry("Spotify Connect","Dispositivo de reproducción","devices",null));
        entries.add(new Entry("Ajustes","Cuenta, color y reverso","settings",null));render();
    }
    private void begin(String newMode,String heading){savePage();mode=newMode;pageTitle=heading;selection=0;entries.clear();generation++;render();}
    private void select(int index){
        if(index<0||index>=entries.size())return;Entry entry=entries.get(index);
        try{switch(entry.action){
            case "search":searchDialog();break;case "playlists":begin("playlists","Mis Playlists");loadPlaylists(0);break;
            case "now":now();break;case "models":models();break;case "devices":devices();break;case "settings":settings();break;
            case "playlist":playlistUri=entry.data.optString("uri");playlistId=entry.data.getString("id");begin("playlist",entry.title);loadSongs(0);break;
            case "track":play(new JSONObject().put("uris",new JSONArray().put(entry.data.getString("uri"))));break;
            case "context":play(new JSONObject().put("context_uri",playlistUri));break;
            case "contextTrack":play(new JSONObject().put("context_uri",playlistUri).put("offset",new JSONObject().put("position",entry.data.getInt("offset"))));break;
            case "moreSearch":searchPage(nextOffset);break;case "morePlaylists":loadPlaylists(nextOffset);break;case "moreSongs":loadSongs(nextOffset);break;
        }}catch(Exception e){message(e.getMessage());}
    }
    private void searchDialog(){
        EditText input=new EditText(this);input.setHint("Canción o artista");input.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Buscar en Spotify").setView(input).setPositiveButton("Buscar",(d,w)->{
            String q=input.getText().toString().trim();if(q.isEmpty())return;begin("search","Buscar: "+q);query=q;searchPage(0);
        }).setNegativeButton("Cancelar",null).show();
    }
    private void removeMore(){entries.removeIf(e->e.action.startsWith("more"));}
    private void searchPage(int offset){
        if(requestPending)return;requestPending=true;render();int version=generation;String q=query;
        background(()->{try{
            JSONObject data=spotify.api("/search?q="+Uri.encode(q)+"&type=track&limit=10&offset="+offset,"GET",null).getJSONObject("tracks");
            post(()->{if(version!=generation)return;removeMore();JSONArray items=data.optJSONArray("items");if(items!=null)for(int i=0;i<items.length();i++){JSONObject t=items.optJSONObject(i);if(t!=null&&!t.optString("uri").isEmpty())entries.add(new Entry(t.optString("name"),artists(t),"track",t));}
                nextOffset=data.isNull("next")?-1:offset+10;if(nextOffset>=0)entries.add(new Entry("Más resultados","","moreSearch",null));render();});
        }finally{post(()->{requestPending=false;if(version==generation&&entries.isEmpty())render();});}});
    }
    private void loadPlaylists(int offset){
        if(requestPending)return;requestPending=true;render();int version=generation;
        background(()->{try{JSONObject data=spotify.api("/me/playlists?limit=50&offset="+offset,"GET",null);
            post(()->{if(version!=generation)return;removeMore();JSONArray items=data.optJSONArray("items");if(items!=null)for(int i=0;i<items.length();i++){JSONObject p=items.optJSONObject(i);if(p!=null)entries.add(new Entry(p.optString("name"),"Playlist de Spotify","playlist",p));}
                nextOffset=data.isNull("next")?-1:offset+50;if(nextOffset>=0)entries.add(new Entry("Más playlists","","morePlaylists",null));render();});
        }finally{post(()->{requestPending=false;if(version==generation&&entries.isEmpty())render();});}});
    }
    private void loadSongs(int offset){
        if(requestPending)return;requestPending=true;int version=generation;String id=playlistId;
        if(offset==0)entries.add(new Entry("▶ Reproducir playlist","Spotify Connect","context",null));render();
        background(()->{try{
            JSONObject data;
            try{data=spotify.api("/playlists/"+Uri.encode(id)+"/items?limit=50&offset="+offset,"GET",null);}
            catch(SpotifyClient.ApiError e){if(e.status!=404)throw e;data=spotify.api("/playlists/"+Uri.encode(id)+"/tracks?limit=50&offset="+offset,"GET",null);}
            final JSONObject result=data;
            post(()->{if(version!=generation)return;removeMore();JSONArray items=result.optJSONArray("items");if(items!=null)for(int i=0;i<items.length();i++){
                JSONObject wrapper=items.optJSONObject(i);if(wrapper==null)continue;JSONObject t=wrapper.optJSONObject("item");if(t==null)t=wrapper.optJSONObject("track");
                if(t==null||t.optBoolean("is_local")||!t.optString("type","track").equals("track"))continue;
                try{JSONObject payload=new JSONObject().put("offset",offset+i);entries.add(new Entry(t.optString("name"),artists(t),"contextTrack",payload));}catch(JSONException ignored){}
            }
                nextOffset=result.isNull("next")?-1:offset+50;if(nextOffset>=0)entries.add(new Entry("Más canciones","","moreSongs",null));render();});
        }catch(SpotifyClient.ApiError e){if(e.status==403)post(()->{if(version==generation)toast("Spotify restringe este listado. Puedes intentar reproducir la playlist completa.");});else throw e;}
        finally{post(()->{requestPending=false;if(version==generation&&entries.isEmpty())render();});}});
    }
    private static String artists(JSONObject t){JSONArray a=t.optJSONArray("artists");StringBuilder b=new StringBuilder();if(a!=null)for(int i=0;i<a.length();i++){if(i>0)b.append(", ");b.append(a.optJSONObject(i).optString("name"));}return b.toString();}
    private void now(){if(!mode.equals("now"))begin("now","Reproduciendo");else render();}
    private void render(){
        albumView=null;seek=null;timer=null;rowViews.clear();menuScroll=null;menuList=null;
        LinearLayout screen=device.screen;screen.removeAllViews();screen.setPadding(0,0,0,0);
        screen.setBackgroundColor(model.mono?0xffbbcaad:0xffe7edef);
        if(model.shuffle){
            footer.setText(currentTrack==null?"Sin pantalla · Menú abre el panel auxiliar":(playing?"▶ ":"Ⅱ ")+title);
            if(!mode.equals("home")&&!mode.equals("now")&&!entries.isEmpty()){
                if(shuffleDialog!=null)shuffleDialog.dismiss();
                String[] labels=new String[entries.size()];for(int i=0;i<labels.length;i++)labels[i]=entries.get(i).title;
                shuffleDialog=new AlertDialog.Builder(this).setTitle(pageTitle+" · panel auxiliar")
                    .setItems(labels,(dialog,index)->select(index)).setNegativeButton("Menú",(dialog,which)->home()).create();
                shuffleDialog.show();
            }
            return;
        }
        boolean icons=model.touch&&mode.equals("home");
        screenTitle=text(icons?"♫  iPod                         ▰":pageTitle,model.touch?10:12,0xff1b2b31);screenTitle.setSingleLine(true);screenTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);screenTitle.setPadding(dp(7),0,dp(7),0);
        screenTitle.setTypeface(null,Typeface.BOLD);screenTitle.setBackgroundColor(model.mono?0xffa8b99e:0xffcedce2);screen.addView(screenTitle,new LinearLayout.LayoutParams(-1,dp(model.family.equals("nano6")?18:23)));
        screenContent=new LinearLayout(this);screenContent.setOrientation(LinearLayout.VERTICAL);screen.addView(screenContent,new LinearLayout.LayoutParams(-1,0,1));
        if(icons)renderIcons();else if(mode.equals("now"))renderNow();else renderList();
        footer.setText(currentTrack==null?"Spotify · "+(deviceName.isEmpty()?"elige un dispositivo":deviceName):(playing?"▶ ":"Ⅱ ")+title+" — "+artist);
    }
    private void renderIcons(){
        boolean nano=model.family.equals("nano6")||model.family.equals("nano7");
        GradientDrawable wallpaper=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            model.family.equals("touch1")?new int[]{0xff353b44,0xff131719}:new int[]{0xff145f77,0xff53a6b3,0xffdfc9b2});screenContent.setBackground(wallpaper);
        int columns=nano?2:4;
        boolean tiny=model.family.equals("nano6");
        int first=tiny?iconPage*4:0,last=Math.min(entries.size(),tiny?first+4:entries.size());
        String[] glyphs={"♫","≡","▶","◉","⌁","⚙"};int[] shades={0xffed8a25,0xffb94e91,0xffe4584e,0xff7279c5,0xff55b888,0xff7e8993};
        for(int start=first;start<last;start+=columns){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);screenContent.addView(row,nano?new LinearLayout.LayoutParams(-1,0,1):new LinearLayout.LayoutParams(-1,dp(74)));
            for(int j=0;j<columns;j++){int index=start+j;if(index>=last)break;Entry e=entries.get(index);LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);
                TextView icon=text(glyphs[index],nano?22:27,Color.WHITE);icon.setGravity(Gravity.CENTER);icon.setBackground(rounded(shades[index],model.family.equals("nano7")?18:nano?13:10));
                int size=dp(nano?36:42);cell.addView(icon,new LinearLayout.LayoutParams(size,size));TextView label=text(new String[]{"Buscar","Playlists","Música","Modelos","Connect","Ajustes"}[index],9,Color.WHITE);label.setGravity(Gravity.CENTER);cell.addView(label,new LinearLayout.LayoutParams(-1,dp(20)));
                cell.setContentDescription(e.title);cell.setOnClickListener(v->select(index));row.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
            }
        }
        if(tiny){TextView pages=text(iconPage==0?"● ○":"○ ●",8,Color.WHITE);pages.setGravity(Gravity.CENTER);pages.setContentDescription("Cambiar página de iconos");pages.setOnClickListener(v->{iconPage=1-iconPage;render();});screenContent.addView(pages,new LinearLayout.LayoutParams(-1,dp(14)));}
        if(!nano){View spacer=new View(this);screenContent.addView(spacer,new LinearLayout.LayoutParams(-1,0,1));TextView dock=text("Música     ·     Spotify",11,Color.WHITE);dock.setGravity(Gravity.CENTER);dock.setBackgroundColor(0x44334050);screenContent.addView(dock,new LinearLayout.LayoutParams(-1,dp(30)));dock.setOnClickListener(v->now());}
    }
    private void renderList(){
        menuScroll=new ScrollView(this);menuScroll.setFillViewport(true);screenContent.addView(menuScroll,new LinearLayout.LayoutParams(-1,-1));menuList=new LinearLayout(this);menuList.setOrientation(LinearLayout.VERTICAL);menuScroll.addView(menuList);
        if(entries.isEmpty()){TextView empty=text(requestPending?"Cargando…":"Sin resultados. MENU para volver.",12,0xff263b42);empty.setPadding(dp(9),dp(12),dp(9),dp(12));menuList.addView(empty);return;}
        for(int i=0;i<entries.size();i++){Entry entry=entries.get(i);final int index=i;
            TextView row=text(entry.title+"  ›",model.touch?13:12,0xff152630);row.setMaxLines(2);row.setEllipsize(android.text.TextUtils.TruncateAt.END);row.setPadding(dp(8),dp(7),dp(6),dp(7));row.setMinHeight(dp(model.touch?42:30));
            if(model.mono)row.setTypeface(Typeface.MONOSPACE);row.setOnClickListener(v->{selection=index;select(index);});menuList.addView(row,new LinearLayout.LayoutParams(-1,-2));rowViews.add(row);
        }
        selection=Math.min(selection,entries.size()-1);highlight();
    }
    private void renderNow(){
        if(currentTrack==null){TextView blank=text("Selecciona una canción.\n\nLa reproducción ocurre en Spotify o en tu dispositivo Connect.",12,0xff253a43);blank.setPadding(dp(12),dp(14),dp(12),dp(14));screenContent.addView(blank);return;}
        boolean large=model.family.startsWith("touch")||model.family.equals("nano7");
        albumView=new ImageView(this);albumView.setScaleType(ImageView.ScaleType.FIT_CENTER);albumView.setContentDescription("Carátula de "+title);albumView.setBackgroundColor(0xffd4dde1);
        screenContent.addView(albumView,new LinearLayout.LayoutParams(-1,0,large?1:0.75f));if(artwork!=null)albumView.setImageBitmap(artwork);
        TextView titleView=text(title,large?15:11,0xff152b34);titleView.setTypeface(null,Typeface.BOLD);titleView.setSingleLine(true);titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);titleView.setPadding(dp(8),0,dp(8),0);screenContent.addView(titleView,new LinearLayout.LayoutParams(-1,dp(large?28:19)));
        TextView artistView=text(artist,large?11:9,0xff3f5964);artistView.setSingleLine(true);artistView.setPadding(dp(8),0,dp(8),0);screenContent.addView(artistView,new LinearLayout.LayoutParams(-1,dp(large?21:16)));
        if(large){
            seek=new SeekBar(this);seek.setMax(1000);screenContent.addView(seek,new LinearLayout.LayoutParams(-1,dp(30)));
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){}public void onStopTrackingTouch(SeekBar s){long pos=duration*s.getProgress()/1000;background(()->{spotify.api("/me/player/seek?position_ms="+pos+(deviceId.isEmpty()?"":"&device_id="+Uri.encode(deviceId)),"PUT",null);post(()->{progress=pos;progressAt=SystemClock.elapsedRealtime();});});}});
        }
        timer=text("",large?10:9,0xff36515d);timer.setGravity(Gravity.CENTER);screenContent.addView(timer,new LinearLayout.LayoutParams(-1,dp(17)));
        if(large){LinearLayout controls=new LinearLayout(this);String[] t={"◀◀",playing?"Ⅱ":"▶","▶▶"};String[] a={"previous","toggle","next"};for(int i=0;i<3;i++){final String action=a[i];Button b=button(t[i],()->transport(action));controls.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));}screenContent.addView(controls);}
        TextView link=text("Abrir en Spotify ↗",10,0xff27617d);link.setGravity(Gravity.CENTER);screenContent.addView(link,new LinearLayout.LayoutParams(-1,dp(22)));link.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(currentTrack.optJSONObject("external_urls")==null?"https://open.spotify.com":currentTrack.optJSONObject("external_urls").optString("spotify","https://open.spotify.com"))));}catch(ActivityNotFoundException ignored){}});
        updatePosition();
    }
    private void updatePosition(){long pos=Math.min(duration,progress+(playing?SystemClock.elapsedRealtime()-progressAt:0));if(timer!=null)timer.setText(format(pos)+" / "+format(duration));if(seek!=null&&!seek.isPressed())seek.setProgress(duration==0?0:(int)(pos*1000/duration));}
    private String format(long ms){long s=Math.max(0,ms/1000);return s/60+":"+String.format(Locale.ROOT,"%02d",s%60);}
    private void sync(boolean report){
        if(pollPending)return;pollPending=true;
        network.execute(()->{try{
            if(!spotify.connected())return;
            JSONObject state=spotify.api("/me/player","GET",null);
            post(()->{
                if(!state.has("item"))return;
                JSONObject item=state.optJSONObject("item");if(item==null)return;
                boolean changed=currentTrack==null||!item.optString("id").equals(currentTrack.optString("id"));boolean pauseChanged=playing!=state.optBoolean("is_playing");
                currentTrack=item;playing=state.optBoolean("is_playing");progress=state.optLong("progress_ms");progressAt=SystemClock.elapsedRealtime();duration=item.optLong("duration_ms");title=item.optString("name");artist=artists(item);
                JSONObject d=state.optJSONObject("device");if(deviceId.isEmpty()&&d!=null){deviceId=d.optString("id","");deviceName=d.optString("name","");}
                status.setText("SPOTIFY · "+(d==null?"Conectado":d.optString("name","Conectado")));
                footer.setText((playing?"▶ ":"Ⅱ ")+title+" — "+artist);
                if(changed){artwork=null;loadArtwork(item);}
                if(mode.equals("now")&&(changed||pauseChanged))render();else updatePosition();
            });
        }catch(Exception e){if(report)post(()->message(e.getMessage()));else post(()->status.setText("SPOTIFY · revisa la conexión en Ajustes"));}
        finally{post(()->pollPending=false);}});
    }
    private void loadArtwork(JSONObject item){
        JSONObject album=item.optJSONObject("album");JSONArray list=album==null?null:album.optJSONArray("images");
        if(list==null||list.length()==0){artUrl="";return;}String url=list.optJSONObject(list.length()>1?1:0).optString("url");artUrl=url;
        images.execute(()->{HttpURLConnection c=null;try{
            if(!url.startsWith("https://"))return;c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);
            Bitmap bitmap;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("Artwork too large");out.write(bytes,0,n);}byte[] data=out.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,bounds);BitmapFactory.Options options=new BitmapFactory.Options();while(bounds.outWidth/Math.max(1,options.inSampleSize)>1024)options.inSampleSize=Math.max(1,options.inSampleSize)*2;bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);
            }
            post(()->{if(!url.equals(artUrl))return;artwork=bitmap;if(albumView!=null)albumView.setImageBitmap(bitmap);});
        }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}});
    }
    private void shuffleMenu(){String[] options={"Buscar Canciones","Mis Playlists","Reproduciendo","Cambiar Modelo","Spotify Connect","Ajustes"};new AlertDialog.Builder(this).setTitle("Panel auxiliar · shuffle sin pantalla").setItems(options,(d,pos)->{
        switch(pos){case 0:searchDialog();break;case 1:begin("playlists","Mis Playlists");loadPlaylists(0);break;case 2:message(title+"\n"+artist);break;case 3:models();break;case 4:devices();break;case 5:settings();break;}
    }).show();}
    @Override public void onBackPressed(){if(!mode.equals("home"))back();else super.onBackPressed();}
}
