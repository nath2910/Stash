package backend.controller;

import backend.dto.LoginResponse;
import backend.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import backend.dto.LoginRequest;
import backend.dto.ChangePasswordRequest;
import backend.dto.EmailVerificationRequest;
import backend.dto.ForgotPasswordRequest;
import backend.dto.RegisterRequest;
import backend.dto.ResetPasswordRequest;
import backend.dto.UserMapper;
import backend.dto.UserMeResponse;
import backend.entity.User;
import backend.service.EmailVerificationService;
import backend.service.PasswordResetService;
import backend.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          PasswordResetService passwordResetService,
                          EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserMeResponse register(@RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return UserMapper.toMe(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user.getId());
        return new LoginResponse(UserMapper.toMe(user), token);
    }

    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser.getId(), request);
        return "Mot de passe modifie";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request != null ? request.getEmail() : null);
        return "Si un compte existe, un email a ete envoye";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return "Mot de passe modifie";
    }

    @GetMapping("/verify-email")
    public LoginResponse verifyEmail(@RequestParam("token") String token) {
        User user = emailVerificationService.verifyToken(token);
        String tokenJwt = jwtService.generateToken(user.getId());
        return new LoginResponse(UserMapper.toMe(user), tokenJwt);
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestBody EmailVerificationRequest request) {
        emailVerificationService.requestVerification(request != null ? request.getEmail() : null);
        return "Si un compte existe, un email a ete envoye";
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal User currentUser) {
        return new UserMeResponse(
            currentUser.getId(),
            currentUser.getEmail(),
            currentUser.getFirstName(),
            currentUser.getLastName(),
            currentUser.getPictureUrl(),
            currentUser.getProvider(),
            currentUser.isEmailVerified()
        );
    }

    @DeleteMapping("/me")
    public String deleteMe(@AuthenticationPrincipal User currentUser) {
        userService.deleteAccount(currentUser.getId());
        return "Compte supprim\u00e9";
    }
}
