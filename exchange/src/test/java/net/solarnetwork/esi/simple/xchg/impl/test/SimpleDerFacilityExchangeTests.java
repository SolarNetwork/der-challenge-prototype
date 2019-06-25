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

import static java.util.Arrays.asList;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.MessageSignature;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeBlockingStub;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.impl.SimpleDerFacilityExchange;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;
import net.solarnetwork.esi.util.CryptoUtils;

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
  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private KeyPair facilityKeyPair;
  private SimpleDerFacilityExchange service;
  private ManagedChannel channel;
  private FacilityRegistrationService facilityRegistrationService;

  @Before
  public void setUp() throws Exception {
    registrationForms = new ArrayList<>();
    registrationForms.add(loadForm("registration-form-01.json"));
    registrationForms.add(loadForm("registration-form-02.json"));

    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = STANDARD_HELPER.generateKeyPair();
    service = new SimpleDerFacilityExchange(exchangeUid, exchangeKeyPair, registrationForms);

    facilityKeyPair = STANDARD_HELPER.generateKeyPair();

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
  public void getPublicKey() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    CryptoKey res = client.getPublicCryptoKey(Empty.getDefaultInstance());

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Key algorithm", res.getAlgorithm(), equalTo("EC"));
    assertThat("Key encoding", res.getEncoding(), equalTo("X.509"));
    assertThat("Key data", res.getKey(),
        equalTo(ByteString.copyFrom(exchangeKeyPair.getPublic().getEncoded())));
  }

  @Test
  public void registrationFormForLang() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    DerFacilityRegistrationFormRequest req = DerFacilityRegistrationFormRequest.newBuilder()
        .setExchangeUid(exchangeUid).setLanguageCode(TEST_LANG).build();
    DerFacilityRegistrationForm res = client.getDerFacilityRegistrationForm(req);

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Operator UID", res.getExchangeUid(), equalTo(exchangeUid));
    Form form = res.getForm();
    assertThat("Form for matching language", form, equalTo(registrationForms.get(0)));
  }

  @Test
  public void registrationFormForLangAlt() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    DerFacilityRegistrationFormRequest req = DerFacilityRegistrationFormRequest.newBuilder()
        .setExchangeUid(exchangeUid).setLanguageCode(TEST_LANG_ALT).build();
    DerFacilityRegistrationForm res = client.getDerFacilityRegistrationForm(req);

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Operator UID", res.getExchangeUid(), equalTo(exchangeUid));
    Form form = res.getForm();
    assertThat("Form for matching alt language", form, equalTo(registrationForms.get(1)));
  }

  @Test
  public void registrationFormForLangUnsupported() {
    // given
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    //when
    DerFacilityRegistrationFormRequest req = DerFacilityRegistrationFormRequest.newBuilder()
        .setExchangeUid(exchangeUid).setLanguageCode("es").build();
    DerFacilityRegistrationForm res = client.getDerFacilityRegistrationForm(req);

    //then
    assertThat("Result available", res, notNullValue());

    assertThat("Operator UID", res.getExchangeUid(), equalTo(exchangeUid));
    Form form = res.getForm();
    assertThat("First form returned for unsupported language", form,
        equalTo(registrationForms.get(0)));
  }

  private DerFacilityRegistrationFormData defaultFacilityRegFormData() {
    String facilityUid = UUID.randomUUID().toString();

    // @formatter:off
    MessageSignature msgSig = generateMessageSignature(STANDARD_HELPER, 
        facilityKeyPair, exchangeKeyPair.getPublic(), asList(exchangeUid, facilityUid));
    
    return DerFacilityRegistrationFormData.newBuilder()
        .setRoute(DerRoute.newBuilder()
          .setExchangeUid(exchangeUid)
          .setFacilityUid(facilityUid)
          .setSignature(msgSig)
          .build())
        .setFacilityEndpointUri(TEST_FACILITY_ENDPOINT_URI)
        .setFacilityPublicKey(CryptoKey.newBuilder()
            .setAlgorithm(facilityKeyPair.getPublic().getAlgorithm())
            .setEncoding(facilityKeyPair.getPublic().getFormat())
            .setKey(ByteString.copyFrom(facilityKeyPair.getPublic().getEncoded()))
            .build())
        .setFacilityNonce(ByteString.copyFrom(TEST_NONCE))
        .setData(FormData.newBuilder()
            .setKey(TEST_FORM_KEY)
            .putData(FacilityRegistrationService.FORM_KEY_CUSTOMER_ID, TEST_CUST_ID)
            .putData(FacilityRegistrationService.FORM_KEY_CUSTOMER_SURNAME, TEST_CUST_SURNAME)
            .putData(FacilityRegistrationService.FORM_KEY_UICI, TEST_UICI)
            .build())
        .build();
    // @formatter:on
  }

  @Test
  public void submitRegistrationOk() throws NoSuchAlgorithmException {
    // given
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    FacilityRegistrationEntity reg = new FacilityRegistrationEntity(Instant.now());
    reg.setExchangeNonce(CryptoUtils.generateRandomBytes(8));
    given(facilityRegistrationService.submitDerFacilityRegistrationForm(formData)).willReturn(reg);

    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    DerFacilityRegistrationFormDataReceipt receipt = client
        .submitDerFacilityRegistrationForm(formData);

    // then
    assertThat("Receipt available", receipt, notNullValue());
    assertThat("Operator nonce", receipt.getExchangeNonce(),
        equalTo(ByteString.copyFrom(reg.getExchangeNonce())));
  }

  @Test
  public void submitRegistrationgIllegalArgument() {
    // given
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    // @formatter:off
    formData = formData.toBuilder()
        .setRoute(formData.getRoute().toBuilder()
            .setExchangeUid("not.the.right.exchange.uid")
            .build())
        .build();
    // @formatter:on

    given(facilityRegistrationService.submitDerFacilityRegistrationForm(formData))
        .willThrow(new IllegalArgumentException("Test"));

    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

    // when
    try {
      client.submitDerFacilityRegistrationForm(formData);
      fail("Validation exception expected");
    } catch (StatusRuntimeException e) {
      assertThat("Invalid argument", e.getStatus().getCode(),
          equalTo(Status.INVALID_ARGUMENT.getCode()));
    }
  }

}
