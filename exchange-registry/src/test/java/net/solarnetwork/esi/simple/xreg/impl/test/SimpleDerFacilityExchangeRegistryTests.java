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

package net.solarnetwork.esi.simple.xreg.impl.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityExchangeRequest;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc.DerFacilityExchangeRegistryBlockingStub;
import net.solarnetwork.esi.simple.xreg.impl.SimpleDerFacilityExchangeRegistry;

/**
 * Test cases for the {@link SimpleDerFacilityExchangeRegistry} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDerFacilityExchangeRegistryTests {

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private List<DerFacilityExchangeInfo> infos;
  private SimpleDerFacilityExchangeRegistry service;
  private ManagedChannel channel;

  @Before
  public void setUp() throws Exception {
    infos = new ArrayList<>();
    infos.add(DerFacilityExchangeInfo.newBuilder().setName("Test Server").setUid("foo.bar")
        .setEndpointUri("dns:///example.com").build());

    service = new SimpleDerFacilityExchangeRegistry(infos);

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(service).build().start());

    channel = grpcCleanup
        .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @Test
  public void listExchanges() {
    // given
    DerFacilityExchangeRegistryBlockingStub client = DerFacilityExchangeRegistryGrpc
        .newBlockingStub(channel);

    //when
    Iterator<DerFacilityExchangeInfo> itr = client
        .listDerFacilityExchanges(DerFacilityExchangeRequest.newBuilder().build());

    //then
    assertThat("Result available", itr, notNullValue());

    List<DerFacilityExchangeInfo> list = new ArrayList<>();
    itr.forEachRemaining(list::add);
    assertThat("Result count", list, hasSize(1));

    DerFacilityExchangeInfo info = list.get(0);
    assertThat("Info", info, equalTo(infos.get(0)));
  }

}
