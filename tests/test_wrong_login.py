"""Unit tests for the 30-minute wrong login protection mechanism."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

test_code = """
package tests;

import GameObjects.MenuUP;
import CLib.mSystem;
import Main.GameCanvas;

public class TestWrongLogin {
    static int checks = 0;
    static void check(boolean condition, String msg) {
        checks++;
        if (!condition) {
            throw new RuntimeException("Assertion failed: " + msg);
        }
    }

    public static void main(String[] args) {
        testStringDetection();
        testCountdownFormatting();
        testProtectionWorkflow();
        testDisconnectGuards();
        testCancellation();
        testRepeatCycle();
        System.out.println("PASS: " + checks + " wrong login protection checks passed!");
    }

    static void testStringDetection() {
        check(MenuUP.isWrongLoginString("Thông tin tài khoản hoặc mật khẩu không chính xác."), "tai khoan hoac mat khau khong chinh xac");
        check(MenuUP.isWrongLoginString("Tài khoản hoặc mật khẩu không chính xác"), "tai khoan hoac mat khau khong chinh xac no dot");
        check(MenuUP.isWrongLoginString("Sai tài khoản hoặc mật khẩu"), "sai tai khoan hoac mat khau");
        check(MenuUP.isWrongLoginString("Tên đăng nhập hoặc mật khẩu không đúng"), "ten dang nhap hoac mat khau khong dung");
        check(MenuUP.isWrongLoginString("Mật khẩu không chính xác"), "mat khau khong chinh xac");
        check(MenuUP.isWrongLoginString("Mật khẩu không đúng"), "mat khau khong dung");
        check(MenuUP.isWrongLoginString("Tài khoản không tồn tại"), "tai khoan khong ton tai");
        check(MenuUP.isWrongLoginString("Sai mật khẩu"), "sai mat khau");
        check(MenuUP.isWrongLoginString("Sai tài khoản"), "sai tai khoan");
        check(MenuUP.isWrongLoginString("sai tk"), "sai tk short");
        check(MenuUP.isWrongLoginString("sai mk"), "sai mk short");
        check(MenuUP.isWrongLoginString("Incorrect username or password"), "english incorrect user pass");
        check(MenuUP.isWrongLoginString("Wrong password"), "english wrong password");
        check(MenuUP.isWrongLoginString("Account does not exist"), "english account does not exist");
        check(MenuUP.isWrongLoginString("Đăng nhập sai quá 5 lần"), "dang nhap sai qua 5 lan");
        check(MenuUP.isWrongLoginString("Tài khoản bị tạm khóa 30 phút do đăng nhập sai"), "tam khoa 30 phut dang nhap sai");

        // Negative cases
        check(!MenuUP.isWrongLoginString("Máy chủ đang bảo trì, vui lòng quay lại sau"), "maintenance not wrong login");
        check(!MenuUP.isWrongLoginString("Mất kết nối với máy chủ"), "disconnect not wrong login");
        check(!MenuUP.isWrongLoginString("Phiên bản đã cũ, vui lòng tải bản mới"), "version outdated not wrong login");
        check(!MenuUP.isWrongLoginString("Tài khoản của bạn đã bị khóa vĩnh viễn"), "permanent ban not wrong login");
        check(!MenuUP.isWrongLoginString(null), "null string safe");
        check(!MenuUP.isWrongLoginString(""), "empty string safe");
    }

    static void testCountdownFormatting() {
        check(MenuUP.formatCountdown(1800).equals("30p00s"), "1800s is 30p00s");
        check(MenuUP.formatCountdown(1799).equals("29p59s"), "1799s is 29p59s");
        check(MenuUP.formatCountdown(65).equals("1p05s"), "65s is 1p05s");
        check(MenuUP.formatCountdown(59).equals("59s"), "59s is 59s");
        check(MenuUP.formatCountdown(5).equals("5s"), "5s is 5s");
        check(MenuUP.formatCountdown(0).equals("0s"), "0s is 0s");
        check(MenuUP.formatCountdown(3665).equals("1h01p05s"), "3665s is 1h01p05s");
    }

    static void testProtectionWorkflow() {
        MenuUP.cancelReconnect();
        check(!MenuUP.isWaitingReconnect, "initially not waiting");
        check(!MenuUP.isWrongLoginWait, "initially not wrong login wait");

        long start = mSystem.currentTimeMillis();
        boolean handled = MenuUP.onLoginFail("Thông tin tài khoản hoặc mật khẩu không chính xác.");
        check(handled, "onLoginFail handled wrong login message");
        check(MenuUP.isWaitingReconnect, "waiting reconnect active");
        check(MenuUP.isWrongLoginWait, "wrong login wait active");
        check(MenuUP.timeTargetReconnect >= start + 1800000L, "reconnect target is at least 30 minutes in future");
        check(MenuUP.timeTargetReconnect <= start + 1800000L + 1000L, "reconnect target bounded within 30m window");
    }

    static void testDisconnectGuards() {
        long currentTarget = MenuUP.timeTargetReconnect;
        check(MenuUP.isWrongLoginWait, "still in wrong login wait");

        // onDisconnect must NOT reset timeTargetReconnect to 10-15s
        MenuUP.onDisconnect();
        check(MenuUP.timeTargetReconnect == currentTarget, "onDisconnect preserved 30m target");
        check(MenuUP.isWrongLoginWait, "isWrongLoginWait preserved through onDisconnect");

        // onConnectFail must NOT reset timeTargetReconnect to 10-15s
        MenuUP.onConnectFail();
        check(MenuUP.timeTargetReconnect == currentTarget, "onConnectFail preserved 30m target");
        check(MenuUP.isWrongLoginWait, "isWrongLoginWait preserved through onConnectFail");
    }

    static void testCancellation() {
        MenuUP.cancelReconnect();
        check(!MenuUP.isWaitingReconnect, "cancel cleared isWaitingReconnect");
        check(!MenuUP.isWrongLoginWait, "cancel cleared isWrongLoginWait");
        check(MenuUP.timeTargetReconnect == 0L, "cancel cleared timeTargetReconnect");
    }

    static void testRepeatCycle() {
        // Attempt 1 fails
        MenuUP.onLoginFail("Sai mật khẩu");
        check(MenuUP.isWrongLoginWait, "attempt 1 triggers 30m wait");
        long target1 = MenuUP.timeTargetReconnect;

        // Reset for next simulated attempt
        MenuUP.isWaitingReconnect = false;
        MenuUP.isWrongLoginWait = false;

        // Attempt 2 fails again ("cứ như vậy sai tiếp thì lại 30 phút")
        MenuUP.onLoginFail("Sai tài khoản hoặc mật khẩu");
        check(MenuUP.isWrongLoginWait, "attempt 2 triggers 30m wait again");
        check(MenuUP.isWaitingReconnect, "waiting reconnect active again");

        MenuUP.cancelReconnect();
    }
}
"""

with tempfile.TemporaryDirectory(prefix='wrong-login-test-') as tmpdir:
    folder = Path(tmpdir)
    test_src = folder / 'TestWrongLogin.java'
    test_src.write_text(test_code, encoding='utf-8')

    java = os.environ.get('JAVA', 'java')
    classpath = f"{ROOT / 'KnightOnline_v403_AutoChat.jar'};{ROOT}" if os.name == 'nt' else f"{ROOT / 'KnightOnline_v403_AutoChat.jar'}:{ROOT}"
    subprocess.run([java, '-jar', str(ROOT / 'ecj.jar'), '-1.4', '-encoding', 'UTF-8', '-nowarn', '-cp', classpath, '-d', tmpdir, str(test_src)], check=True)
    subprocess.run([java, '-cp', f"{tmpdir};{classpath}" if os.name == 'nt' else f"{tmpdir}:{classpath}", 'tests.TestWrongLogin'], check=True)
