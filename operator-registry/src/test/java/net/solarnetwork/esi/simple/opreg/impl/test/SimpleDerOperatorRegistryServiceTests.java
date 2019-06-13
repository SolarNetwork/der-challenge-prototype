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

package net.solarnetwork.esi.simple.opreg.impl.test;

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
import net.solarnetwork.esi.domain.DerOperatorInfo;
import net.solarnetwork.esi.domain.DerOperatorRequest;
import net.solarnetwork.esi.service.DerOperatorRegistryServiceGrpc;
import net.solarnetwork.esi.service.DerOperatorRegistryServiceGrpc.DerOperatorRegistryServiceBlockingStub;
import net.solarnetwork.esi.simple.opreg.impl.SimpleDerOperatorRegistryService;

/**
 * Test cases for the {@link SimpleDerOperatorRegistryService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDerOperatorRegistryServiceTests {

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private List<DerOperatorInfo> infos;
  private SimpleDerOperatorRegistryService service;
  private ManagedChannel channel;

  @Before
  public void setUp() throws Exception {
    infos = new ArrayList<>();
    infos.add(DerOperatorInfo.newBuilder().setName("Test Server").setUid("foo.bar")
        .setEndpointUri("dns:///example.com").build());

    service = new SimpleDerOperatorRegistryService(infos);

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(service).build().start());

    channel = grpcCleanup
        .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @Test
  public void foo() {
    // given
    DerOperatorRegistryServiceBlockingStub client = DerOperatorRegistryServiceGrpc
        .newBlockingStub(channel);

    //when
    Iterator<DerOperatorInfo> itr = client
        .listDerOperators(DerOperatorRequest.newBuilder().build());

    //then
    assertThat("Result available", itr, notNullValue());

    List<DerOperatorInfo> list = new ArrayList<>();
    itr.forEachRemaining(list::add);
    assertThat("Result count", list, hasSize(1));

    DerOperatorInfo info = list.get(0);
    assertThat("Info", info, equalTo(infos.get(0)));
  }

}
