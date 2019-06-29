/* ========================================================================
 * Copyright 2019 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.esi.simple.xchg.impl;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DatumRequest;
import net.solarnetwork.esi.domain.DerCharacteristics;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.DerProgramSet;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.PriceDatum;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.PriceMapOfferStatusResponse;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;

/**
 * Really, really, really simple gRPC implementation of a DER facility exchange.
 * 
 * @author matt
 * @version 1.0
 */
@GrpcService
public class SimpleDerFacilityExchange extends DerFacilityExchangeImplBase {

  private final String exchangeUid;
  private final List<Form> registrationForms;
  private final KeyPair exchangeKeyPair;

  @Autowired
  private FacilityRegistrationService facilityRegistrationService;

  @Autowired
  private FacilityCharacteristicsService facilityCharacteristicsService;

  private static final Logger log = LoggerFactory.getLogger(SimpleDerFacilityExchange.class);

  /**
   * Constructor.
   * 
   * @param exchangeUid
   *        the UID to use for this service
   * @param exchangeKeyPair
   *        the key pair to use for asymmetric encryption with facilities
   * @param registrationForms
   *        the registration form, as a list to support multiple languages
   * @throws IllegalArgumentException
   *         if any parameter is {@literal null} or empty
   */
  @Autowired
  public SimpleDerFacilityExchange(@Qualifier("exchange-uid") String exchangeUid,
      @Qualifier("exchange-key-pair") KeyPair exchangeKeyPair,
      @Qualifier("regform-list") List<Form> registrationForms) {
    super();
    if (exchangeUid == null || exchangeUid.isEmpty()) {
      throw new IllegalArgumentException("The exchange UID must not be empty.");
    }
    this.exchangeUid = exchangeUid;
    if (exchangeKeyPair == null) {
      throw new IllegalArgumentException("The exchange key pair must be provided.");
    }
    this.exchangeKeyPair = exchangeKeyPair;
    if (registrationForms == null || registrationForms.isEmpty()) {
      throw new IllegalArgumentException("The registration forms list must not be empty.");
    }
    this.registrationForms = Collections.unmodifiableList(new ArrayList<>(registrationForms));
  }

  @Override
  public void getPublicCryptoKey(Empty request, StreamObserver<CryptoKey> responseObserver) {
    // @formatter:off
    CryptoKey key = CryptoKey.newBuilder()
        .setAlgorithm(exchangeKeyPair.getPublic().getAlgorithm())
        .setEncoding(exchangeKeyPair.getPublic().getFormat())
        .setKey(ByteString.copyFrom(exchangeKeyPair.getPublic().getEncoded()))
        .build();
    // @formatter:on
    responseObserver.onNext(key);
    responseObserver.onCompleted();
  }

  @Override
  public void getDerFacilityRegistrationForm(DerFacilityRegistrationFormRequest request,
      StreamObserver<DerFacilityRegistrationForm> responseObserver) {
    // try to match the request language to an available form language
    Locale reqLocale = (request.getLanguageCode() != null && !request.getLanguageCode().isEmpty()
        ? Locale.forLanguageTag(request.getLanguageCode())
        : null);
    Form form;
    if (registrationForms.size() == 1 || reqLocale == null) {
      form = registrationForms.get(0);
    } else {
      form = registrationForms.stream().filter(f -> {
        return (f.getLanguageCode() != null && !f.getLanguageCode().isEmpty() && Locale
            .forLanguageTag(f.getLanguageCode()).getLanguage().equals(reqLocale.getLanguage()));
      }).findFirst().orElse(registrationForms.get(0));
    }
    DerFacilityRegistrationForm regForm = DerFacilityRegistrationForm.newBuilder()
        .setExchangeUid(exchangeUid).setForm(form).build();
    responseObserver.onNext(regForm);
    responseObserver.onCompleted();
  }

  @Override
  public void submitDerFacilityRegistrationForm(DerFacilityRegistrationFormData request,
      StreamObserver<DerFacilityRegistrationFormDataReceipt> responseObserver) {
    log.info("Received facility registration submission: {}", request);
    try {
      FacilityRegistrationEntity entity = facilityRegistrationService
          .submitDerFacilityRegistrationForm(request);

      // automatically kick off async registration confirmation process
      facilityRegistrationService.processFacilityRegistration(entity);

      responseObserver.onNext(DerFacilityRegistrationFormDataReceipt.newBuilder()
          .setExchangeNonce(ByteString.copyFrom(entity.getExchangeNonce())).build());
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
    } catch (RuntimeException e) {
      responseObserver
          .onError(Status.INTERNAL.withDescription("Internal error").withCause(e).asException());
    }
  }

  @Override
  public void providePriceMapOfferStatus(PriceMapOfferStatus request,
      StreamObserver<PriceMapOfferStatusResponse> responseObserver) {
  }

  @Override
  public void listPrices(DatumRequest request, StreamObserver<PriceDatum> responseObserver) {
    // TODO Auto-generated method stub
    super.listPrices(request, responseObserver);
  }

  @Override
  public StreamObserver<DerCharacteristics> provideDerCharacteristics(
      StreamObserver<Empty> responseObserver) {
    return new StreamObserver<DerCharacteristics>() {

      private boolean error = false;

      @Override
      public void onNext(DerCharacteristics value) {
        log.info("Received DER characteristics submission: {}", value);
        try {
          facilityCharacteristicsService.saveResourceCharacteristics(value);
        } catch (IllegalArgumentException e) {
          error = true;
          responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
        } catch (RuntimeException e) {
          error = true;
          log.error("Error processing DER characteristics: " + e.getMessage(), e);
          responseObserver.onError(
              Status.INTERNAL.withDescription("Internal error").withCause(e).asException());
        }
      }

      @Override
      public void onError(Throwable t) {
        log.error("Error receiving facility DER characteristics", t);
      }

      @Override
      public void onCompleted() {
        if (!error) {
          responseObserver.onNext(Empty.getDefaultInstance());
          responseObserver.onCompleted();
        }
      }
    };
  }

  @Override
  public StreamObserver<PriceMap> providePriceMaps(StreamObserver<Empty> responseObserver) {
    // TODO Auto-generated method stub
    return super.providePriceMaps(responseObserver);
  }

  @Override
  public StreamObserver<DerProgramSet> provideSupportedDerPrograms(
      StreamObserver<Empty> responseObserver) {
    return new StreamObserver<DerProgramSet>() {

      private boolean error = false;

      @Override
      public void onNext(DerProgramSet value) {
        log.info("Received DER program set submission: {}", value);
        try {
          facilityCharacteristicsService.saveActiveProgramTypes(value);
        } catch (IllegalArgumentException e) {
          error = true;
          responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
        } catch (RuntimeException e) {
          error = true;
          log.error("Error processing DER characteristics: " + e.getMessage(), e);
          responseObserver.onError(
              Status.INTERNAL.withDescription("Internal error").withCause(e).asException());
        }
      }

      @Override
      public void onError(Throwable t) {
        log.error("Error receiving facility DER program set", t);
      }

      @Override
      public void onCompleted() {
        if (!error) {
          responseObserver.onNext(Empty.getDefaultInstance());
          responseObserver.onCompleted();
        }
      }
    };
  }

  /**
   * Set the registration service.
   * 
   * @param facilityRegistrationService
   *        the registration service to use
   */
  public void setFacilityRegistrationService(
      FacilityRegistrationService facilityRegistrationService) {
    this.facilityRegistrationService = facilityRegistrationService;
  }

}
