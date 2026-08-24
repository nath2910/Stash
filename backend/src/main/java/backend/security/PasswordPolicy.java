package backend.security;

public final class PasswordPolicy {

  public static final int MIN_LENGTH = 10;
  public static final int MAX_LENGTH = 100;

  private PasswordPolicy() {
  }

  public static boolean isTooShort(String password) {
    return password == null || password.length() < MIN_LENGTH;
  }
}
