package com.servemenu.keycloakevent.utils;

import org.keycloak.models.KeycloakTransaction;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction wrapper for publishing events after commit.
 * This ensures Kafka messages are only sent after the database transaction succeeds.
 */
public class EventPublisherTransaction implements KeycloakTransaction {

    private static final Logger LOG = Logger.getLogger(EventPublisherTransaction.class);

    private final List<Runnable> afterCommitActions = new ArrayList<>();
    private boolean active = false;
    private boolean rollbackOnly = false;

    @Override
    public void begin() {
        active = true;
        rollbackOnly = false;
        LOG.trace("EventPublisherTransaction begun");
    }

    @Override
    public void commit() {
        if (rollbackOnly) {
            LOG.warn("Transaction marked for rollback, skipping event publishing");
            afterCommitActions.clear();
            active = false;
            return;
        }

        LOG.debugf("Committing transaction with %d pending actions", afterCommitActions.size());

        for (Runnable action : afterCommitActions) {
            try {
                action.run();
            } catch (Exception e) {
                LOG.errorf(e, "Failed to execute after-commit action");
            }
        }

        afterCommitActions.clear();
        active = false;
        LOG.trace("EventPublisherTransaction committed");
    }

    @Override
    public void rollback() {
        LOG.infof("Transaction rolled back, clearing %d pending events", afterCommitActions.size());
        afterCommitActions.clear();
        active = false;
    }

    @Override
    public void setRollbackOnly() {
        rollbackOnly = true;
        LOG.warn("Transaction marked as rollback-only");
    }

    @Override
    public boolean getRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void addAfterCommitAction(Runnable action) {
        if (action == null) {
            LOG.warn("Attempted to add null action, ignoring");
            return;
        }
        afterCommitActions.add(action);
        LOG.tracef("Added after-commit action. Total pending: %d", afterCommitActions.size());
    }
}