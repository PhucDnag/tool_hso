import os,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
java=os.environ.get('JAVA','java')
with tempfile.TemporaryDirectory(prefix='multi-smoke-') as d:
 subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-nowarn','-cp',str(ROOT/'microemulator.jar'),'-d',d,str(ROOT/'tests/MultiSmoke.java')],check=True)
 for count in (2,3,5,10):
  home=Path(d)/('home'+str(count));home.mkdir()
  log=ROOT/'multi_build'/('smoke_x'+str(count)+'.log')
  with log.open('w') as out:
   result=subprocess.run([java,'-Djava.awt.headless=true','-Duser.home='+str(home),'-cp',d+';'+str(ROOT/'microemulator.jar'),'MultiSmoke',str(ROOT/'multi_build'/('KnightOnline_v403_x'+str(count)+'.jar')),str(ROOT/'multi_build'/('button_x'+str(count)+'.png'))],stdout=out,stderr=subprocess.STDOUT,timeout=120)
  lines=log.read_text(errors='replace').splitlines()
  print('x'+str(count)+': '+next((line for line in reversed(lines) if line.startswith('PASS:')), '\n'.join(lines[-15:])),flush=True)
  result.check_returncode()
