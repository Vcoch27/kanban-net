# Kanban-Net (LAN Realtime Kanban)

Ứng dụng quản lý công việc nhóm theo phương pháp **Kanban**, hoạt động **offline trong mạng LAN**, được xây dựng bằng **JavaFX (Client)** và **Java Socket (Server)**.

---

## 🔧 Cấu trúc

```yaml
kanban-net/
├── server/   # Java TCP server (Board/List/Card, broadcast realtime)
├── client/   # JavaFX desktop client (send/recv JSON)
├── docs/     # protocol.md – mô tả giao thức JSON frame
└── scripts/  # tiện ích test/dev
```

---

## 🚀 Cách chạy

### Server

```bash
.\gradlew :server:run
```

→ Lắng nghe TCP trên port `9000`.

### Client

```bash
.\gradlew :client:run
```

→ Mở giao diện JavaFX → nhập IP server → Connect → Send Test.

## 🧩 Giao thức

**Frame Format:** `| LEN (4-byte big-endian) | JSON (UTF-8) |`

**Các loại message chính:**

- `CREATE_BOARD`, `CREATE_LIST`, `CREATE_CARD`
- `MOVE_CARD`, `UPDATE_CARD`, `DELETE_CARD`
- `SUBSCRIBE_BOARD` (để nhận snapshot và event realtime)
- `EV_*` (các event được server broadcast như `EV_CARD_CREATED`, `EV_CARD_MOVED`)

Xem chi tiết tại: `docs/protocol.md`.

## 👥 Thành viên

- **Nguyễn Văn Hoàng** – Server, Protocol, TCP, Broadcast
- **Đặng Long Nhật** – Client JavaFX, UI, Realtime sync

## ✅ Trạng thái

- [✔] TCP socket hoạt động ổn định.
- [✔] Hai client trong cùng mạng LAN có thể đồng bộ trạng thái realtime.
- [🔜] Chuẩn bị test trên mạng LAN thật (sử dụng IP khác nhau).
