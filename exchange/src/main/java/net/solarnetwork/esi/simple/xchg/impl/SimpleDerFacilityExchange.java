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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DatumRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.DerProgram;
import net.solarnetwork.esi.domain.DerResourceCharacteristics;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.PriceDatum;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.PriceMapOfferStatusResponse;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;

/**
 * Really, really, really simple gRPC implementation of a DER facility exchange.
 * 
 * @author matt
 * @version 1.0
 */
@GrpcService
public class SimpleDerFacilityExchange extends DerFacilityExchangeImplBase {

  private final String operatorUid;
  private final List<Form> registrationForms;
  private final KeyPair operatorKeyPair;

  @Autowired
  private FacilityRegistrationService facilityRegistrationService;

  /**
   * Constructor.
   * 
   * @param operatorUid
   *        the UID to use for this service
   * @param operatorKeyPair
   *        the key pair to use for asymmetric encryption with facilities
   * @param registrationForms
   *        the registration form, as a list to support multiple languages
   * @throws IllegalArgumentException
   *         if any parameter is {@literal null} or empty
   */
  @Autowired
  public SimpleDerFacilityExchange(@Qualifier("operator-uid") String operatorUid,
      @Qualifier("operator-key-pair") KeyPair operatorKeyPair,
      @Qualifier("regform-list") List<Form> registrationForms) {
    super();
    if (operatorUid == null || operatorUid.isEmpty()) {
      throw new IllegalArgumentException("The operator UID must not be empty.");
    }
    this.operatorUid = operatorUid;
    if (operatorKeyPair == null) {
      throw new IllegalArgumentException("The operator key pair must be provided.");
    }
    this.operatorKeyPair = operatorKeyPair;
    if (registrationForms == null || registrationForms.isEmpty()) {
      throw new IllegalArgumentException("The registration forms list must not be empty.");
    }
    this.registrationForms = Collections.unmodifiableList(new ArrayList<>(registrationForms));
  }

  @Override
  public void getPublicCryptoKey(Empty request, StreamObserver<CryptoKey> responseObserver) {
    // @formatter:off
    CryptoKey key = CryptoKey.newBuilder()
        .setAlgorithm(operatorKeyPair.getPublic().getAlgorithm())
        .setEncoding(operatorKeyPair.getPublic().getFormat())
        .setKey(ByteString.copyFrom(operatorKeyPair.getPublic().getEncoded()))
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
        .setOperatorUid(operatorUid).setForm(form).build();
    responseObserver.onNext(regForm);
    responseObserver.onCompleted();
  }

  @Override
  public void submitDerFacilityRegistrationForm(DerFacilityRegistrationFormData request,
      StreamObserver<DerFacilityRegistrationFormDataReceipt> responseObserver) {

    try {
      FacilityRegistrationEntity entity = facilityRegistrationService
          .submitDerFacilityRegistrationForm(request);

      // automatically kick off async registration confirmation process
      facilityRegistrationService.processFacilityRegistration(entity);

      responseObserver.onNext(DerFacilityRegistrationFormDataReceipt.newBuilder()
          .setOperatorNonce(ByteString.copyFrom(entity.getOperatorNonce())).build());
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
    } catch (RuntimeException e) {
      responseObserver.onError(Status.INTERNAL.withDescription("Internal crypto setup error")
          .withCause(e).asException());
    }
  }

  @Override
  public void providePriceMapOfferStatus(PriceMapOfferStatus request,
      StreamObserver<PriceMapOfferStatusResponse> responseObserver) {
    // TODO Auto-generated method stub
    super.providePriceMapOfferStatus(request, responseObserver);
  }

  @Override
  public void listPrices(DatumRequest request, StreamObserver<PriceDatum> responseObserver) {
    // TODO Auto-generated method stub
    super.listPrices(request, responseObserver);
  }

  @Override
  public StreamObserver<DerResourceCharacteristics> provideDerResourceCharacteristics(
      StreamObserver<Empty> responseObserver) {
    // TODO Auto-generated method stub
    return super.provideDerResourceCharacteristics(responseObserver);
  }

  @Override
  public StreamObserver<PriceMap> providePriceMaps(StreamObserver<Empty> responseObserver) {
    // TODO Auto-generated method stub
    return super.providePriceMaps(responseObserver);
  }

  @Override
  public StreamObserver<DerProgram> provideSupportedDerPrograms(
      StreamObserver<Empty> responseObserver) {
    // TODO Auto-generated method stub
    return super.provideSupportedDerPrograms(responseObserver);
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
