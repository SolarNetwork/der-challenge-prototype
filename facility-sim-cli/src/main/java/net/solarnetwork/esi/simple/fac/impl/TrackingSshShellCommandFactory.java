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

package net.solarnetwork.esi.simple.fac.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.validation.ValidatorFactory;

import org.jline.reader.Parser;
import org.springframework.boot.Banner;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.ParameterResolver;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.JLineShellAutoConfiguration.CompleterAdapter;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import com.github.fonimus.ssh.shell.SshContext;
import com.github.fonimus.ssh.shell.SshShellCommandFactory;
import com.github.fonimus.ssh.shell.SshShellProperties;

/**
 * Extension of {@link SshShellCommandFactory} to provide tracking of active SSH sessions.
 * 
 * @author matt
 * @version 1.0
 */
@Primary
@Component("trackingSshShellCommandFactory")
public class TrackingSshShellCommandFactory extends SshShellCommandFactory {

  private static final Map<SshContext, Boolean> SSH_CONTEXTS = new WeakHashMap<>(4, 0.9f);

  /**
   * Constructor.
   * 
   * @param banner
   *        shell banner
   * @param promptProvider
   *        prompt provider
   * @param shell
   *        spring shell
   * @param completerAdapter
   *        completer adapter
   * @param parser
   *        jline parser
   * @param environment
   *        spring environment
   * @param historyFile
   *        history file location
   * @param properties
   *        shell properties
   */
  public TrackingSshShellCommandFactory(Banner banner, PromptProvider promptProvider, Shell shell,
      CompleterAdapter completerAdapter, Parser parser, Environment environment, File historyFile,
      SshShellProperties properties) {
    super(banner, promptProvider, new TrackingShell(shell), completerAdapter, parser, environment,
        historyFile, properties);
  }

  private static final class TrackingShell extends Shell {

    private final Shell delegate;

    private TrackingShell(Shell delegate) {
      super(null);
      this.delegate = delegate;
    }

    @Override
    public void setValidatorFactory(ValidatorFactory validatorFactory) {
      delegate.setValidatorFactory(validatorFactory);
    }

    @Override
    public Map<String, MethodTarget> listCommands() {
      return delegate.listCommands();
    }

    @Override
    public void gatherMethodTargets() throws Exception {
      delegate.gatherMethodTargets();
    }

    @Override
    public void setParameterResolvers(List<ParameterResolver> resolvers) {
      delegate.setParameterResolvers(resolvers);
    }

    @Override
    public boolean equals(Object obj) {
      return delegate.equals(obj);
    }

    @Override
    public void run(InputProvider inputProvider) throws IOException {
      // assumption that SSH_THREAD_CONTEXT has been set for the current thread at this point
      SshContext ctx = SSH_THREAD_CONTEXT.get();
      if (ctx != null) {
        synchronized (SSH_CONTEXTS) {
          SSH_CONTEXTS.put(ctx, Boolean.TRUE);
        }
      }
      delegate.run(inputProvider);
    }

    @Override
    public Object evaluate(Input input) {
      return delegate.evaluate(input);
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
      return delegate.complete(context);
    }

    @Override
    public String toString() {
      return delegate.toString();
    }

  }

  /**
   * Get all available SSH contexts.
   * 
   * @return all available contexts
   */
  public static Iterable<SshContext> sshContexts() {
    synchronized (SSH_CONTEXTS) {
      Set<SshContext> keys = SSH_CONTEXTS.keySet();
      List<SshContext> contexts = new ArrayList<>(keys.size());
      keys.forEach(c -> {
        if (c != null) {
          contexts.add(c);
        }
      });
      return contexts;
    }
  }

}
