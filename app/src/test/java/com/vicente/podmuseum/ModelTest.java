package com.vicente.podmuseum;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class ModelTest {
 @Test public void everyGenerationAndRevisionHasUniqueIdentity(){
  Set<String> ids=new HashSet<>();for(Model m:Model.ALL){assertTrue(m.id,ids.add(m.id));assertTrue(m.width>0&&m.height>0);assertTrue(m.colors.length>0);}
  assertEquals(52,ids.size());
 }
 @Test public void allScreensFitTheirEnclosures(){
  for(Model m:Model.ALL)if(!m.shuffle){assertTrue(m.id,m.sx>0&&m.sy>0&&m.sw>0&&m.sh>0);assertTrue(m.id,m.sx+m.sw<1&&m.sy+m.sh<1);}
 }
 @Test public void touchScreenActuallyOccupiesMostOfItsFront(){
  for(Model m:Model.ALL)if(m.family.startsWith("touch"))assertTrue(m.id,m.sw*m.sh>.55);
 }
 @Test public void originalControlFamiliesStayDistinct(){
  assertFalse(Model.find("touch7").hasWheel());assertFalse(Model.find("nano6").hasWheel());
  assertFalse(Model.find("shuffle3").hasWheel());assertTrue(Model.find("classic6").hasWheel());
  assertTrue(Model.find("shuffle4").shuffle);assertTrue(Model.find("ipod1").mono);
  assertFalse(Model.find("touch5basic").hasCamera());assertTrue(Model.find("touch4").hasCamera());
 }
}
