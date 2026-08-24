package backend.dto;

import backend.security.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
  @NotBlank
  private String token;

  @NotBlank
  @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
  private String newPassword;

  public ResetPasswordRequest() {}

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }

  public String getNewPassword() { return newPassword; }
  public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
