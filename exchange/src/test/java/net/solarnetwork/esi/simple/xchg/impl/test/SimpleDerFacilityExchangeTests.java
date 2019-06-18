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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeBlockingStub;
import net.solarnetwork.esi.simple.xchg.impl.SimpleDerFacilityExchange;

/**
 * Test cases for the {@link SimpleDerFacilityExchange} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDerFacilityExchangeTests {

  private static final String TEST_LANG = "en-NZ";
  private static final String TEST_LANG_ALT = "mi";

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private List<Form> registrationForms;
  private String operatorUid;
  private SimpleDerFacilityExchange service;
  private ManagedChannel channel;

  @Before
  public void setUp() throws Exception {
    registrationForms = new ArrayList<>();
    registrationForms.add(loadForm("registration-form-01.json"));
    registrationForms.add(loadForm("registration-form-02.json"));

    operatorUid = UUID.randomUUID().toString();
    service = new SimpleDerFacilityExchange(operatorUid, registrationForms);

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(service).build().start());

    channel = grpcCleanup
        .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
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
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc
        .newBlockingStub(channel);

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
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc
        .newBlockingStub(channel);

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
    DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc
        .newBlockingStub(channel);

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
}
