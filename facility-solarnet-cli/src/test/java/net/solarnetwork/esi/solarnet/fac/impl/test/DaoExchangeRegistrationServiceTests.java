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

package net.solarnetwork.esi.solarnet.fac.impl.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.esi.solarnet.fac.test.TestUtils.invocationArg;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityExchangeRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistration;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.grpc.InProcessChannelProvider;
import net.solarnetwork.esi.grpc.StaticInProcessChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc.DerFacilityExchangeRegistryImplBase;
import net.solarnetwork.esi.solarnet.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.solarnet.fac.dao.ExchangeRegistrationEntityDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeRegistrationEntity;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeRegistrationNotification.ExchangeRegistrationCompleted;
import net.solarnetwork.esi.solarnet.fac.impl.DaoExchangeRegistrationService;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoExchangeRegistrationService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoExchangeRegistrationServiceTests {

  private static final String TEST_FORM_KEY = "simple-oper-reg-form";
  private static final String TEST_FORM_FIELD = "test-field";
  private static final String TEST_CUST_ID = "ABC123456789";

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private FacilityService facilityService;
  private String facilityUid;
  private URI facilityUri;
  private KeyPair facilityKeyPair;
  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private ExchangeEntityDao exchangeDao;
  private ExchangeRegistrationEntityDao exchangeRegistrationDao;
  private ApplicationEventPublisher eventPublisher;
  private DaoExchangeRegistrationService service;

  @Before
  public void setup() {
    facilityUid = UUID.randomUUID().toString();
    facilityUri = URI.create("//test-facility");
    facilityKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityService = mock(FacilityService.class);
    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = CryptoUtils.STANDARD_HELPER.generateKeyPair();
    exchangeDao = mock(ExchangeEntityDao.class);
    exchangeRegistrationDao = mock(ExchangeRegistrationEntityDao.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    service = new DaoExchangeRegistrationService(facilityService, exchangeDao,
        exchangeRegistrationDao);
    service.setExchangeChannelProvider(new InProcessChannelProvider(true));
    service.setEventPublisher(eventPublisher);
  }

  @Test
  public void listExchanges() throws IOException {
    // given
    String regServerName = InProcessServerBuilder.generateName();
    URI exchangeRegUri = URI.create("//" + regServerName);

    DerFacilityExchangeInfo exchInfo = DerFacilityExchangeInfo.newBuilder()
        .setUid(UUID.randomUUID().toString()).setEndpointUri(exchangeRegUri.toString()).build();

    DerFacilityExchangeRegistryImplBase regService = new DerFacilityExchangeRegistryImplBase() {

      @Override
      public void listDerFacilityExchanges(DerFacilityExchangeRequest request,
          StreamObserver<DerFacilityExchangeInfo> responseObserver) {
        responseObserver.onNext(exchInfo);
        responseObserver.onCompleted();
      }

    };

    grpcCleanup.register(InProcessServerBuilder.forName(regServerName).directExecutor()
        .addService(regService).build().start());

    service.setExchangeRegistryChannelProvider(
        new StaticInProcessChannelProvider(regServerName, true));

    // when
    Iterable<DerFacilityExchangeInfo> result = service
        .listExchanges(DerFacilityExchangeRequest.getDefaultInstance());
    assertThat("Result avaialble", result, notNullValue());

    List<DerFacilityExchangeInfo> list = new ArrayList<>();
    result.forEach(list::add);
    assertThat("Result count", list, hasSize(1));
    assertThat("Result instance", list.get(0), sameInstance(exchInfo));
  }

  @Test
  public void getExchangeRegistrationForm() throws IOException {
    // given
    DerFacilityRegistrationForm regForm = DerFacilityRegistrationForm.newBuilder()
        .setExchangeUid(UUID.randomUUID().toString()).build();
    DerFacilityExchangeImplBase exchangeService = new DerFacilityExchangeImplBase() {

      @Override
      public void getDerFacilityRegistrationForm(DerFacilityRegistrationFormRequest request,
          StreamObserver<DerFacilityRegistrationForm> responseObserver) {
        responseObserver.onNext(regForm);
        responseObserver.onCompleted();
      }

    };
    String serverName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + serverName);
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(exchangeService).build().start());

    // when
    DerFacilityExchangeInfo exchInfo = DerFacilityExchangeInfo.newBuilder()
        .setUid(regForm.getExchangeUid()).setEndpointUri(exchangeUri.toString()).build();
    DerFacilityRegistrationForm result = service.getExchangeRegistrationForm(exchInfo,
        Locale.getDefault());

    // then
    assertThat("Registration form returned", result, sameInstance(regForm));
  }

  private FormData defaultRegisterFormData() {
    // @formatter:off
    return FormData.newBuilder()
            .setKey(TEST_FORM_KEY)
            .putData(TEST_FORM_FIELD, TEST_CUST_ID)
            .build();
    // @formatter:on
  }

  private void givenDefaultFacilityService() {
    given(facilityService.getCryptoHelper()).willReturn(STANDARD_HELPER);
    given(facilityService.getKeyPair()).willReturn(facilityKeyPair);
    given(facilityService.getUid()).willReturn(facilityUid);
    given(facilityService.getUri()).willReturn(facilityUri);
  }

  @Test
  public void registerWithExchangeOk() throws IOException {
    // given
    givenDefaultFacilityService();

    // @formatter:off
    CryptoKey exchangePublicKey = CryptoKey.newBuilder()
        .setAlgorithm(exchangeKeyPair.getPublic().getAlgorithm())
        .setEncoding(exchangeKeyPair.getPublic().getFormat())
        .setKey(ByteString.copyFrom(exchangeKeyPair.getPublic().getEncoded()))
        .build();
    // @formatter:on

    FormData regFormData = defaultRegisterFormData();
    // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
    AtomicReference<DerFacilityRegistrationFormData> submittedFormData = new AtomicReference<DerFacilityRegistrationFormData>();

    ArgumentCaptor<ExchangeRegistrationEntity> exchangeRegCaptor = ArgumentCaptor
        .forClass(ExchangeRegistrationEntity.class);
    given(exchangeRegistrationDao.save(exchangeRegCaptor.capture()))
        .willAnswer(invocationArg(0, ExchangeRegistrationEntity.class));

    DerFacilityRegistrationFormDataReceipt receipt = DerFacilityRegistrationFormDataReceipt
        .newBuilder().setExchangeNonce(ByteString.copyFrom(CryptoUtils.generateRandomBytes(8)))
        .build();

    DerFacilityExchangeImplBase exchangeService = new DerFacilityExchangeImplBase() {

      @Override
      public void getPublicCryptoKey(Empty request, StreamObserver<CryptoKey> responseObserver) {
        responseObserver.onNext(exchangePublicKey);
        responseObserver.onCompleted();
      }

      @Override
      public void submitDerFacilityRegistrationForm(DerFacilityRegistrationFormData request,
          StreamObserver<DerFacilityRegistrationFormDataReceipt> responseObserver) {
        submittedFormData.set(request);

        FormData formData = request.getData();
        assertThat("Expected form data", formData, sameInstance(regFormData));

        DerRoute route = request.getRoute();
        assertThat("Route included", route, notNullValue());

        // @formatter:off
        validateMessageSignature(STANDARD_HELPER, route.getSignature(), 
            exchangeKeyPair, facilityKeyPair.getPublic(),
            asList(
                exchangeUid, 
                facilityUid,
                facilityUri,
                request.getFacilityNonce()));
        // @formatter:on

        responseObserver.onNext(receipt);
        responseObserver.onCompleted();
      }

    };
    String exchangeName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeName);
    grpcCleanup.register(InProcessServerBuilder.forName(exchangeName).directExecutor()
        .addService(exchangeService).build().start());

    // when
    DerFacilityExchangeInfo exchange = DerFacilityExchangeInfo.newBuilder().setUid(exchangeUid)
        .setEndpointUri(exchangeUri.toString()).build();
    ExchangeRegistrationEntity result = service.registerWithExchange(exchange, regFormData);

    // then
    assertThat("Registration entity returned", result, notNullValue());
    assertThat("Result ID", result.getId(), notNullValue());
    assertThat("Result creation date", result.getCreated(), notNullValue());
    assertThat("Result exchange URI", result.getExchangeEndpointUri(),
        equalTo(exchangeUri.toString()));
    assertThat("Result exchange public key", ByteString.copyFrom(result.getExchangePublicKey()),
        equalTo(exchangePublicKey.getKey()));
    assertThat("Result facility nonce", ByteString.copyFrom(result.getFacilityNonce()),
        equalTo(submittedFormData.get().getFacilityNonce()));
    assertThat("Result exchange nonce", ByteString.copyFrom(result.getExchangeNonce()),
        equalTo(receipt.getExchangeNonce()));
  }

  @Test
  public void completeRegistrationOk() throws IOException {
    // given
    givenDefaultFacilityService();

    URI exchangeUri = URI.create("//test-exchange");

    ExchangeRegistrationEntity exchangeRegistration = new ExchangeRegistrationEntity(Instant.now());
    exchangeRegistration.setExchangeEndpointUri(exchangeUri.toString());
    exchangeRegistration.setExchangePublicKey(exchangeKeyPair.getPublic().getEncoded());
    exchangeRegistration.setFacilityNonce(CryptoUtils.generateRandomBytes(8));
    exchangeRegistration.setId(exchangeUid);
    exchangeRegistration.setExchangeNonce(CryptoUtils.generateRandomBytes(8));

    // look up the registration to confirm
    given(exchangeRegistrationDao.findById(exchangeUid))
        .willReturn(Optional.of(exchangeRegistration));

    // save the new exchange
    given(exchangeDao.save(Mockito.any(ExchangeEntity.class)))
        .willAnswer(invocationArg(0, ExchangeEntity.class));

    // when
    // @formatter:off
    DerFacilityRegistration derReg = DerFacilityRegistration.newBuilder()
        .setSuccess(true)
        .setRegistrationToken(ByteString.copyFrom(CryptoUtils.sha256(Arrays.asList(
            exchangeRegistration.getExchangeNonce(),
            exchangeRegistration.getFacilityNonce(),
            exchangeUid,
            facilityService.getUid(),
            facilityService.getUri()))))
        .setRoute(DerRoute.newBuilder()
            .setFacilityUid(facilityUid)
            .setExchangeUid(exchangeUid)
            .setSignature(generateMessageSignature(
                STANDARD_HELPER, 
                exchangeKeyPair, 
                facilityKeyPair.getPublic(), 
                asList(
                    exchangeUid,
                    facilityService.getUid(),
                    facilityService.getUri(),
                    exchangeRegistration.getFacilityNonce())))
            .build())
        .build();
    ExchangeEntity exchange = service.completeExchangeRegistration(derReg);
    // @formatter:on

    // then
    assertThat("Exchange created", exchange, notNullValue());
    assertThat("Exchange UID", exchange.getId(), equalTo(exchangeUid));
    assertThat("Exchange URI", exchange.getExchangeEndpointUri(), equalTo(exchangeUri.toString()));
    assertThat("Exchange public key", ByteString.copyFrom(exchange.getExchangePublicKey()),
        equalTo(ByteString.copyFrom(exchangeKeyPair.getPublic().getEncoded())));

    verify(exchangeRegistrationDao, times(1)).deleteById(exchangeUid);

    ArgumentCaptor<ExchangeRegistrationCompleted> completedEventCaptor = ArgumentCaptor
        .forClass(ExchangeRegistrationCompleted.class);
    verify(eventPublisher, times(1)).publishEvent(completedEventCaptor.capture());

    ExchangeRegistrationCompleted evt = completedEventCaptor.getValue();
    assertThat("Event entity", evt.getSource(), equalTo(exchangeRegistration));
    assertThat("Event success", evt.isSuccess(), equalTo(true));
  }

}
