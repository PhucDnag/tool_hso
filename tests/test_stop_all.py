"""Execute the actual central stop method against enabled subsystem doubles."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'src_decompiled/GameObjects/MenuUP.java').read_text(encoding='utf-8')
start = source.index('    public static void stopAllAuto()')
end = source.index('    public static void showAutoSellMenu()', start)
methods = source[start:end]
body = '''
package GameObjects;
public class StopAllTest {
static boolean isAutoReconnect=true,isWaitingReconnect=true,isReconnectingChar=true,isCheckingGoldRevive=true;
static boolean isReviveCancelled,isAutoAttack=true,isAutoHpMp=true,isGoBackEnabled=true,isGoBackActive=true,isGoBackPaused,resumeNativeAutoFire=true;
static int autoReviveMode=2,reviveState=1,autoMountType=1,farmMapId=5;
static long timeTargetReconnect=123;
static String goBackStepInfo="moving";
static int saves,movementStops;
static void loadConfig(){}
static void saveConfig(){saves++;}
static void stopGoBackMovement(){movementStops++;}
METHODS
public static void main(String[] args){
 if(!isAnyActive())throw new RuntimeException("initial state");
 stopAllAuto();
 if(isAnyActive() || isWaitingReconnect || isReconnectingChar || reviveState!=0 || isCheckingGoldRevive
    || isGoBackActive || resumeNativeAutoFire || timeTargetReconnect!=0 || !isReviveCancelled || !isGoBackPaused)
    throw new RuntimeException("automation remained active");
 if(saves!=1 || movementStops!=1 || MainRMS.saves!=1 || AutoSellManager.stops!=1 || AutoFarmSupport.stops!=1 || AutoPetFeed.stops!=1 || AutoUpgrade.active)
    throw new RuntimeException("stop not dispatched/persisted");
 stopAllAuto();if(isAnyActive())throw new RuntimeException("repeated stop");
 System.out.println("StopAllTest passed");
}
}
class GoBackProfile {static void bind(){}}
class Player {static int isAutoFire=1,isAutoBuff=1;static boolean isCurAutoFire=true,isAutoHPMP=true;static Object autoItem=new Object();}
class AutoChat {static boolean isAutoChat=true;}
class AutoSellManager {static boolean isAutoSellEnabled=true;static int stops;static void stopAll(){stops++;isAutoSellEnabled=false;}}
class AutoFarmSupport {static boolean zoneEnabled=true,fireEnabled=true,oilEnabled=true,bookEnabled=true;static int stops;static void stopAll(){stops++;zoneEnabled=fireEnabled=oilEnabled=bookEnabled=false;}}
class AutoPetFeed {static boolean equipmentEnabled=true,candyEnabled=true;static int stops;static void stopAll(){stops++;equipmentEnabled=candyEnabled=false;}}
class AutoUpgrade {static boolean active=true;static void stop(String s){active=false;}}
class MedalFilter {static boolean blockWhite=true,blockGreen=true,blockYellow=true,blockPurple=true;}
class MainRMS {static int saves;static void setSaveAuto(){saves++;}}
class GameCanvas {static void addInfoCharCline(String s){}}
'''.replace('METHODS', methods)
with tempfile.TemporaryDirectory(prefix='stop-all-test-') as directory:
    path = Path(directory) / 'StopAllTest.java'
    path.write_text(body, encoding='utf-8')
    java = os.environ.get('JAVA', 'java')
    subprocess.run([java, '-jar', str(ROOT / 'ecj.jar'), '-1.4', '-encoding', 'UTF-8', '-nowarn', '-d', directory, str(path)], check=True)
    subprocess.run([java, '-cp', directory, 'GameObjects.StopAllTest'], check=True)
