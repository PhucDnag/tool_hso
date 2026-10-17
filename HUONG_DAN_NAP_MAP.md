# Nạp level quái theo map

Bản mới tự xóa mốc level của dữ liệu phiên bản cũ một lần; giữ đường đi, cổng đã học và vị trí UP đã lưu. Dữ liệu mới tiếp tục được lưu qua các lần mở game, riêng theo server trong bộ nhớ RMS của game.

1. Dùng bản JAR mới. Tắt GoBack tự động khi đi khảo sát để tránh bị đưa về bãi đã lưu.
2. Đi tới map cần nạp, chờ quái hiện đầy đủ rồi đi qua từng bãi có loại quái khác nhau. Không cần đánh quái; có thể chọn quái để game lấy thông tin nếu cần.
3. Ở mỗi bãi chờ khoảng 5–10 giây. Tool quét mỗi 2 giây, chỉ nhận quái có thông tin từ server, chưa bị xóa; không lấy boss và không học level ở làng.
4. Mở danh sách map UP để kiểm tra. “Chưa có dữ liệu quái” là map đã thăm nhưng chưa nhận được level hợp lệ; “Lv … đã gặp” là khoảng level thực tế đã quan sát, chưa khẳng định đã gặp đủ quái trong map.
5. Sau khi khảo sát, đứng ở map ít nhất 5–10 giây với GoBack tắt để dữ liệu được lưu. Không xóa dữ liệu ứng dụng/RMS của trình giả lập.

Không cần nạp lại các map đã học bằng bản mới. Đi thêm bãi hoặc khu có quái khác sẽ bổ sung khoảng level. Dữ liệu nằm trong game/trình giả lập đang dùng, không tự gửi về mã nguồn hay chia sẻ sang máy khác.

## Bản sửa vật thể level 1 và boss hằng ngày

Bộ lọc chỉ nhận đối tượng MainMonster có thông tin server; bỏ vật thể nhiệm vụ, nhà, hộp, vận tiêu, cổng, vật thể hiệu ứng, đối tượng có chủ và boss khỏi mốc quái thường.

Khi nâng từ bản học map trước, chỉ các khoảng bắt đầu ở Lv 1 đã được báo lỗi tại Bờ biển, Đèo băng giá, Rừng hoa đỏ, Mê cung/tầng 1/tầng 2, Vách đá cheo leo và Rừng chết được đặt về chưa có dữ liệu để học lại. Các map khác và đường đi đã nạp được giữ. Chưa có đủ thông tin để suy ra level thấp nhất đúng của những map lỗi, nên không tự thay Lv 1 bằng level cao nhất.

Khu vườn (map 50) luôn có trong danh sách với nhãn Quản lý chuồng thú. Danh sách hiển thị riêng thông tin boss hằng ngày do người chơi cung cấp, 3 lượt mỗi con/ngày: Khe Vực — Cây Quái Vật Lv10; Bãi đất trống — Ong Khổng Lồ Lv20; Nghĩa địa tàu đắm — Thủy Quái Lv30; Hang sói quỷ — Sói Bất Tử Lv40; Boss Guardian — Guardian Lv50; Hầm mộ quái vật — Xác ướp ngàn năm Lv50; Đồi xác chết — Quái vật đầu bò Lv70. Đây là nhãn thông tin, không tự săn boss hoặc theo dõi số lượt còn lại.

## Danh sách map rút gọn

Nhãn level chỉ hiện Lv, không thêm “đã gặp” hay số lượt boss. Map có level được xếp tăng dần theo level thấp nhất (map boss theo level boss); map chưa có dữ liệu rồi đến map chức năng/làng ở cuối.
Rừng chết được cố định Lv41–44 theo người chơi, không lấy boss Xà nữ. Khoảng Lv1–110 cũ của Vịnh Caribe bị xóa một lần để học lại; không tự đoán level thấp nhất.
Khu mỏ ghi Đổi thú cưỡi; Đấu trường ghi Khu PK; thêm Làng Sói Trắng, Thành Phố Kho Báu, Thành phố cảng, Thị trấn mùa đông. Địa ngục tầng 1 bị ẩn khỏi danh sách chọn, các cạnh đường đi được giữ để không phá dữ liệu định tuyến.

## Đấu trường, Ice Golem và GoBack

GoBack luôn ưu tiên đá kết hợp đi bộ, không dùng cờ Tắt ưu tiên đá lưu ở bản cũ. Khi vào làng hoặc map từng có đá, chờ tối đa 1 giây để NPC tải; nếu đá xuất hiện muộn hơn khi đang đi bộ, lập lại tuyến. NPC chưa xuất hiện chỉ là tạm thiếu dữ liệu, không khóa tuyến đá. Menu đá chậm được yêu cầu lại một lần trước khi chuyển sang đường khác. Dòng trạng thái phân biệt “Đến đá dịch chuyển” với “Đi bộ chặng”.

GoBack qua biển tự đi tới Mr Haku ở Thành phố cảng hoặc Cửa biển, chọn Đồng ý khi phí vận chuyển là 500 vàng. Khi lên thuyền, tự gửi Tăng tốc một lần. Cập bến xong tiếp tục kết hợp đá dịch chuyển và đường bộ tới map đã chọn. Nếu chưa rõ kết quả xác nhận đi thuyền, dừng để kiểm tra thay vì tự trả phí lần nữa.

Danh sách chỉ giữ một mục Đấu Trường (Khu PK), map 36; các map trùng tên 46/87 vẫn được giữ trong dữ liệu đường đi. Cổng trắng hiển thị Ice Golem Lv124 và được xếp theo level 124.
GoBack chọn đường kết hợp đá dịch chuyển tại map hiện tại với các chặng chạy bộ tới đích. Khi đá đã chọn không còn hiện diện, tự loại nhánh đá tạm thời và lập lại đường ngay, không dừng toàn bộ GoBack. Nếu menu đá chưa phản hồi sau 3 giây và có đường bộ hợp lệ, chuyển sang đi bộ. Khi đã gửi lệnh dịch chuyển, vẫn đợi server xác nhận trước khi gửi lệnh khác.

## Dữ liệu đi kèm các bản mới — 17/10/2026

Đã xuất dữ liệu map từ bản đơn và hai ô của bản x2 trong MicroEmulator. Bản đơn, x2, x3, x5 và x10 đều có cùng bản dữ liệu map theo server; mỗi ô nạp phần còn thiếu, giữ dữ liệu RMS hiện tại. Đây là bản chụp dữ liệu đã khảo sát, không tự đồng bộ những lần khảo sát tiếp theo giữa các ứng dụng. Không kèm tài khoản hoặc mật khẩu.
