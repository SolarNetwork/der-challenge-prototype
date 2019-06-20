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

package net.solarnetwork.esi.simple.xchg.impl.test;

import static net.solarnetwork.esi.simple.xchg.test.TestUtils.invocationArg;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeBlockingStub;
import net.solarnetwork.esi.simple.xchg.dao.FacilityRegistrationEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.impl.SimpleDerFacilityExchange;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;

/**
 * Test cases for the {@link SimpleDerFacilityExchange} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDerFacilityExchangeTests {

  private static final String TEST_LANG = "en-NZ";
  private static final String TEST_LANG_ALT = "mi";
  private static final String TEST_FACILITY_ENDPOINT_URI = "dns:///localhost:9090";
  private static final String TEST_FORM_KEY = "simple-oper-reg-form";
  private static final byte[] TEST_NONCE = generateNonce(8);
  private static final String TEST_CUST_ID = "ABC123456789";
  private static final String TEST_CUST_SURNAME = "Doe-Smith";
  private static final String TEST_UICI = "123-1234-1234";

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private List<Form> registrationForms;
  private String operatorUid;
  private SimpleDerFacilityExchange service;
  private ManagedChannel channel;
  private FacilityRegistrationEntityDao facilityRegistrationDao;
  private FacilityRegistrationService facilityRegistrationService;

  @Before
  public void setUp() throws Exception {
    registrationForms = new ArrayList<>();
    registrationForms.add(loadForm("registration-form-01.json"));
    registrationForms.add(loadForm("registration-form-02.json"));

    operatorUid = UUID.randomUUID().toString();
    service = new SimpleDerFacilityExchange(operatorUid, registrationForms);

    facilityRegistrationDao = mock(FacilityRegistrationEntityDao.class);
    service.setFacilityRegistrationDao(facilityRegistrationDao);

    facilityRegistrationService = mock(FacilityRegistrationService.class);
    service.setFacilityRegistrationService(facilityRegistrationService);

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(service).build().start());

    channel = grpcCleanup
        .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  private static byte[] generateNonce(int size) {
    byte[] nonce = new byte[size];
    try {
      SecureRandom.getInstanceStrong().nextBytes(nonce);
    } catch (NoSuchAlgorithmException e) {
      Arrays.fill(nonce, (byte) 8);
    }
    return nonce;
  }

  private Form loadForm(String resource) throws IOException {
    try (Reader r = new InputStreamReader(getClass().getResourceAsStream(resource),
        Charset.forName("UTF-8"))) {
      Form.Builder builder = Form.newBuilder();
      JsonFormat.parser().merge(r, builder);
      return builder.build();
    }
  }

  @Test
  public void registrationFormForLang() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    DerFacilityRegistrationFormRequest req = DerFacilityRegistrationFormRequest.newBuilder()
        .setOperatorUid(operatorUid).setLanguageCode(TEST_LANG).build();
    DerFacilityRegistrationForm res = client.getDerFacilityRegistrationForm(req);

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Operator UID", res.getOperatorUid(), equalTo(operatorUid));
    Form form = res.getForm();
    assertThat("Form for matching language", form, equalTo(registrationForms.get(0)));
  }

  @Test
  public void registrationFormForLangAlt() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    DerFacilityRegistrationFormRequest req = DerFacilityRegistrationFormRequest.newBuilder()
        .setOperatorUid(operatorUid).setLanguageCode(TEST_LANG_ALT).build();
    DerFacilityRegistrationForm res = client.getDerFacilityRegistrationForm(req);

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Operator UID", res.getOperatorUid(), equalTo(operatorUid));
    Form form = res.getForm();
    assertThat("Form for matching alt language", form, equalTo(registrationForms.get(1)));
  }

  @Test
  public void registrationFormForLangUnsupported() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    DerFacilityRegistrationFormRequest req = DerFacilityRegistrationFormRequest.newBuilder()
        .setOperatorUid(operatorUid).setLanguageCode("es").build();
    DerFacilityRegistrationForm res = client.getDerFacilityRegistrationForm(req);

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Operator UID", res.getOperatorUid(), equalTo(operatorUid));
    Form form = res.getForm();
    assertThat("First form returned for unsupported language", form,
        equalTo(registrationForms.get(0)));
  }

  private DerFacilityRegistrationFormData defaultFacilityRegFormData() {
    // @formatter:off
    return DerFacilityRegistrationFormData.newBuilder()
        .setRoute(DerRoute.newBuilder()
          .setOperatorUid(operatorUid)
          .setFacilityUid(UUID.randomUUID().toString())
          .build())
        .setFacilityEndpointUri(TEST_FACILITY_ENDPOINT_URI)
        .setFacilityNonce(ByteString.copyFrom(TEST_NONCE))
        .setData(FormData.newBuilder()
            .setKey(TEST_FORM_KEY)
            .putData(SimpleDerFacilityExchange.FORM_KEY_CUSTOMER_ID, TEST_CUST_ID)
            .putData(SimpleDerFacilityExchange.FORM_KEY_CUSTOMER_SURNAME, TEST_CUST_SURNAME)
            .putData(SimpleDerFacilityExchange.FORM_KEY_UICI, TEST_UICI)
            .build())
        .build();
    // @formatter:on
  }

  @Test
  public void submitRegistrationOk() throws NoSuchAlgorithmException {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);
    ArgumentCaptor<FacilityRegistrationEntity> facilityRegCaptor = ArgumentCaptor
        .forClass(FacilityRegistrationEntity.class);
    given(facilityRegistrationDao.save(facilityRegCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityRegistrationEntity.class));

    ArgumentCaptor<FacilityRegistrationEntity> facilityRegServiceCaptor = ArgumentCaptor
        .forClass(FacilityRegistrationEntity.class);
    given(
        facilityRegistrationService.processFacilityRegistration(facilityRegServiceCaptor.capture()))
            .willReturn(new CompletableFuture<>());

    // when
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    DerFacilityRegistrationFormDataReceipt receipt = client
        .submitDerFacilityRegistrationForm(formData);

    // then
    assertThat("Receipt available", receipt, notNullValue());
    assertThat("Operator nonce", receipt.getOperatorNonce(), notNullValue());
    assertThat("Operator nonce size", receipt.getOperatorNonce().size(), equalTo(24));

    FacilityRegistrationEntity reg = facilityRegCaptor.getValue();
    assertThat("Registration saved", reg, notNullValue());
    assertThat("Registration customer ID", reg.getCustomerId(), equalTo(TEST_CUST_ID));
    assertThat("Registration facility URI", reg.getFacilityEndpointUri(),
        equalTo(TEST_FACILITY_ENDPOINT_URI));
    assertThat("Registration facility ID", reg.getFacilityUid(),
        equalTo(formData.getRoute().getFacilityUid()));
    assertThat("Registration facility nonce", ByteString.copyFrom(reg.getFacilityNonce()),
        equalTo(formData.getFacilityNonce()));

    FacilityRegistrationEntity regService = facilityRegServiceCaptor.getValue();
    assertThat("Service reg same instance", regService, sameInstance(reg));
  }

  @Test
  public void submitRegistrationBadOperatorUid() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setRoute(formData.getRoute().toBuilder()
            .setOperatorUid("not.the.right.operator.uid")
            .build())
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationMissingFacilityUid() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setRoute(formData.getRoute().toBuilder()
            .clearFacilityUid()
            .build())
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationMissingFacilityEndpointUri() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .clearFacilityEndpointUri()
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationMalformedFacilityEndpointUri() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .setFacilityEndpointUri("not a URI")
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationSmallFacilityNonce() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .setFacilityNonce(ByteString.copyFrom(generateNonce(7)))
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationLargeFacilityNonce() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .setFacilityNonce(ByteString.copyFrom(generateNonce(25)))
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationBadFormKey() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .setKey("not.the.form.key")
            .build())
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationMalformedUici() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .putData(SimpleDerFacilityExchange.FORM_KEY_UICI, "not a uici")
            .build())
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationCustomerId() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .putData(SimpleDerFacilityExchange.FORM_KEY_CUSTOMER_ID, "not a customer id")
            .build())
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

  @Test
  public void submitRegistrationMissingCustomerSurname() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .removeData(SimpleDerFacilityExchange.FORM_KEY_CUSTOMER_SURNAME)
            .build())
        .build();
    // @formatter:on

    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }
}
