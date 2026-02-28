package com.ecommerce.paymentservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.paymentservice.dto.PaymentEvent;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.RefundEvent;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * 1. INITIATE PAYMENT
	 */
	@Transactional
	public String initiatePayment(PaymentRequest request) throws Exception {
		log.info("Creating Stripe Intent for Order: {} | Amount: {}", request.orderId(), request.amount());

		Payment payment = paymentRepository.findByOrderId(request.orderId()).orElseGet(() -> {
			Payment p = new Payment();
			p.setOrderId(request.orderId());
			p.setAmount(request.amount());
			p.setStatus(PaymentStatus.INITIATED);
			return p;
		});

		// Stripe requires amount in CENTS
		long amountInCents = request.amount().multiply(new BigDecimal(100)).longValue();

		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder().setAmount(amountInCents)
				.setCurrency("usd")
				// Adding Metadata correctly (one by one)
				.putMetadata("orderId", request.orderId().toString())
				.putMetadata("customerEmail", request.customerEmail())
				.putMetadata("amount", request.amount().toString())
				.setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true)
						.setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
						.build())
				.build();

		PaymentIntent intent = PaymentIntent.create(params);

		payment.setStripePaymentIntentId(intent.getId());
		paymentRepository.save(payment);

		return intent.getClientSecret();
	}

	/**
	 * 2. HANDLE WEBHOOK (SUCCESS OR FAILURE)
	 */
	@Transactional
	public void updatePaymentStatus(PaymentIntent intent) {
		String intentId = intent.getId();
		String status = intent.getStatus();

		// Extract metadata we saved earlier
		String customerEmail = intent.getMetadata().get("customerEmail");
		String orderIdStr = intent.getMetadata().get("orderId");
		String amountStr = intent.getMetadata().get("amount");
		UUID orderId = UUID.fromString(orderIdStr);
		BigDecimal amount = new BigDecimal(amountStr);

		paymentRepository.findByStripePaymentIntentId(intentId).ifPresentOrElse(payment -> {
			if ("succeeded".equals(status)) {
				payment.setStatus(PaymentStatus.SUCCESS);
				emitPaymentEvent(orderId, intentId, amount, "SUCCESS", customerEmail);
				log.info("Payment SUCCESS for Order: {}", orderId);
			} else if ("requires_payment_method".equals(status)) {
				payment.setStatus(PaymentStatus.FAILED);
				emitPaymentEvent(orderId, intentId, amount, "FAILED", customerEmail);
				log.error("Payment FAILED for Order: {}", orderId);
			}
			paymentRepository.save(payment);
		}, () -> log.error("CRITICAL: Received webhook for unknown Stripe ID: {}", intentId));
	}

	/**
	 * 3. PROCESS REFUND (Triggered by Kafka)
	 */
	@Transactional
	public void processRefund(RefundEvent event) {
		log.info("Processing refund logic for Order: {}", event.orderId());

		Payment payment = paymentRepository.findByOrderId(event.orderId())
				.orElseThrow(() -> new RuntimeException("No payment record found for Order: " + event.orderId()));

		try {
			if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
				log.info("Triggering Stripe Refund for Intent: {}", payment.getStripePaymentIntentId());

				RefundCreateParams params = RefundCreateParams.builder()
						.setPaymentIntent(payment.getStripePaymentIntentId())
						.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER).build();

				Refund refund = Refund.create(params);

				payment.setStatus(PaymentStatus.REFUNDED);
				emitPaymentEvent(payment.getOrderId(), refund.getId(), payment.getAmount(), "REFUND_INITIATED",
						event.customerEmail());
				log.info("Stripe Refund successful. Refund ID: {}", refund.getId());

			} else if (PaymentStatus.INITIATED.equals(payment.getStatus())) {
				log.info("Payment was only INITIATED. Marking as CANCELLED for Order: {}", event.orderId());
				payment.setStatus(PaymentStatus.CANCELLED);
				emitPaymentEvent(payment.getOrderId(), payment.getStripePaymentIntentId(), payment.getAmount(),
						"CANCELLED", event.customerEmail());
			}

			paymentRepository.save(payment);

		} catch (Exception e) {
			log.error("CRITICAL: Failed to process Stripe refund for Order {}: {}", event.orderId(), e.getMessage());
			payment.setStatus(PaymentStatus.REFUND_FAILED);
			paymentRepository.save(payment);
		}
	}

	private void emitPaymentEvent(UUID orderId, String paymentId, BigDecimal amount, String status,
			String customerEmail) {
		PaymentEvent event = new PaymentEvent(orderId, paymentId, amount, status, customerEmail);
		kafkaTemplate.send("payment-response-topic", orderId.toString(), event);
	}
}