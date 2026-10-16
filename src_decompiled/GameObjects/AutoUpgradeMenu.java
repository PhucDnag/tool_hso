package GameObjects;

import CLib.mVector;
import InterfaceComponents.InputDialog;
import InterfaceComponents.TabRebuildItem;
import InterfaceComponents.iCommand;
import InterfaceComponents.IAction;
import Main.GameCanvas;

public final class AutoUpgradeMenu {
    private static TabRebuildItem tab;
    public static boolean isShowing = false;

    public static void show(TabRebuildItem selected) {
        AutoUpgrade.loadConfig();
        isShowing = true;
        if (selected != null) tab = selected;
        if (tab == null && GameCanvas.currentScreen == GameCanvas.shopNpc) {
            for (int i = 0; i < GameCanvas.shopNpc.VecTabScreen.size(); i++) {
                Object t = GameCanvas.shopNpc.VecTabScreen.elementAt(i);
                if (t instanceof TabRebuildItem && ((TabRebuildItem)t).isEquipmentRebuild()) {
                    tab = (TabRebuildItem)t;
                }
            }
        }

        mVector menu = new mVector("Auto cuong hoa");

        if (AutoUpgrade.active) {
            menu.addElement(new iCommand("DỪNG CƯỜNG HÓA", new IAction() {
                public void perform() {
                    AutoUpgrade.stop("Đã dừng theo yêu cầu.");
                    show(tab);
                }
            }));
            menu.addElement(new iCommand("Trạng thái: " + AutoUpgrade.status, new IAction() {
                public void perform() {
                    show(tab);
                }
            }));
            menu.addElement(new iCommand("Mục tiêu: +" + AutoUpgrade.targetLevel + " | " + AutoUpgrade.formatDelay(AutoUpgrade.delayMs), new IAction() {
                public void perform() {
                    show(tab);
                }
            }));
            menu.addElement(new iCommand("Dùng tiền: " + (AutoUpgrade.currency == 0 ? "Xu" : "Ngọc"), new IAction() {
                public void perform() {
                    show(tab);
                }
            }));
            menu.addElement(new iCommand("Đóng (Auto chạy tiếp)", new IAction() {
                public void perform() {
                    isShowing = false;
                    GameCanvas.menu2.isShowMenu = false;
                }
            }));
        } else {
            String itemStr;
            int curTier = 0;
            if (TabRebuildItem.itemRe == null) {
                itemStr = "(Chưa bỏ đồ vào ô)";
            } else {
                itemStr = TabRebuildItem.itemRe.itemName + " +" + TabRebuildItem.itemRe.tier;
                curTier = TabRebuildItem.itemRe.tier;
            }

            final int finalCurTier = curTier;

            // 1. Nút Bắt đầu
            menu.addElement(new iCommand("BẮT ĐẦU: " + itemStr + " -> +" + AutoUpgrade.targetLevel, new IAction() {
                public void perform() {
                    if (TabRebuildItem.itemRe == null) {
                        GameCanvas.addInfoCharCline("Vui lòng bỏ trang bị vào ô cường hóa trước!");
                        show(tab);
                        return;
                    }
                    if (finalCurTier >= AutoUpgrade.targetLevel) {
                        GameCanvas.addInfoCharCline("Trang bị đã đạt cấp +" + finalCurTier + " (mục tiêu: +" + AutoUpgrade.targetLevel + ")!");
                        show(tab);
                        return;
                    }
                    isShowing = false;
                    GameCanvas.menu2.isShowMenu = false;
                    AutoUpgrade.start(tab);
                }
            }));

            // 2. Cấp mục tiêu
            menu.addElement(new iCommand("Cấp mục tiêu: +" + AutoUpgrade.targetLevel, new IAction() {
                public void perform() {
                    showTargetLevels();
                }
            }));

            // 3. Delay cường hóa
            menu.addElement(new iCommand("Delay cường hóa: " + AutoUpgrade.formatDelay(AutoUpgrade.delayMs), new IAction() {
                public void perform() {
                    promptCustomDelay();
                }
            }));

            // 4. Loại tiền
            menu.addElement(new iCommand("Dùng tiền: " + (AutoUpgrade.currency == 0 ? "Xu" : "Ngọc") + " (Bấm để đổi)", new IAction() {
                public void perform() {
                    AutoUpgrade.currency = 1 - AutoUpgrade.currency;
                    AutoUpgrade.saveConfig();
                    show(tab);
                }
            }));

            // 5. Cài đặt Cỏ may mắn
            menu.addElement(new iCommand("Cài đặt Cỏ may mắn...", new IAction() {
                public void perform() {
                    showCloverMenu();
                }
            }));

            // 7. Đóng
            menu.addElement(new iCommand("Đóng", new IAction() {
                public void perform() {
                    isShowing = false;
                    GameCanvas.menu2.isShowMenu = false;
                }
            }));
        }

        GameCanvas.menu2.startAt(menu, 2, "TỰ ĐỘNG CƯỜNG HÓA", false, null);
    }

    private static void promptCustomDelay() {
        final InputDialog dlg = new InputDialog();
        dlg.setinfo("Nhập số giây delay (tối thiểu 4s, mặc định 4s):", new iCommand("Xong", new IAction() {
            public void perform() {
                try {
                    String txt = dlg.tfInput != null ? dlg.tfInput.getText() : "";
                    float sec = -1.0f;
                    try {
                        sec = Float.parseFloat(txt.trim().replace(',', '.'));
                    } catch (Exception ignored) {}
                    int ms = AutoUpgrade.parseDelaySeconds(txt);
                    if (ms < 4000) ms = 4000;
                    AutoUpgrade.delayMs = ms;
                    AutoUpgrade.saveConfig();
                    if (sec < 4.0f) {
                        GameCanvas.addInfoCharCline("Delay tối thiểu 4s; đã đặt về mặc định 4s.");
                    } else {
                        GameCanvas.addInfoCharCline("Đã đổi delay cường hóa: " + AutoUpgrade.formatDelay(ms));
                    }
                } catch (Exception e) {
                    AutoUpgrade.delayMs = 4000;
                    AutoUpgrade.saveConfig();
                    GameCanvas.addInfoCharCline("Lỗi nhập liệu; đã đặt về mặc định 4s.");
                }
                GameCanvas.currentDialog = null;
                show(tab);
            }
        }), false, "DELAY CƯỜNG HÓA");
        if (dlg.tfInput != null) {
            String defText = (AutoUpgrade.delayMs % 1000 == 0)
                ? String.valueOf(AutoUpgrade.delayMs / 1000)
                : String.valueOf((float)AutoUpgrade.delayMs / 1000f);
            dlg.tfInput.setText(defText);
        }
        GameCanvas.currentDialog = dlg;
    }

    private static void showTargetLevels() {
        mVector menu = new mVector("Cap muc tieu");
        for (int i = 1; i <= AutoUpgrade.MAX_LEVEL; i++) {
            final int level = i;
            String check = (AutoUpgrade.targetLevel == level) ? "[] " : "";
            menu.addElement(new iCommand(check + "Dừng khi đạt +" + i, new IAction() {
                public void perform() {
                    AutoUpgrade.targetLevel = level;
                    AutoUpgrade.saveConfig();
                    show(tab);
                }
            }));
        }
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() {
                show(tab);
            }
        }));
        GameCanvas.menu2.startAt(menu, 2, "CHỌN CẤP MỤC TIÊU", false, null);
    }

    private static void showCloverMenu() {
        mVector menu = new mVector("Cai co");
        menu.addElement(new iCommand("✨ Cài chuẩn (+5: 3 lá | +6,9..11: 4 lá | +13: 3 lá | +14,15: 3 màu)", new IAction() {
            public void perform() {
                AutoUpgrade.applyStandardPolicy();
                AutoUpgrade.saveConfig();
                GameCanvas.addInfoCharCline("Đã áp dụng cài cỏ/bảo hộ chuẩn (+15)!");
                showCloverMenu();
            }
        }));
        menu.addElement(new iCommand("❌ Không dùng cỏ cho tất cả cấp", new IAction() {
            public void perform() {
                for (int i = 1; i <= AutoUpgrade.MAX_LEVEL; i++) AutoUpgrade.policy[i] = (byte)AutoUpgrade.NONE;
                AutoUpgrade.saveConfig();
                GameCanvas.addInfoCharCline("Đã tắt dùng cỏ cho mọi cấp!");
                showCloverMenu();
            }
        }));
        menu.addElement(new iCommand("Dùng Cỏ 3 lá từ cấp...", new IAction() {
            public void perform() {
                showCloverBatch(AutoUpgrade.THREE);
            }
        }));
        menu.addElement(new iCommand("Dùng Cỏ 4 lá từ cấp...", new IAction() {
            public void perform() {
                showCloverBatch(AutoUpgrade.FOUR);
            }
        }));
        menu.addElement(new iCommand("Dùng Đá 3 màu từ cấp...", new IAction() {
            public void perform() {
                showCloverBatch(AutoUpgrade.COLOR);
            }
        }));
        menu.addElement(new iCommand("Cài chi tiết từng cấp (+1 -> +15)...", new IAction() {
            public void perform() {
                showDetailedCloverList();
            }
        }));
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() {
                show(tab);
            }
        }));
        GameCanvas.menu2.startAt(menu, 2, "CÀI ĐẶT CỎ MAY MẮN", false, null);
    }

    private static void showCloverBatch(final int type) {
        mVector menu = new mVector("Batch clover");
        int[] startLevels = new int[]{4, 5, 6, 7, 8, 9, 10, 11};
        for (int i = 0; i < startLevels.length; i++) {
            final int lvl = startLevels[i];
            menu.addElement(new iCommand("Từ cấp +" + lvl + " trở lên", new IAction() {
                public void perform() {
                    for (int j = lvl; j <= AutoUpgrade.MAX_LEVEL; j++) {
                        AutoUpgrade.policy[j] = (byte)type;
                    }
                    AutoUpgrade.saveConfig();
                    showCloverMenu();
                }
            }));
        }
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() {
                showCloverMenu();
            }
        }));
        GameCanvas.menu2.startAt(menu, 2, "DÙNG " + AutoUpgrade.cloverName(type).toUpperCase() + " TỪ CẤP", false, null);
    }

    private static void showDetailedCloverList() {
        mVector menu = new mVector("Co tung cap");
        for (int i = 1; i <= AutoUpgrade.MAX_LEVEL; i++) {
            final int level = i;
            menu.addElement(new iCommand("+" + (i - 1) + " → +" + i + ": " + AutoUpgrade.cloverName(AutoUpgrade.policy[i]), new IAction() {
                public void perform() {
                    showCloverChoices(level);
                }
            }));
        }
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() {
                showCloverMenu();
            }
        }));
        GameCanvas.menu2.startAt(menu, 2, "CỎ CHO TỪNG CẤP ĐÍCH", false, null);
    }

    private static void showCloverChoices(final int level) {
        mVector menu = new mVector("Chon co");
        for (int i = 0; i <= 3; i++) {
            final int type = i;
            String check = (AutoUpgrade.policy[level] == i) ? "[] " : "";
            menu.addElement(new iCommand(check + AutoUpgrade.cloverName(i), new IAction() {
                public void perform() {
                    AutoUpgrade.policy[level] = (byte)type;
                    AutoUpgrade.saveConfig();
                    showDetailedCloverList();
                }
            }));
        }
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() {
                showDetailedCloverList();
            }
        }));
        GameCanvas.menu2.startAt(menu, 2, "CƯỜNG HÓA LÊN +" + level, false, null);
    }

}
