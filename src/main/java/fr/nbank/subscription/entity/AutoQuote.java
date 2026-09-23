package fr.nbank.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auto_quote", indexes = @Index(name = "idx_auto_quote_owner_created", columnList = "owner_id,created_at"))
public class AutoQuote {
  @Id private UUID id;
  @Column(name = "owner_id", nullable = false, length = 160) private String ownerId;
  @Column(name = "registration_number", nullable = false, length = 16) private String registrationNumber;
  @Column(name = "vehicle_description", nullable = false, length = 140) private String vehicleDescription;
  @Column(name = "insured_name", nullable = false, length = 120) private String insuredName;
  @Column(name = "details_json", nullable = false, columnDefinition = "TEXT") private String detailsJson;
  @Column(name = "options_json", nullable = false, columnDefinition = "TEXT") private String optionsJson;
  @Column(nullable = false, length = 24) private String status;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  @Version private long version;

  protected AutoQuote() {}
  public AutoQuote(UUID id, String ownerId, String registrationNumber, String vehicleDescription, String insuredName,
      String detailsJson, String optionsJson, String status, Instant createdAt, Instant expiresAt) {
    this.id=id; this.ownerId=ownerId; this.registrationNumber=registrationNumber; this.vehicleDescription=vehicleDescription;
    this.insuredName=insuredName; this.detailsJson=detailsJson; this.optionsJson=optionsJson; this.status=status;
    this.createdAt=createdAt; this.expiresAt=expiresAt;
  }
  public UUID getId(){return id;} public String getOwnerId(){return ownerId;} public String getRegistrationNumber(){return registrationNumber;}
  public String getVehicleDescription(){return vehicleDescription;} public String getInsuredName(){return insuredName;}
  public String getDetailsJson(){return detailsJson;} public String getOptionsJson(){return optionsJson;} public String getStatus(){return status;}
  public void setStatus(String status){this.status=status;} public Instant getCreatedAt(){return createdAt;} public Instant getExpiresAt(){return expiresAt;}
}
