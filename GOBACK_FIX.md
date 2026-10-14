# GoBack chống lag, tọa độ minimap và tự cường hóa — 14/10/2026

## Bản sửa theo phản hồi lượt dịch chuyển thứ hai

- Chờ menu tối đa 15 giây và chờ chuyển map 30 giây. Timeout dịch chuyển được thử lại sau 30 giây, tối đa ba vòng trong một lượt GoBack; vẫn xét tuyến thay thế trong lúc chờ. Chết/hồi sinh bắt đầu lại trạng thái thử đường, không giữ lỗi của lượt trước. Map tới trễ không được dùng để xác nhận nhầm cạnh dịch chuyển khác.
- Điểm UP là ô minimap (`player.x / wTile`, `player.y / wTile`), quay về tâm ô tương ứng. Hồ sơ phiên bản 2 lưu số ô; đọc hồ sơ pixel cũ bằng chuyển đổi đơn vị. Chỉ tự lấy điểm ban đầu một lần, không ghi đè điểm đã lưu khi chạy đánh quái. Sau khi cài bản này, đứng đúng ô mong muốn rồi chọn **GoBack → Lưu lại vị trí** để thay điểm sai cũ.
- Thêm **Tiện ích & Khác → Tự cường hóa vật phẩm**. Cũng có mục **Tự cường hóa...** trong menu **Đập đồ** của thợ rèn. Bỏ trang bị vào ô cường hóa của game, chọn cấp dừng +1…+15, chọn Xu/Ngọc, đặt cỏ cho từng cấp đích rồi bắt đầu. Ví dụ hàng +7 → +8 áp dụng cho lần đập lên +8. Mỗi hàng chọn một trong: không dùng / cỏ 3 lá / cỏ 4 lá / cỏ 3 màu, đúng một ô may mắn của giao thức.
- Cường hóa chờ xác nhận thêm/bỏ cỏ, kết quả và cập nhật đúng vật phẩm trước khi đập tiếp. Thiếu đúng loại cỏ, nguyên liệu, tiền; đóng/đổi tab thợ rèn; đổi món; mất kết nối; server từ chối hoặc phản hồi quá hạn đều dừng. Không gửi lại lệnh đập tốn tiền khi timeout. Đạt cấp mục tiêu thì dừng ngay. Cấu hình lưu riêng theo server/nhân vật, trạng thái đang chạy không tự khôi phục.
- Auto bán đồ tạm dừng khi tự cường hóa đang chạy và không bán vật phẩm đang được cường hóa.

Kiểm thử bổ sung: dịch chuyển hai lượt với phản hồi map chậm 12 giây; menu timeout rồi phục hồi không cần bật/tắt; tọa độ minimap cố định; cỏ theo từng cấp; kết quả/túi đồ về đảo thứ tự; thất bại/tụt cấp; không đập vượt mục tiêu; timeout không gửi lại; lưu cấu hình và thao tác menu. Các test dùng server mô phỏng, chưa xác minh trực tiếp với tài khoản game thật.

## Bản sửa trước đó trong ngày

- Phân biệt cổng đi bộ (`npc == -1`) với ID NPC có dấu, gồm `-56`; giữ nguyên ID `0` và ID menu do server trả về.
- Mở menu một lần bằng `getIDnpc()`, không gọi chồng `GiaoTiep()` và nhiều ID khác nhau. Dùng lại menu vừa nhận; hỗ trợ phản hồi đồng bộ/bất đồng bộ và chuỗi menu gốc → nhóm → map.
- Chỉ xử lý menu của NPC đang giao tiếp; menu thuộc GoBack không bật đè giao diện. Khớp nhóm level bằng số đầu tiên, tránh nhầm Lv 110 với Lv 1. Khớp tên map chính xác hoặc hậu tố giá/level, bỏ reflection không phù hợp Java ME.
- Xét các điểm dịch chuyển trong atlas cùng đường đi bộ; chỉ chọn tuyến có đường đầy đủ tới đích. Tối thiểu hóa số lần chuyển map, rồi quãng đi bộ tới lối ra hiện tại. Đây không phải bảo đảm thời gian thực ngắn nhất vì chưa đo thời gian tải map/server.
- Giữ chặn tuyến vừa thất bại để chọn điểm đáp khác gần đích. NPC không trả menu chỉ được yêu cầu tối đa ba lần mỗi lượt; không tự xóa lỗi hoặc gửi lệnh hồi sinh khi nhân vật còn sống.
- Cache tuyến mới dùng `hso_gb_routes_v5_`, tránh tái sử dụng cạnh NPC âm bị xử lý sai trong cache cũ. Điểm UP trong hồ sơ vẫn giữ nguyên.

Kiểm tra: `tests/test_goback.py` qua 8.274 điều kiện; `tests/test_stone_optimization.py` qua các ca ID âm/0/khác ID đối tượng, menu trễ, NPC không liên quan, giá trong tên map, giới hạn retry và dịch chuyển tới map lân cận rồi đi bộ về đúng map/khu/tọa độ; `tests/test_autosell.py` qua 55 điều kiện. Build Java 1.4 thành công và kiểm tra ZIP/class trong cả hai JAR. Chưa thử trên server game thật.

## Ghi chú bản 13/10/2026

## Thay đổi

- Chọn một loại thú hoặc ID từ túi: chỉ dùng đúng ID đó. Thiếu vật phẩm thì không dùng con khác; đổi lựa chọn sang thú không có sẽ gửi lệnh xuống thú. Chế độ “Bất kỳ trong túi” chỉ hoạt động khi người dùng chủ động chọn nó.
- Khu hiển thị và lưu trong hồ sơ mới bắt đầu từ **1**. `LoadMap.Area` và packet `Change_Area` vẫn dùng chỉ số bắt đầu từ **0**, đúng giao thức game. Ví dụ Khu 1 lưu số 1, gửi byte 0.
- Điểm UP lưu riêng theo server và tên nhân vật bằng `GoBackProfile`. Cấu hình GoBack toàn cục cũ chỉ được nhận vào hồ sơ đầu tiên khi nâng cấp, không sao chép sang mọi nhân vật. Hồ sơ mới kiểm tra định danh trong nội dung RMS để tránh đọc nhầm.
- Tách bốn map “Chuẩn bị” và hai “Đấu trường” theo ID gốc, không gộp các nhánh vì trùng tên. Tên cổng trùng chỉ được phân giải nếu map xuất phát có đúng một liên kết tương ứng đã biết.
- Cổng không tên vẫn là vật cản cần tránh. Cổng đã đi qua giữ đích được xác nhận theo tọa độ kể cả khi nhãn mất. Các lần đi tay gần cổng cũng được quan sát; chết/mất kết nối hủy ứng viên đang chờ.
- Tính đường bốn hướng trên va chạm thật. Gộp đoạn thẳng tối đa 12 ô; không bỏ qua góc rẽ. `posTransRoad` chỉ chứa một phần tử để điều khiển waypoint pixel, không tăng theo kích thước map và không bị tọa độ byte có dấu trên map rộng.
- Nếu đứng kẹt hơn 3 giây, thử bước sang ô bên cạnh qua kiểm tra va chạm rồi tính lại phần đường còn lại, tối đa hai lần. Không sửa trực tiếp tọa độ nhân vật.
- Cổng nằm trên ô tường/biên: tìm ô đứng hợp lệ trong phạm vi 36 pixel. Không biến ô tường thành ô đi được. Chờ server 5 giây; nếu chưa chuyển, lùi khoảng hai ô rồi vào lại một lần, sau đó loại cổng khỏi lượt chạy nếu vẫn thất bại.
- Đổi khu tối đa ba lần, cách nhau 5 giây. Nếu khu đầy/khóa, thông báo và tiếp tục tới điểm UP ở khu hiện tại thay vì đứng chờ vô hạn.
- GoBack đang bật và có điểm UP sẽ tự yêu cầu về làng sau khi chết nếu chưa chọn chế độ hồi sinh khác. Chế độ hồi sinh tại chỗ và thao tác hủy của người chơi vẫn được tôn trọng.
- HUD tách tên map/khu và tiến độ chặng thành hai dòng, cắt chuỗi quá dài theo chiều rộng khung. Chạm khung để hủy.
- **Tối ưu Đá Dịch Chuyển (Teleport Stone) & Menu 2 cấp (Two-Tier Dynamic Menu)**:
  - Học trọn vẹn cơ sở dữ liệu dịch chuyển từ bản gốc `toolmau` (`KnightAge280 v3.0.jar`): gồm 6 danh mục với 80 map phủ sóng toàn bộ thế giới:
    + Cấp 1 ("Map Lv 1 - 39"): 19 map (Ngôi Làng Nhỏ, Làng Sói Trắng, Bìa Rừng, Hang Lửa, Rặng Đá Ngầm, ...).
    + Cấp 2 ("Map Lv 40 - 69"): 21 map (Thành Phố Kho Báu, Rừng Chết, Sa Mạc, ...).
    + Cấp 3 ("Map Lv 70 - 89"): 13 map (Thành Phố Cảng, Rừng Cao Nguyên, Vực Đá, ...).
    + Cấp 4 ("Map Lv 90 - 109"): 10 map (Rừng Medusa, Vịnh Caribe, Đầm Lầy Ma, ...).
    + Cấp 5 ("Map Lv 110+"): 13 map (Trạm Núi Tuyết, Cổng Trời, ...).
    + Cấp 6 ("Chiến Trường & Khác"): 11 map (Chiến Trường, Khu Mua Bán, Căn Cứ 4 Hệ, ...).
  - Tự động hóa menu 2 cấp thông minh: Cấp 1 nhận diện danh mục (`GoBackAtlas.matchesCategory`), Cấp 2 nhận diện map đích (`GoBackAtlas.getCategoryForMap`). Hỗ trợ cả phản hồi đồng bộ và bất đồng bộ (`menuOptions == prevOpts`).
  - Khi ở làng/thành phố, ưu tiên Đá Dịch Chuyển (`prioritizeStone = true`) để nhảy 1 chặng trực tiếp tới map farm hoặc nhảy xuyên đảo (cross-island), không cần đi bộ 4-8 map qua các cổng liên kết.
  - Bộ nhớ RMS cache được nâng cấp lên `hso_gb_routes_v4_`.
- **Tự động Bán Đồ (Auto-Sell) chuẩn an toàn**:
  - Ma trận lọc an toàn tuyệt đối (`AutoSellManager`): chỉ bán trang bị thường (color 0 - trắng) và đồ xanh lá (color 1), tuyệt đối giữ lại trang bị cam/tím (color >= 3), trang bị đã cường hóa (`plus > 0`), trang bị đang mặc (`charWear != 0`), trang bị khóa và nguyên liệu/ngọc/vật phẩm nhiệm vụ.
  - Cơ chế hàng đợi bán cách nhau 350ms (`delaySellMs = 350L`) chống ngắt kết nối (packet flood).
  - Tự động kích hoạt bán đồ khi hành trang đầy (số ô trống <= 2).

## Đường đi và phạm vi dữ liệu

`GoBackAtlas` dùng sơ đồ trong `x1/wm.png` và tên map của client kết hợp cơ sở dữ liệu `toolmau`; chứng cứ đối chiếu ở `tests/WORLD_MAP_TOPOLOGY.md`. Cổng thực tế của map đang đứng thay thế các gợi ý atlas. BFS tối thiểu hóa số chặng trên dữ liệu đã biết, ưu tiên lối ra gần hơn khi số chặng bằng nhau. Cạnh dịch chuyển từ menu server được xét như các cạnh khác; có tuyến hoàn chỉnh thì không vòng đi dò menu NPC dọc đường.

Bộ điều hướng dùng chung cho mọi ID map và level quái, không chỉ Rặng Đá Ngầm. Với 80 map được hỗ trợ trực tiếp từ Đá Dịch Chuyển, nhân vật có thể dịch chuyển tức thì từ làng đến hầu hết mọi khu vực luyện cấp và phó bản trong game.

Cache tuyến chuyển sang namespace `hso_gb_routes_v4_` để cập nhật cơ sở dữ liệu danh mục 2 cấp và sơ đồ 99 map chuẩn.

## Kiểm thử

Chạy các bộ kiểm thử đơn vị:
- `python tests/test_goback.py`: Kiểm tra 8264 điều kiện (đồ thị, lưu trữ RMS, làng nguyên tố, vật cản va chạm, map Lv134, hồi sinh, đổi khu).
- `python tests/test_autosell.py`: Kiểm tra 43 điều kiện (ma trận an toàn lọc đồ, hàng đợi trễ 350ms, tự động kích hoạt khi đầy túi).
- `python tests/test_stone_optimization.py`: Kiểm tra mô phỏng menu 2 cấp từ làng đến Cấp 1 (Rặng Đá Ngầm) và Cấp 3 xuyên đảo (Thành Phố Cảng).

Build bằng `python build_mod.py` (hỗ trợ biến `JAVA`). Hai file `KnightOnline_v403.jar` và `KnightOnline_v403_AutoChat.jar` được đóng gói hoàn chỉnh.
