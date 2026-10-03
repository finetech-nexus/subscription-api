package fr.nbank.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_subscription", indexes = {
    @Index(name = "idx_subscription_owner_created", columnList = "owner_id,created_at"),
    @Index(name = "idx_subscription_owner_external", columnList = "owner_id,product_type,external_reference")
})
public class CustomerSubscription {
  @Id private UUID id;
  @Column(name = "owner_id", nullable = false, length = 160) private String ownerId;
  @Column(name = "product_type", nullable = false, length = 40) private String productType;
  @Column(name = "product_id", length = 80) private String productId;
  @Column(name = "product_name", length = 160) private String productName;
  @Column(name = "tenant_id", length = 16) private String tenantId;
  @Column(name = "quote_id", unique = true) private UUID quoteId;
  @Column(name = "plan_code", length = 40) private String planCode;
  @Column(name = "plan_name", length = 100) private String planName;
  @Column(name = "annual_premium", precision = 12, scale = 2) private BigDecimal annualPremium;
  @Column(precision = 12, scale = 2) private BigDecimal amount;
  @Column(name = "monthly_fee", precision = 12, scale = 2) private BigDecimal monthlyFee;
  @Column(nullable = false, length = 8) private String currency;
  @Column(nullable = false, length = 24) private String status;
  @Column(name = "external_reference", length = 80) private String externalReference;
  @Column(length = 2) private String language;
  @Column(name = "details_json", nullable = false, columnDefinition = "TEXT") private String detailsJson;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  protected CustomerSubscription() {}
  public CustomerSubscription(UUID id, String ownerId, String productType, UUID quoteId, String planCode, String planName,
      BigDecimal annualPremium, String currency, String status, String language, String detailsJson, Instant createdAt, Instant updatedAt) {
    this.id=id; this.ownerId=ownerId; this.productType=productType; this.quoteId=quoteId; this.planCode=planCode;
    this.planName=planName; this.annualPremium=annualPremium; this.currency=currency; this.status=status;
    this.language=language; this.detailsJson=detailsJson; this.createdAt=createdAt; this.updatedAt=updatedAt;
  }
  public UUID getId(){return id;} public String getOwnerId(){return ownerId;} public String getProductType(){return productType;}
  public String getProductId(){return productId;} public String getProductName(){return productName;} public String getTenantId(){return tenantId;}
  public UUID getQuoteId(){return quoteId;} public String getPlanCode(){return planCode;} public String getPlanName(){return planName;}
  public BigDecimal getAnnualPremium(){return annualPremium;} public BigDecimal getAmount(){return amount;} public BigDecimal getMonthlyFee(){return monthlyFee;}
  public String getCurrency(){return currency;} public String getStatus(){return status;} public String getExternalReference(){return externalReference;}
  public String getLanguage(){return language;} public String getDetailsJson(){return detailsJson;}
  public void setStatus(String status){this.status=status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
  public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
  public void setPlanCode(String planCode){this.planCode=planCode;}
  public void setPlanName(String planName){this.planName=planName;}
  public void setDetailsJson(String detailsJson){this.detailsJson=detailsJson;}
  public void setProduct(String productId, String productName, String tenantId){this.productId=productId; this.productName=productName; this.tenantId=tenantId;}
  public void setPricing(BigDecimal amount, BigDecimal monthlyFee){this.amount=amount; this.monthlyFee=monthlyFee;}
  public void setExternalReference(String externalReference){this.externalReference=externalReference;}
}
