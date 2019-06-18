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

package net.solarnetwork.esi.cli.xreg;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.util.StreamUtils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityExchangeRequest;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc.DerFacilityExchangeRegistryBlockingStub;

/**
 * Main entry point for ESI Simple Operator Registry App.
 * 
 * @author matt
 */
public class ExchangeRegistryTool implements ApplicationRunner {

  /** The --uri switch for the gRPC service to connect to. */
  public static final String ARG_URI = "uri";

  /** The --no-ssl switch to turn off SSL. */
  public static final String ARG_NO_SSL = "no-ssl";

  /** The --help switch to show help info. */
  public static final String ARG_HELP = "help";

  private static final Logger log = LoggerFactory.getLogger(ExchangeRegistryTool.class);

  /**
   * Command-line entry point.
   * 
   * @param args
   *        the command-line arguments
   */
  public static void main(String[] args) {
    new SpringApplicationBuilder().sources(ExchangeRegistryTool.class).web(WebApplicationType.NONE)
        .logStartupInfo(false).build().run(args);
  }

  /**
   * Print out command line help to {@code System.out}.
   */
  private void showHelp() {
    try (InputStream in = getClass().getResourceAsStream("/help.txt")) {
      StreamUtils.copy(in, System.out);
    } catch (IOException e) {
      log.error("Error printing help", e);
    }
  }

  private void showCmdHelp() {
    try (InputStream in = getClass().getResourceAsStream("/help-cmd.txt")) {
      StreamUtils.copy(in, System.out);
    } catch (IOException e) {
      log.error("Error printing cmd help", e);
    }
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    boolean wantHelp = args.getOptionNames().contains(ARG_HELP);
    if (args.getOptionNames().isEmpty() || wantHelp) {
      showHelp();
      return;
    }

    String uri = "dns:///localhost:9090";
    if (args.containsOption(ARG_URI)) {
      List<String> uris = args.getOptionValues(ARG_URI);
      if (!uris.isEmpty()) {
        // get last specified one
        uri = uris.get(uris.size() - 1);
      }
    }

    boolean usePlaintext = args.getOptionNames().contains(ARG_NO_SSL);

    ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(uri);
    if (usePlaintext) {
      channelBuilder.usePlaintext();
    }
    ManagedChannel channel = channelBuilder.build();
    DerFacilityExchangeRegistryBlockingStub client = DerFacilityExchangeRegistryGrpc
        .newBlockingStub(channel);
    AnsiConsole.systemInstall();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        System.out.print(ansi().fg(Color.BLUE).a("XReg").fg(Color.BLACK).a(">").reset().a(" "));
        String cmd = r.readLine();
        if ("list".equalsIgnoreCase(cmd)) {
          Iterator<DerFacilityExchangeInfo> itr = client
              .listDerFacilityExchanges(DerFacilityExchangeRequest.newBuilder().build());
          int i = 0;
          String fmt = "  %-10s %s";
          while (itr.hasNext()) {
            i += 1;
            DerFacilityExchangeInfo info = itr.next();
            System.out.println(ansi().bold().a("Result " + i).reset());
            System.out.println(String.format(fmt, "Name", info.getName()));
            System.out.println(String.format(fmt, "ID", info.getUid()));
            System.out.println(String.format(fmt, "URI", info.getEndpointUri()));
          }
          System.out.println("");
        } else if ("help".equalsIgnoreCase(cmd) || "h".equalsIgnoreCase(cmd) || "?".equals(cmd)) {
          showCmdHelp();
        } else if ("exit".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)
            || "q".equalsIgnoreCase(cmd)) {
          break;
        } else if (cmd != null && !cmd.isEmpty()) {
          System.err.println(ansi().fg(Color.RED).a("Unknown command.").reset());
        }
      }
    } finally {
      AnsiConsole.systemUninstall();
    }
  }
}
