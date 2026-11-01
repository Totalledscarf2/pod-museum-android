package com.vicente.podmuseum;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w393dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DeviceLayoutTest {
    @Test public void everyScreenIsMeasuredAndRendersAtPhoneSize() throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();
        File dir=new File("build/reports/screenshots");dir.mkdirs();
        for(Model m:Model.ALL){
            DeviceView view=new DeviceView(a);view.setModel(m,m.colors[0]);
            TextView label=new TextView(a);label.setText(m.touch?"Pantalla táctil\n\n♫  Música     ≡ Playlists\n\n⌕ Buscar      ⚙ Ajustes":"iPod\n\nBuscar Canciones\nMis Playlists\nReproduciendo Ahora\nCambiar Modelo");label.setTextColor(Color.BLACK);label.setTextSize(12);view.screen.addView(label);
            view.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));view.layout(0,0,393,540);
            if(!m.shuffle){
                assertTrue(m.id,view.screen.getMeasuredWidth()>60);
                assertTrue(m.id,view.screen.getMeasuredHeight()>45);
                assertEquals(m.id,view.screen.getMeasuredHeight(),view.screen.getHeight());
                if(m.family.startsWith("touch"))assertTrue(m.id,view.screen.getHeight()>300);
            }else assertEquals(View.GONE,view.screen.getVisibility());
            Bitmap bitmap=Bitmap.createBitmap(393,540,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(0xff101a20);view.draw(canvas);
            try(FileOutputStream out=new FileOutputStream(new File(dir,m.id+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        }
        a.finish();
    }
    @Test public void renderActualHomeScreens() throws Exception {
        for(String id:new String[]{"ipod1","ipod3","classic6","nano3","nano6","nano7","shuffle3","touch1","touch7"}){
            RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().putString("model",id).remove("color").commit();
            org.robolectric.android.controller.ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
            MainActivity activity=controller.get();View decor=activity.getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(852,View.MeasureSpec.EXACTLY));decor.layout(0,0,393,852);
            Bitmap bitmap=Bitmap.createBitmap(393,852,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(bitmap));
            File dir=new File("build/reports/screenshots");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,"ui-"+id+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
            controller.pause().stop().destroy();
        }
    }
    @Test public void centerAndWheelGenerateDifferentActions(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();DeviceView v=new DeviceView(a);v.setModel(Model.find("classic6"),"Plata");
        final String[] called={""};final int[] steps={0};v.setListener(new DeviceView.Listener(){public void action(String s){called[0]=s;}public void step(int d){steps[0]+=d;}});
        v.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));v.layout(0,0,393,540);
        float width=(540-24)*61.8f/103.5f,x=393/2f,y=12+(540-24)*.72f,r=width*.35f;
        v.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,x,y,0));v.onTouchEvent(MotionEvent.obtain(0,20,MotionEvent.ACTION_UP,x,y,0));assertEquals("select",called[0]);
        v.onTouchEvent(MotionEvent.obtain(50,50,MotionEvent.ACTION_DOWN,x+r*.75f,y,0));
        for(int i=1;i<=12;i++){double theta=i*Math.PI/24;v.onTouchEvent(MotionEvent.obtain(50,50+i*10,MotionEvent.ACTION_MOVE,x+(float)Math.cos(theta)*r*.75f,y+(float)Math.sin(theta)*r*.75f,0));}
        assertTrue(steps[0]>0);a.finish();
    }
}
