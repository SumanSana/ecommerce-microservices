package com.ecommerce.notificationservice.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.notificationservice.dto.OrderEvent;
import com.ecommerce.notificationservice.dto.OrderItem;
import com.ecommerce.notificationservice.dto.PaymentEvent;
import com.ecommerce.notificationservice.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

	private final MailService mailService;

	@KafkaListener(topics = "order-events-topic", containerFactory = "orderEvent")
	public void handleOrderEvents(OrderEvent event) {
		log.info("Notification: Order Event {} received for order {}", event.status(), event.orderId());

		String itemsHtml = buildItemsTable(event.items());

		switch (event.status()) {
		case "CONFIRMED" -> mailService.sendEmail(event.customerEmail(), "Order Received: #" + event.orderId(),
				"<h1>Order Confirmed</h1>" + "<p>We've received your order for <b>$" + event.amount() + "</b>.</p>"
						+ "<h3>Order Summary:</h3>" + itemsHtml
						+ "<p>We are checking our stock and will update you shortly.</p>");

		case "CANCELLED" -> mailService.sendEmail(event.customerEmail(), "Order Cancelled: #" + event.orderId(),
				"<h1>Order Cancelled</h1>" + "<p>Your order for the following items has been cancelled:</p>" + itemsHtml
						+ "<p>If you were already charged, a refund has been triggered.</p>");
		}
	}

	@KafkaListener(topics = "payment-response-topic", containerFactory = "paymentEvent")
	public void handlePaymentEvents(PaymentEvent event) {
		log.info("Notification: Payment Event {} received for order {}", event.status(), event.orderId());

		String email = event.customerEmail();
		String orderId = event.orderId().toString();
		String amount = event.amount().toString();

		switch (event.status()) {
		case "SUCCESS" ->
			mailService.sendEmail(email, "Payment Successful: #" + orderId, "<h1>Thank You!</h1><p>Your payment of $"
					+ amount + " was successful. We are now preparing your shipment.</p>");

		case "FAILED" -> mailService.sendEmail(email, "Action Required: Payment Failed",
				"<h1>Payment Failed</h1><p>Your payment for order #" + orderId
						+ " was declined. Please try a different card.</p>");

		case "REFUND_INITIATED" -> mailService.sendEmail(email, "Refund Initiated: #" + orderId,
				"<h1>Refund in Progress</h1><p>We have initiated a refund of $" + amount
						+ ". It should appear in your account within 5-10 business days.</p>");
		}
	}

	private String buildItemsTable(List<OrderItem> items) {
		if (items == null || items.isEmpty())
			return "";

		StringBuilder builder = new StringBuilder();
		builder.append("<table style='width:100%; border-collapse: collapse; border: 1px solid #ddd;'>")
				.append("<tr style='background-color: #f2f2f2;'>")
				.append("<th style='padding: 8px; text-align: left;'>Item SKU</th>")
				.append("<th style='padding: 8px; text-align: center;'>Qty</th>")
				.append("<th style='padding: 8px; text-align: right;'>Price</th>").append("</tr>");

		for (OrderItem item : items) {
			builder.append("<tr>").append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
					.append(item.skuId()).append("</td>")
					.append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: center;'>")
					.append(item.quantity()).append("</td>")
					.append("<td style='padding: 8px; border-bottom: 1px solid #ddd; text-align: right;'>$")
					.append(item.price()).append("</td>").append("</tr>");
		}

		builder.append("</table>");
		return builder.toString();
	}
}