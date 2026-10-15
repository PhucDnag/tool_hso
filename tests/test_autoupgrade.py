"""Deterministic upgrade protocol tests; no live account or consumables."""
import os
import re
import subprocess
import tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
source=(ROOT/'src_decompiled/GameObjects/MenuUP.java').read_text(encoding='utf-8')
start=source.index('    public static String stripAccents(')
end=source.index('{',start)+1
depth=1
while depth:
    depth+=(source[end]=='{')-(source[end]=='}');end+=1
strip=source[start:end]
stubs={
'CLib/Session_ME.java': '''package CLib; public class Session_ME {
static Session_ME instance=new Session_ME();public static Session_ME gI(){return instance;}
public void sendMessage(net.Message message){try{
if(message.command!=67)throw new RuntimeException("wrong command");
java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(message.getData()));
byte op=in.readByte();short id=in.readShort();byte cat=in.readByte();
if(in.available()!=0)throw new RuntimeException("extra payload");
netcommand.global.GlobalService.gI().Rebuild_Item(op,id,cat);
}catch(java.io.IOException e){throw new RuntimeException(e.toString());}}}''',
'CLib/mSound.java': 'package CLib; public class mSound {public static int volumeSound=100; public static void playSound(int a,int b){}}',
'CLib/mSystem.java': 'package CLib; public class mSystem {public static long now=10000;public static long currentTimeMillis(){return now;}}',
'CLib/mVector.java': 'package CLib; public class mVector extends java.util.Vector {public mVector(){} public mVector(String s){}}',
'GameObjects/MenuUP.java': 'package GameObjects; public class MenuUP {public static boolean isGoBackActive;'+strip+'}',
'GameObjects/AutoSellManager.java': 'package GameObjects;public class AutoSellManager {public static boolean isReturningToSell;public static int getQueueCount(){return 0;}}',
'GameObjects/Item.java': """package GameObjects; public class Item {
public int Id,ItemCatagory,typeMaterial,numPotion;public byte tier=0,canSell=0,canTrade=0;public String itemName="",itemNameExcludeLv="";
public int imageId,colorNameItem,classcharItem,type_Only_Item,LvItem;public short IdTem;public long priceItem;public MainInfoItem[] mInfo;
public static CLib.mVector VecInvetoryPlayer=new CLib.mVector();
public static Item getItemInventory(int cat,short id){for(int i=0;i<VecInvetoryPlayer.size();i++){
Item a=(Item)VecInvetoryPlayer.elementAt(i);if(a.Id==id&&a.ItemCatagory==cat)return a;}return null;}
public static MainItem getMaterial(int id){return null;}
public static MainItem getMaterial(short id){return null;}}""",
'GameObjects/MainInfoItem.java': 'package GameObjects;public class MainInfoItem {}',
'GameObjects/MainItem.java': """package GameObjects;public class MainItem extends Item {
public static MainItem MainItem_Item(int a, String b, int c, byte d, int e, int f, int g, MainInfoItem[] h, int i, boolean j, short k, long l, short m, byte n, byte o, int p, byte q, byte r){
MainItem it=new MainItem();it.Id=a;it.itemNameExcludeLv=b;it.imageId=c;it.tier=d;return it;
}}""",
'GameObjects/Player.java': 'package GameObjects;public class Player {public int hp=100,Action;public long coin=100000,gold=1000;public String name="test";}',
'GameScreen/GameScreen.java': 'package GameScreen;public class GameScreen {public static GameObjects.Player player=new GameObjects.Player();}',
'GameScreen/LoginScreen.java': 'package GameScreen;public class LoginScreen {public static String ip="server";}',
'GameScreen/TabScreenNew.java': 'package GameScreen;public class TabScreenNew {public CLib.mVector VecTabScreen=new CLib.mVector();public int selectTab;}',
'Main/GameCanvas.java': """package Main;public class GameCanvas {
public static GameScreen.TabScreenNew shopNpc=new GameScreen.TabScreenNew();
public static Object currentScreen=shopNpc,currentDialog;public static int IndexServer;
public static Model.Menu2 menu2=new Model.Menu2();public static String lastInfo;public static int notifications;
public static void addInfoCharCline(String s){lastInfo=s;notifications++;}
public static void addInfoChar(String s){lastInfo=s;notifications++;}}""",
'Model/Menu2.java': 'package Model;public class Menu2 {public boolean isShowMenu;public CLib.mVector items;public void startAt(CLib.mVector m,int a,String s,boolean b,Object o){items=m;isShowMenu=true;}public void doCloseMenu(){isShowMenu=false;}}',
'Model/CRes.java': 'package Model;public class CRes {static java.util.Hashtable rms=new java.util.Hashtable();public static byte[] loadRMS(String k){return (byte[])rms.get(k);}public static void saveRMS(String k,byte[] v){rms.put(k,v);}}',
'InterfaceComponents/TabRebuildItem.java': """package InterfaceComponents;import GameObjects.*;
public class TabRebuildItem {public static MainItem itemRe;public static Item itemDaMayMan;
public static boolean isDapnhanh,isLucky;public static int isBeginEff,isNextRebuild;
public static String contentShow="";
public static CLib.mVector vecEffRe = new CLib.mVector();
public static DataRebuildItem[] dataRebuild;public static short[] idMaterial;
public boolean plain=true;public boolean isEquipmentRebuild(){return plain;}
public long time=0L;
public void setPaintInfo(){}
public void restoreCommands(){}
public void commandPointer(int a,int b){
    if(a==0||a==9||a==2){try{Class.forName("GameObjects.AutoUpgrade").getMethod("stop",new Class[]{String.class}).invoke(null,new Object[]{"stop"});}catch(Exception e){}}
}}""",
'InterfaceComponents/iCommand.java': 'package InterfaceComponents;public class iCommand {public String caption;public IAction action;public iCommand(String s,IAction a){caption=s;action=a;}}',
'InterfaceComponents/InputDialog.java': 'package InterfaceComponents; public class InputDialog { public CLib.TField tfInput=new CLib.TField(); public iCommand left; public void setinfo(String s, iCommand c, boolean b, String t){left=c;} }',
'CLib/TField.java': 'package CLib; public class TField { public String text="1.0"; public String getText(){return text;} public void setText(String s){text=s;} }',
'netcommand/global/GlobalService.java': """package netcommand.global;public class GlobalService {
static GlobalService s=new GlobalService();public static int packets,attempts,op,id,cat;
public static GlobalService gI(){return s;}public void Rebuild_Item(byte o,short i,byte c){
packets++;if(o==2)attempts++;op=o;id=i;cat=c;}}""",
}
with tempfile.TemporaryDirectory(prefix='upgrade-test-') as directory:
    folder=Path(directory);files=[]
    for name,body in stubs.items():
        path=folder/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body,encoding='utf-8');files.append(str(path))
    for name in ('GameObjects/AutoUpgrade.java','GameObjects/AutoUpgradeMenu.java','GameObjects/DisplayPrivacy.java','InterfaceComponents/DataRebuildItem.java','InterfaceComponents/IAction.java','net/Message.java'):
        files.append(str(ROOT/'src_decompiled'/name))
    files.append(str(ROOT/'tests/AutoUpgradeTest.java'))
    java=os.environ.get('JAVA','java')
    subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',directory]+files,check=True)
    subprocess.run([java,'-cp',directory,'GameObjects.AutoUpgradeTest'],check=True)
