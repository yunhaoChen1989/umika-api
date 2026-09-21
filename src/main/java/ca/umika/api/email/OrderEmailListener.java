package ca.umika.api.email;

import ca.umika.api.order.OrderResponse;
import ca.umika.api.store.LocationEntity;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserProfileEntity;
import ca.umika.api.user.UserProfileRepository;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);
    private static final DateTimeFormatter PICKUP_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a", Locale.CANADA);

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final UserProfileRepository userProfileRepository;
    private final LocationRepository locationRepository;
    private final boolean enabled;
    private final String fromAddress;
    private final String fromName;
    private final String replyTo;
    private final String publicBaseUrl;

    public OrderEmailListener(
            JavaMailSender mailSender,
            EmailLogRepository emailLogRepository,
            UserProfileRepository userProfileRepository,
            LocationRepository locationRepository,
            @Value("${app.email.enabled:false}") boolean enabled,
            @Value("${app.email.from-address:info@umikasushi.ca}") String fromAddress,
            @Value("${app.email.from-name:Umika Sushi}") String fromName,
            @Value("${app.email.reply-to:info@umikasushi.ca}") String replyTo,
            @Value("${app.public-base-url:https://umikasushi.ca}") String publicBaseUrl
    ) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.userProfileRepository = userProfileRepository;
        this.locationRepository = locationRepository;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.replyTo = replyTo;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(OrderEmailEvent event) {
        OrderResponse order = event.order();
        if (!enabled || order == null || isBlank(order.customerEmail())) {
            return;
        }

        String language = preferredLanguage(order);
        EmailContent content = content(event, language);
        EmailLogEntity emailLog = new EmailLogEntity();
        emailLog.setUserId(order.userId());
        emailLog.setRecipientEmail(order.customerEmail());
        emailLog.setSubject(content.subject());
        emailLog.setStatus("PENDING");
        emailLog.setProvider("BREVO_SMTP");
        emailLog = emailLogRepository.save(emailLog);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setReplyTo(replyTo);
            helper.setTo(order.customerEmail());
            helper.setSubject(content.subject());
            helper.setText(content.plainText(), content.html());
            mailSender.send(message);

            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
            emailLogRepository.save(emailLog);
            log.info("order email sent type={} orderId={} orderNumber={} recipient={}",
                    event.type(), order.id(), order.orderNumber(), order.customerEmail());
        } catch (Exception exception) {
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(limit(exception.getMessage(), 1000));
            emailLogRepository.save(emailLog);
            log.warn("order email failed type={} orderId={} orderNumber={} recipient={} message={}",
                    event.type(), order.id(), order.orderNumber(), order.customerEmail(), exception.getMessage());
        }
    }

    private EmailContent content(OrderEmailEvent event, String language) {
        return switch (event.type()) {
            case ACCEPTED -> acceptedContent(event.order(), language);
            case READY -> readyContent(event.order(), language);
            case CANCELLED -> cancelledContent(event.order(), language);
            case REFUNDED -> refundedContent(event, language);
        };
    }

    private EmailContent acceptedContent(OrderResponse order, String language) {
        String pickupTime = formatPickupTime(order.requestedPickupTime());
        String subject;
        String heading;
        String message;
        if ("zh".equals(language)) {
            subject = "Umika Sushi 已接受你的订单 " + orderNumber(order);
            heading = "订单已接受";
            message = "我们已收到付款并接受你的订单。预计自取时间：" + pickupTime + "。";
        } else if ("ko".equals(language)) {
            subject = "Umika Sushi 주문이 접수되었습니다 " + orderNumber(order);
            heading = "주문이 접수되었습니다";
            message = "결제가 완료되었으며 주문이 접수되었습니다. 픽업 예정 시간: " + pickupTime + ".";
        } else {
            subject = "Your Umika Sushi order was accepted " + orderNumber(order);
            heading = "Order accepted";
            message = "Your payment is complete and the restaurant has accepted your order. Estimated pickup time: " + pickupTime + ".";
        }
        return buildOrderContent(order, subject, heading, message, language);
    }

    private EmailContent readyContent(OrderResponse order, String language) {
        String subject;
        String heading;
        String message;
        if ("zh".equals(language)) {
            subject = "Umika Sushi 订单可以取餐了 " + orderNumber(order);
            heading = "你的订单已准备好";
            message = "你的订单已经准备好，可以到店取餐。";
        } else if ("ko".equals(language)) {
            subject = "Umika Sushi 주문 픽업 준비 완료 " + orderNumber(order);
            heading = "주문 픽업 준비가 완료되었습니다";
            message = "주문이 준비되었습니다. 매장에서 픽업해 주세요.";
        } else {
            subject = "Your Umika Sushi order is ready for pickup " + orderNumber(order);
            heading = "Ready for pickup";
            message = "Your order is ready. Please come to the restaurant to pick it up.";
        }
        return buildOrderContent(order, subject, heading, message, language);
    }

    private EmailContent cancelledContent(OrderResponse order, String language) {
        String subject;
        String heading;
        String message;
        if ("zh".equals(language)) {
            subject = "Umika Sushi 订单已取消 " + orderNumber(order);
            heading = "订单已取消";
            message = "你的订单已取消。如果已经付款，退款确认将通过另一封邮件发送。";
        } else if ("ko".equals(language)) {
            subject = "Umika Sushi 주문 취소 안내 " + orderNumber(order);
            heading = "주문이 취소되었습니다";
            message = "주문이 취소되었습니다. 결제가 완료된 경우 환불 확인은 별도의 이메일로 보내드립니다.";
        } else {
            subject = "Your Umika Sushi order was cancelled " + orderNumber(order);
            heading = "Order cancelled";
            message = "Your order has been cancelled. If payment was collected, a separate email will confirm the refund.";
        }
        return buildOrderContent(order, subject, heading, message, language);
    }

    private EmailContent refundedContent(OrderEmailEvent event, String language) {
        OrderResponse order = event.order();
        String amount = money(event.refundAmount());
        String subject;
        String heading;
        String message;
        if ("zh".equals(language)) {
            subject = "Umika Sushi 退款确认 " + orderNumber(order);
            heading = event.fullyRefunded() ? "订单已全额退款" : "订单已部分退款";
            message = "退款金额：" + amount + "。款项返回原付款方式所需时间由银行决定。";
        } else if ("ko".equals(language)) {
            subject = "Umika Sushi 환불 확인 " + orderNumber(order);
            heading = event.fullyRefunded() ? "전액 환불 완료" : "부분 환불 완료";
            message = "환불 금액: " + amount + ". 환불 금액이 원래 결제 수단에 표시되는 시점은 은행에 따라 다릅니다.";
        } else {
            subject = "Umika Sushi refund confirmation " + orderNumber(order);
            heading = event.fullyRefunded() ? "Full refund completed" : "Partial refund completed";
            message = "Refund amount: " + amount + ". Your bank determines when the refund appears on the original payment method.";
        }
        return buildOrderContent(order, subject, heading, message, language);
    }

    private EmailContent buildOrderContent(OrderResponse order, String subject, String heading, String message, String language) {
        String customerName = isBlank(order.customerName()) ? greeting(language) : escape(order.customerName());
        String items = order.items() == null || order.items().isEmpty()
                ? ""
                : order.items().stream()
                .map(item -> "<tr><td style=\"padding:6px 0\">" + escape(item.itemName()) + " × " + item.quantity()
                        + "</td><td style=\"padding:6px 0;text-align:right\">" + money(item.totalPrice()) + "</td></tr>")
                .reduce("", String::concat);
        String location = locationName(order);
        String html = """
                <!doctype html><html><body style="margin:0;background:#f6f3ed;font-family:Arial,sans-serif;color:#173b2d">
                <div style="max-width:600px;margin:0 auto;padding:28px 16px">
                  <div style="background:#fff;border-radius:14px;padding:28px;border:1px solid #e5ded2">
                    <div style="font-size:14px;font-weight:700;color:#b42318;letter-spacing:.08em">UMIKA SUSHI</div>
                    <h1 style="font-size:26px;margin:14px 0">%s</h1>
                    <p style="font-size:16px;line-height:1.6">%s,</p>
                    <p style="font-size:16px;line-height:1.6">%s</p>
                    <div style="margin:22px 0;padding:16px;background:#f3f8f5;border-radius:10px">
                      <div><strong>%s</strong></div>
                      <div style="margin-top:6px">%s</div>
                      <div style="margin-top:6px">%s</div>
                    </div>
                    <table style="width:100%%;border-collapse:collapse">%s</table>
                    <div style="border-top:1px solid #ddd;margin-top:14px;padding-top:14px;text-align:right;font-size:18px"><strong>%s</strong></div>
                    <p style="margin-top:26px"><a href="%s/order#order-history" style="color:#176b4d">%s</a></p>
                  </div>
                </div></body></html>
                """.formatted(
                escape(heading), customerName, escape(message), escape(orderNumber(order)),
                escape(location), escape(formatPickupTime(order.requestedPickupTime())), items,
                money(order.finalTotal()), escape(publicBaseUrl), accountLabel(language)
        );
        String plain = heading + "\n\n" + strip(customerName) + ",\n\n" + message
                + "\n\n" + orderNumber(order) + "\n" + location
                + "\nPickup: " + formatPickupTime(order.requestedPickupTime())
                + "\nTotal: " + money(order.finalTotal()) + "\n\n" + publicBaseUrl + "/order#order-history";
        return new EmailContent(subject, plain, html);
    }

    private String preferredLanguage(OrderResponse order) {
        if (order.userId() == null) {
            return "en";
        }
        UserProfileEntity profile = userProfileRepository.findByUserId(order.userId()).orElse(null);
        String value = profile == null ? null : profile.getPreferredLanguage();
        if (value == null) {
            return "en";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("zh")) return "zh";
        if (normalized.startsWith("ko")) return "ko";
        return "en";
    }

    private String locationName(OrderResponse order) {
        if (order.locationId() == null) {
            return "Umika Sushi";
        }
        LocationEntity location = locationRepository.findById(order.locationId()).orElse(null);
        if (location == null) {
            return "Umika Sushi";
        }
        return location.getName() + " — " + location.getAddressLine1() + ", " + location.getCity();
    }

    private static String orderNumber(OrderResponse order) {
        return "Order #" + (isBlank(order.orderNumber()) ? order.id() : order.orderNumber());
    }

    private static String formatPickupTime(LocalDateTime value) {
        return value == null ? "To be confirmed" : PICKUP_FORMAT.format(value);
    }

    private static String money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String greeting(String language) {
        return switch (language) {
            case "zh" -> "您好";
            case "ko" -> "안녕하세요";
            default -> "Hello";
        };
    }

    private static String accountLabel(String language) {
        return switch (language) {
            case "zh" -> "查看订单";
            case "ko" -> "주문 보기";
            default -> "View your order";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String strip(String value) {
        return value == null ? "" : value.replaceAll("<[^>]+>", "");
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private record EmailContent(String subject, String plainText, String html) {
    }
}
