import os
import subprocess
import zipfile
import shutil

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
ECJ_JAR = os.path.join(ROOT_DIR, "ecj.jar")
ORIG_JAR = os.path.join(ROOT_DIR, "KnightOnline_v403_original.jar")
TARGET_JAR = os.path.join(ROOT_DIR, "KnightOnline_v403.jar")
MOD_JAR = os.path.join(ROOT_DIR, "KnightOnline_v403_AutoChat.jar")
BUILD_DIR = os.path.join(ROOT_DIR, "bin_mod")
MICROEMU_JAR = os.path.join(ROOT_DIR, "microemulator.jar")

def build():
    print("[1/4] Preparing build directory...")
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
    os.makedirs(BUILD_DIR, exist_ok=True)

    sources = [
        os.path.join(ROOT_DIR, "src_decompiled", "GameObjects", "AutoChat.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "GameObjects", "MenuUP.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "GameScreen", "GameScreen.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "GameScreen", "PaintInfoGameScreen.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "InterfaceComponents", "TabConfig.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "Main", "GameCanvas.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "netcommand", "global", "GlobalLogicHandler.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "GameScreen", "LoginScreen.java"),
        os.path.join(ROOT_DIR, "src_decompiled", "GameScreen", "SelectCharScreen.java"),
    ]

    classpath = f"{ORIG_JAR};{MICROEMU_JAR}"

    print("[2/4] Compiling Java classes with ECJ...")
    cmd = [
        "java", "-jar", ECJ_JAR,
        "-1.4",
        "-encoding", "UTF-8",
        "-cp", classpath,
        "-d", BUILD_DIR,
        "-nowarn"
    ] + sources

    res = subprocess.run(cmd, cwd=ROOT_DIR)
    if res.returncode != 0:
        print("Compile FAILED!")
        return False

    print("[3/4] Copying compiled classes to project workspace...")
    for root, _, files in os.walk(BUILD_DIR):
        for f in files:
            full_src = os.path.join(root, f)
            rel = os.path.relpath(full_src, BUILD_DIR)
            dest = os.path.join(ROOT_DIR, rel)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            shutil.copy2(full_src, dest)

    print("[4/4] Packaging into JAR files...")
    new_files = {}
    for root, _, files in os.walk(BUILD_DIR):
        for f in files:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, BUILD_DIR).replace(os.sep, "/")
            new_files[rel] = full

    with zipfile.ZipFile(ORIG_JAR, "r") as zin, zipfile.ZipFile(MOD_JAR, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            name = item.filename
            if name in new_files:
                zout.write(new_files[name], name)
                del new_files[name]
            else:
                zout.writestr(item, zin.read(name))
        for name, full in new_files.items():
            zout.write(full, name)

    shutil.copy2(MOD_JAR, TARGET_JAR)
    shutil.rmtree(BUILD_DIR)
    print(f"Build SUCCESSFUL! Output jar: {MOD_JAR}")
    return True

if __name__ == "__main__":
    build()
