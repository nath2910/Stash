package backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import backend.dto.LoginRequest;
import backend.dto.RegisterRequest;
import backend.dto.ChangePasswordRequest;

import backend.entity.User;
import backend.repository.UserRepository;
import backend.repository.EmailVerificationTokenRepository;
import backend.repository.PasswordResetTokenRepository;
import backend.repository.UserStatsLayoutRepository;
import backend.repository.SnkVenteRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserStatsLayoutRepository userStatsLayoutRepository;
    private final SnkVenteRepository snkVenteRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailVerificationService emailVerificationService,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       UserStatsLayoutRepository userStatsLayoutRepository,
                       SnkVenteRepository snkVenteRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userStatsLayoutRepository = userStatsLayoutRepository;
        this.snkVenteRepository = snkVenteRepository;
    }

    public User register(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email manquant");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe trop court");
        }

        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email)
                .ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise");
                });

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        String firstName = request.getFirstName() != null ? request.getFirstName().trim() : "";
        String lastName = request.getLastName() != null ? request.getLastName().trim() : "";

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(hashedPassword);

        try {
            User savedUser = userRepository.save(user);
            emailVerificationService.sendVerification(savedUser);
            return savedUser;
        } catch (DataIntegrityViolationException ex) {
            // Si une contrainte d'unicite est levee (course condition ou compte deja existant)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise");
        }
    }

    public User login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email manquant");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe manquant");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide"));

        if (user.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide");
        }

        boolean ok = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide");
        }

        // Pour l'instant, on renvoie juste le user (sans le hash dans la version front)
        return user;
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getNewPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champs manquants");
        }

        if (request.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le nouveau mot de passe doit faire au moins 6 caracteres");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (user.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte sans mot de passe local");
        }

        boolean ok = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }

        passwordResetTokenRepository.deleteByUser_Id(userId);
        emailVerificationTokenRepository.deleteByUserId(userId);
        userStatsLayoutRepository.deleteByUserId(userId);
        snkVenteRepository.deleteByUser_Id(userId);
        userRepository.deleteById(userId);
    }
}
