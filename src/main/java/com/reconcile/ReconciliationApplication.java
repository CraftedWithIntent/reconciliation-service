package com.reconcile;

import com.reconcile.config.ReconciliationConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.reconcile")
@EnableConfigurationProperties(ReconciliationConfigProperties.class)
public class ReconciliationApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReconciliationApplication.class, args);
  }
}
