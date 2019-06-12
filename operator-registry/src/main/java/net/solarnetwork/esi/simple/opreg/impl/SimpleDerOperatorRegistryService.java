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

package net.solarnetwork.esi.simple.opreg.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.solarnetwork.esi.domain.DerOperatorInfo;
import net.solarnetwork.esi.domain.DerOperatorRequest;
import net.solarnetwork.esi.service.DerOperatorRegistryServiceGrpc.DerOperatorRegistryServiceImplBase;

/**
 * Really, really, really simple gRPC implementation of a DER operator registry service.
 * 
 * <p>
 * This service does not even use any information passed to
 * {@link #listDerOperators(DerOperatorRequest, StreamObserver)}. It merely returns
 * statically-configured data loaded from configurable properties.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@GrpcService
public class SimpleDerOperatorRegistryService extends DerOperatorRegistryServiceImplBase {

  private final List<DerOperatorInfo> infos;

  /**
   * Constructor.
   * 
   * @param infos
   *        the operator information to serve
   */
  @Autowired
  public SimpleDerOperatorRegistryService(@Qualifier("operator-list") List<DerOperatorInfo> infos) {
    super();
    this.infos = (infos != null && !infos.isEmpty()
        ? Collections.unmodifiableList(new ArrayList<>(infos))
        : Collections.emptyList());
  }

  @Override
  public void listDerOperators(DerOperatorRequest request,
      StreamObserver<DerOperatorInfo> responseObserver) {
    for (DerOperatorInfo info : infos) {
      responseObserver.onNext(info);
    }
    responseObserver.onCompleted();
  }

}
