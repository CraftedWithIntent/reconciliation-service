package com.reconcile.controller;

import com.reconcile.dto.ReconciliationResult;
import com.reconcile.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reconcile")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReconciliationController {

  @Autowired private ReconciliationService reconciliationService;

  /**
   * Perform full reconciliation using SQL engine with MD5 row hashing GET /api/reconcile (uses
   * default domain: vendor-invoices) GET /api/reconcile?domain=expense-reports
   *
   * <p>The domain parameter specifies which reconciliation configuration to use, allowing this
   * endpoint to work with any configurable domain from application.yml
   */
  @GetMapping
  public ResponseEntity<ReconciliationResult> performFullReconciliation(
      @RequestParam(value = "domain", defaultValue = "vendor-invoices") String domain) {
    try {
      System.out.println("[ReconciliationController] Reconciliation request for domain: " + domain);
      ReconciliationResult result = reconciliationService.reconcile(domain);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException ex) {
      System.err.println("[ReconciliationController] Invalid domain: " + ex.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception ex) {
      System.err.println("[ReconciliationController] Error: " + ex.getMessage());
      ex.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /** Health check endpoint GET /api/reconcile/health */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Reconciliation Service is running");
  }
}
