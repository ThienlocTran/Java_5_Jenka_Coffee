# Admin Dashboard UX Improvements

**Date**: 2026-04-18  
**Status**: ✅ COMPLETED  
**Focus**: Improve admin workflow efficiency

---

## 🎯 OVERVIEW

Improved admin dashboard based on real-world usage feedback:
1. ✅ Notification system: Click to mark all as read
2. ✅ Contact system: Phone-based instead of email
3. ✅ Order status: Simplified to 2 states (Confirmed/Cancelled)

---

## 🔧 FIX 1: NOTIFICATION SYSTEM - MARK AS READ

### Problem
- Admin sees notification badge (red dot with count)
- Clicks on "Liên hệ" menu item
- Badge stays visible even after viewing contacts
- No way to clear notifications

### Solution
**Backend** (Already implemented):
- Endpoint: `PATCH /api/admin/contacts/mark-all-read`
- Method: `ContactService.markAllAsRead()`
- Marks all contacts as `isRead = true`

**Frontend**:
- Added `markContactsAsViewed()` function in AdminLayout
- Calls backend API when clicking "Liên hệ" menu
- Resets badge count immediately for better UX
- Stores timestamp in localStorage to prevent re-showing after refresh

**User Flow**:
1. Admin sees badge: "Liên hệ (3)"
2. Admin clicks "Liên hệ"
3. Badge disappears immediately
4. All contacts marked as read in database
5. Badge stays hidden even after page refresh (1 hour cooldown)

### Files Changed
- `front_end_Jenka_Coffee/jenka-coffee-ui/src/layouts/AdminLayout.vue`

---

## 🔧 FIX 2: CONTACT SYSTEM - PHONE INSTEAD OF EMAIL

### Problem
- Current system uses email as primary contact method
- Admin has to click "Trả lời qua Email" → opens email client
- Slow workflow, requires email setup
- Real-world: Phone call is faster and more direct

### Solution
**Database**:
- Added `phone` field to `Contacts` table
- Made `email` nullable (phone is now primary)
- Added indexes for performance

**Backend**:
- Updated `Contact` entity with `phone` field
- Email is now optional

**Frontend**:
- Replaced email display with phone number
- Changed "Trả lời qua Email" button to "Gọi ngay" button
- Click button → opens phone dialer (`tel:` protocol)
- Email still shown if provided (optional field)

**User Flow**:
1. Customer submits contact form with phone number
2. Admin sees notification
3. Admin clicks contact → expands details
4. Admin sees phone number prominently
5. Admin clicks "Gọi ngay" → phone dialer opens
6. Admin calls customer directly

### Files Changed
- `Java_5_Jenka_Coffee/src/main/resources/db/migration/V20__improve_admin_dashboard.sql`
- `Java_5_Jenka_Coffee/src/main/java/com/springboot/jenka_coffee/entity/Contact.java`
- `front_end_Jenka_Coffee/jenka-coffee-ui/src/views/admin/ContactsView.vue`

---

## 🔧 FIX 3: ORDER STATUS - SIMPLIFIED WORKFLOW

### Problem
- Current system has 5 statuses: Mới → Đã xác nhận → Đang giao → Hoàn thành → Đã hủy
- Too complex for small coffee shop
- No integration with 3rd party shipping
- Admin manually calls customer anyway

### Solution
**Simplified to 3 statuses**:
1. **Mới (NEW)** - Status 0
   - Order just placed
   - Waiting for admin confirmation
   
2. **Đã xác nhận (CONFIRMED)** - Status 1, 2, 4
   - Admin confirmed order
   - Shop will call customer to arrange delivery
   - Merged: "Đã xác nhận", "Đang giao", "Hoàn thành"
   
3. **Đã hủy (CANCELLED)** - Status 3
   - Order cancelled by admin or customer

**Status Mapping** (backward compatible):
```javascript
0 → Mới (NEW)
1 → Đã xác nhận (CONFIRMED)
2 → Đã xác nhận (SHIPPING → CONFIRMED)
3 → Đã hủy (CANCELLED)
4 → Đã xác nhận (COMPLETED → CONFIRMED)
```

**Admin Workflow**:
1. New order arrives → Status: "Mới"
2. Admin clicks "Xác nhận đơn hàng" → Status: "Đã xác nhận"
3. Shop staff calls customer to arrange delivery
4. Shop staff delivers order manually
5. (Optional) Admin can click "Hủy đơn" if needed

**UI Changes**:
- Order list: Shows "Mới", "Đã xác nhận", "Đã hủy"
- Order detail: Only 2 buttons: "Xác nhận đơn hàng" and "Hủy đơn"
- After confirmation: Shows green message "Đơn hàng đã được xác nhận. Quán sẽ liên hệ khách hàng để giao hàng."
- Filter dropdown: Only 3 options

### Files Changed
- `front_end_Jenka_Coffee/jenka-coffee-ui/src/views/admin/OrdersManageView.vue`
- `front_end_Jenka_Coffee/jenka-coffee-ui/src/views/admin/OrderDetailView.vue`

---

## 📊 BENEFITS

### 1. Notification System
- ✅ Clear visual feedback (badge disappears)
- ✅ No confusion about read/unread state
- ✅ Faster workflow (no manual marking)
- ✅ Persistent across page refreshes

### 2. Contact System
- ✅ Faster response time (call vs email)
- ✅ Direct communication with customer
- ✅ No email client setup required
- ✅ Mobile-friendly (click to call)
- ✅ Better for urgent inquiries

### 3. Order Status
- ✅ Simpler workflow (2 actions instead of 4)
- ✅ Less confusion for admin staff
- ✅ Matches real-world process
- ✅ Faster order processing
- ✅ No unnecessary status transitions

---

## 🧪 TESTING CHECKLIST

### Notification System
- [ ] Click "Liên hệ" → badge disappears
- [ ] Refresh page → badge stays hidden
- [ ] Wait 1 hour → badge reappears if new contacts
- [ ] Check database → all contacts marked as read

### Contact System
- [ ] Submit contact form with phone number
- [ ] Admin sees phone number in list
- [ ] Click contact → phone number displayed prominently
- [ ] Click "Gọi ngay" → phone dialer opens
- [ ] Email still shown if provided

### Order Status
- [ ] New order → Status shows "Mới"
- [ ] Click "Xác nhận đơn hàng" → Status changes to "Đã xác nhận"
- [ ] Green message appears after confirmation
- [ ] Click "Hủy đơn" → Status changes to "Đã hủy"
- [ ] Filter dropdown only shows 3 options
- [ ] Old orders (status 2, 4) display as "Đã xác nhận"

---

## 🔄 MIGRATION NOTES

### Database Migration
Run Flyway migration V20:
```sql
-- Add phone field to Contacts
ALTER TABLE "Contacts" ADD COLUMN IF NOT EXISTS "phone" VARCHAR(15);

-- Make email nullable
ALTER TABLE "Contacts" ALTER COLUMN "email" DROP NOT NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_contacts_isread ON "Contacts"("isRead");
CREATE INDEX IF NOT EXISTS idx_contacts_createdat ON "Contacts"("createdAt" DESC);
```

### Backward Compatibility
- ✅ Old contacts without phone: Email still shown
- ✅ Old orders with status 2, 4: Mapped to "Đã xác nhận"
- ✅ No breaking changes to API
- ✅ Frontend handles missing phone gracefully

---

## 📝 FUTURE IMPROVEMENTS

### Contact System
- Add SMS notification option
- Add Zalo integration (popular in Vietnam)
- Add contact history tracking

### Order System
- Add order notes field (admin can add internal notes)
- Add delivery time slot selection
- Add customer rating after delivery
- Add order statistics dashboard

### Notification System
- Add sound notification for new orders
- Add browser push notifications
- Add notification preferences (email, SMS, push)

---

**Completed by**: Kiro AI Assistant  
**Review Status**: Ready for testing
