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
import static net.solarnetwork.esi.simple.xchg.test.TestUtils.invocationArg;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;

import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.MessageSignature;
import net.solarnetwork.esi.simple.xchg.dao.FacilityRegistrationEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.impl.DaoFacilityRegistrationService;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoFacilityRegistrationService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityRegistrationServiceTests {

  private static final String TEST_FACILITY_ENDPOINT_URI = "dns:///localhost:9090";
  private static final String TEST_FORM_KEY = "simple-oper-reg-form";
  private static final byte[] TEST_NONCE = CryptoUtils.generateRandomBytes(8);
  private static final String TEST_CUST_ID = "ABC123456789";
  private static final String TEST_CUST_SURNAME = "Doe-Smith";
  private static final String TEST_UICI = "123-1234-1234";

  private List<Form> registrationForms;
  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private KeyPair facilityKeyPair;
  private DaoFacilityRegistrationService service;
  private FacilityRegistrationEntityDao facilityRegistrationDao;

  @Before
  public void setUp() throws Exception {
    registrationForms = new ArrayList<>();
    registrationForms.add(loadForm("registration-form-01.json"));
    registrationForms.add(loadForm("registration-form-02.json"));

    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = STANDARD_HELPER.generateKeyPair();
    service = new DaoFacilityRegistrationService(exchangeUid, exchangeKeyPair, registrationForms,
        STANDARD_HELPER);

    facilityKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityRegistrationDao = mock(FacilityRegistrationEntityDao.class);
    service.setFacilityRegistrationDao(facilityRegistrationDao);
  }

  private Form loadForm(String resource) throws IOException {
    try (Reader r = new InputStreamReader(getClass().getResourceAsStream(resource),
        Charset.forName("UTF-8"))) {
      Form.Builder builder = Form.newBuilder();
      JsonFormat.parser().merge(r, builder);
      return builder.build();
    }
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
    ArgumentCaptor<FacilityRegistrationEntity> facilityRegCaptor = ArgumentCaptor
        .forClass(FacilityRegistrationEntity.class);
    given(facilityRegistrationDao.save(facilityRegCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityRegistrationEntity.class));

    // when
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    FacilityRegistrationEntity reg = service.submitDerFacilityRegistrationForm(formData);

    // then
    assertThat("Registration saved", reg, notNullValue());
    assertThat("Registration customer ID", reg.getCustomerId(), equalTo(TEST_CUST_ID));
    assertThat("Registration facility URI", reg.getFacilityEndpointUri(),
        equalTo(TEST_FACILITY_ENDPOINT_URI));
    assertThat("Registration facility ID", reg.getFacilityUid(),
        equalTo(formData.getRoute().getFacilityUid()));
    assertThat("Registration facility nonce", ByteString.copyFrom(reg.getFacilityNonce()),
        equalTo(formData.getFacilityNonce()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationBadOperatorUid() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setRoute(formData.getRoute().toBuilder()
            .setExchangeUid("not.the.right.exchange.uid")
            .build())
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationMissingFacilityUid() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setRoute(formData.getRoute().toBuilder()
            .clearFacilityUid()
            .build())
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationMissingFacilityEndpointUri() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .clearFacilityEndpointUri()
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationMalformedFacilityEndpointUri() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .setFacilityEndpointUri("not a URI")
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationSmallFacilityNonce() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .setFacilityNonce(ByteString.copyFrom(CryptoUtils.generateRandomBytes(7)))
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationLargeFacilityNonce() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData().toBuilder()
        .setFacilityNonce(ByteString.copyFrom(CryptoUtils.generateRandomBytes(25)))
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationBadFormKey() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .setKey("not.the.form.key")
            .build())
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationMalformedUici() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .putData(FacilityRegistrationService.FORM_KEY_UICI, "not a uici")
            .build())
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationCustomerId() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .putData(FacilityRegistrationService.FORM_KEY_CUSTOMER_ID, "not a customer id")
            .build())
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

  @Test(expected = IllegalArgumentException.class)
  public void submitRegistrationMissingCustomerSurname() {
    // given

    // when
    // @formatter:off
    DerFacilityRegistrationFormData formData = defaultFacilityRegFormData();
    formData = formData.toBuilder()
        .setData(formData.getData().toBuilder()
            .removeData(FacilityRegistrationService.FORM_KEY_CUSTOMER_SURNAME)
            .build())
        .build();
    // @formatter:on

    service.submitDerFacilityRegistrationForm(formData);
  }

}
