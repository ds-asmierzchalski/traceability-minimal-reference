package net.catenax.traceability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppReadyListener {

   @Autowired
   private EdcSetup edcSetup;

   @EventListener(ApplicationReadyEvent.class)
   public void onApplicationReady() {
      edcSetup.setupTraceabilityEdcOffer();
   }
}
