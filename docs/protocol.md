[//]: # 'Protocol specification formatted as Markdown'

# Giao thức (protocol)

Tài liệu này mô tả định dạng frame, cấu trúc JSON và các loại message tối thiểu cho hệ thống Kanban realtime.

## 1) Khung frame & nguyên tắc

- Frame format (binary):

  | LEN (4 bytes, big-endian) | JSON (LEN bytes, UTF-8) |

  - LEN: số byte của phần JSON (UTF-8).
  - JSON: nội dung thông điệp (UTF-8).

### Cấu trúc JSON chung

```json
{
	"type": "STRING",    // loại thông điệp
	"corr": "UUID-OPT",  // id tương quan request/response (tùy chọn)
	"data": { ... }       // payload (tuỳ loại)
}
```

### Response chuẩn

```json
{
	"status": "ok" | "error",
	"corr": "UUID-OPT",
	"data": { ... },
	"error": { "code": "E_xxx", "message": "..." }
}
```

- Time/Version: mọi entity có các trường `id`, `version` (int tăng dần), `updated_at` (epoch ms).

## 2) Các loại message (bản tối thiểu)

### 2.1 AUTH / LOGIN

Request:

```json
{ "type": "LOGIN", "corr": "...", "data": { "username": "hoang", "password": "1234" } }
```

Response (ok):

```json
{ "status": "ok", "corr": "...", "data": { "user_id": 1, "token": "abc123" } }
```

Response (error):

```json
{
  "status": "error",
  "corr": "...",
  "error": { "code": "E_AUTH", "message": "invalid credentials" }
}
```

Ghi chú: Ở phiên bản đầu, có thể mock và bỏ password — chỉ cần kiểm tra `username` không rỗng.

### 2.2 BOARD / LIST / CARD — CRUD tối thiểu

Entity mẫu:

```json
// Board
{ "id": 1, "name": "Sprint 1", "version": 3, "updated_at": 1731080000000 }

// List
{ "id": 10, "board_id": 1, "name": "To Do", "position": 0, "version": 2, "updated_at": 1731080000000 }

// Card
{ "id": 100, "board_id": 1, "list_id": 10, "title": "Task A", "desc": "...", "position": 0, "version": 5, "updated_at": 1731080000000 }
```

CREATE_BOARD

Request:

```json
{ "type": "CREATE_BOARD", "corr": "...", "data": { "name": "Sprint 1" } }
```

Response:

```json
{
  "status": "ok",
  "corr": "...",
  "data": {
    "board": {
      /* Board */
    }
  }
}
```

Event broadcast:

```json
{
  "type": "EV_BOARD_CREATED",
  "data": {
    "board": {
      /* Board */
    }
  }
}
```

CREATE_LIST

```json
{ "type": "CREATE_LIST", "corr": "...", "data": { "board_id": 1, "name": "To Do", "position": 0 } }
```

Response / Event tương tự `CREATE_BOARD` (thay `list` và `EV_LIST_CREATED`).

CREATE_CARD

```json
{
  "type": "CREATE_CARD",
  "corr": "...",
  "data": { "board_id": 1, "list_id": 10, "title": "Task A", "desc": "", "position": 0 }
}
```

Response / Event: trả `card` và broadcast `EV_CARD_CREATED`.

MOVE_CARD

Request:

```json
{
  "type": "MOVE_CARD",
  "corr": "...",
  "data": { "card_id": 100, "to_list_id": 11, "to_position": 0, "base_version": 5 }
}
```

Response (ok): trả `card` đã cập nhật; server broadcast `EV_CARD_MOVED`.

Xử lý xung đột (version mismatch):

```json
{
  "status": "error",
  "corr": "...",
  "error": { "code": "E_CONFLICT", "message": "version mismatch" }
}
```

UPDATE_CARD (tiêu đề/mô tả)

```json
{
  "type": "UPDATE_CARD",
  "corr": "...",
  "data": { "card_id": 100, "title": "...", "desc": "...", "base_version": 5 }
}
```

Response / Event tương tự `MOVE_CARD`.

DELETE_CARD

```json
{ "type": "DELETE_CARD", "corr": "...", "data": { "card_id": 100 } }
```

Response:

```json
{ "status": "ok", "corr": "..." }
```

Event:

```json
{ "type": "EV_CARD_DELETED", "data": { "card_id": 100 } }
```

### 2.3 SUBSCRIBE & SYNC

SUBSCRIBE_BOARD — client đăng ký nhận event của một board.

Request:

```json
{ "type": "SUBSCRIBE_BOARD", "corr": "...", "data": { "board_id": 1, "since_version": 0 } }
```

Response (snapshot):

```json
{
  "status": "ok",
  "corr": "...",
  "data": {
    "snapshot": {
      "board": {
        /* ... */
      },
      "lists": [
        /* ... */
      ],
      "cards": [
        /* ... */
      ]
    },
    "applied": true
  }
}
```

Server trả snapshot lần đầu để render nhanh; sau đó gửi các `EVENT_*` realtime.

PING / PONG

```json
{ "type": "PING", "corr": "...", "data": { "t": 1731080000000 } }
```

Response:

```json
{ "status": "ok", "corr": "...", "data": { "pong": true } }
```

## 3) Mã lỗi chuẩn (tối thiểu)

| Mã lỗi      | Ý nghĩa                      |
| ----------- | ---------------------------- |
| E_AUTH      | Sai thông tin đăng nhập      |
| E_NOT_FOUND | Không tìm thấy entity        |
| E_CONFLICT  | Lệch version / xung đột      |
| E_INVALID   | Dữ liệu đầu vào không hợp lệ |
| E_INTERNAL  | Lỗi hệ thống                 |

## 4) Quy ước versioning & broadcast

- Mọi thay đổi hợp lệ tăng `version` của entity tương ứng.
- Server broadcast `EVENT_*` tới tất cả client đã SUBSCRIBE board tương ứng.

Khi client nhận `EVENT`:

- Nếu entity chưa có → thêm mới.
- Nếu có và `event.version` mới hơn → cập nhật.
- Nếu cũ hơn → bỏ qua.

---

Tệp này đã được cấu trúc lại thành Markdown để dễ đọc và tích hợp vào tài liệu kỹ thuật.
