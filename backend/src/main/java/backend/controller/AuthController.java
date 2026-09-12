package backend.controller;

import backend.dto.LoginResponse;
import backend.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import backend.dto.LoginRequest;
import backend.dto.ChangePasswordRequest;
import backend.dto.EmailVerificationRequest;
import backend.dto.EmailVerificationStatusResponse;
import backend.dto.ForgotPasswordRequest;
import backend.dto.RegisterRequest;
import backend.dto.ResetPasswordRequest;
import backend.dto.UserMapper;
import backend.dto.UserMeResponse;
import backend.entity.User;
import backend.service.EmailVerificationService;
import backend.service.PasswordResetService;
import backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final backend.service.AccountDeletionService accountDeletionService;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          PasswordResetService passwordResetService,
                          EmailVerificationService emailVerificationService,
                          backend.service.AccountDeletionService accountDeletionService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.accountDeletionService = accountDeletionService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @jakarta.validation.Valid RegisterRequest request) {
        try {
            User user = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toMe(user));
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT && request != null && request.getEmail() != null) {
                // Compte déjà présent : on renvoie un message explicite et on renvoie le lien de vérification au besoin.
                emailVerificationService.requestVerification(request.getEmail());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    Map.of("message", "Compte déjà existant. Si l'email n'est pas validé, un nouveau lien vient d'être envoyé.")
                );
            }
            throw ex;
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @jakarta.validation.Valid LoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user);
        return new LoginResponse(UserMapper.toMe(user), token);
    }

    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @jakarta.validation.Valid ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser.getId(), request);
        return "Mot de passe modifie";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody @jakarta.validation.Valid ForgotPasswordRequest request) {
        passwordResetService.requestReset(request != null ? request.getEmail() : null);
        return "Si un compte existe, un email a ete envoye";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody @jakarta.validation.Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return "Mot de passe modifie";
    }

    @GetMapping("/verify-email")
    public LoginResponse verifyEmail(@RequestParam("token") String token) {
        User user = emailVerificationService.verifyToken(token);
        String tokenJwt = jwtService.generateToken(user);
        return new LoginResponse(UserMapper.toMe(user), tokenJwt);
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestBody @jakarta.validation.Valid EmailVerificationRequest request) {
        emailVerificationService.requestVerification(request != null ? request.getEmail() : null);
        return "Si un compte existe, un email a ete envoye";
    }

    @PostMapping("/email-verification-status")
    public EmailVerificationStatusResponse emailVerificationStatus(
            @RequestBody @jakarta.validation.Valid EmailVerificationRequest request
    ) {
        boolean verified = emailVerificationService.isEmailVerified(request != null ? request.getEmail() : null);
        return new EmailVerificationStatusResponse(verified);
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal User currentUser) {
        return UserMapper.toMe(currentUser);
    }

    @PostMapping("/logout")
    public void logout(@AuthenticationPrincipal User currentUser) {
        userService.revokeSessions(currentUser.getId());
    }

    @DeleteMapping("/me")
    public String deleteMe(@AuthenticationPrincipal User currentUser) throws Exception {
        accountDeletionService.delete(currentUser.getId());
        return "Compte supprim\u00e9";
    }
}
