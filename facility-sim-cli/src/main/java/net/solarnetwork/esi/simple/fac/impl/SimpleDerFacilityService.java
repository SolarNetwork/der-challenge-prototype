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

package net.solarnetwork.esi.simple.fac.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.solarnetwork.esi.domain.DatumRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistration;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PowerParameters;
import net.solarnetwork.esi.domain.PowerProfileDatum;
import net.solarnetwork.esi.domain.PriceDatum;
import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.domain.PriceMapOfferFeedback;
import net.solarnetwork.esi.domain.PriceMapOfferFeedbackResponse;
import net.solarnetwork.esi.domain.PriceMapOfferResponse;
import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.PriceMapOfferStatusRequest;
import net.solarnetwork.esi.domain.PriceParameters;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc.DerFacilityServiceImplBase;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.simple.fac.service.ExchangeRegistrationService;
import net.solarnetwork.esi.simple.fac.service.PriceMapService;

/**
 * Simple gRPC implementation of a DER facility service.
 * 
 * @author matt
 * @version 1.0
 */
@GrpcService
public class SimpleDerFacilityService extends DerFacilityServiceImplBase {

  @Autowired
  private ExchangeRegistrationService registrationService;

  @Autowired
  private PriceMapService priceMapService;

  private static final Logger log = LoggerFactory.getLogger(SimpleDerFacilityService.class);

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void completeDerFacilityRegistration(DerFacilityRegistration request,
      StreamObserver<Empty> responseObserver) {
    try {
      registrationService.completeExchangeRegistration(request);

      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();

    } catch (IllegalArgumentException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
    }
  }

  @Override
  public StreamObserver<PriceMapOffer> proposePriceMapOffer(
      StreamObserver<PriceMapOfferResponse> responseObserver) {
    return new StreamObserver<PriceMapOffer>() {

      private boolean error = false;
      private boolean completed = false;

      @Override
      public void onNext(PriceMapOffer offer) {
        log.info("Received price map offer: {}", offer);
        try {
          PriceMapOfferEventEntity event = priceMapService.proposePriceMapOffer(offer);
          PriceMapOfferResponse.Builder response = PriceMapOfferResponse.newBuilder()
              .setOfferId(offer.getOfferId());
          if (event.getCounterOffer() != null) {
            response.setCounterOffer(
                ProtobufUtils.priceMapForPriceMapEmbed(event.getCounterOffer().priceMap()));
          } else {
            response.setAccept(event.isAccepted());
          }
          responseObserver.onNext(response.build());
          if (!response.hasCounterOffer()) {
            completed = true;
            responseObserver.onCompleted();
          }
        } catch (IllegalArgumentException e) {
          error = true;
          responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asException());
        } catch (RuntimeException e) {
          error = true;
          log.error("Error processing price map offer: " + e.getMessage(), e);
          responseObserver.onError(
              Status.INTERNAL.withDescription("Internal error").withCause(e).asException());
        }

      }

      @Override
      public void onError(Throwable t) {
        log.error("Error receiving price map offer", t);
      }

      @Override
      public void onCompleted() {
        if (!(error || completed)) {
          responseObserver.onCompleted();
        }
      }
    };
  }

  @Override
  public void getPriceMapOfferStatus(PriceMapOfferStatusRequest request,
      StreamObserver<PriceMapOfferStatus> responseObserver) {
    // TODO Auto-generated method stub
    super.getPriceMapOfferStatus(request, responseObserver);
  }

  @Override
  public void providePriceMapOfferFeedback(PriceMapOfferFeedback request,
      StreamObserver<PriceMapOfferFeedbackResponse> responseObserver) {
    // TODO Auto-generated method stub
    super.providePriceMapOfferFeedback(request, responseObserver);
  }

  @Override
  public StreamObserver<PriceDatum> providePrices(StreamObserver<Empty> responseObserver) {
    // TODO Auto-generated method stub
    return super.providePrices(responseObserver);
  }

  @Override
  public void listPowerProfile(DatumRequest request,
      StreamObserver<PowerProfileDatum> responseObserver) {
    // TODO Auto-generated method stub
    super.listPowerProfile(request, responseObserver);
  }

  @Override
  public void getPowerParameters(DerRoute request,
      StreamObserver<PowerParameters> responseObserver) {
    // TODO Auto-generated method stub
    super.getPowerParameters(request, responseObserver);
  }

  @Override
  public void setPowerParameters(PowerParameters request,
      StreamObserver<PowerParameters> responseObserver) {
    // TODO Auto-generated method stub
    super.setPowerParameters(request, responseObserver);
  }

  @Override
  public void getPriceParameters(DerRoute request,
      StreamObserver<PriceParameters> responseObserver) {
    // TODO Auto-generated method stub
    super.getPriceParameters(request, responseObserver);
  }

  /**
   * Set the exchange registration service.
   * 
   * @param registrationService
   *        the registrationService to set
   */
  public void setRegistrationService(ExchangeRegistrationService registrationService) {
    this.registrationService = registrationService;
  }

}
