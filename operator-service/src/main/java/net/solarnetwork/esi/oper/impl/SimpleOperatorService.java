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

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.solarnetwork.esi.domain.DatumRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
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
public class SimpleOperatorService extends DerOperatorServiceImplBase {

  /**
   * Constructor.
   */
  public SimpleOperatorService() {
    super();
  }

  @Override
  public void getDerFacilityRegistrationForm(DerFacilityRegistrationFormRequest request,
      StreamObserver<DerFacilityRegistrationForm> responseObserver) {
    // TODO Auto-generated method stub
    super.getDerFacilityRegistrationForm(request, responseObserver);
  }

  @Override
  public void submitDerFacilityRegistrationForm(DerFacilityRegistrationFormData request,
      StreamObserver<Empty> responseObserver) {
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
