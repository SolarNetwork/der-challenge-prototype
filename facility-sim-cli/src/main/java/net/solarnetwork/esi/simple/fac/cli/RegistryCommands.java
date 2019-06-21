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

package net.solarnetwork.esi.simple.fac.cli;

import java.util.Iterator;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import io.grpc.ManagedChannel;
import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityExchangeRequest;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc.DerFacilityExchangeRegistryBlockingStub;

/**
 * Shell commands for the ESI Facility Registry client.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Registry")
public class RegistryCommands {

  @Resource(name = "facility-registration")
  private ObjectFactory<ManagedChannel> channelFactory;

  @Autowired
  private SshShellHelper shell;

  /**
   * List the available facilities in the registry.
   * 
   * @return
   */
  @ShellMethod("List available facilities.")
  public String list() {
    ManagedChannel channel = channelFactory.getObject();
    try {
      DerFacilityExchangeRegistryBlockingStub client = DerFacilityExchangeRegistryGrpc
          .newBlockingStub(channel);
      Iterator<DerFacilityExchangeInfo> itr = client
          .listDerFacilityExchanges(DerFacilityExchangeRequest.newBuilder().build());
      int i = 0;
      String fmt = "  %-10s %s";
      while (itr.hasNext()) {
        i += 1;
        DerFacilityExchangeInfo info = itr.next();
        shell.print("Result " + i, PromptColor.MAGENTA);
        shell.print(String.format(fmt, "Name", info.getName()));
        shell.print(String.format(fmt, "ID", info.getUid()));
        shell.print(String.format(fmt, "URI", info.getEndpointUri()));
      }
      shell.print("");
      return shell.getColored("OK", PromptColor.GREEN);
    } finally {
      channel.shutdown();
    }
  }

}
