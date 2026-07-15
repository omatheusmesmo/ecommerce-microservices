package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.ActionType;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserActionToken;
import com.ecommerce.event.TokenConfirmationEvent;
import com.ecommerce.event.TokenUrlEvent;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.CryptoUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class UserService {

    private final static Logger LOG = Logger.getLogger(UserService.class);

    @Inject
    UserRepository userRepository;
    @Inject
    UserActionTokenService userActionTokenService;
    @Inject
    Event<TokenUrlEvent> tokenUrlEventEmitter;
    @Inject
    Event<TokenConfirmationEvent> tokenConfirmationEventEmitter;

    @Inject
    @ConfigProperty(name = "ADMIN_PASSWORD")
    Optional<String> adminPassword;

    @Inject
    @ConfigProperty(name = "app.frontend.base-url")
    String frontendBaseUrl;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (User.count("role = ?1", Role.ADMIN) == 0) {
            if (adminPassword.isPresent() && !adminPassword.get().isBlank()) {
                registerAdmin(adminPassword.get());
            } else {
                LOG.warn("ADMIN_PASSWORD not set; skipping initial admin provisioning");
            }
        }
    }

    private void registerAdmin(String password) {
        User admin = new User();
        admin.email = "admin@ecommerce.com";
        admin.passwordHash = CryptoUtil.hashPassword(password);
        admin.fullName = "Admin";
        admin.role = Role.ADMIN;
        admin.active = true;
        userRepository.persist(admin);
        LOG.infof("Admin user registered: %d", admin.id);
    }

    @Transactional
    public UserResponse register (RegisterRequest request){
        LOG.infof("Self-register attempt for email: %s", request.email());
        if (userRepository.findByEmail(request.email()) != null){
            LOG.warnf("Email already exists: %s", request.email());
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = CryptoUtil.hashPassword(request.password());
        User user = request.toUser(hashedPassword);
        user.active = false;

        userRepository.persist(user);
        LOG.infof("User self-registered successfully: %d", user.id);
        UserActionToken token = userActionTokenService.createForUser(user.id, ActionType.ACTIVATE);
        String rawToken = userActionTokenService.getRawToken(token);

        LOG.debugf("Activation token created: %s for user %s", rawToken, user.email);

        String activationUrl = frontendBaseUrl + "/activate?token=" + rawToken;
        tokenUrlEventEmitter.fire(new TokenUrlEvent(user.id, user.email, ActionType.ACTIVATE, activationUrl));
        return UserResponse.from(user);
    }

    @Transactional
    public void activateUser(ActivationRequest request) {
        var token = userActionTokenService.findByToken(request.token(), ActionType.ACTIVATE);
        if (token == null) {
            throw new SecurityException("Invalid activation token");
        }
        User user = userRepository.findById(token.userId);
        user.active = true;
        user.persist();
        userActionTokenService.deleteToken(token);
        tokenConfirmationEventEmitter.fire(new TokenConfirmationEvent(user.id, user.email, ActionType.ACTIVATE));
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email());
        if (user != null) {
            var token = userActionTokenService.createForUser(user.id, ActionType.RESET);
            String rawToken = userActionTokenService.getRawToken(token);
            LOG.debugf("Password reset token created: %s for user %s", rawToken, user.email);
            String resetUrl = frontendBaseUrl + "/reset-password?token=" + rawToken;
            tokenUrlEventEmitter.fire(new TokenUrlEvent(user.id, user.email, ActionType.RESET, resetUrl));
        }
    }

    @Transactional
    public void resetPassword(PasswordResetUpdateRequest request) {
        var token = userActionTokenService.findByToken(request.token(), ActionType.RESET);
        if (token == null) {
            throw new SecurityException("Invalid reset token");
        }
        User user = userRepository.findById(token.userId);
        user.passwordHash = CryptoUtil.hashPassword(request.newPassword());
        user.persist();
        userActionTokenService.deleteToken(token);
        tokenConfirmationEventEmitter.fire(new TokenConfirmationEvent(user.id, user.email, ActionType.RESET));
    }

    @Transactional
    public UserResponse promote(Long userId, Role newRole, Long promoteId){
        LOG.infof("Promotion attempt for user %d to role %s by user %d", userId, newRole, promoteId);

        if (!isAdmin(promoteId)){
            LOG.warnf("Unauthorized promotion attempt by user: %d", promoteId);
            throw new SecurityException("Only admins can promote users");
        }

        User user = userRepository.findById(userId);
        if (user == null){
            LOG.warnf("User not found for promotion: %d", userId);
            throw new IllegalArgumentException("User not found");
        }

        user.role = newRole;
        userRepository.persist(user);
        LOG.infof("User promoted successfully: %d to %s", userId, newRole);
        return UserResponse.from(user);
    }

    public UserResponse getProfile(Long userId){
        LOG.debugf("Fetching profile for user: %d", userId);
        User user = userRepository.findById(userId);
        if (user == null){
            LOG.warnf("User not found for profile: %d", userId);
            return null;
        }
        return UserResponse.from(user);
    }

    private boolean isAdmin(Long userId){
        User user = userRepository.findById(userId);
        return user != null && user.role == Role.ADMIN;
    }

    public List<UserResponse> listAll() {
        List<User> users = userRepository.listAll();
        return users.stream().map(UserResponse::from).toList();
    }

    public UserResponse getById(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            LOG.warnf("ID %d user not found!", userId);
            throw new NoSuchElementException("User not found");
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long userId, Long deleterId) {
        if (!isAdmin(deleterId)) {
            LOG.warnf("Unauthorized deletion attempt");
            throw new SecurityException("Only admins can delete users");
        }
        boolean deleted = userRepository.deleteById(userId);
        if (!deleted) {
            LOG.warnf("ID %d user not found!", userId);
            throw new NoSuchElementException("User not found");
        }
        LOG.infof("User deleted: %d", userId);
    }
}
