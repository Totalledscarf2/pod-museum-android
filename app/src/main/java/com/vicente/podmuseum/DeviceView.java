package com.vicente.podmuseum;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** A native layout with a measured screen, over an interactive Canvas enclosure. */
public final class DeviceView extends FrameLayout {
    public interface Listener {void action(String action);void step(int direction);}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public final LinearLayout screen;
    private final RectF body=new RectF(),display=new RectF();
    private Model model=Model.find("classic6");
    private String color="Plata";
    private Listener listener;
    private boolean back=false,drag=false;
    private float wheelX,wheelY,wheelRadius,lastAngle,sum,startX,startY;
    private final float density;
    public DeviceView(Context context){
        super(context);density=getResources().getDisplayMetrics().density;
        setWillNotDraw(false);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        screen=new LinearLayout(context);screen.setOrientation(LinearLayout.VERTICAL);
        screen.setClipToOutline(true);addView(screen,new LayoutParams(1,1));
        setFocusable(true);setContentDescription("iPod. Rueda circular para navegar, centro para seleccionar. Controles accesibles también al pie.");
    }
    public void setListener(Listener value){listener=value;}
    public void setModel(Model value,String color){model=value;this.color=color;back=false;requestLayout();invalidate();}
    public Model getModel(){return model;}
    public String getColorName(){return color;}
    public void flip(){back=!back;requestLayout();invalidate();}
    public static int shell(String name){switch(name){
        case "Negro":case "U2":return Color.rgb(33,35,39);case "Blanco":return 0xfff7f7f3;
        case "Azul":return 0xff5aabd4;case "Rosa":return 0xffe994b9;case "Verde":return 0xff92be63;
        case "Dorado":return 0xffd9c391;case "Rojo":return 0xffcf4853;case "Morado":return 0xff9673c5;
        case "Naranjo":return 0xffe99c44;case "Amarillo":return 0xffe4d254;case "Pizarra":return 0xff46515e;
        case "Gris espacial":case "Grafito":return 0xff7c8792;default:return 0xffcbd1d6;
    }}
    private int mix(int c,float amount){return Color.rgb((int)(Color.red(c)+(255-Color.red(c))*amount),(int)(Color.green(c)+(255-Color.green(c))*amount),(int)(Color.blue(c)+(255-Color.blue(c))*amount));}
    @Override protected void onMeasure(int ws,int hs){
        int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);
        float unit=(float)Math.min((w-38*density)/model.width,(h-24*density)/model.height);
        unit=Math.max(.1f,unit);
        // Keep tiny shuffles visibly small while remaining operable; physical proportions stay fixed.
        if(model.shuffle)unit=Math.min(unit,4.0f*density);
        else if(model.family.equals("nano6"))unit=Math.min(unit,4.8f*density);
        float bw=(float)model.width*unit,bh=(float)model.height*unit;
        body.set((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2);
        display.set(body.left+(float)model.sx*bw,body.top+(float)model.sy*bh,
            body.left+(float)(model.sx+model.sw)*bw,body.top+(float)(model.sy+model.sh)*bh);
        screen.setVisibility(model.shuffle||back?GONE:VISIBLE);
        screen.measure(MeasureSpec.makeMeasureSpec(Math.max(1,(int)display.width()),MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(Math.max(1,(int)display.height()),MeasureSpec.EXACTLY));
        wheelX=body.centerX();
        if(model.family.equals("nano3")){wheelRadius=bw*.25f;wheelY=body.top+bh*.77f;}
        else if(model.family.startsWith("nano")){wheelRadius=bw*.39f;wheelY=body.top+bh*.77f;}
        else if(model.family.equals("wheel3")){wheelRadius=bw*.31f;wheelY=body.top+bh*.75f;}
        else{wheelRadius=bw*.35f;wheelY=body.top+bh*.72f;}
        if(model.shuffle){
            wheelRadius=Math.min(bw,bh)*.40f;wheelY=model.family.equals("shuffle1")?body.top+bh*.29f:body.centerY();
        }
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int left=(int)display.left,top=(int)display.top;
        screen.layout(left,top,left+screen.getMeasuredWidth(),top+screen.getMeasuredHeight());
    }
    private void fill(int color){paint.setColor(color);paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.clearShadowLayer();}
    private void label(Canvas c,String text,float x,float y,float size,int color){fill(color);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(text,x,y,paint);}
    private void circle(Canvas c,float x,float y,float radius,int color){fill(color);c.drawCircle(x,y,radius,paint);}
    private void round(Canvas c,RectF r,float radius,int color){fill(color);c.drawRoundRect(r,radius,radius,paint);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=body.width(),h=body.height();if(w<=0)return;
        float radius=model.family.startsWith("touch")?w*.11f:model.family.startsWith("nano")?w*.065f:w*.08f;
        int base=shell(color);
        if(!back&&model.family.startsWith("touch")){
            boolean dark=color.equals("Negro")||color.equals("Pizarra")||color.equals("Gris espacial")||model.id.equals("touch5basic");
            base=dark?0xff111316:0xfff4f4f1;
        }
        fill(base);paint.setShadowLayer(9*density,2*density,7*density,0x88000000);
        c.drawRoundRect(body,radius,radius,paint);paint.clearShadowLayer();
        paint.setShader(new LinearGradient(body.left,body.top,body.right,body.bottom,
            new int[]{mix(base,.52f),base,mix(base,.10f),base},new float[]{0,.22f,.8f,1},Shader.TileMode.CLAMP));
        c.drawRoundRect(body,radius,radius,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f*density);paint.setColor(0xffaab3ba);c.drawRoundRect(body,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
        if(back){drawBack(c,w,h,radius);return;}
        if(!model.shuffle){
            RectF bezel=new RectF(display);bezel.inset(-2*density,-2*density);
            round(c,bezel,3*density,model.family.equals("nano7")?0xfffafafa:0xff1b252b);
        }
        if(model.cameraFront())circle(c,body.centerX()-w*.23f,body.top+h*.048f,w*.014f,0xff253440);
        if(model.hasWheel())drawWheel(c,w,h);
        if(model.family.equals("wheel3")){
            String[] symbols={"MENU","◀◀","▶Ⅱ","▶▶"};
            for(int i=0;i<4;i++){float x=body.left+w*(.17f+i*.22f),y=body.top+h*.47f;circle(c,x,y,w*.066f,0xfff4f1ee);label(c,symbols[i],x,y+w*.015f,w*.04f,0xffcf483d);}
        }
        if(model.family.startsWith("touch")||model.family.equals("nano7")){
            float hy=body.top+h*.918f;circle(c,body.centerX(),hy,w*.061f,base);
            fill(0xff8e969b);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);
            c.drawCircle(body.centerX(),hy,w*.061f,paint);
            c.drawRoundRect(new RectF(body.centerX()-w*.019f,hy-w*.019f,body.centerX()+w*.019f,hy+w*.019f),2,2,paint);paint.setStyle(Paint.Style.FILL);
        }
        if(model.shuffle){
            circle(c,body.right-w*.10f,body.top+h*.07f,2*density,0xffa2d352);
            if(!model.family.equals("shuffle3"))drawShuffle(c);
            if(model.family.equals("shuffle1")){fill(0xffc6c9c8);c.drawRect(body.left,body.top+h*.80f,body.right,body.top+h*.805f,paint);}
        }
        // Physical side controls, not a fake camera dot on a classic.
        if(model.touch){round(c,new RectF(body.left-2*density,body.top+h*.15f,body.left,body.top+h*.24f),density,0xff939da4);}
    }
    private void drawBack(Canvas c,float w,float h,float radius){
        boolean metal=model.hasWheel()&&!model.family.startsWith("nano")&&!model.family.equals("mini") || model.family.equals("nano1")||model.family.equals("nano3")||model.family.matches("touch[1-4]");
        int base=shell(color);fill(base);
        paint.setShader(new LinearGradient(body.left,body.top,body.right,body.bottom,
            metal?new int[]{0xffe9edf0,0xffa5afb8,0xfff8fafb,0xff9da9b3}:new int[]{mix(base,.4f),base,mix(base,.16f)},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(body,radius,radius,paint);paint.setShader(null);
        if(model.hasCamera()){
            float y=model.family.equals("nano5")?body.bottom-h*.13f:body.top+h*.055f;
            circle(c,body.left+w*.13f,y,w*.026f,0xff3e4851);circle(c,body.left+w*.13f,y,w*.016f,0xff172b38);
        }
        label(c,"iPod",body.centerX(),body.top+h*.48f,w*.16f,0xff647078);
        if(model.id.startsWith("u2"))label(c,"U2 · Special Edition",body.centerX(),body.top+h*.62f,w*.065f,0xff657078);
        if(model.id.startsWith("hp"))label(c,"hp",body.centerX(),body.top+h*.61f,w*.11f,0xff657078);
        if(model.id.startsWith("potter"))label(c,"HOGWARTS",body.centerX(),body.top+h*.61f,w*.07f,0xff657078);
        if(model.shuffle||model.family.equals("nano6")){
            RectF clip=new RectF(body.left+w*.1f,body.top+h*.08f,body.right-w*.1f,body.bottom-h*.15f);
            fill(0xff9da8af);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);c.drawRoundRect(clip,3*density,3*density,paint);paint.setStyle(Paint.Style.FILL);
        }
        label(c,model.year,body.centerX(),body.bottom-h*.12f,w*.04f,0xff66737c);
    }
    private void drawWheel(Canvas c,float w,float h){
        boolean dark=color.equals("Negro")||color.equals("U2");
        int wheel=color.equals("U2")?0xffd02d40:dark?0xff303237:0xffefefed;
        if(model.family.equals("mono4")||model.id.equals("photo")||model.id.equals("color"))wheel=0xffd1d1ce;
        circle(c,wheelX,wheelY,wheelRadius,wheel);
        circle(c,wheelX,wheelY,wheelRadius*.36f,dark?0xff202226:0xfff9f9f5);
        if(model.family.equals("wheel3"))return;
        int ink=dark?0xffdddddd:0xff7b7f82;
        if(model.id.equals("mini2"))ink=shell(color);
        label(c,"MENU",wheelX,wheelY-wheelRadius*.65f,wheelRadius*.17f,ink);
        label(c,"◀◀",wheelX-wheelRadius*.72f,wheelY+wheelRadius*.07f,wheelRadius*.19f,ink);
        label(c,"▶▶",wheelX+wheelRadius*.72f,wheelY+wheelRadius*.07f,wheelRadius*.19f,ink);
        label(c,"▶Ⅱ",wheelX,wheelY+wheelRadius*.80f,wheelRadius*.22f,ink);
    }
    private void drawShuffle(Canvas c){
        circle(c,wheelX,wheelY,wheelRadius,0xfffafafa);circle(c,wheelX,wheelY,wheelRadius*.35f,0xffe3e4e4);
        label(c,"+",wheelX,wheelY-wheelRadius*.58f,wheelRadius*.35f,0xff6e7477);
        label(c,"−",wheelX,wheelY+wheelRadius*.83f,wheelRadius*.35f,0xff6e7477);
        label(c,"◀◀",wheelX-wheelRadius*.72f,wheelY+wheelRadius*.09f,wheelRadius*.20f,0xff6e7477);
        label(c,"▶▶",wheelX+wheelRadius*.72f,wheelY+wheelRadius*.09f,wheelRadius*.20f,0xff6e7477);
        label(c,"▶Ⅱ",wheelX,wheelY+wheelRadius*.09f,wheelRadius*.24f,0xff646d72);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        if(listener==null)return false;float x=e.getX(),y=e.getY();
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){startX=x;startY=y;lastAngle=(float)Math.atan2(y-wheelY,x-wheelX);sum=0;drag=false;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&model.hasWheel()&&!back&&Math.hypot(startX-wheelX,startY-wheelY)<wheelRadius*1.1&&Math.hypot(startX-wheelX,startY-wheelY)>wheelRadius*.38){
            float a=(float)Math.atan2(y-wheelY,x-wheelX),delta=a-lastAngle;lastAngle=a;
            if(delta>Math.PI)delta-=2*Math.PI;if(delta<-Math.PI)delta+=2*Math.PI;sum+=delta;
            while(Math.abs(sum)>.28f){int dir=sum>0?1:-1;listener.step(dir);sum-=dir*.28f;drag=true;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}return true;
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            performClick();if(drag||Math.hypot(x-startX,y-startY)>20*density)return true;
            if(back){flip();return true;}
            if(model.touch&&y>body.top+body.height()*.85f){listener.action("home");return true;}
            if(model.family.equals("wheel3")&&y>body.top+body.height()*.41&&y<body.top+body.height()*.53){
                int n=Math.max(0,Math.min(3,(int)((x-body.left)/body.width()*4)));listener.action(new String[]{"back","previous","toggle","next"}[n]);return true;
            }
            float dx=x-wheelX,dy=y-wheelY;
            if(Math.hypot(dx,dy)>wheelRadius*1.1)return true;
            if(model.family.equals("shuffle3"))return true;
            if(Math.hypot(dx,dy)<wheelRadius*.38){listener.action(model.shuffle?"toggle":"select");return true;}
            if(Math.abs(dx)>Math.abs(dy))listener.action(dx<0?"previous":"next");
            else listener.action(dy<0?(model.shuffle?"volumeUp":"back"):(model.shuffle?"volumeDown":"toggle"));
            return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
