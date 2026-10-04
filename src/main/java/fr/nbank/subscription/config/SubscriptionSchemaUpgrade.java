package fr.nbank.subscription.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Card and offer subscriptions have no insurance plan or premium. Hibernate's
 * {@code ddl-auto=update} never relaxes NOT NULL on columns created by older versions.
 * Also migrates parrainage from email invites to shareable codes.
 */
@Component
public class SubscriptionSchemaUpgrade implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SubscriptionSchemaUpgrade.class);
  private static final String[] NULLABLE_COLUMNS = {"plan_code", "plan_name", "annual_premium"};
  private final JdbcTemplate jdbc;

  public SubscriptionSchemaUpgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    for (String column : NULLABLE_COLUMNS) {
      try {
        jdbc.execute("ALTER TABLE customer_subscription ALTER COLUMN " + column + " DROP NOT NULL");
      } catch (RuntimeException ex) {
        log.warn("Could not make customer_subscription.{} nullable: {}", column, ex.getMessage());
      }
    }
    // Parrainage: invited_email is optional (filled when friend signs up).
    try {
      jdbc.execute("ALTER TABLE customer_referral ALTER COLUMN invited_email DROP NOT NULL");
    } catch (RuntimeException ex) {
      log.warn("Could not make customer_referral.invited_email nullable: {}", ex.getMessage());
    }
    // Backfill referral_code for legacy rows (unique placeholder from id prefix).
    try {
      jdbc.execute("""
          UPDATE customer_referral
          SET referral_code = UPPER(REPLACE(CAST(id AS varchar), '-', ''))
          WHERE referral_code IS NULL OR referral_code = ''
          """);
    } catch (RuntimeException ex) {
      log.warn("Could not backfill customer_referral.referral_code: {}", ex.getMessage());
    }
  }
}
