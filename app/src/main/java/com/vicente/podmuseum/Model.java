package com.vicente.podmuseum;

import java.util.*;

/** Geometry is normalized to the real enclosure, never to the phone screen. */
public final class Model {
    public final String id, name, year, family, detail;
    public final double width, height, sx, sy, sw, sh;
    public final String[] colors;
    public final boolean mono, touch, shuffle;
    public Model(String id,String name,String year,String family,double width,double height,
                 double sx,double sy,double sw,double sh,boolean mono,String colors,String detail) {
        this.id=id;this.name=name;this.year=year;this.family=family;this.width=width;this.height=height;
        this.sx=sx;this.sy=sy;this.sw=sw;this.sh=sh;this.mono=mono;
        this.touch=family.startsWith("touch")||family.equals("nano6")||family.equals("nano7");
        this.shuffle=family.startsWith("shuffle");this.colors=colors.split(",");this.detail=detail;
    }
    public boolean hasWheel(){return !touch&&!shuffle;}
    public boolean hasCamera(){return family.equals("nano5") || family.equals("touch4") ||
        (family.equals("touch5")&&!id.equals("touch5basic")) || family.equals("touch6") || family.equals("touch7");}
    public boolean cameraFront(){return family.equals("touch4")||family.equals("touch5")||family.equals("touch6")||family.equals("touch7");}
    public static Model find(String id){for(Model m:ALL)if(m.id.equals(id))return m;return ALL.get(9);}
    private static Model m(String id,String name,String year,String f,boolean mono,String colors,String detail){
        double w=61.8,h=103.5,x=.105,y=.065,sw=.79,sh=.40;
        switch(f){
            case "wheel1": case "wheel2": h=102;x=.10;y=.09;sw=.80;sh=.31;break;
            case "wheel3":h=104.1;x=.10;y=.07;sw=.80;sh=.31;break;
            case "mono4":h=104.1;sh=.33;break;
            case "mini":w=50.8;h=91.4;x=.12;y=.10;sw=.76;sh=.30;break;
            case "nano1":w=40;h=89;x=.15;y=.085;sw=.70;sh=.32;break;
            case "nano2":w=40;h=90;x=.15;y=.08;sw=.70;sh=.32;break;
            case "nano3":w=52.3;h=69.8;x=.075;y=.07;sw=.85;sh=.49;break;
            case "nano4":w=38.7;h=90.7;x=.10;y=.06;sw=.80;sh=.47;break;
            case "nano5":w=38.7;h=90.7;x=.10;y=.055;sw=.80;sh=.52;break;
            case "nano6":w=40.9;h=37.5;x=.075;y=.075;sw=.85;sh=.85;break;
            case "nano7":w=39.6;h=76.5;x=.07;y=.065;sw=.86;sh=.78;break;
            case "shuffle1":w=25;h=84;sw=sh=0;break;
            case "shuffle2":w=41.2;h=27.3;sw=sh=0;break;
            case "shuffle3":w=17.5;h=45.2;sw=sh=0;break;
            case "shuffle4":w=31.6;h=29;sw=sh=0;break;
            case "touch1":w=61.8;h=110;x=.085;y=.10;sw=.83;sh=.70;break;
            case "touch2":case "touch3":w=61.8;h=110;x=.08;y=.10;sw=.84;sh=.70;break;
            case "touch4":w=58.9;h=111;x=.065;y=.115;sw=.87;sh=.69;break;
            case "touch5":case "touch6":case "touch7":w=58.6;h=123.4;x=.065;y=.105;sw=.87;sh=.735;break;
        }
        return new Model(id,name,year,f,w,h,x,y,sw,sh,mono,colors,detail);
    }
    public static final List<Model> ALL=Collections.unmodifiableList(Arrays.asList(
        m("ipod1","iPod · 1.ª generación","2001–2002","wheel1",true,"Blanco","Rueda mecánica, botones separados y FireWire."),
        m("ipod2","iPod · 2.ª generación","2002","wheel2",true,"Blanco","Rueda táctil inmóvil y tapa para FireWire."),
        m("ipod3","iPod · 3.ª generación","2003","wheel3",true,"Blanco","Cuatro botones iluminados y conector de 30 pines."),
        m("ipod4","iPod · 4.ª generación","2004","mono4",true,"Blanco","Click Wheel gris y pantalla monocroma."),
        m("photo","iPod photo","2004–2005","classic",false,"Blanco","Primer iPod a color, frontal blanco y reverso de acero."),
        m("color","iPod con pantalla a color","2005","classic",false,"Blanco","Unificación de iPod y photo."),
        m("video5","iPod video · 5.ª generación","2005","classic",false,"Blanco,Negro","Plástico brillante y pantalla de 2,5 pulgadas."),
        m("video55","iPod video · revisión 5.5","2006","classic",false,"Blanco,Negro","Revisión con pantalla más luminosa."),
        m("classic6","iPod classic · 80 / 160 GB","2007","classic",false,"Plata,Negro","Frontal de aluminio y reverso de acero pulido."),
        m("classic120","iPod classic · 120 GB","2008","classic",false,"Plata,Negro","Carcasa delgada y disco de 120 GB."),
        m("classic160","iPod classic · 160 GB","2009","classic",false,"Plata,Negro","Última revisión del classic."),
        m("mini1","iPod mini · 1.ª generación","2004","mini",true,"Plata,Dorado,Rosa,Azul,Verde","Aluminio anodizado y pantalla monocroma."),
        m("mini2","iPod mini · 2.ª generación","2005","mini",true,"Plata,Rosa,Azul,Verde","Colores más intensos y símbolos a juego."),
        m("nano1","iPod nano · 1.ª generación","2005–2006","nano1",false,"Blanco,Negro","Plástico frontal, acero posterior y formato estrecho."),
        m("nano2","iPod nano · 2.ª generación","2006","nano2",false,"Plata,Negro,Azul,Verde,Rosa,Rojo","Aluminio con laterales redondeados."),
        m("nano3","iPod nano · 3.ª generación","2007–2008","nano3",false,"Plata,Negro,Azul,Verde,Rojo,Rosa","Formato corto y ancho para reproducir video."),
        m("nano4","iPod nano · 4.ª generación","2008","nano4",false,"Plata,Negro,Morado,Azul,Verde,Naranjo,Amarillo,Rosa,Rojo","Carcasa curva de sección ovalada."),
        m("nano5","iPod nano · 5.ª generación","2009","nano5",false,"Plata,Negro,Morado,Azul,Verde,Naranjo,Amarillo,Rosa,Rojo","Pantalla más alta y cámara posterior."),
        m("nano6","iPod nano · 6.ª generación","2010","nano6",false,"Plata,Grafito,Azul,Verde,Naranjo,Rosa,Rojo","Cuadrado táctil, con pinza y sin botón de inicio."),
        m("nano7","iPod nano · 7.ª generación","2012","nano7",false,"Plata,Pizarra,Azul,Verde,Morado,Amarillo,Rosa,Rojo","Pantalla táctil, botón de inicio y Lightning."),
        m("nano7gray","iPod nano · gris espacial","2013","nano7",false,"Gris espacial","Revisión de acabado de la séptima generación."),
        m("nano72015","iPod nano · colores 2015","2015","nano7",false,"Plata,Gris espacial,Dorado,Azul,Rosa,Rojo","Última paleta de colores del nano."),
        m("shuffle1","iPod shuffle · 1.ª generación","2005","shuffle1",false,"Blanco","Sin pantalla y con USB bajo una tapa."),
        m("shuffle2","iPod shuffle · 2.ª generación","2006","shuffle2",false,"Plata","Rectángulo horizontal con pinza."),
        m("shuffle22007","iPod shuffle · colores 2007","2007","shuffle2",false,"Plata,Azul,Rosa,Verde,Naranjo,Morado,Rojo","Varias tandas de colores; misma carcasa."),
        m("shuffle22008","iPod shuffle · colores 2008","2008","shuffle2",false,"Plata,Azul,Rosa,Verde,Rojo","Revisión de colores, sin nueva generación."),
        m("shuffle3","iPod shuffle · 3.ª generación","2009","shuffle3",false,"Plata,Negro","Controles originales en el cable de los auriculares."),
        m("shuffle3late","iPod shuffle · finales de 2009","2009","shuffle3",false,"Plata,Negro,Azul,Verde,Rosa","Más capacidades y acabados."),
        m("shuffle3steel","iPod shuffle · Limited Edition","2009","shuffle3",false,"Acero","Edición de 4 GB en acero inoxidable pulido."),
        m("shuffle4","iPod shuffle · 4.ª generación","2010","shuffle4",false,"Plata,Azul,Verde,Naranjo,Rosa","Regreso del aro de botones y formato casi cuadrado."),
        m("shuffle42012","iPod shuffle · colores 2012","2012","shuffle4",false,"Plata,Pizarra,Azul,Verde,Morado,Amarillo,Rosa,Rojo","Mismo cuerpo, nueva paleta."),
        m("shuffle42013","iPod shuffle · gris espacial","2013","shuffle4",false,"Gris espacial","Nuevo acabado de cuarta generación."),
        m("shuffle42015","iPod shuffle · colores 2015","2015","shuffle4",false,"Plata,Gris espacial,Dorado,Azul,Rosa,Rojo","Últimos colores del shuffle."),
        m("touch1","iPod touch · 1.ª generación","2007","touch1",false,"Negro","Pantalla de 3,5 pulgadas, sin cámaras."),
        m("touch2","iPod touch · 2.ª generación","2008","touch2",false,"Negro","Trasera curvada y botones laterales de volumen."),
        m("touch3","iPod touch · 3.ª generación","2009","touch3",false,"Negro","Misma apariencia exterior que la segunda generación."),
        m("touch4","iPod touch · 4.ª generación","2010","touch4",false,"Negro","Pantalla Retina y dos cámaras."),
        m("touch4white","iPod touch · edición blanca","2011–2012","touch4",false,"Negro,Blanco","Revisión que incorporó frontal blanco."),
        m("touch5","iPod touch · 5.ª generación","2012","touch5",false,"Plata,Pizarra,Azul,Rosa,Amarillo,Rojo","Pantalla de 4 pulgadas, aluminio y anclaje loop."),
        m("touch5basic","iPod touch · 16 GB sin cámara trasera","2013","touch5",false,"Plata","Frontal negro, sin cámara trasera ni anclaje loop."),
        m("touch5gray","iPod touch · gris espacial","2013","touch5",false,"Gris espacial","Revisión con nuevo acabado."),
        m("touch52014","iPod touch · 16 GB con cámara","2014","touch5",false,"Plata,Gris espacial,Azul,Rosa,Amarillo,Rojo","Cámara posterior y colores para el modelo de 16 GB."),
        m("touch6","iPod touch · 6.ª generación","2015","touch6",false,"Plata,Gris espacial,Dorado,Azul,Rosa,Rojo","Desapareció el anclaje de la correa."),
        m("touch7","iPod touch · 7.ª generación","2019","touch7",false,"Plata,Gris espacial,Dorado,Azul,Rosa,Rojo","Última generación de iPod; cambios principalmente internos."),
        m("u24","iPod U2 · monocromo","2004","mono4",true,"U2","Frontal negro, rueda roja y firmas grabadas."),
        m("u2color","iPod U2 · color","2005","classic",false,"U2","Cuarta generación con pantalla a color."),
        m("u2video","iPod U2 · video","2006","classic",false,"U2","Quinta generación, rueda roja y firmas."),
        m("u2late","iPod U2 · video revisado","2006","classic",false,"U2","Revisión de finales de 2006."),
        m("hp4","Apple iPod + HP · monocromo","2004","mono4",true,"Blanco","Variante distribuida por HP."),
        m("hpcolor","Apple iPod + HP · color","2005","classic",false,"Blanco","Variante a color distribuida por HP."),
        m("potter","iPod Harry Potter · Collector’s Edition","2005","classic",false,"Blanco","Edición con grabado de Hogwarts."),
        m("pottervideo","iPod Harry Potter · video","2005","classic",false,"Blanco","Variante video con grabado de Hogwarts.")
    ));
}
