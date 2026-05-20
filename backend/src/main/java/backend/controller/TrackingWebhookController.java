package backend.controller;

import backend.dto.TrackingWebhookResponse;
import backend.service.TrackingAggregatorService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delivery/webhooks")
public class TrackingWebhookController {

  private final TrackingAggregatorService trackingAggregatorService;

  public TrackingWebhookController(TrackingAggregatorService trackingAggregatorService) {
    this.trackingAggregatorService = trackingAggregatorService;
  }

  @PostMapping("/aftership")
  public TrackingWebhookResponse afterShip(
      @RequestBody String payload,
      @RequestHeader Map<String, String> headers
  ) {
    String signature = firstHeader(
        headers,
        "aftership-hmac-sha256",
        "X-AfterShip-Hmac-SHA256",
        "AfterShip-Hmac-SHA256",
        "X-Aftership-Signature",
        "X-Webhook-Signature"
    );
    String sharedSecret = firstHeader(headers, "X-Webhook-Secret", "X-AfterShip-Webhook-Secret");
    return trackingAggregatorService.handleWebhook(payload, signature, sharedSecret);
  }

  private String firstHeader(Map<String, String> headers, String... names) {
    if (headers == null || names == null) {
      return null;
    }
    for (String name : names) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }
}
