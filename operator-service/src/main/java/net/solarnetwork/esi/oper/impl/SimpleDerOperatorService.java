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

package net.solarnetwork.esi.oper.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.solarnetwork.esi.domain.DatumRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.PriceDatum;
import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.PriceMapOfferStatusResponse;
import net.solarnetwork.esi.service.DerOperatorServiceGrpc.DerOperatorServiceImplBase;

/**
 * Really, really, really simple gRPC implementation of a DER operator service.
 * 
 * @author matt
 * @version 1.0
 */
@GrpcService
public class SimpleDerOperatorService extends DerOperatorServiceImplBase {

  private final String operatorUid;
  private final List<Form> registrationForms;

  /**
   * Constructor.
   * 
   * @param operatorUid
   *        the UID to use for this service
   * @param registrationForms
   *        the registration form, as a list to support multiple languages
   * @throws IllegalArgumentException
   *         if either {@code operatorUid} or {@code registreationForms} are {@literal null} or
   *         empty
   */
  @Autowired
  public SimpleDerOperatorService(@Qualifier("operator-uid") String operatorUid,
      @Qualifier("regform-list") List<Form> registrationForms) {
    super();
    if (operatorUid == null || operatorUid.isEmpty()) {
      throw new IllegalArgumentException("The operator UID must not be empty.");
    }
    this.operatorUid = operatorUid;
    if (registrationForms == null || registrationForms.isEmpty()) {
      throw new IllegalArgumentException("The registration forms list must not be empty.");
    }
    this.registrationForms = Collections.unmodifiableList(new ArrayList<>(registrationForms));
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
    // TODO Auto-generated method stub
    super.submitDerFacilityRegistrationForm(request, responseObserver);
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

}
