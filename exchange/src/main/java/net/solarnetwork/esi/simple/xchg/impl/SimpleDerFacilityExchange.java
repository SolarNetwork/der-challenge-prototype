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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
import net.solarnetwork.esi.domain.DerRouteOrBuilder;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.PriceDatum;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.PriceMapOfferStatusResponse;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityRegistrationEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;
import net.solarnetwork.esi.util.CryptoHelper;

/**
 * Really, really, really simple gRPC implementation of a DER operator service.
 * 
 * @author matt
 * @version 1.0
 */
@GrpcService
public class SimpleDerFacilityExchange extends DerFacilityExchangeImplBase {

  /** The form field key for the Utility Interconnection Customer Identifier value. */
  public static final String FORM_KEY_UICI = "uici";

  /** The form field key for the cutomer's ID value. */
  public static final String FORM_KEY_CUSTOMER_ID = "cust-id";

  /** The form field key for the cutomer's surname value. */
  public static final String FORM_KEY_CUSTOMER_SURNAME = "cust-surname";

  private final String operatorUid;
  private final List<Form> registrationForms;
  private final KeyPair operatorKeyPair;
  private final CryptoHelper cryptoHelper;

  @Autowired
  private FacilityEntityDao facilityDao;

  @Autowired
  private FacilityRegistrationEntityDao facilityRegistrationDao;

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
   * @param cryptoHelper
   *        the {@link CryptoHelper} to use
   * @throws IllegalArgumentException
   *         if either {@code operatorUid} or {@code registreationForms} are {@literal null} or
   *         empty
   */
  @Autowired
  public SimpleDerFacilityExchange(@Qualifier("operator-uid") String operatorUid,
      @Qualifier("operator-key-pair") KeyPair operatorKeyPair,
      @Qualifier("regform-list") List<Form> registrationForms, CryptoHelper cryptoHelper) {
    super();
    if (operatorUid == null || operatorUid.isEmpty()) {
      throw new IllegalArgumentException("The operator UID must not be empty.");
    }
    this.operatorUid = operatorUid;
    if (registrationForms == null || registrationForms.isEmpty()) {
      throw new IllegalArgumentException("The registration forms list must not be empty.");
    }
    this.registrationForms = Collections.unmodifiableList(new ArrayList<>(registrationForms));
    this.operatorKeyPair = operatorKeyPair;
    this.cryptoHelper = cryptoHelper;
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

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void submitDerFacilityRegistrationForm(DerFacilityRegistrationFormData request,
      StreamObserver<DerFacilityRegistrationFormDataReceipt> responseObserver) {

    try {
      DerRouteOrBuilder route = request.getRouteOrBuilder();
      if (route == null) {
        throw new IllegalArgumentException("Route missing");
      }

      if (!operatorUid.equals(route.getOperatorUid())) {
        throw new IllegalArgumentException("Operator UID not valid");
      }

      String facilityUid = route.getFacilityUid();
      if (facilityUid == null || facilityUid.trim().isEmpty()) {
        throw new IllegalArgumentException("Facility UID missing");
      }

      String facilityEndpointUri = request.getFacilityEndpointUri();
      if (facilityEndpointUri == null || facilityEndpointUri.trim().isEmpty()) {
        throw new IllegalArgumentException("Facility endpoint URI missing");
      }
      try {
        new URI(facilityEndpointUri);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Facility endpoint URI syntax not valid", e);
      }

      ByteString facilityNonce = request.getFacilityNonce();
      if (facilityNonce == null || facilityNonce.isEmpty()) {
        throw new IllegalArgumentException("Facility nonce missing");
      } else if (facilityNonce.size() < 8) {
        throw new IllegalArgumentException("Facility nonce must be at least 8 bytes long");
      } else if (facilityNonce.size() > 24) {
        throw new IllegalArgumentException("Facility nonce must be at most 24 bytes long");
      }

      FormData formData = request.getData();
      if (formData == null) {
        throw new IllegalArgumentException("Form data missing");
      }
      String formKey = formData.getKey();
      if (formKey == null || formKey.trim().isEmpty()) {
        throw new IllegalArgumentException("Form key missing");
      }
      Form form = registrationForms.stream().filter(f -> formKey.equals(f.getKey())).findFirst()
          .orElse(null);
      if (form == null) {
        throw new IllegalArgumentException("Form key invalid");
      }

      String uici = formData.getDataOrDefault(FORM_KEY_UICI, null);
      if (uici == null || uici.trim().isEmpty()) {
        throw new IllegalArgumentException("UICI value missing");
      } else if (!uici.matches("[1-9]{3}-[1-9]{4}-[1-9]{4}")) {
        throw new IllegalArgumentException("UICI invliad syntax; must be in form 123-1234-1234");
      }

      String custId = formData.getDataOrDefault(FORM_KEY_CUSTOMER_ID, null);
      if (custId == null || custId.trim().isEmpty()) {
        throw new IllegalArgumentException("Customer number value missing");
      } else if (!custId.matches("[A-Z]{3}[0-9]{9}")) {
        throw new IllegalArgumentException(
            "Customer number invalid syntax; must be in form ABC123456789");
      }

      String custSurname = formData.getDataOrDefault(FORM_KEY_CUSTOMER_SURNAME, null);
      if (custSurname == null || custSurname.trim().isEmpty()) {
        throw new IllegalArgumentException("Customer surname value missing");
      }

      // wow, it passed validation checks; generate our nonce and persist registration entity
      byte[] opNonce = new byte[24];
      SecureRandom.getInstanceStrong().nextBytes(opNonce);

      FacilityRegistrationEntity entity = new FacilityRegistrationEntity(Instant.now());
      entity.setCustomerId(custId);
      entity.setUici(uici);
      entity.setFacilityUid(facilityUid);
      entity.setFacilityEndpointUri(facilityEndpointUri);
      entity.setFacilityNonce(facilityNonce.toByteArray());
      entity.setOperatorNonce(opNonce);
      entity = facilityRegistrationDao.save(entity);

      // kick off async registration confirmation process
      facilityRegistrationService.processFacilityRegistration(entity);

      responseObserver.onNext(DerFacilityRegistrationFormDataReceipt.newBuilder()
          .setOperatorNonce(ByteString.copyFrom(opNonce)).build());
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
    } catch (NoSuchAlgorithmException e) {
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
   * Set the DAO to use for facility data.
   * 
   * @param facilityDao
   *        the facility DAO to use
   */
  public void setFacilityDao(FacilityEntityDao facilityDao) {
    this.facilityDao = facilityDao;
  }

  /**
   * Set the DAO to use for facility registration data.
   * 
   * @param facilityRegistrationDao
   *        the facility registration DAO to use
   */
  public void setFacilityRegistrationDao(FacilityRegistrationEntityDao facilityRegistrationDao) {
    this.facilityRegistrationDao = facilityRegistrationDao;
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
