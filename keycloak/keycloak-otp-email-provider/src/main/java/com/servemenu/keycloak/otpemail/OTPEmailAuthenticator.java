package com.servemenu.keycloak.otpemail;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class OTPEmailAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(OTPEmailAuthenticator.class);
    private static final String OTP_CODE = "otp_code";
    private static final String OTP_EXPIRY = "otp_expiry";
    private static final String OTP_ATTEMPTS = "otp_attempts";
    private static final String LAST_RESEND_TIME = "last_resend_time";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RESEND_COOLDOWN_MINUTES = 2;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String otp = OtpGenerator.generateOTP();
        long expiryTime = OtpGenerator.getExpiryTime();

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(OTP_CODE, otp);
        authSession.setAuthNote(OTP_EXPIRY, String.valueOf(expiryTime));
        authSession.setAuthNote(OTP_ATTEMPTS, "0");

        UserModel user = context.getUser();
        if (user == null || user.getEmail() == null) {
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        try {
            sendOTPEmail(context, user, otp);
            logger.infof("OTP sent to user: %s", user.getEmail());
        } catch (EmailException e) {
            logger.error("Failed to send OTP email", e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        Response challenge = context.form()
                .setAttribute("email", user.getEmail())
                .createForm("otp-email-verify.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String resendCode = formData.getFirst("resendCode");

        if ("true".equals(resendCode)) {
            handleResendCode(context);
            return;
        }
        
        String enteredOTP = formData.getFirst("otp");

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String storedOTP = authSession.getAuthNote(OTP_CODE);
        String expiryTimeStr = authSession.getAuthNote(OTP_EXPIRY);
        String attemptsStr = authSession.getAuthNote(OTP_ATTEMPTS);

        int attempts = Integer.parseInt(attemptsStr != null ? attemptsStr : "0");
        long expiryTime = Long.parseLong(expiryTimeStr != null ? expiryTimeStr : "0");

        if (attempts >= MAX_ATTEMPTS) {
            logger.warnf("Max OTP attempts exceeded for user: %s", context.getUser().getEmail());
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            return;
        }

        if (!OtpGenerator.isOTPValid(expiryTime)) {
            logger.warnf("OTP expired for user: %s", context.getUser().getEmail());
            Response challenge = context.form()
                    .setError("otpExpired")
                    .setAttribute("email", context.getUser().getEmail())
                    .createForm("otp-email-verify.ftl");
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
            return;
        }

        if (enteredOTP == null || !enteredOTP.equals(storedOTP)) {
            attempts++;
            authSession.setAuthNote(OTP_ATTEMPTS, String.valueOf(attempts));

            logger.warnf("Invalid OTP attempt %d/%d for user: %s", attempts, MAX_ATTEMPTS, context.getUser().getEmail());

            Response challenge = context.form()
                    .setError("invalidOTP")
                    .setAttribute("email", context.getUser().getEmail())
                    .setAttribute("attemptsLeft", MAX_ATTEMPTS - attempts)
                    .setAttribute("remainingMinutes", OtpGenerator.getRemainingMinutes(expiryTime))
                    .createForm("otp-email-verify.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        logger.infof("OTP verified successfully for user: %s", context.getUser().getEmail());

        // Mark email as verified since OTP verification confirms email ownership
        UserModel user = context.getUser();
        user.setEmailVerified(true);
        logger.infof("Email marked as verified for user: %s", user.getEmail());

        // Trigger VERIFY_EMAIL event for Kafka event listener
        context.getEvent().event(org.keycloak.events.EventType.VERIFY_EMAIL);
        context.getEvent().user(user);
        context.getEvent().success();
        logger.infof("VERIFY_EMAIL event triggered for user: %s", user.getEmail());

        authSession.removeAuthNote(OTP_CODE);
        authSession.removeAuthNote(OTP_EXPIRY);
        authSession.removeAuthNote(OTP_ATTEMPTS);

        context.success();
    }

    private void handleResendCode(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String lastResendTimeStr = authSession.getAuthNote(LAST_RESEND_TIME);

        if (lastResendTimeStr != null) {
            long lastResendTime = Long.parseLong(lastResendTimeStr);
            long cooldownMillis = RESEND_COOLDOWN_MINUTES * 60 * 1000;
            long timeSinceLastResend = System.currentTimeMillis() - lastResendTime;
            
            if (timeSinceLastResend < cooldownMillis) {
                long remainingSeconds = (cooldownMillis - timeSinceLastResend) / 1000;
                logger.warnf("Resend cooldown active for user: %s, remaining: %d seconds", 
                        context.getUser().getEmail(), remainingSeconds);
                
                Response challenge = context.form()
                        .setError("resendCooldown")
                        .setAttribute("email", context.getUser().getEmail())
                        .setAttribute("cooldownSeconds", remainingSeconds)
                        .createForm("otp-email-verify.ftl");
                context.failureChallenge(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, challenge);
                return;
            }
        }
        
        String otp = OtpGenerator.generateOTP();
        long expiryTime = OtpGenerator.getExpiryTime();

        authSession.setAuthNote(OTP_CODE, otp);
        authSession.setAuthNote(OTP_EXPIRY, String.valueOf(expiryTime));
        authSession.setAuthNote(OTP_ATTEMPTS, "0");
        authSession.setAuthNote(LAST_RESEND_TIME, String.valueOf(System.currentTimeMillis()));

        UserModel user = context.getUser();
        try {
            sendOTPEmail(context, user, otp);
            logger.infof("OTP resent to user: %s", user.getEmail());
        } catch (EmailException e) {
            logger.error("Failed to resend OTP email", e);
            Response challenge = context.form()
                    .setError("emailSendError")
                    .setAttribute("email", user.getEmail())
                    .createForm("otp-email-verify.ftl");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
            return;
        }

        Response challenge = context.form()
                .setAttribute("email", user.getEmail())
                .setAttribute("remainingMinutes", OtpGenerator.getRemainingMinutes(expiryTime))
                .createForm("otp-email-verify.ftl");
        context.challenge(challenge);
    }

    private void sendOTPEmail(AuthenticationFlowContext context, UserModel user, String otp) throws EmailException {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
        emailProvider.setRealm(realm);
        emailProvider.setUser(user);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("otp", otp);
        attributes.put("validityMinutes", "5");
        attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());

        emailProvider.setAttribute("otp", otp);
        emailProvider.setAttribute("validityMinutes", "5");

        emailProvider.send(
                "emailOTPSubject",
                "email-otp-verification.ftl",
                attributes
        );
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getEmail() != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Required actions yok
    }

    @Override
    public void close() {
        // Cleanup
    }
}
