package com.springboot.jenka_coffee.exception;

public final class BannerDataIntegrityException {

    private BannerDataIntegrityException() {
    }

    public static String toFriendlyMessage(String rawMessage) {
        String msg = rawMessage == null ? "" : rawMessage.toLowerCase();

        if (!msg.contains("banner_set") && !msg.contains("banner_image")) {
            return null;
        }

        String columnName = extractQuotedValue(rawMessage);
        if (msg.contains("null value in column")) {
            return switch (columnName == null ? "" : columnName) {
                case "name" -> "Thiếu trường bắt buộc: tên bộ banner.";
                case "effect" -> "Thiếu trường bắt buộc: hiệu ứng chuyển banner.";
                case "active" -> "Thiếu trường bắt buộc: trạng thái hiển thị banner.";
                case "image" -> "Thiếu trường bắt buộc: ảnh banner.";
                case "banner_set_id" -> "Thiếu liên kết bộ banner cho ảnh banner.";
                default -> "Có trường banner bắt buộc chưa được nhập.";
            };
        }

        if (msg.contains("duplicate key") || msg.contains("unique constraint")) {
            return columnName != null && !columnName.isBlank()
                    ? "Dữ liệu banner bị trùng ở trường: " + columnName + "."
                    : "Dữ liệu banner bị trùng, vui lòng kiểm tra lại.";
        }

        if ((msg.contains("check constraint") || msg.contains("violates check constraint")) && msg.contains("effect")) {
            return "Giá trị hiệu ứng banner không hợp lệ.";
        }

        if (msg.contains("value too long")) {
            return "Dữ liệu banner vượt quá độ dài cho phép.";
        }

        return null;
    }

    private static String extractQuotedValue(String message) {
        if (message == null) {
            return null;
        }
        int start = message.indexOf('"');
        int end = start >= 0 ? message.indexOf('"', start + 1) : -1;
        if (start >= 0 && end > start) {
            return message.substring(start + 1, end);
        }
        return null;
    }
}
