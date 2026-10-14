# Hiển thị và giảm lag

Vào **Auto → Hiển thị & giảm lag**:

- **Ẩn người chơi khác**: bỏ vẽ nhân vật khác, tên/chat trên đầu và pet đi theo họ. Giữ nhân vật của mình, pet của mình, quái, NPC và đồ rơi.
- **Ẩn chiêu/hiệu ứng**: bỏ vẽ hiệu ứng chiêu, đạn, buff, hiệu ứng trang bị và số sát thương. Các vòng cập nhật vẫn chạy để kết thúc hiệu ứng đúng hạn và giữ trạng thái chiến đấu.
- **Map ô vuông**: thay nền và vật thể trang trí bằng các ô đi được. Vị trí bị dữ liệu va chạm đánh dấu chặn không có ô vuông. Vẫn hiện nhân vật, quái, NPC, đồ rơi và mũi tên chuyển map.
- **Bật cả 3 để giảm tải**: bật ba tùy chọn cùng lúc.
- **Hiển thị đầy đủ**: tắt ba tùy chọn.

Thiết lập được lưu trên thiết bị. Mặc định giữ giao diện đầy đủ. Các tùy chọn hiển thị độc lập với nút **Tắt tất cả auto**.

Ô vuông dựa trên `mapType` mà bộ tìm đường dùng: giá trị 1 và -1 bị chặn; các giá trị còn lại đi được. Vật trang trí không có va chạm không tạo chướng ngại mới. Đây là thay đổi cách vẽ, không mở đường xuyên vật cản hoặc thay đổi quy tắc di chuyển của server.

## Các tối ưu đã áp dụng

- Lưu cấu hình nhân vật khi tên/ID thay đổi, thay vì ghi RMS mỗi khung hình; vẫn cập nhật khu hiện tại trong bộ nhớ.
- Kiểm tra thứ tự độ sâu trước khi sắp xếp đối tượng; bỏ quicksort khi danh sách đã đúng thứ tự.
- Không duyệt/vẽ các lớp hiệu ứng đã ẩn; bỏ xử lý tên chồng nhau khi ẩn người chơi.
- Chỉ vẽ ô vuông trong vùng camera, không duyệt toàn bộ map.
- Khi hết vật phẩm cho pet hoặc vật phẩm hỗ trợ UP, chờ một giây rồi mới quét lại túi.
- Sửa kiểm tra null trước khi truy cập đối tượng và xử lý phần tử đếm ngược liền kề sau khi xóa.

## Kiểm chứng

`tests/test_render_settings.py`: 27 kiểm tra bộ lọc người chơi/pet, ô bị chặn, biên map, camera, lưu tùy chọn, điều kiện sắp xếp và số lần lưu cấu hình.

Đã chạy lại các bộ kiểm thử auto pet, hỗ trợ UP, tắt tất cả, tự bán, cường hóa, GoBack và dịch chuyển bằng đá. Chưa đo FPS hay kiểm chứng hình ảnh trên tài khoản/server thật; mức cải thiện thực tế tùy máy và độ đông của map.
