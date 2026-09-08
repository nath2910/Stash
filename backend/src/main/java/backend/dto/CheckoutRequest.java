package backend.dto;

public record CheckoutRequest(
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Pattern(regexp = "monthly|annual") String plan,
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.AssertTrue Boolean termsAccepted
) {
  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void rejectUnexpectedField(String name, Object value) {
    throw new IllegalArgumentException("Unexpected checkout field");
  }
}
